package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Модель для передачи данных регистрации пользователя из UI
 *
 * @author Aleksey Gorbachev
 */
public class UserRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ФИО пользователя
     */
    private String name;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * Внешний идентификатор пользователя
     */
    private String socialUserId;

    /**
     * Пароль пользователя
     */
    private String password;

    /**
     * Повторение пароля пользователя
     */
    private String confirmPassword;

    /**
     * CAPTCHA код
     */
    private String captcha;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public void validateRegistration() {
        checkArgument(isNotBlank(name), "ФИО пользователя не указано");
        checkArgument(isNotBlank(email), "Email не указан");
        checkArgument(isNotBlank(password), "Пароль не указан");
        checkArgument(isNotBlank(confirmPassword), "Подтверждение пароля не указано");
        checkArgument(password.equals(confirmPassword), "Пароли не совпадают");

    }

    public String getSocialUserId() {
        return socialUserId;
    }

    public void setSocialUserId(String socialUserId) {
        this.socialUserId = socialUserId;
    }
}
