package ru.airlabs.ego.survey.service.impl;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.survey.dto.invitation.*;
import ru.airlabs.ego.survey.service.UserInvitationService;
import ru.airlabs.ego.survey.service.VacancyService;

import javax.sql.DataSource;
import java.sql.Clob;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static ru.airlabs.ego.survey.dto.invitation.InvitationRequest.buildRequest;

/**
 * Сервис отправки приглашения на вакансию или исследование
 *
 * @author Aleksey Gorbachev
 */
@Service("userInvitationService")
public class UserInvitationServiceImpl implements UserInvitationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInvitationServiceImpl.class);

    /**
     * название хранимой процедуры получения кол-ва лимитов по приглашениям
     */
    private static final String GET_INVITE_COUNT_PROCEDURE_NAME = "GET_INVITES_COUNT";

    /**
     * название хранимой процедуры отправки приглашений по email
     */
    private static final String INVITE_PROCEDURE_NAME = "CREATE_INVITE";
    /**
     * название хранимой функции для создания текста приглашений
     */
    private static final String CREATE_INVITE_TEXT_FUNCTION_NAME = "CREATE_INVITE_TEXT";

    /**
     * Название хранимой функции для получения ссылки на персональный опрос для пользователя
     */
    private static final String GET_INVITE_URL_FUNCTION_NAME = "GET_INVITE_URL";

    /**
     * название пакета с хранимыми процедурами
     */
    private static final String INVITE_PACKAGE_NAME = "Q_INVITE_PKG";

    /**
     * Сервис вакансий
     */
    @Autowired
    private VacancyService vacancyService;

    /**
     * Источник данных
     */
    private DataSource dataSource;

    /**
     * Jdbc template
     */
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Transactional
    @Override
    public void sendInvitations(Set<Invitation> invitations, User user, Long vacancyId) {
        final SimpleJdbcCall inviteCall = createInviteCall();
        final boolean isResearch = isResearch(vacancyId);
        for (Invitation invitation : invitations) {
            final Boolean isEmployee = isResearch ? Boolean.TRUE : invitation.getEmployee();
            InvitationRequest request = buildRequest(invitation, user, vacancyId, isEmployee);
            sendInvite(inviteCall, request);
        }
    }

    /**
     * Получение текста приглашения
     *
     * @param invitation     приглашение
     * @param user           пользователь
     * @param vacancyId      идентификатор вакансии
     * @param invitationType тип приглашения
     * @return текст приглашения пользователя
     */
    @Transactional
    @Override
    public InvitationText createInvitationText(Invitation invitation,
                                               User user,
                                               Long vacancyId,
                                               InvitationType invitationType) {
        try {
            final SimpleJdbcCall inviteCall = createTextInviteCall();
            final Boolean isEmployee = isResearch(vacancyId) ? Boolean.TRUE : invitation.getEmployee();
            InvitationRequest request = buildRequest(invitation, user, vacancyId, isEmployee);
            request.setInvitationType(invitationType);
            SqlParameterSource parameters = createTextRequestParameters(request);
            Map<String, Object> results = inviteCall.execute(parameters);
            Long id = (Long) results.get("PID_EVENT");
            Clob clob = (Clob) results.get("PTEXT");

            return new InvitationText(id, clob != null ? clob.getSubString(1, (int) clob.length()) : null);
        } catch (Exception err) {
            LOGGER.error(err.getMessage(), err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Получение персональной ссылки для прохождения опроса на вакансию для пользователя
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии
     * @return персональная ссылка для прохождения опроса
     */
    @Transactional
    @Override
    public String getSurveyLinkForUserAndVacancy(Long userId, Long vacancyId) {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName(INVITE_PACKAGE_NAME)
                .withFunctionName(GET_INVITE_URL_FUNCTION_NAME)
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_V_DATA", Types.VARCHAR))
                .executeFunction(String.class, new MapSqlParameterSource()
                        .addValue("PID_USER", userId)
                        .addValue("PID_V_DATA", vacancyId));
    }

    /**
     * Установить статус отправки приглашения
     *
     * @param invitationId идентификатор приглашения
     * @param status       статус отправки приглашения
     */
    @Override
    @Transactional
    public void setInvitationStatus(Long invitationId, InvitationStatus status) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("state", status.name())
                .addValue("id", invitationId);
        jdbcTemplate.update("update event set id_state = :state where id_event = :id", params);
    }

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
    @Transactional
    @Override
    public InvitationCount getInvitationCountForUser(User user,
                                                     Date fromDate,
                                                     String inviteType,
                                                     String inviteSource) {
        Date truncDate = DateUtils.truncate(fromDate, Calendar.DATE);
        InvitationCount invitationCount = new InvitationCount();
        Map<String, Object> result = new SimpleJdbcCall(dataSource)
                .withCatalogName(INVITE_PACKAGE_NAME)
                .withProcedureName(GET_INVITE_COUNT_PROCEDURE_NAME)
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_TYPE", Types.VARCHAR),
                        new SqlParameter("PDT_FROM", Types.DATE),
                        new SqlParameter("PDT_TO", Types.DATE),
                        new SqlParameter("PID_SOURCE", Types.VARCHAR),

                        new SqlOutParameter("PCOUNT", Types.BIGINT),
                        new SqlOutParameter("PDT_LAST", Types.DATE),
                        new SqlOutParameter("PLIMIT", Types.BIGINT),
                        new SqlOutParameter("PDAY_LIMIT", Types.BIGINT))
                .execute(new MapSqlParameterSource()
                        .addValue("PID_USER", user.getId())
                        .addValue("PID_TYPE", inviteType)
                        .addValue("PDT_FROM", truncDate)
                        .addValue("PDT_TO", null)
                        .addValue("PID_SOURCE", inviteSource != null ? inviteSource : null));

        invitationCount.invitationOnEmailCount = ((Long) result.get("PCOUNT")).intValue();
        invitationCount.invitationOnEmailLimit = ((Long) result.get("PLIMIT")).intValue();
        invitationCount.invitationDayLimit = ((Long) result.get("PDAY_LIMIT")).intValue();
        return invitationCount;
    }

    private void sendInvite(SimpleJdbcCall inviteCall, InvitationRequest request) {
        SqlParameterSource parameters = createRequestParameters(request);
        inviteCall.execute(parameters);
    }

    private SqlParameterSource createRequestParameters(InvitationRequest request) {
        return new MapSqlParameterSource()
                .addValue("PID_USER", request.getUserId())
                .addValue("PID_RECIEVER", request.getReceiverId())
                .addValue("PID_V_DATA", request.getVacancyId())
                .addValue("PIS_EMPLOYEE", request.getEmployee())
                .addValue("PLOCALE", request.getLocale())
                .addValue("PID_SOURCE", request.getSourceCode());
    }

    private SqlParameterSource createTextRequestParameters(InvitationRequest request) {
        return new MapSqlParameterSource()
                .addValue("PID_USER", request.getUserId())
                .addValue("PID_RECIEVER", request.getReceiverId())
                .addValue("PID_V_DATA", request.getVacancyId())
                .addValue("PIS_EMPLOYEE", request.getEmployee())
                .addValue("PLOCALE", request.getLocale())
                .addValue("PID_SOURCE", request.getSourceCode())
                .addValue("PID_TYPE", request.getInvitationType().getTypeId());
    }

    private SimpleJdbcCall createInviteCall() {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName(INVITE_PACKAGE_NAME)
                .withProcedureName(INVITE_PROCEDURE_NAME)
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("ID_RECIEVER", Types.BIGINT),
                        new SqlParameter("PID_V_DATA", Types.BIGINT),
                        new SqlParameter("PIS_EMPLOYEE", Types.BOOLEAN),
                        new SqlParameter("PLOCALE", Types.VARCHAR),
                        new SqlParameter("PID_SOURCE", Types.CHAR));
    }

    private SimpleJdbcCall createTextInviteCall() {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName(INVITE_PACKAGE_NAME)
                .withProcedureName(CREATE_INVITE_TEXT_FUNCTION_NAME)
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("ID_RECIEVER", Types.BIGINT),
                        new SqlOutParameter("PTEXT", Types.CLOB),
                        new SqlOutParameter("PID_EVENT", Types.BIGINT),
                        new SqlParameter("PID_V_DATA", Types.BIGINT),
                        new SqlParameter("PIS_EMPLOYEE", Types.BOOLEAN),
                        new SqlParameter("PLOCALE", Types.VARCHAR),
                        new SqlParameter("PID_SOURCE", Types.CHAR),
                        new SqlParameter("PID_TYPE", Types.CHAR));
    }

    /**
     * Проверка, является ли вакансия исследованием
     *
     * @param vacancyId идентификатор вакансии\исследования
     * @return true - для исследований, иначе false
     */
    private boolean isResearch(Long vacancyId) {
        Vacancy vacancy = vacancyService.findById(vacancyId);
        return !vacancy.getVacancy();
    }

}
