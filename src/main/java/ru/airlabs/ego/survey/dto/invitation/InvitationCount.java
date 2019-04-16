package ru.airlabs.ego.survey.dto.invitation;

/**
 * Модель для получения информации по кол-ву приглашений для пользователй
 *
 * @author Aleksey Gorbachev
 */
public class InvitationCount {

    /**
     * Кол-во отправленных пользователем Email приглашений
     */
    public Integer invitationOnEmailCount;

    /**
     * Лимит Email приглашений для пользователя
     */
    public Integer invitationOnEmailLimit;

    /**
     * Дневной лимит приглашений
     */
    public Integer invitationDayLimit;
}
