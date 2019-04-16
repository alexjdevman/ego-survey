package ru.airlabs.ego.survey.service.impl;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.QuestionAnswer;
import ru.airlabs.ego.core.entity.UserSurveyState;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.repository.QuestionAnswerRepository;
import ru.airlabs.ego.core.repository.UserSurveyStateRepository;
import ru.airlabs.ego.core.repository.VacancyRepository;
import ru.airlabs.ego.survey.config.FileUploadConfig;
import ru.airlabs.ego.survey.dto.FileContent;
import ru.airlabs.ego.survey.exception.ImageUploadException;
import ru.airlabs.ego.survey.service.ImageUploadService;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.survey.utils.ControllerUtils.GET_USER_IMAGE_REST_ADDRESS;
import static ru.airlabs.ego.survey.utils.ImageUtils.compressAndWriteImageInJPG;
import static ru.airlabs.ego.survey.utils.ImageUtils.scaleImage;


/**
 * Сервис для загрузки изображений в приложении
 *
 * @author Aleksey Gorbachev
 */
@Service("imageUploadService")
@Transactional(readOnly = true)
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUploadServiceImpl.class);

    /**
     * Максимальный размер фотографии пользователя по большей стороне (в пикселях)
     */
    private static final int MAX_IMAGE_DIMENSION = 1100;
    /**
     * Качество изображения
     */
    private static final float IMAGE_JPG_QUALITY = 0.9f;

    /**
     * название поддиректории для фотографий пользователей загружаемых из резюме
     */
    private static final String RESUME_IMAGE_SUBDIRECTORY = "resume";

    /**
     * Конфигурация параметров загрузки файлов
     */
    private FileUploadConfig fileUploadConfig;

    /**
     * Репозиторий ответов на вопросы
     */
    @Autowired
    private QuestionAnswerRepository questionAnswerRepository;

    /**
     * Репозиторий состояния прохождения опроса пользователем
     */
    @Autowired
    private UserSurveyStateRepository userSurveyStateRepository;

    /**
     * Репозиторий вакансий
     */
    @Autowired
    private VacancyRepository vacancyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Веб-адрес приложения
     */
    @Value("${application.url}")
    private String appUrl;


    /**
     * Сохранение фотографии пользователя
     *
     * @param userId     идентификатор пользователя
     * @param questionId идентификатор вопроса
     * @param imageFile  файл, содержащий фотографию пользователя
     */
    @Transactional
    @Override
    public void saveUserImage(Long userId, Long questionId, MultipartFile imageFile) {
        final long limit = 5 * 1024 * 1024;    // максимальный допустимый размер фото в байтах = 5 MB
        if (imageFile.getSize() > limit) {
            throw new ImageUploadException("Размер файла превышает допустимый");
        }
        final String imageRootLocation = fileUploadConfig.getUserImageRootLocation();
        checkArgument(isNotBlank(imageRootLocation), "Не указан путь к корневой директории для фотографий пользователей");
        final QuestionAnswer answer = questionAnswerRepository.findByUserIdAndQuestionId(userId, questionId);

        try (InputStream stream = new ByteArrayInputStream(imageFile.getBytes())) {
            File rootDirectory = new File(imageRootLocation);
            if (!rootDirectory.exists()) rootDirectory.mkdirs();
            final String fileExtension = getExtension(imageFile.getOriginalFilename());
            final File currentSubDirectory = getCurrentSubDirectory(rootDirectory, answer); // получаем текущую субдиректорию для изображения
            if (!currentSubDirectory.exists()) { // создаем новую, если не существует
                currentSubDirectory.mkdirs();
            }
            BufferedImage image = ImageIO.read(stream);
            checkNotNull(image, "Файл не является изображением. Проверьте формат файла");
            // масштабируем изображение
            BufferedImage scaledImage = scaleImage(image, MAX_IMAGE_DIMENSION);
            File destFile = new File(buildDestFileName(currentSubDirectory, userId, questionId, fileExtension));
            // оптимизируем изображение в JPG и записываем в файл
            compressAndWriteImageInJPG(scaledImage, destFile, IMAGE_JPG_QUALITY);
            // отмечаем признак загрузки фото для ответа на вопрос
            if (answer != null) {
                answer.setPhoto(Boolean.TRUE);
            }
        } catch (Exception err) {
            return;
        }
    }

    /**
     * Получение списка ссылок на все загруженные фотографии пользователя
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии
     * @return список ссылок на загруженные фотографии
     */
    @Override
    public List<String> getImagesForUserAndVacancy(Long userId, Long vacancyId) {
        List<String> imagePaths = new ArrayList<>();
        List<QuestionAnswer> answers = questionAnswerRepository.findAllByUserIdAndPhoto(userId, Boolean.TRUE);
        for (QuestionAnswer answer : answers) {
            final String imagePath = format(GET_USER_IMAGE_REST_ADDRESS, appUrl, userId, vacancyId, answer.getQuestionId());
            imagePaths.add(imagePath);
        }
        return imagePaths;
    }

    /**
     * Получение содержимого фото пользователя в рамках вакансии и ответа на вопрос
     *
     * @param userId     идентификатор пользователя
     * @param vacancyId  идентификатор вакансии
     * @param questionId идентификатор вопроса
     * @return содержимое файла с фото пользователя
     */
    @Override
    public FileContent getUserImageContent(Long userId, Long vacancyId, Long questionId) {
        FileContent fileContent = new FileContent();
        final String imageRootLocation = fileUploadConfig.getUserImageRootLocation();
        File rootDirectory = new File(imageRootLocation);
        if (!rootDirectory.exists()) rootDirectory.mkdirs();
        Vacancy vacancy = vacancyRepository.findById(vacancyId).get();
        final Long surveyId = vacancy.getSurveyId();    // проверка прохождения пользователем опроса по вакансии
        UserSurveyState surveyState = userSurveyStateRepository.findByUserIdAndSurveyId(userId, surveyId);
        checkNotNull(surveyState, "Пользователь с id %s не проходил опросы по вакансии с id %s", userId, vacancyId);
        QuestionAnswer answer = questionAnswerRepository.findByUserIdAndQuestionId(userId, questionId);
        checkNotNull(answer, "Не найден ответ на вопрос с id %s пользователя с id %s", questionId, userId);
        checkArgument(answer.getPhoto(), format("Для вопроса с id %s пользователь с id %s не загружал фото", questionId, userId));

        final String fileName = userId + "_" + questionId + ".jpg";
        final long folderNumber = answer.getId() / fileUploadConfig.getMaxDirectorySize() + 1;
        final String filePath = rootDirectory.getAbsolutePath()
                + File.separator + folderNumber
                + File.separator + fileName;
        File imageFile = new File(filePath);
        checkArgument(imageFile.exists(), "Не найден файл: %s", filePath);
        try (InputStream stream = new FileInputStream(imageFile)) {
            fileContent.fileName = fileName;
            fileContent.content = IOUtils.toByteArray(stream);
            return fileContent;
        } catch (Exception err) {
            LOGGER.error(err.getMessage(), err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Проверка, загружена ли фотография для ответа на вопрос
     *
     * @param questionAnswer ответ на вопрос
     * @return true - если фото загружено, иначе - false
     */
    @Override
    public boolean isImageUploadForQuestionAnswer(QuestionAnswer questionAnswer) {
        File rootDirectory = new File(fileUploadConfig.getUserImageRootLocation());
        if (!rootDirectory.exists()) rootDirectory.mkdirs();
        final String fileName = questionAnswer.getUserId() + "_" + questionAnswer.getQuestionId() + ".jpg";
        final long folderNumber = questionAnswer.getId() / fileUploadConfig.getMaxDirectorySize() + 1;
        final String filePath = rootDirectory.getAbsolutePath()
                + File.separator + folderNumber
                + File.separator + fileName;
        File imageFile = new File(filePath);

        return imageFile.exists();
    }

    /**
     * Сохранение фото пользователя из файла резюме
     *
     * @param userId        идентификатор пользователя
     * @param imageContent  содержимое фото из резюме
     */
    @Override
    public void saveUserImageFromResume(Long userId, byte[] imageContent) {
        final String imageRootLocation = fileUploadConfig.getUserImageRootLocation();
        try (InputStream stream = new ByteArrayInputStream(imageContent)) {
            File rootDirectory = new File(imageRootLocation);
            if (!rootDirectory.exists()) rootDirectory.mkdirs();
            final File currentSubDirectory = new File(rootDirectory, RESUME_IMAGE_SUBDIRECTORY);
            if (!currentSubDirectory.exists()) { // создаем новую, если не существует
                currentSubDirectory.mkdirs();
            }
            BufferedImage image = ImageIO.read(stream);
            checkNotNull(image, "Файл не является изображением. Проверьте формат файла");
            // масштабируем изображение
            BufferedImage scaledImage = scaleImage(image, MAX_IMAGE_DIMENSION);
            File destFile = new File(buildDestFileName(currentSubDirectory, userId, null, "jpg"));
            // оптимизируем изображение в JPG и записываем в файл
            compressAndWriteImageInJPG(scaledImage, destFile, IMAGE_JPG_QUALITY);
        } catch (Exception err) {
            return;
        }
    }

    private String buildDestFileName(File directory,
                                     Long userId,
                                     Long questionId,
                                     String fileExtension) {
        return directory.getAbsolutePath() +
                File.separator +
                userId + "_" + (questionId != null ? questionId : 0) +
                "." + fileExtension;
    }

    /**
     * Определение текущей субдиректории для размещения фотографии
     *
     * @param rootDirectory корневая директория
     * @param answer        ответ на вопрос
     * @return текущая субдиректории
     */
    private File getCurrentSubDirectory(File rootDirectory, QuestionAnswer answer) {
        long subDirectoryNumber;
        if (answer != null) {
            subDirectoryNumber = answer.getId() / fileUploadConfig.getMaxDirectorySize() + 1;
        } else {
            Long maxAnswerId = (Long) entityManager     // определяем макс. id вопроса
                    .createQuery("select max(a.id) from QuestionAnswer a")
                    .getSingleResult();
            subDirectoryNumber = (++maxAnswerId) / fileUploadConfig.getMaxDirectorySize() + 1;
        }
        return new File(rootDirectory, String.valueOf(subDirectoryNumber));
    }

    @Resource(name = "fileUploadConfig")
    public void setFileUploadConfig(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }
}
