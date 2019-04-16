package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.core.entity.User;

/**
 * Интерфейс сервиса для уведомления пользователей
 *
 * @author Aleksey Gorbachev
 */
public interface UserNotificationService {

    /**
     * Отправка новому пользователю письма для подтверждения регистрации
     *
     * @param user       новый зарегистрированный пользователь
     * @param sourceSite источник регистрации пользователя
     */
    void notifyUserForRegistration(User user, String sourceSite);

    /**
     * Отправка пользователю письма для восстановления пароля
     *
     * @param email имейл, на который высылаем письмо
     */
    void notifyUserForPasswordRecovery(String email);
}
