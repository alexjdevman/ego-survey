package ru.airlabs.ego.survey.utils;

import org.springframework.data.domain.Sort;

import java.util.Iterator;

/**
 * Вспомогательный класс для формирования запросов сортировки данных
 *
 * @author Aleksey Gorbachev
 */
public class SortingUtils {


    /**
     * Получение подстроки SQL-запроса сортировки приглашений и фидбеков,
     * используя настройки сортировки из UI.
     * Используемая модель (ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail)
     *
     * @param sort настройки сортировки
     * @return подстрока запроса сортировки
     */
    public static String createOrderQueryForVacancyUserDetails(Sort sort) {
        String query = "order by ";
        Iterator<Sort.Order> iterator = sort.iterator();
        if (!iterator.hasNext()) return "";
        while (iterator.hasNext()) {
            Sort.Order sortOrder = iterator.next();
            String sortProperty = sortOrder.getProperty();
            Sort.Direction direction = sortOrder.getDirection();

            switch (sortProperty) { // формируем подстроку запроса сортировки
                case "email":
                    query = query + "u.email " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "name":
                    query = query + "u.name " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "phone":
                    query = query + "u.phone " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "surveyCreate":
                    query = query + "vu.dt_create " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "surveyUpdate":
                    query = query + "q.dt_update " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "progress":
                    query = query + "q.progress " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "compareResult":
                    query = query + "c.compared " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                default:
                    throw new IllegalArgumentException("Not supported sorting property: " + sortProperty);
            }
        }
        query = query.trim();
        if (query.endsWith(",")) {
            query = query.substring(0, query.length() - 1);
        }
        query += " nulls last";
        return query;
    }

    /**
     * Получение подстроки SQL-запроса сортировки результатов для пользовательских опросов,
     * используя настройки сортировки из UI.
     * Используемая модель (ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail)
     *
     * @param sort настройки сортировки
     * @return подстрока запроса сортировки
     */
    public static String createOrderQueryForCustomSurveyResults(Sort sort) {
        String query = "order by ";
        Iterator<Sort.Order> iterator = sort.iterator();
        if (!iterator.hasNext()) return "";
        while (iterator.hasNext()) {
            Sort.Order sortOrder = iterator.next();
            String sortProperty = sortOrder.getProperty();
            Sort.Direction direction = sortOrder.getDirection();

            switch (sortProperty) { // формируем подстроку запроса сортировки
                case "surveyUpdate":
                    query = query + "q.dt_update " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                case "compareResult":
                    query = query + "q.list_result_perc " + (direction == Sort.Direction.DESC ? "desc, " : "asc, ");
                    break;
                default:
                    throw new IllegalArgumentException("Not supported sorting property: " + sortProperty);
            }
        }
        query = query.trim();
        if (query.endsWith(",")) {
            query = query.substring(0, query.length() - 1);
        }
        query += " nulls last";
        return query;
    }

}
