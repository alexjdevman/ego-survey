package ru.airlabs.ego.survey.service.resume;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.ResumeData;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.resume.ResumeEducation;
import ru.airlabs.ego.core.entity.resume.ResumeExperience;
import ru.airlabs.ego.core.repository.ResumeDataRepository;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.core.repository.resume.ResumeEducationRepository;
import ru.airlabs.ego.core.repository.resume.ResumeExperienceRepository;
import ru.airlabs.ego.model.AdditionalEducation;
import ru.airlabs.ego.model.Education;
import ru.airlabs.ego.model.Examination;
import ru.airlabs.ego.model.Experience;
import ru.airlabs.ego.survey.config.FileUploadConfig;
import ru.airlabs.ego.survey.dto.resume.ResumeInfo;
import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.service.UserService;
import ru.airlabs.ego.survey.service.mail.MailSenderService;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Types;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.*;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Type;
import static ru.airlabs.ego.survey.utils.DateUtils.asDate;
import static ru.airlabs.ego.survey.utils.NameUtils.trimStringToLength;

/**
 * Сервис для работы с резюме
 *
 * @author Roman Kochergin
 */
@Service
public class ResumeServiceImpl implements ResumeService {

    /**
     * Шаблоны для темы и текста письма для аудио презентации
     */
    private static final String RESUME_ERROR_MAIL_SUBJECT = "Ошибка при загрузке резюме через плагин";
    private static final String RESUME_ERROR_MAIL_TEXT = "При загрузке резюме %s у пользователя %s возникла ошибка: %s. Файл резюме сохранен с именем %s";

    /**
     * Почтовый адрес для отправки писем об ошибках
     */
    private static final String BLACK_BOX_EMAIL = "blackbox@pro.hr";

    /**
     * Логгер
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeServiceImpl.class);

    /**
     * Репозиторий для работы с резюме
     */
    @Autowired
    private ResumeDataRepository resumeDataRepository;

    /**
     * Репозиторий опыта для резюме
     */
    @Autowired
    private ResumeExperienceRepository resumeExperienceRepository;

    /**
     * Репозиторий образования для резюме
     */
    @Autowired
    private ResumeEducationRepository resumeEducationRepository;

    /**
     * Сервис для отправки писем
     */
    @Autowired
    private MailSenderService mailSenderService;

    /**
     * Сервис для работы с пользователями
     */
    @Autowired
    private UserService userService;

    /**
     * Репозитори для работы с пользователями
     */
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    /**
     * Конфигурация параметров загрузки файлов
     */
    private FileUploadConfig fileUploadConfig;

