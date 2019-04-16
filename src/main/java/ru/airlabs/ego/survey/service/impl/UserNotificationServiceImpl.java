package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.exception.UserNotFoundException;
import ru.airlabs.ego.survey.service.UserNotificationService;

import javax.sql.DataSource;
import java.sql.Types;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service("userNotificationService")
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final String NOTIFY_REGISTRATION_PROCEDURE_NAME = "CREATE_REGISTRATION";
    private static final String NOTIFY_PASSWORD_CHANGE_PROCEDURE_NAME = "CREATE_PASSWORD_REMIND";
    private static final String USERS_PACKAGE_NAME = "USERS_PKG";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    /**
     * Web-адрес приложения
     */
    @Value("${application.url}")
    private String applicationUrl;


    @Transactional
    @Override
    public void notifyUserForRegistration(User user, String sourceSite) {
        final SimpleJdbcCall notifyCall = createNotificationCall(NOTIFY_REGISTRATION_PROCEDURE_NAME);
        final SqlParameterSource parameters = createRequestParameters(user, sourceSite);
        notifyCall.execute(parameters);
    }

    @Transactional
    @Override
    public void notifyUserForPasswordRecovery(String email) {
        final SimpleJdbcCall notifyCall = createNotificationCall(NOTIFY_PASSWORD_CHANGE_PROCEDURE_NAME);
        User user = userRepository.findByEmailAndActive(email, Boolean.TRUE);
        if (user == null) {
            throw new UserNotFoundException(format("Пользователь с почтовым адресом %s не найден в системе", email));
        }
        final SqlParameterSource parameters = createRequestParameters(user, null);
        notifyCall.execute(parameters);
    }

    private SqlParameterSource createRequestParameters(User user, String sourceSite) {
        return new MapSqlParameterSource()
                .addValue("PID_USER", user.getId())
                .addValue("PLOCALE", user.getLocale())
                .addValue("PSOURCE_SITE", isBlank(sourceSite) ? applicationUrl : sourceSite);
    }


    private SimpleJdbcCall createNotificationCall(String procedureName) {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName(USERS_PACKAGE_NAME)
                .withProcedureName(procedureName)
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PLOCALE", Types.VARCHAR),
                        new SqlParameter("PSOURCE_SITE", Types.VARCHAR));
    }
}
