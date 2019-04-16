package ru.airlabs.ego.survey.service.resume;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.ResumeData;
import ru.airlabs.ego.survey.dto.resume.ResumeInfo;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с резюме
 *
 * @author Roman Kochergin
 */
public interface ResumeService {

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
    ResumeData saveResume(Long vacancyId, Long parentUserId, Resume resume, MultipartFile file, String locale);

    /**
     * Сохранить файл резюме для существующего конкретного пользователя
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя, которому принадлежит файл резюме
     * @param file      файл
     * @param resume    резюме
     * @param url       откуда загружено резюме
     * @return резюме
     */
    ResumeData saveResumeFile(Long vacancyId,
                              Long userId,
                              MultipartFile file,
                              Resume resume,
                              String url);

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
    ResumeData saveResumeFileFromHtml(Long vacancyId,
                                      Long userId,
                                      MultipartFile file,
                                      Resume resume,
                                      String url);

    /**
     * Получить файл с резюме
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @return файл
     */
    Optional<File> getResumeFile(Long vacancyId, Long userId);

    /**
     * Найти резюме
     *
     * @param userId       идентификатор пользователя
     * @param parentUserId идентификатор менеджера (HR)
     * @return резюме
     */
    Optional<ResumeData> findResume(Long userId, Long parentUserId);

    /**
     * Проверка списка ссылок на резюме
     *
     * @param vacancyId  идентификатор вакансии
     * @param resumeUrls список ссылок на резюме
     * @return результат проверки
     */
    List<ResumeInfo> checkResumeLoaded(Long vacancyId, List<String> resumeUrls);

    /**
     * Сохранение файла резюме, который не удалось обработать
     *
     * @param userId    идентификатор пользователя
     * @param file      файл с резюме
     * @param resumeUrl урл, откуда загружено резюме
     * @param errorCode код ошибки
     */
    void saveResumeErrorFile(Long userId,
                             MultipartFile file,
                             String resumeUrl,
                             HttpStatus errorCode);
}
