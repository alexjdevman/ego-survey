package ru.airlabs.ego.survey.utils;

/**
 * Утилиты для работы с БД
 */
public class DbUtils {

    /**
     * Обертка для постраничной выборки
     */
    private static final String paginationWrapper = "SELECT *\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         sub.*,\n" +
            "         ROWNUM rn\n" +
            "       FROM\n" +
            "         (\n" +
            "           %sql\n" +
            "         ) sub\n" +
            "       WHERE ROWNUM <= :toNumber\n" +
            "     ) sub2\n" +
            "WHERE sub2.rn > :fromNumber";

    /**
     * Обертка для выборки количества записей
     */
    private static final String countWrapper = "SELECT count(1)\n" +
            "FROM (\n" +
            "  %sql\n" +
            ")";

    /**
     * Обернуть запрос в постраничную выборку
     *
     * @param sql запрос
     * @return обернутый запрос
     */
    public static String wrapWithPagination(String sql) {
        return paginationWrapper.replace("%sql", sql);
    }

    /**
     * Обернуть запрос в количественную выборку
     *
     * @param sql запрос
     * @return обернутый запрос
     */
    public static String wrapWithCount(String sql) {
        return countWrapper.replace("%sql", sql);
    }
}
