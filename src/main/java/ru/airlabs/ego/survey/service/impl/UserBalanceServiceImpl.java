package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.service.UserBalanceService;

import javax.sql.DataSource;

/**
 * Сервис для работы с балансом пользователя
 *
 * @author Aleksey Gorbachev
 */
@Service("userBalanceService")
public class UserBalanceServiceImpl implements UserBalanceService {

    /**
     * Стоимость 1 телефонного звонка (в рублях)
     */
    public static final double PHONE_CALL_DEFAULT_COST = 2.0;

    /**
     * Источник данных
     */
    private DataSource dataSource;

    /**
     * Jdbc template
     */
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Получение текущего баланса пользователя
     *
     * @param userId идентификатор пользователя
     * @return текущий баланс пользователя
     */
    @Transactional(readOnly = true)
    @Override
    public Double getUserBalance(Long userId) {
        return jdbcTemplate.queryForObject("select t.balance from USERS_BALANCE t where t.id_user = :userId",
                new MapSqlParameterSource().addValue("userId", userId),
                Double.class);
    }
}
