package ru.airlabs.ego.survey.dto.invitation;

import java.io.Serializable;

/**
 * Модель для приглашения на вакансию или исследование, получаемая из UI
 *
 * @author Aleksey Gorbachev
 */
public class Invitation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор приглашенного пользователя
     */
    private Long receiverId;

    /**
     * Признак, откуда производится отправка приглашений
     *
     * L - личный кабинет (самый распространенный сорс)
     * D - директ линк из внешнего сайта (HR разместил где то общую ссылку)
     * A - автоматически из БД (это работа внутренних алгоритмов)
     * S - системное копирование из одной вакансии в другую (чтобы добавлять / убирать всех кандидатов)
     * B - буфер (те, кто еще не приглашен, просто созданы пользователи с файлом резюме)
     * P - плагин
     */
    private String sourceCode;

    /**
     * Признак IS_EMPLOYEE
     * (заполняется TRUE для инвайтов на исследование (vacancy:false) и инвайтов эталона на вакансию (vacancy:true, leader:true)
     */
    private Boolean employee = Boolean.FALSE;

    public Invitation(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Invitation(Long receiverId, String sourceCode) {
        this.receiverId = receiverId;
        this.sourceCode = sourceCode;
    }

    public Invitation(Long receiverId, String sourceCode, Boolean employee) {
        this.receiverId = receiverId;
        this.sourceCode = sourceCode;
        this.employee = employee;
    }

    public Invitation() {
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Boolean getEmployee() {
        return employee;
    }

    public void setEmployee(Boolean employee) {
        this.employee = employee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invitation)) return false;
        Invitation that = (Invitation) o;
        return !(receiverId != null ? !receiverId.equals(that.receiverId) :
                that.receiverId != null) && !(sourceCode != null ? !sourceCode.equals(that.sourceCode) : that.sourceCode != null);
    }

    @Override
    public int hashCode() {
        int result = receiverId != null ? receiverId.hashCode() : 0;
        result = 31 * result + (sourceCode != null ? sourceCode.hashCode() : 0);
        return result;
    }
}
