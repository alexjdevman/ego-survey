package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

/**
 * Модель для передачи данных о сравнении пользователей для исследования
 *
 * @author Aleksey Gorbachev
 */
public class UserCompareResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор пользователя
     */
    private Long userId;

    /**
     * ФИО участника
     */
    private String name;

    /**
     * Email участника
     */
    private String email;

    /**
     * Процент совпадения ("похожести") с остальными участниками исследования
     */
    private Double compareResult;

    public UserCompareResult() {
    }

    public UserCompareResult(Long userId,
                             String name,
                             String email,
                             Double compareResult) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.compareResult = compareResult;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public Double getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(Double compareResult) {
        this.compareResult = compareResult;
    }
}
