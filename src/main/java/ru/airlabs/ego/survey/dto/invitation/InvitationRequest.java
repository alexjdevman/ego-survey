package ru.airlabs.ego.survey.dto.invitation;

import ru.airlabs.ego.core.entity.User;

/**
 * Модель для отправки приглашения на вакансию или исследование
 *
 * @author Aleksey Gorbachev
 */
public class InvitationRequest {

    /**
     * Идентификатор пользователя HR
     */
    private Long userId;

    /**
     * Идентификатор пользователя-получателя
     */
    private Long receiverId;

    /**
     * Вакансию на которую отправляется приглашение
     */
    private Long vacancyId;

    /**
     * Флаг признака сотрудника, либо соискателя
     */
    private Boolean employee;

    /**
     * Локализация нотификации
     */
    private String locale;

    /**
     * Признак, откуда производится отправка приглашений
     *
     * При инвайте со страницы - D
     * При инвайте из личного кабинета - L
     */
    private String sourceCode;

    /**
     * Тип приглашения для получения текста приглашения
     *
     *  'E' – Email
     *  'S' – SMS
     *  'W' – WhatsApp
     *  'V' – Viber
     *  'C' – Skype
     *  'T' – Telegram
     *  'U' - Universal - получить текст приглашения и как то его потом отправить
     */
    private InvitationType invitationType;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(Long vacancyId) {
        this.vacancyId = vacancyId;
    }

    public Boolean getEmployee() {
        return employee;
    }

    public void setEmployee(Boolean employee) {
        this.employee = employee;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public InvitationType getInvitationType() {
        return invitationType;
    }

    public void setInvitationType(InvitationType invitationType) {
        this.invitationType = invitationType;
    }

    /**
     * Создание объекта запроса на приглашение
     *
     * @param invitation приглашение (имя и email или телефон)
     * @param hrUser     пользователь, который высылает приглашение
     * @param vacancyId  идентификатор вакансии
     * @param isEmployee флаг признака приглашения как сотрудника, или как соискателя
     * @return запрос на приглашение
     */
    public static InvitationRequest buildRequest(Invitation invitation,
                                                 User hrUser,
                                                 Long vacancyId,
                                                 boolean isEmployee) {
        InvitationRequest request = new InvitationRequest();
        request.setUserId(hrUser != null ? hrUser.getId() : 0); // если нет текущего пользователя - используется системный с id == 0
        request.setReceiverId(invitation.getReceiverId());
        request.setSourceCode(invitation.getSourceCode());
        request.setVacancyId(vacancyId);
        request.setEmployee(isEmployee);
        request.setLocale(hrUser != null ? hrUser.getLocale() : "RU");  // используется русская локаль по умолчанию
        return request;
    }

}
