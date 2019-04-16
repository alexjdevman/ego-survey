package ru.airlabs.ego.survey.dto.resume;

import java.io.Serializable;

/**
 * Модель для передачи результатов проверки ссылок на резюме на UI
 *
 * @author Aleksey Gorbachev
 */
public class ResumeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор пользователя
     */
    private Long userId;

    /**
     * Код статуса проверки
     *
     * 0 - такого резюме в БД нет, надо грузить
     * 1 - пользователь есть и он помещён в "буфер" вакансии на приглашение
     * 2 - такой пользователь есть и он уже ранее приглашался в этой вакансии, ничего не делаем
     * -1 - общая ошибка
     * -2 - вакансия закрыта, приглашения запрещены
     * -3 - такой вакансии не существует
     * -4 - у пользователя истекла лицензия, приглашения запрещены
     */
    private Integer status;

    /**
     * Ссылка на резюме
     */
    private String url;

    public ResumeInfo() {
    }

    public ResumeInfo(Long userId, Integer status, String url) {
        this.userId = userId;
        this.status = status;
        this.url = url;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
