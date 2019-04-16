package ru.airlabs.ego.survey.controller;

import lombok.Data;
import oracle.jdbc.OracleTypes;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.utils.MapUtils;

import javax.sql.DataSource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.Types;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Контроллер для работы с плагином WhatsApp
 *
 * @author Roman Kochergin
 */
@Controller
public class WhatsAppPluginController {

    /**
     * Логгер
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppPluginController.class);

    /**
     * Источник данных
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Получить список сообщений к отправке
     *
     * @param count          кол-во сообщений
     * @param authentication текущий авторизованный пользователь
     * @return ответ
     */
    @GetMapping(value = "plugin/wa/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(@RequestParam(defaultValue = "30") int count,
                                         @AuthenticationPrincipal Authentication authentication) {
        if (authentication == null || authentication.getUser() == null || authentication.getAuthorities() == null) {
            return new ResponseEntity<>(UNAUTHORIZED);
        }
        return new ResponseEntity<>(
                new SimpleJdbcCall(dataSource)
                        .withoutProcedureColumnMetaDataAccess()
                        .withCatalogName("WA_PKG")
                        .withProcedureName("GET_MESSAGES")
                        .declareParameters(
                                new SqlOutParameter("CUR", OracleTypes.CURSOR, (rs, i) ->
                                        MapUtils.<String, Object>builder()
                                                .add("id", rs.getLong("ID_EVENT"))
                                                .add("phone", rs.getString("PHONE"))
                                                .add("text", rs.getString("TEXT"))
                                                .build()),
                                new SqlParameter("PID_USER", Types.BIGINT),
                                new SqlParameter("PCOUNT", Types.INTEGER))
                        .execute(new MapSqlParameterSource()
                                .addValue("PID_USER", authentication.getUser().getId())
                                .addValue("PCOUNT", count))
                        .get("CUR"),
                OK);
    }

    /**
     * Логировать
     *
     * @param entity сущность логирования
     * @return ответ
     */
    @PostMapping(value = "plugin/wa/log")
    public ResponseEntity<Void> log(@RequestBody @Valid LogEntity entity) {
        writeLog(entity);
        return new ResponseEntity<>(OK);
    }

    /**
     * Записать сущность в лог
     *
     * @param entity сущность логирования
     */
    private void writeLog(LogEntity entity) {
        switch (entity.level) {
            case INFO:
                LOGGER.info(formatLogEntity(entity));
                break;
            case WARN:
                LOGGER.warn(formatLogEntity(entity));
                break;
            default:
                LOGGER.error(formatLogEntity(entity));
                break;
        }
    }

    /**
     * Преобразовать сущность логирования в строку
     *
     * @param entity сущность логирования
     * @return строковое представление
     */
    private String formatLogEntity(LogEntity entity) {
        return new StringBuilder("PLUGIN: ")
                .append("ver=").append(entity.ver).append(", ")
                .append("user=").append(entity.user).append(", ")
                .append("vacancy=").append(entity.vacancy).append(", ")
                .append("event=").append(entity.event).append(", ")
                .append("type=").append(entity.type).append(", ")
                .append("result=").append(entity.result).append(", ")
                .append("desc=").append(entity.desc)
                .toString();
    }

    /**
     * Уровень логирования
     */
    enum LogLevel {
        INFO, WARN, ERROR
    }

    /**
     * Сущность логирования
     */
    @Data
    private static class LogEntity {

        /**
         * Уровень логирования
         */
        @NotNull
        private LogLevel level;

        /**
         * Версия плагина
         */
        @NotBlank
        private String ver;

        /**
         * Пользователь (ИД или e-mail)
         */
        @NotBlank
        private String user;

        /**
         * Идентификатор вакансии
         */
        private Long vacancy;

        /**
         * Идентификатор события
         */
        private Long event;

        /**
         * Тип операции
         */
        @NotBlank
        private String type;

        /**
         * Результат операции
         */
        private String result;

        /**
         * Описание
         */
        private String desc;
    }

}
