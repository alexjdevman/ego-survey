package ru.airlabs.ego.survey.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Утилиты для работы с именами пользователей
 *
 * @author Aleksey Gorbachev
 */
public class NameUtils {

    /**
     * Получение простого имени пользователя из ФИО
     *
     * @param fio ФИО пользователя
     * @return простое имя пользователя
     */
    public static String extractFirstNameFromFIO(String fio) {
        if (isBlank(fio)) {
            return null;
        } else {
            String[] nameParts = fio.split("\\s+");
            if (nameParts.length >= 2) {
                return nameParts[1];
            } else {
                return nameParts[0];
            }
        }
    }

    /**
     * Получение фамилии пользователя из ФИО
     *
     * @param fio ФИО пользователя
     * @return фамилия пользователя
     */
    public static String extractSurenameFromFIO(String fio) {
        if (isBlank(fio)) {
            return null;
        } else {
            String[] nameParts = fio.split("\\s+");
            if (nameParts.length >= 2) {
                return nameParts[0];
            } else {
                return null;
            }
        }
    }

    /**
     * Получение отчества пользователя из ФИО
     *
     * @param fio ФИО пользователя
     * @return отчество пользователя
     */
    public static String extractParentnameFromFIO(String fio) {
        if (isBlank(fio)) {
            return null;
        } else {
            String[] nameParts = fio.split("\\s+");
            if (nameParts.length == 3) {
                return nameParts[2];
            } else {
                return null;
            }
        }
    }

    /**
     * Обрезание строки по определнной длинне
     *
     * @param str    строка
     * @param length длинна
     * @return новая строка
     */
    public static String trimStringToLength(String str, int length) {
        if (isNotBlank(str)) {
            return str.substring(0, Math.min(str.length(), length));
        }
        return str;
    }

}
