package ru.airlabs.ego.survey.service.social;

import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.user.UserSocial;
import ru.airlabs.ego.survey.dto.user.UserRegistration;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса для работы с пользователями Telegram
 *
 * @author Aleksey Gorbachev
 */
public interface TelegramUserService {

    /**
     * Добавить нового владельца аккаунта в ЛК из Telegram
     *
     * @param registration данные регистрации пользователя
     * @return новый пользователь
     */
    User registerOwnerUser(UserRegistration registration);

    /**
     * Получить список пользователей, доступных текущему пользователю-владельцу аккаунта Telegram
     *
     * @param ownerId идентификатор владельца аккаунта Telegram
     * @param active  признак активности
     * @return список данных по пользователям
     */
    List<Map<String, Object>> getTelegramUsersForOwner(Long ownerId, Boolean active);

    /**
     * Добавление нового пользователя из Telegram
     *
     * @param ownerId  идентификатор владельца аккаунта в ЛК
     * @param name     имя пользователя в Telegram
     * @param password пароль пользователя для авторизации в Telegram
     * @return данные по новому пользователю
     */
    UserSocial addTelegramUser(Long ownerId, String name, String password);

    /**
     * Генерация пароля для пользователя Telegram
     *
     * @param length длинна пароля
     * @return пароль
     */
    String generateTelegramPassword(int length);
}
