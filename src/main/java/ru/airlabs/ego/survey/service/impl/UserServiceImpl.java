package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.service.UserService;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Date;

/**
 * Сервис работы с пользователями
 *
 * @author Roman Kochergin
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private DataSource dataSource;

    /**
     * Получить или создать пользователя
     *
     * @param name  имя
     * @param email email
     * @param phone телефон
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    @Override
    @Transactional
    public Long getOrCreateUser(String name, String email, String phone, Long parentUserId) {
        return createUserByJdbcCall(name, email, phone, null, null, null, null, null, null, null, null, parentUserId);
    }

    /**
     * Получить или создать пользователя
     *
     * @param name  имя
     * @param email email
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    @Override
    @Transactional
    public Long getOrCreateUser(String name, String email, Long parentUserId) {
        return createUserByJdbcCall(name, email, null, null, null, null, null, null, null, null, null, parentUserId);
    }

    /**
     * Получить или создать пользователя
     *
     * @param name        имя
     * @param email       email
     * @param phone       телефон
     * @param locale      локаль
     * @param lastName    фамилия
     * @param middleName  отчество
     * @param sex         пол
     * @param birthDate   дата рождения
     * @param city        город
     * @param description описание
     * @param salary      зарплата
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    @Override
    @Transactional
    public Long getOrCreateUser(String name, String email, String phone,
                                String locale, String lastName, String middleName,
                                Sex sex, Date birthDate, String city, String description,
                                Integer salary, Long parentUserId) {
        return createUserByJdbcCall(name, email, phone, locale, lastName, middleName, sex, birthDate, city, description, salary, parentUserId);
    }


    /**
     * Создать или получить пользователя посредством хранимой процедуры
     *
     * @param name        имя
     * @param email       email
     * @param phone       телефон
     * @param locale      локаль
     * @param lastName    фамилия
     * @param middleName  отчество
     * @param sex         пол
     * @param birthDate   дата рождения
     * @param city        город
     * @param description описание
     * @param salary      зарплата
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    private Long createUserByJdbcCall(String name, String email, String phone,
                                      String locale, String lastName, String middleName,
                                      Sex sex, Date birthDate, String city, String description,
                                      Integer salary, Long parentUserId) {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_INVITE_PKG")
                .withFunctionName("CREATE_USER")
                .declareParameters(
                        new SqlParameter("PID_PARENT", Types.BIGINT),
                        new SqlParameter("PNAME", Types.VARCHAR),
                        new SqlParameter("PEMAIL", Types.VARCHAR),
                        new SqlParameter("PPHONE", Types.VARCHAR),
                        new SqlParameter("PLOCALE", Types.VARCHAR),
                        new SqlParameter("PSURENAME", Types.VARCHAR),
                        new SqlParameter("PPARENTNAME", Types.VARCHAR),
                        new SqlParameter("PSEX", Types.CHAR),
                        new SqlParameter("PDT_BIRTH", Types.TIMESTAMP),
                        new SqlParameter("PCITY", Types.VARCHAR),
                        new SqlParameter("PDESCR", Types.VARCHAR),
                        new SqlParameter("PSALARY", Types.BIGINT),
                        new SqlOutParameter("RESULT", Types.BIGINT))
                .executeFunction(Long.class,
                        new MapSqlParameterSource()
                                .addValue("PID_PARENT", parentUserId != null ? parentUserId : 0)
                                .addValue("PNAME", name)
                                .addValue("PEMAIL", email)
                                .addValue("PPHONE", phone)
                                .addValue("PLOCALE", locale)
                                .addValue("PSURENAME", lastName)
                                .addValue("PPARENTNAME", middleName)
                                .addValue("PSEX", sex != null ? sex.name() : null)
                                .addValue("PDT_BIRTH", birthDate)
                                .addValue("PCITY", city)
                                .addValue("PDESCR", description)
                                .addValue("PSALARY", salary)
                );
    }

}
