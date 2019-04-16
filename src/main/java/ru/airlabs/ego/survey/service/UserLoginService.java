package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.user.UserPasswordChange;
import ru.airlabs.ego.survey.dto.user.UserRegistration;

/**
 * Интерфейс сервиса для регистрации и авторизации пользователей
 *
 * @author Aleksey Gorbachev
 */
public interface UserLoginService {

    /**
     * Регистрация пользователя в системе
     *
     * @param registration данные формы регистрации
     * @return созданный новый пользователь
     */
    User registerUser(UserRegistration registration);

    /**
     * Подтверждение регистрации пользователя
     *
     * @param email имейл пользователя
     * @param password закодированный пароль пользователя
     */
    void confirmUserRegistration(String email, String password);

    /**
     * Изменение пароля пользователя
     *
     * @param passwordChange данные из формы смены пароля
     */
    void changePassword(UserPasswordChange passwordChange);
}
