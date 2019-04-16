package ru.airlabs.ego.survey.service;

import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.FileContent;
import ru.airlabs.ego.survey.dto.settings.AccountSettings;

/**
 * Интерфейс сервиса для работы с настройками ЛК HR
 *
 * @author Aleksey Gorbachev
 */
public interface AccountSettingsService {

    /**
     * Получение настроек для ЛК пользователя
     *
     * @param currentUser пользователь HR
     * @return настройки ЛК пользователя
     */
    AccountSettings getAccountSettings(User currentUser);

    /**
     * Сохранение настроек для ЛК пользователя
     *
     * @param settings настройки ЛК
     * @param currentUser пользователь HR
     */
    void saveAccountSettings(AccountSettings settings, User currentUser);

    /**
     * Сохранение фото HR компании
     *
     * @param companyId идентификатор компании
     * @param file файл с фото HR
     */
    void saveCompanyImage(Long companyId, MultipartFile file);

    /**
     * Получение фото HR компании
     *
     * @param companyId идентификатор компании
     * @param preview флаг превью (уменьшенное изображение)
     * @return фото HR компании с именем файла
     */
    FileContent getCompanyImage(Long companyId, boolean preview);

    /**
     * Проверка, загружено ли фото HR для компании
     *
     * @param companyId идентификатор компании
     * @return true - если загружен, false - в противном случае
     */
    boolean isCompanyImageExists(Long companyId);
}
