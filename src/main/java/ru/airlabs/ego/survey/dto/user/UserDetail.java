package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Модель для редактирования карточки пользователя на UI
 *
 * @author Aleksey Gorbachev
 */
public class UserDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * ФИО пользователя
     */
    private String name;

    /**
     * Телефон пользователя
     */
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Валидация полей при редактировании карточки пользователя
     */
    public void validate() {
        // должен быть заполнен либо телефон либо email
        if (isBlank(phone)) {
            checkArgument(isNotBlank(email), "Не заполнен email пользователя");
        }
        if (isBlank(email)) {
            checkArgument(isNotBlank(phone), "Не заполнен телефон пользователя");
        }
        checkArgument(isNotBlank(name), "Не заполнено ФИО пользователя");
    }
}
