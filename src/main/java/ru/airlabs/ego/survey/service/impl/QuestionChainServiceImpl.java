package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.service.QuestionChainService;

import javax.sql.DataSource;
import java.sql.Types;

@Service("questionChainService")
public class QuestionChainServiceImpl implements QuestionChainService {

    /**
     * Название хранимой процедуры
     */
    private static final String FILL_Q_CHAIN_PROCEDURE_NAME = "FILL_ITEM_CHAINS";
    /**
     * Название пакета
     */
    private static final String Q_LIST_PACKAGE_NAME = "Q_LIST_PKG";


    @Autowired
    private DataSource dataSource;

    /**
     * Формирование необходимых цепочек вопросов для опроса
     *
     * @param surveyId идентификатор опроса
     */
    @Transactional
    @Override
    public void fillQuestionChainForSurvey(Long surveyId) {
        final SimpleJdbcCall jdbcCall = createCall(FILL_Q_CHAIN_PROCEDURE_NAME);
        final SqlParameterSource parameters =  new MapSqlParameterSource().addValue("PID_LIST", surveyId);
        jdbcCall.execute(parameters);
    }

    private SimpleJdbcCall createCall(String procedureName) {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName(Q_LIST_PACKAGE_NAME)
                .withProcedureName(procedureName)
                .declareParameters(
                        new SqlParameter("PID_LIST", Types.BIGINT));
    }

}