    /**
     * Jdbc template
     */
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Сохранить резюме
     *
     * @param vacancyId    идентификатор вакансии
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @param resume       данные резюме
     * @param file         файл
     * @param locale       локаль
     * @return резюме
     */
    @Override
    @Transactional
    public ResumeData saveResume(Long vacancyId, Long parentUserId, Resume resume, MultipartFile file, String locale) {
        Date birthDate = resume.getBirthDate() != null ?
                Date.from(resume.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Long userId = userService.getOrCreateUser(resume.getFirstName(), resume.getEmail(), resume.getPhone(),
                locale, resume.getLastName(), resume.getMiddleName(), resume.getSex(),
                birthDate, resume.getAddress(), resume.getPosition(), resume.getSalary(), parentUserId);
        if (userId != null) {
            List<ResumeData> resumeDataList = resumeDataRepository.findAllByUserIdAndVacancyIdAndDeleted(userId, vacancyId, false);
            if (resumeDataList.size() > 1) {
                throw new IllegalStateException(String.format("Кол-во резюме для вакансии %s и пользователя %s превышает 1", vacancyId, userId));
            } else if (resumeDataList.isEmpty()) {
                ResumeData data = new ResumeData();
                data.setUserId(userId);
                data.setVacancyId(vacancyId);
                data.setDateCreate(new Date());
                data.setSource(resume.getSource());
                data.setType(resume.getType());
                data.setFileName(file.getOriginalFilename());
                data.setFileSize(file.getSize());
                data.setDescription(resume.getPosition());
                resumeDataRepository.save(data);
                saveResumeFile(data.getId(), file, resume.getType());
                return data;
            } else {
                ResumeData data = resumeDataList.get(0);
                data.setSource(resume.getSource());
                data.setType(resume.getType());
                data.setFileName(file.getOriginalFilename());
                data.setFileSize(file.getSize());
                data.setDescription(resume.getPosition());
                resumeDataRepository.save(data);
                saveResumeFile(data.getId(), file, resume.getType());
                return data;
            }
        }
        return null;
    }

    /**
     * Сохранить файл резюме для существующего конкретного пользователя
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя, которому принадлежит файл резюме
     * @param file      файл
     * @param resume    резюме
     * @return резюме
     */
    @Override
    @Transactional
    public ResumeData saveResumeFile(Long vacancyId,
                                     Long userId,
                                     MultipartFile file,
                                     Resume resume,
                                     String url) {
        String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        Type type = getFileType(ext);
        if (type == null) {
            throw new IllegalArgumentException(String.format("Resource type '%s' is not supported", ext));
        }
        List<ResumeData> resumeDataList = resumeDataRepository.findAllByUserIdAndVacancyIdAndDeleted(userId, vacancyId, false);
        if (resumeDataList.size() > 1) {
            throw new IllegalStateException(String.format("Кол-во резюме для вакансии %s и пользователя %s превышает 1", vacancyId, userId));
        } else if (resumeDataList.isEmpty()) {
            ResumeData data = new ResumeData();
            data.setUserId(userId);
            data.setVacancyId(vacancyId);
            data.setDateCreate(new Date());

            fillResumeData(data, file, resume, url, type);
            resumeDataRepository.save(data);
            saveResumeFile(data.getId(), file, type);
            return data;
        } else {
            ResumeData data = resumeDataList.get(0);
            fillResumeData(data, file, resume, url, type);
            resumeDataRepository.save(data);
            saveResumeFile(data.getId(), file, type);
            return data;
        }
    }

    /**
     * Сохранить HTML-файл резюме для существующего конкретного пользователя
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя, которому принадлежит файл резюме
     * @param file      файл
     * @param resume    данные резюме
     * @param url       откуда загружено резюме
     * @return резюме
     */
    @Transactional
    @Override
    public ResumeData saveResumeFileFromHtml(Long vacancyId,
                                             Long userId,
                                             MultipartFile file,
                                             Resume resume,
                                             String url) {
        List<ResumeData> resumeDataList = resumeDataRepository.findAllByUserIdAndVacancyIdAndDeleted(userId, vacancyId, false);
        ResumeData data;
        try {
            if (resumeDataList.size() > 1) {
                throw new IllegalStateException(String.format("Кол-во резюме для вакансии %s и пользователя %s превышает 1", vacancyId, userId));
            } else {
                if (resumeDataList.isEmpty()) {
                    data = new ResumeData();
                    data.setUserId(userId);
                    data.setVacancyId(vacancyId);
                    data.setDateCreate(new Date());
                } else {
                    data = resumeDataList.get(0);
                }
                fillResumeDataFromHtml(data, file, resume, url);
                resumeDataRepository.save(data);

                saveResumeExperienceAndEducationFromHtml(data.getId(), resume);
                saveResumeFile(data.getId(), file, data.getType());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return data;
    }

    /**
     * Получить файл с резюме
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @return файл
     */
    @Override
    public Optional<File> getResumeFile(Long vacancyId, Long userId) {
        List<ResumeData> resumeDataList = resumeDataRepository.findAllByUserIdAndVacancyIdAndDeleted(userId, vacancyId, false);
        if (resumeDataList.size() > 1) {
            throw new IllegalStateException(String.format("Кол-во резюме для вакансии %s и пользователя %s превышает 1", vacancyId, userId));
        }
        if (resumeDataList.isEmpty()) {
            return Optional.empty();
        }
        ResumeData data = resumeDataList.get(0);
        final String root = fileUploadConfig.getUserResumeRootLocation();
        final long folderNumber = data.getId() / fileUploadConfig.getMaxDirectorySize() + 1;
        String ext = getFileExtension(data.getType());
        if (ext == null) {
            throw new IllegalArgumentException(String.format("Resource type '%s' is not supported", data.getType().name()));
        }
        final String fileName = data.getId() + "." + ext;
        File file = new File(root + File.separator + folderNumber + File.separator + fileName);
        if (!file.exists()) {
            throw new IllegalStateException(String.format("Файл резюме для вакансии %s и пользователя %s не найден", vacancyId, userId));
        }
        return Optional.of(file);
    }

    /**
     * Найти резюме
     *
     * @param userId       идентификатор пользователя
     * @param parentUserId идентификатор менеджера (HR)
     * @return резюме
     */
    @Override
    public Optional<ResumeData> findResume(Long userId, Long parentUserId) {
        List<ResumeData> result =
                jdbcTemplate.query("" +
                                "SELECT r.* " +
                                "FROM R_DATA r, " +
                                "  V_DATA v " +
                                "WHERE r.id_user  = :userId " +
                                "AND r.is_deleted = 0 " +
                                "AND r.id_v_data  = v.id " +
                                "AND v.id_user    = :parentUserId " +
                                "AND rownum       = 1",
                        new MapSqlParameterSource()
                                .addValue("userId", userId)
                                .addValue("parentUserId", parentUserId),
                        (rs, i) -> {
                            ResumeData data = new ResumeData();
                            data.setId(rs.getLong("ID"));
                            data.setUserId(rs.getLong("ID_USER"));
                            data.setSource(ResumeData.Source.valueOf(rs.getString("SOURCE")));
                            data.setType(Type.valueOf(rs.getString("TYPE")));
                            data.setDescription(rs.getString("DESCR"));
                            data.setFileName(rs.getString("FILE_NAME"));
                            data.setFileSize(rs.getLong("FILE_SIZE"));
                            data.setDateCreate(rs.getTimestamp("DT_CREATE"));
                            data.setVacancyId(rs.getLong("ID_V_DATA"));
                            data.setDeleted(rs.getInt("IS_DELETED") > 0);
                            return data;
                        });
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Проверка списка ссылок на резюме
     *
     * @param vacancyId  идентификатор вакансии
     * @param resumeUrls список ссылок на резюме
     * @return результат проверки
     */
    @Transactional
    @Override
    public List<ResumeInfo> checkResumeLoaded(Long vacancyId, List<String> resumeUrls) {
        List<ResumeInfo> resumeInfoList = new ArrayList<>();
        for (String url : resumeUrls) {
            Map<String, Object> result = new SimpleJdbcCall(dataSource)
                    .withCatalogName("Q_INVITE_PKG")
                    .withProcedureName("IS_RESUME_LOADED")
                    .declareParameters(
                            new SqlParameter("PID_V_DATA", Types.BIGINT),
                            new SqlParameter("PURL", Types.VARCHAR),

                            new SqlOutParameter("PRESULT", Types.BIGINT),
                            new SqlOutParameter("PID_USER", Types.BIGINT))
                    .execute(new MapSqlParameterSource()
                            .addValue("PID_V_DATA", vacancyId)
                            .addValue("PURL", url));
            Long userId = (Long) result.get("PID_USER");
            Integer status = ((Long) result.get("PRESULT")).intValue();

            writeCheckResumeInLog(vacancyId, userId, url, status);
            resumeInfoList.add(new ResumeInfo(userId, status, url));
        }
        return resumeInfoList;
    }

    /**
     * Сохранение файла резюме, который не удалось обработать
     *
     * @param userId    идентификатор пользователя
     * @param file      файл с резюме
     * @param resumeUrl урл, откуда загружено резюме
     * @param errorCode код ошибки
     */
    @Transactional(readOnly = true)
    @Override
    public void saveResumeErrorFile(Long userId,
                                    MultipartFile file,
                                    String resumeUrl,
                                    HttpStatus errorCode) {
        try {
            File errorDirectory = new File(fileUploadConfig.getUserResumeErrorLocation());
            if (!errorDirectory.exists()) errorDirectory.mkdirs();
            String fileExtension = getExtension(file.getOriginalFilename());
            final String fileName = getResumeErrorFileName(userId, fileExtension);
            final String filePath = errorDirectory.getAbsolutePath() + File.separator + fileName;
            Optional<User> user = userRepository.findById(userId);
            final String userEmail = user.isPresent() ? user.get().getEmail() : null;
            final String text = String.format(RESUME_ERROR_MAIL_TEXT, resumeUrl, userEmail, errorCode.value(), fileName);

            file.transferTo(new File(filePath));
            mailSenderService.sendMail(BLACK_BOX_EMAIL, null, RESUME_ERROR_MAIL_SUBJECT, text, fileName, filePath, null);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Логгирование проверки ссылки на резюме
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @param url       ссылка
     * @param status    статус
     */
    private void writeCheckResumeInLog(Long vacancyId, Long userId, String url, Integer status) {
        String info = new StringBuilder("PLUGIN CHECK RESUME URL: ")
                .append("userId=").append(userId).append(", ")
                .append("vacancyId=").append(vacancyId).append(", ")
                .append("url=").append(url).append(", ")
                .append("result=").append(status).append(", ")
                .toString();
        LOGGER.info(info);
    }

    /**
     * Заполнение сущности по резюме из HTML-файла
     *
     * @param data   сущность резюме
     * @param file   HTML-файл
     * @param resume данные резюме
     * @param url    ссылка на резюме
     */
    private void fillResumeDataFromHtml(ResumeData data,
                                        MultipartFile file,
                                        Resume resume,
                                        String url) throws ParseException {
        data.setSource(resume.getSource());
        data.setType(resume.getType());
        data.setFileName(file.getOriginalFilename());
        data.setFileSize(file.getSize());
        data.setDescription(trimStringToLength(resume.getPosition(), 2000));
        data.setDateBirth(resume.getBirthDate() != null ? asDate(resume.getBirthDate()) : null);
        data.setUrl(url);
        if (resume.getHtmlResume() != null) {
            ResumeHtmlModel htmlModel = resume.getHtmlResume();
            data.setVacancy(htmlModel.getVacancy());
            data.setSalary(htmlModel.getSalary());
            data.setName(htmlModel.getName());
            data.setGender(htmlModel.getGender());
            data.setAge(htmlModel.getAge());
            data.setAddressLocality(htmlModel.getAddressLocality());
            data.setMetro(htmlModel.getMetro());
            data.setEmail(htmlModel.getEmail());
            data.setTelephone(htmlModel.getTelephone());
            data.setContactPreffered(htmlModel.getContactPreferred());
            data.setSpecialization(htmlModel.getSpecializationCategory());
            data.setAboutMe(trimStringToLength(htmlModel.getAboutMe(), 2000));
            data.setEducation(htmlModel.getEducation());
            data.setCitizenship(htmlModel.getCitizenship());
            data.setSkills(htmlModel.getSkills() != null ? trimStringToLength(String.join(", ", htmlModel.getSkills()), 1000) : null);
            data.setLanguages(htmlModel.getLanguages() != null ? trimStringToLength(String.join(", ", htmlModel.getLanguages()), 1000) : null);
            data.setPositionSpecialization(htmlModel.getPositionSpecializations() != null ? trimStringToLength(String.join(", ", htmlModel.getPositionSpecializations()), 1000) : null);
        }
    }

    /**
     * Сохранение опыта и образования соискателя для резюме
     *
     * @param resumeId идентификатор резюме
     * @param resume   данные по резюме
     */
    private void saveResumeExperienceAndEducationFromHtml(Long resumeId, Resume resume) {
        if (resume.getHtmlResume() != null) {
            fillResumeExperience(resumeId, resume.getHtmlResume().getExperiences());
            fillResumeEducation(resumeId, resume.getHtmlResume().getEducationsInfo());
            fillResumeAdditionalEducation(resumeId, resume.getHtmlResume().getAdditionalEducations());
            fillResumeExamination(resumeId, resume.getHtmlResume().getExaminations());
        }
    }

    /**
     * Заполняем опыт для резюме
     *
     * @param resumeId    идентификатор резюме
     * @param experiences опыт
     */
    private void fillResumeExperience(Long resumeId, List<Experience> experiences) {
        if (experiences != null) {
            for (Experience experience : experiences) {
                ResumeExperience resumeExperience = new ResumeExperience();
                resumeExperience.setResumeId(resumeId);
                resumeExperience.setExperiencePosition(experience.getExperiencePosition());
                resumeExperience.setExperienceDescription(trimStringToLength(experience.getExperienceDescription(), 2000));
                resumeExperience.setExperienceInterval(experience.getExperienceTimeinterval());
                resumeExperience.setCompanyName(experience.getCompanyName());
                resumeExperience.setAddressLocality(experience.getAddressLocality());
                resumeExperience.setDates(experience.getDates());
                resumeExperienceRepository.save(resumeExperience);
            }
        }
    }

    /**
     * Заполнение данных об образовании в резюме
     *
     * @param resumeId   идентификатор резюме
     * @param educations данные об образовании
     */
    private void fillResumeEducation(Long resumeId, List<Education> educations) {
        if (educations != null) {
            for (Education education : educations) {
                ResumeEducation resumeEducation = new ResumeEducation();
                resumeEducation.setResumeId(resumeId);
                resumeEducation.setName(education.getEducationName());
                resumeEducation.setOrganization(education.getEducationOrganization());
                resumeEducation.setDates(education.getDate());
                resumeEducation.setType("B");   // [B]ase
                resumeEducationRepository.save(resumeEducation);
            }
        }
    }

    /**
     * Заполнение данных об дополнительном образовании в резюме
     *
     * @param resumeId   идентификатор резюме
     * @param educations данные об образовании
     */
    private void fillResumeAdditionalEducation(Long resumeId, List<AdditionalEducation> educations) {
        if (educations != null) {
            for (AdditionalEducation education : educations) {
                ResumeEducation resumeEducation = new ResumeEducation();
                resumeEducation.setResumeId(resumeId);
                resumeEducation.setName(education.getEducationName());
                resumeEducation.setOrganization(education.getEducationOrganization());
                resumeEducation.setDates(education.getDate());
                resumeEducation.setType("A");   // [A]dditional
                resumeEducationRepository.save(resumeEducation);
            }
        }
    }

    /**
     * Заполнение данных об доп. курсах в резюме
     *
     * @param resumeId   идентификатор резюме
     * @param educations данные об образовании
     */
    private void fillResumeExamination(Long resumeId, List<Examination> educations) {
        if (educations != null) {
            for (Examination education : educations) {
                ResumeEducation resumeEducation = new ResumeEducation();
                resumeEducation.setResumeId(resumeId);
                resumeEducation.setName(education.getEducationName());
                resumeEducation.setOrganization(education.getEducationOrganization());
                resumeEducation.setDates(education.getDate());
                resumeEducation.setType("E"); // [E]xamination
                resumeEducationRepository.save(resumeEducation);
            }
        }
    }

    /**
     * Сохранить резюме в файл
     *
     * @param resumeDataId идентификатор резюме
     * @param sourceFile   исходный файл
     * @param type         тип данных
     */
    private void saveResumeFile(Long resumeDataId, MultipartFile sourceFile, Type type) {
        File rootDirectory = new File(fileUploadConfig.getUserResumeRootLocation());
        if (!rootDirectory.exists()) rootDirectory.mkdirs();
        String ext = getFileExtension(type);
        if (ext == null) {
            throw new IllegalArgumentException(String.format("Resource type '%s' is not supported", type.name()));
        }
        final long folderNumber = resumeDataId / fileUploadConfig.getMaxDirectorySize() + 1;
        File folderDirectory = new File(rootDirectory.getAbsolutePath() + File.separator + folderNumber);
        if (!folderDirectory.exists()) folderDirectory.mkdirs();
        final String fileName = resumeDataId + "." + ext;
        final String filePath = folderDirectory.getAbsolutePath() + File.separator + fileName;
        try {
            sourceFile.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Заполнение данных в сущность по резюме
     *
     * @param data   сущность резюме
     * @param file   файл
     * @param resume данные резюме
     * @param url    источник резюме
     * @param type   тип
     */
    private void fillResumeData(ResumeData data,
                                MultipartFile file,
                                Resume resume,
                                String url,
                                Type type) {
        data.setSource(resume.getSource());
        data.setType(type);
        data.setFileName(file.getOriginalFilename());
        data.setFileSize(file.getSize());
        data.setDescription(resume.getPosition());
        data.setUrl(url);
        data.setVacancy(resume.getPosition());
        data.setSalary(resume.getSalary() != null ? String.valueOf(resume.getSalary()) : null);
        data.setAddressLocality(resume.getAddress());
        data.setEmail(resume.getEmail());
        data.setTelephone(resume.getPhone());
        data.setGender(resume.getSex() != null ? (resume.getSex() == Sex.M ? "Мужчина" : "Женщина") : null);
        Date birthDate = resume.getBirthDate() != null ?
                Date.from(resume.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        data.setDateBirth(birthDate);
        String fullName = (isNotBlank(resume.getLastName()) ? resume.getLastName() + " " : "") +
                (isNotBlank(resume.getFirstName()) ? resume.getFirstName() + " " : "") +
                (isNotBlank(resume.getMiddleName()) ? resume.getMiddleName() : "");
        if (isNotBlank(fullName)) {
            data.setName(fullName.trim());
        }
    }

    /**
     * Получить расширение файла
     *
     * @param type тип данных
     * @return расширение
     */
    private String getFileExtension(Type type) {
        switch (type) {
            case E:
                return "xls";
            case R:
                return "rtf";
            case D:
                return "doc";
            case X:
                return "docx";
            case L:
                return "xlsx";
            case T:
                return "txt";
            case H:
                return "html";
            case C:
                return "rar";
            case Z:
                return "zip";
            default:
                return null;
        }
    }

    /**
     * Получить тип данных файла
     *
     * @param extension расширение
     * @return тип данных
     */
    private Type getFileType(String extension) {
        switch (extension) {
            case "xls":
                return Type.E;
            case "rtf":
                return Type.R;
            case "doc":
                return Type.D;
            case "docx":
                return Type.X;
            case "xlsx":
                return Type.L;
            case "txt":
                return Type.T;
            case "html":
                return Type.H;
            case "rar":
                return Type.C;
            case "zip":
                return Type.Z;
            default:
                return null;
        }
    }

    /**
     * Получить имя файла для резюме с ошибкой
     *
     * @param userId идентификатор пользователя
     * @return имя файла для резюме с ошибкой
     */
    private String getResumeErrorFileName(Long userId, String extension) {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DATE);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int sec = now.get(Calendar.SECOND);
        int milliSecond = now.get(Calendar.MILLISECOND);
        Random rand = new Random();
        int randToken = rand.nextInt(100);

        return userId + "_"
                + day + "_"
                + month + "_"
                + year + "_"
                + hour + "_"
                + minute
                + "_"
                + sec
                + "_"
                + milliSecond
                + "_"
                + randToken + "." + extension;
    }

    @Resource(name = "fileUploadConfig")
    public void setFileUploadConfig(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }
}
