package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Модель для смены пароля пользователя
 *
 * @author Aleksey Gorbachev
 */
public class UserPasswordChange implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * Код пользователя (md5 hash старого пароля пользователя)
     */
    private String key;

    /**
     * Новый пароль пользователя
     */
    private String password;

    /**
     * Подтверждение пароля пользователя
     */
    private String confirmPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void validatePasswordChange() {
        checkArgument(isNotBlank(email), "Email пользователя не указан");
        checkArgument(isNotBlank(key), "Ключ пользователя не указан");
        checkArgument(isNotBlank(password), "Пароль не указан");
        checkArgument(isNotBlank(confirmPassword), "Подтверждение пароля не указано");
        checkArgument(password.equals(confirmPassword), "Пароли не совпадают");
    }
}
