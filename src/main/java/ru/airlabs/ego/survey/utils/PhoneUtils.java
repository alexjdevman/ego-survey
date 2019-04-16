package ru.airlabs.ego.survey.utils;

import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Utils handful in working with phones
 */
public class PhoneUtils {

    public static final int MOBILE_PHONE_MAIN_PART_SYMBOLS_NUMBER = 7;

    /**
     * Check is passed phone mobile phone
     * @param phone - phone to check
     * @return true if phone is mobile and false if not
     */
    public static boolean isPhoneMobile(String phone) {
        return phone != null && phone.length() >= 10 && phone.charAt(1) == '9';
    }

    /**
     * Check does the phone number contains only digits (1 <= length <= 15)
     *
     * @param phone phone number
     * @return true if phone contains only digits
     */
    public static boolean isOnlyDigitsInPhone(String phone) {
        return phone.matches("\\d{1,15}");
    }

    /**
     * Check does the phone number contains letters (like A8D0)
     * @param phone phone to check
     * @return true if phone is literal or false if not
     */
    public static boolean isPhoneLiteral(String phone) {
        return phone.matches(".*\\D+.*");
    }

    /**
     * Strips phone from all not number chars
     * @param phone - phone to transform
     * @return stripped phone
     */
    public static String stripPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.replaceAll("[^\\d]+", "");
    }

    /**
     * Strips phone from all not number chars
     * @param phone - phone to transform
     * @return stripped phone
     */
    public static String stripPhoneFromPlus(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.replaceAll("\\+", "");
    }

    /**
     * Convert phone to format that is received by device for it's settings
     * @param phone - phone to transform
     * @return converted phone
     */
    public static String convertPhoneToDeviceFormat(String phone) {
        return ("+" + PhoneUtils.stripPhone(phone));
    }

    /**
     * Extract mobile code from phone
     * @param phone - phone to extract
     * @return mobile code
     */
    public static String extractMobileCode(String phone) {
        if (phone == null || phone.length() < 10) return null;
        return phone.substring(1, 4);
    }

    /**
     * Strip mobile phone from operator code and country prefix
     * @param phone - phone to transform
     * @return stripped phone
     */
    public static String stripPhoneFromCode(String phone) {
        if (phone == null || phone.length() < 10) return null;
        return phone.substring(4);
    }

    /**
     * Check is phone in international format
     * @param phone - phone to check
     * @return true - if phone is international
     */
    public static boolean isPhoneInInternationalFormat(String phone) {
        return StringUtils.hasText(phone) && phone.matches("\\d{11,15}");
    }

    /**
     * Is phone russian mobilePhone
     * @param phone - phone to check
     * @return true if phone is russian mobile phone
     */
    public static boolean isRussianMobilePhone(String phone) {
        return isPhoneMobile(phone) && phone.matches("79\\d{9}");
    }

    /**
     * Transform received phone to russian format
     * @param phone - phone to transform
     * @return transformed phone
     */
    public static String transformPhoneToRussianFormat(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String transformNumber = phone;
        switch (phone.length()) {
            case 11:
                if (transformNumber.charAt(0) != '7') transformNumber = 7 + transformNumber.substring(1);
                break;
            case 12:
                if (transformNumber.charAt(1) == '8') transformNumber = 7 + transformNumber.substring(2);
                break;
            default:
                break;
        }
        return transformNumber;
    }

    /**
     * Check size of phone, is is to big?
     * @param phone - phone to check
     * @return true - if phone is too big, false - if not
     */
    public static boolean isPhoneTooLongToBeReal(String phone) {
        return phone.length() > 15;
    }

    /**
     * Check size of phone, is is to short?
     * @param phone - phone to check
     * @return true - if phone is too short, false - if not
     */
    public static boolean isPhoneTooShortToBeReal(String phone) {
        return phone.length() < 4;
    }

    /**
     * Split dialing into list
     * assert '#151204*10#1214*' == ['#151204', '*10', '#1214', '*']
     * @param dialingToSplit - dialing needed to be splitted
     * @return splitted dialing
     */
    public static List<String> splitDialing(String dialingToSplit) {
        String str = dialingToSplit;
        List<String> result = new LinkedList<>();

        while (str.contains("#") || str.contains("*")) {
            // if we have one last element and this element is special symbol
            if (str.length() == 1) {
                result.add(str);
                break;
            }

            int nextCharPosition = -1;
            int hashCharPosition = str.substring(1).indexOf('#');
            int starCharPosition = str.substring(1).indexOf('*');

            if (hashCharPosition >= 0 && starCharPosition >= 0) {
                nextCharPosition = hashCharPosition < starCharPosition ? hashCharPosition : starCharPosition;
            } else if (hashCharPosition >= 0) {
                nextCharPosition = hashCharPosition;
            } else if (starCharPosition >= 0) {
                nextCharPosition = starCharPosition;
            }

            // if we have string like this '#1200' or '*1200'
            if (nextCharPosition == -1) {
                result.add(str);
                break;
            }

            result.add(str.substring(0, nextCharPosition + 1));
            str = str.substring(nextCharPosition + 1);
        }

        return result;

    }

    public static boolean dialingContainsCommand(String dialing) {
        return dialing.startsWith("#") || dialing.startsWith("*");
    }

    public static boolean dialingCommandIsСorrect(String dialingCommand) {
        return dialingCommand.matches("\\*\\d{0,2}") ||
                dialingCommand.matches("#\\d{2}") ||
                dialingCommand.matches("#\\d{4}") ||
                dialingCommand.matches("#\\d{6}");
    }

    public static String stripPhoneFromCountryCode(String phone) {
        if (!isPhoneMobile(phone)) return phone;
        return phone.substring(1);
    }

    /**
     * Checks can number be just real, landline or mobile, or short
     * @return true is number looks like phone
     */
    public static boolean isPhoneNumber(String phone) {
        return !isPhoneLiteral(phone) && !isPhoneTooLongToBeReal(phone) && !isPhoneTooShortToBeReal(phone);
    }
}
