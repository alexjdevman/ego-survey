package ru.airlabs.ego.survey.service.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Сервис для отправки писем
 *
 * @author Aleksey Gorbachev
 */
@Service("mailSenderService")
public class MailSenderServiceImpl implements MailSenderService {

    /**
     * Источник данных
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Отправить письмо
     *
     * @param email    email адрес
     * @param name     имя пользователя
     * @param subject  тема письма
     * @param text     текст письма
     * @param fileName имя файла
     * @param filePath путь к файлу
     * @param senderId идентификатор пользователя-отправителя
     */
    @Transactional
    @Override
    public void sendMail(String email,
                         String name,
                         String subject,
                         String text,
                         String fileName,
                         String filePath,
                         Long senderId) {
        new SimpleJdbcCall(dataSource)
                .withCatalogName("EMAIL_PKG")
                .withProcedureName("SEND_MESSAGE")
                .declareParameters(
                        new SqlParameter("PTO_EMAIL", Types.VARCHAR),
                        new SqlParameter("PTO_NAME", Types.VARCHAR),
                        new SqlParameter("PSUBJECT", Types.VARCHAR),
                        new SqlParameter("PTEXT", Types.VARCHAR),
                        new SqlParameter("PFILE_NAME", Types.VARCHAR),
                        new SqlParameter("PFILE_PATH", Types.VARCHAR),
                        new SqlParameter("PID_SENDER", Types.BIGINT))
                .execute(new MapSqlParameterSource()
                        .addValue("PTO_EMAIL", email)
                        .addValue("PTO_NAME", name)
                        .addValue("PSUBJECT", subject)
                        .addValue("PTEXT", text)
                        .addValue("PFILE_NAME", fileName)
                        .addValue("PFILE_PATH", filePath)
                        .addValue("PID_SENDER", senderId));

    }
}
