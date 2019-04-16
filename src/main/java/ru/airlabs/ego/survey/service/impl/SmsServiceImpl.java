package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.service.SmsService;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Сервис для работы с СМС
 *
 * @author Roman Kochergin
 */
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private DataSource dataSource;

    /**
     * Отправить сообщение
     *
     * @param senderId   идентификатор пользователя-отправителя (обычно hr id), для системного передавать 0
     * @param receiverId идентификатор пользователя-получателя
     * @param templateId идентификатор шаблона сообщения
     * @param phone      номер телефона, если у пользователя он есть, то передавать не обязательно, если нет - будет обновлен
     * @param vacancyId  идентификатор вакансии, если неоходимо
     * @param surveyId   идентификатор опроса, если необходимо
     * @return результат отправки сообщения
     */
    @Override
    @Transactional
    public SmsSendResult send(Long senderId, Long receiverId, String templateId, String phone, Long vacancyId, Long surveyId) {
        String result = new SimpleJdbcCall(dataSource)
                .withCatalogName("SMS_PKG")
                .withFunctionName("SEND_SMS")
                .declareParameters(
                        new SqlParameter("PID_SENDER", Types.BIGINT),
                        new SqlParameter("PID_RECIEVER", Types.BIGINT),
                        new SqlParameter("PID_TEMPLATE", Types.VARCHAR),
                        new SqlParameter("PPHONE", Types.VARCHAR),
                        new SqlParameter("PID_V_DATA", Types.BIGINT),
                        new SqlParameter("PID_LIST", Types.BIGINT))
                .executeFunction(String.class, new MapSqlParameterSource()
                        .addValue("PID_SENDER", senderId)
                        .addValue("PID_RECIEVER", receiverId)
                        .addValue("PID_TEMPLATE", templateId)
                        .addValue("PPHONE", phone)
                        .addValue("PID_V_DATA", vacancyId)
                        .addValue("PID_LIST", surveyId));
        return SmsSendResult.findByCode(result);
    }
}
