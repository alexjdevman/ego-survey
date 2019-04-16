package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.VacancyUser;
import ru.airlabs.ego.core.repository.VacancyUserRepository;
import ru.airlabs.ego.survey.service.SubscriptionService;

import javax.sql.DataSource;
import java.sql.Types;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Сервис для работы с подписками
 *
 * @author Roman Kochergin
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    /**
     * Репозиторий пользователя вакансии
     */
    @Autowired
    private VacancyUserRepository vacancyUserRepository;

    /**
     * Источник данных
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Подписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   имейл
     */
    @Override
    @Transactional
    public void subscribe(Long eventId, Long userId, String email) {
        new SimpleJdbcCall(dataSource)
                .withCatalogName("EMAIL_PKG")
                .withProcedureName("SET_SUBSCRIBE")
                .declareParameters(
                        new SqlParameter("PEMAIL", Types.VARCHAR),
                        new SqlParameter("PID_EVENT", Types.BIGINT),
                        new SqlParameter("PID_RECIEVER", Types.BIGINT))
                .execute(new MapSqlParameterSource()
                        .addValue("PEMAIL", email)
                        .addValue("PID_EVENT", eventId)
                        .addValue("PID_RECIEVER", userId));
    }

    /**
     * Отписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   имейл
     */
    @Override
    @Transactional
    public void unsubscribe(Long eventId, Long userId, String email) {
        new SimpleJdbcCall(dataSource)
                .withCatalogName("EMAIL_PKG")
                .withProcedureName("SET_UNSUBSCRIBE")
                .declareParameters(
                        new SqlParameter("PEMAIL", Types.VARCHAR),
                        new SqlParameter("PID_EVENT", Types.BIGINT),
                        new SqlParameter("PID_RECIEVER", Types.BIGINT))
                .execute(new MapSqlParameterSource()
                        .addValue("PEMAIL", email)
                        .addValue("PID_EVENT", eventId)
                        .addValue("PID_RECIEVER", userId));
    }

    /**
     * Согласие или отказ на приглашение по вакансии
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @param response  состояние приглашения пользователя
     */
    @Transactional
    @Override
    public void setInvitationResponse(Long vacancyId, Long userId, Integer response) {
        VacancyUser vacancyUser = vacancyUserRepository.findByUserIdAndVacancyId(userId, vacancyId);
        checkNotNull(vacancyUser, "Не найдены данные пользователя с id %s для вакансии с id %s", userId, vacancyId);
        if (response.equals(0)) {
            vacancyUser.setActive(Boolean.FALSE);
        }
        vacancyUser.setResponseId(response);
    }
}
