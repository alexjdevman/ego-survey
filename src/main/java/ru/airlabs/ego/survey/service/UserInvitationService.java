package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.invitation.*;

import java.util.Date;
import java.util.Set;

/**
 * Интерфейс отправки приглашения на вакансию или исследование
 *
 * @author Aleksey Gorbachev
 */
public interface UserInvitationService {

    /**
     * Отправка приглашений на вакансию или исследование
     * (если в базе нет email приглашаемого - создается новый пользователь)
     *
     * @param invitations список приглашаемых
     * @param user        пользователь, выполняющий отправку приглашений (HR)
     * @param vacancyId   идентификатор вакансии
     */
    void sendInvitations(Set<Invitation> invitations,
                         User user,
                         Long vacancyId);

    /**
     * Получение текста приглашения
     *
     * @param invitation     приглашение
     * @param user           пользователь
     * @param vacancyId      идентификатор вакансии
     * @param invitationType тип приглашения
     * @return текст приглашения пользователя
     */
    InvitationText createInvitationText(Invitation invitation,
                                        User user,
                                        Long vacancyId,
                                        InvitationType invitationType);

    /**
     * Получение персональной ссылки для прохождения опроса на вакансию для пользователя
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии
     * @return персональная ссылка для прохождения опроса
     */
    String getSurveyLinkForUserAndVacancy(Long userId,
                                          Long vacancyId);

    /**
     * Установить статус отправки приглашения
     *
     * @param invitationId идентификатор приглашения
     * @param status       статус отправки приглашения
     */
    void setInvitationStatus(Long invitationId, InvitationStatus status);

    /**
     * Получение кол-ва приглашений,
     * отправленных пользователем с определенной даты до текущего времени
     *
     * @param user         пользователь HR
     * @param fromDate     дата, с которой считать отправленные приглашения
     * @param inviteType   тип приглашения (E - почта, W - вассап)
     * @param inviteSource источник отправки (D - с внешнего сайта, L - из личного кабинета, если не передавать - любые)
     * @return кол-во приглашений
     */
    InvitationCount getInvitationCountForUser(User user,
                                              Date fromDate,
                                              String inviteType,
                                              String inviteSource);

}
