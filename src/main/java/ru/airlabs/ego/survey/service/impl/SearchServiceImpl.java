package ru.airlabs.ego.survey.service.impl;

import oracle.jdbc.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.dto.search.SearchUserResult;
import ru.airlabs.ego.survey.service.SearchService;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Service("searchService")
public class SearchServiceImpl implements SearchService {

    /**
     * Источник данных
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Выполнить поиск пользователей по строке запроса
     *
     * @param currentUserId идентификатор HR
     * @param searchQuery   строка поиска
     * @return список результатов поиска по пользователям
     */
    @Transactional
    @Override
    public List<SearchUserResult> searchUsers(Long currentUserId, String searchQuery) {
        Map<String, Object> result = new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_STATS_PKG")
                .withProcedureName("SEARCH_USERS")
                .declareParameters(
                        new SqlOutParameter("CUR", OracleTypes.CURSOR, (rs, i) -> {
                            SearchUserResult searchResult = new SearchUserResult();
                            searchResult.setUserId(rs.getLong("ID_USER"));
                            searchResult.setName(rs.getString("NAME"));
                            searchResult.setEmail(rs.getString("EMAIL"));
                            searchResult.setPhone(rs.getString("PHONE"));
                            searchResult.setFirstName(rs.getString("FIRSTNAME"));
                            searchResult.setSureName(rs.getString("SURENAME"));
                            searchResult.setParentName(rs.getString("PARENTNAME"));
                            searchResult.setSex(rs.getString("SEX"));
                            searchResult.setBirthDate(rs.getTimestamp("DT_BIRTH"));
                            searchResult.setCity(rs.getString("CITY"));
                            searchResult.setDescription(rs.getString("DESCR"));
                            searchResult.setProgress(rs.getInt("PROGRESS"));
                            searchResult.setFinishedParts(rs.getInt("CALCULATED_PARTS"));
                            searchResult.setVacancyId(rs.getLong("ID_V_DATA"));
                            searchResult.setVacancyName(rs.getString("V_DATA_NAME"));
                            searchResult.setCompareResult(rs.getDouble("RESULT_PERC"));
                            return searchResult;
                        }),
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PSEARCH", Types.VARCHAR))
                .execute(new MapSqlParameterSource()
                        .addValue("PID_USER", currentUserId)
                        .addValue("PSEARCH", searchQuery));

        return (List<SearchUserResult>) result.get("CUR");
    }
}
