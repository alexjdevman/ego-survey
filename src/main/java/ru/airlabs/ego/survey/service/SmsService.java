package ru.airlabs.ego.survey.service;

/**
 * Сервис для работы с СМС
 *
 * @author Roman Kochergin
 */
public interface SmsService {

    /**
     * Результат отправки смс
     */
    static enum SmsSendResult {

        /**
         * Сообщение успешно отправлено
         */
        OK("OK"),

        /**
         * Не задан номер телефона
         */
        PHONE_ERROR("ERR_PHONE"),

        /**
         * Получатель не найден
         */
        RECEIVER_ERROR("ERR_RECIEVER"),

        /**
         * Сообщение не прошло спам-фильтр
         */
        SPAM_FILTER_ERROR("ERR_SPAMFILTER"),

        /**
         * Передан неподдерживаемый шаблон
         */
        TEMPLATE_ERROR("ERR_TEMPLATE"),

        /**
         * Ошибка при заполнении параметров шаблона сообщения
         */
        PARAMS_ERROR("ERR_PARAMS"),

        /**
         * Результирующее сообщение пустое
         */
        EMPTY_ERROR("ERR_EMPTY"),

        /**
         * Общая ошибка
         */
        ERROR("ERR");

        /**
         * Код сообщения
         */
        private String code;

        /**
         * Конструктор
         *
         * @param code код сообщения
         */
        SmsSendResult(String code) {
            this.code = code;
        }

        /**
         * Найти значение по коду сообщения
         *
         * @param code код сообщения
         * @return результат отправки смс
         */
        public static SmsSendResult findByCode(String code) {
            for (SmsSendResult result : values()) {
                if (result.code.equals(code)) {
                    return result;
                }
            }
            return null;
        }
    }

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
    SmsSendResult send(Long senderId, Long receiverId, String templateId, String phone, Long vacancyId, Long surveyId);

}
