package ru.airlabs.ego.survey.utils;

import org.springframework.util.StringUtils;

/**
 * Утилиты для работы с email
 *
 * @author Roman Kochergin
 */
public class EmailUtils {

    private static String ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~-]";
    private static String DOMAIN = "(" + ATOM + "+(\\." + ATOM + "+)*";
    private static String IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]";

    private static java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile(
            "^" + ATOM + "+(\\." + ATOM + "+)*@"
                    + DOMAIN
                    + "|"
                    + IP_DOMAIN
                    + ")$",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    /**
     * Валиден ли email
     *
     * @param email email
     * @return да или нет
     */
    public static boolean isValid(String email) {
        return StringUtils.hasText(email) && emailPattern.matcher(email).matches();
    }
}
