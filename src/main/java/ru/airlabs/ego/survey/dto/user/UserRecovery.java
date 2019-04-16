package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

/**
 * Модель для передачи данных по восстановлению пароля пользователя
 *
 * @author Aleksey Gorbachev
 */
public class UserRecovery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * CAPTCHA код
     */
    private String captcha;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
