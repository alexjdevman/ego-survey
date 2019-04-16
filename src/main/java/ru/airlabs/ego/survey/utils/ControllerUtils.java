package ru.airlabs.ego.survey.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.airlabs.ego.survey.dto.RestResponse;

/**
 * Класс, содержащий вспомогательные методы для Spring mvc-контроллеров
 *
 * @author Aleksey Gorbachev
 */
public class ControllerUtils {

    public static final String GET_USER_IMAGE_REST_ADDRESS = "%s/users/u/%s/v/%s/q/%s/image";
    public static final String GET_COMPANY_HR_IMAGE_REST_ADDRESS = "%s/settings/company/image/%s/%s";

    public static final String USER_INVITATION_SUCCESS_MESSAGE = "Запрос на отправку приглашений успешно отправлен";
    public static final String USER_REGISTRATION_SUCCESS_MESSAGE = "Регистрация прошла успешно. На Ваш email %s было выслано письмо с дальнейшими инструкциями по завершению регистрации";
    public static final String USER_RECOVERY_SUCCESS_MESSAGE = "Запрос на восстановление пароля успешно отправлен. На Ваш email %s было выслано письмо с данными восстановления";
    public static final String USER_CONFIRMATION_SUCCESS_MESSAGE = "Ваш email %s был успешно подтвержден";
    public static final String PASSWORD_CHANGE_SUCCESS_MESSAGE = "Пароль пользователя с email %s успешно изменен";
    public static final String VACANCY_STATUS_CHANGE_SUCCESS_MESSAGE = "Статус вакансии с id %s успешно изменен";
    public static final String SURVEY_STATUS_CHANGE_SUCCESS_MESSAGE = "Статус опроса с id %s успешно изменен";
    public static final String SURVEY_SAVE_SUCCESS_MESSAGE = "Сохранение опроса с id %s успешно выполнено";
    public static final String DEVICE_INFO_SAVE_SUCCESS_MESSAGE = "Данные об устройстве пользователя успешно сохранены";
    public static final String USER_IMAGE_UPLOAD_SUCCESS_MESSAGE = "Фотография пользователя успешно загружена";
    public static final String USER_DETAILS_SAVE_SUCCESS_MESSAGE = "Данные о пользователе с id %s успешно сохранены";
    public static final String USER_DELETED_FROM_VACANCY_SUCCESS_MESSAGE = "Пользователь с id %s удален из вакансии с id %s";
    public static final String USER_UNSUBSCRIBED_SUCCESS_MESSAGE = "Пользователь с id %s и эл. почтой %s успешно отписан от рассылки с id %s";
    public static final String USER_SUBSCRIBED_SUCCESS_MESSAGE = "Пользователь с id %s и эл. почтой %s успешно подписан на рассылку с id %s";
    public static final String COMPANY_HR_IMAGE_UPLOAD_SUCCESS_MESSAGE = "Фото HR компании успешно загружено";
    public static final String CLIENT_ERROR_SAVE_SUCCESS_MESSAGE = "Ошибка клиента с id %s успешно сохранена";
    public static final String SMS_SURVEY_LINK_SENT_SUCCESS_MESSAGE = "Сообщение успешно отправлено";
    public static final String SURVEY_STATE_LOCATION_SUCCESS_MESSAGE = "Геолокация состояния опроса с id %s успешно сохранена";
    public static final String RESUME_DATA_NOT_FOUND_MESSAGE = "Не найдены контакты соискателя";
    public static final String USER_INVITATION_RESPONSE_SUCCESS_MESSAGE = "Пользователь с id %s дал согласие/отказ на приглашение по вакансии с id %s";


    /**
     * Подготовка успешного ответа по выполненной операции
     *
     * @param message сообщение о выполненной операции
     * @return успешный ответ
     */
    public static ResponseEntity<RestResponse> prepareSuccessResponse(String message) {
        RestResponse response = new RestResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage(message);
        response.setSuccess(Boolean.TRUE);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Подготовка ответа по ошибке выполнения операции
     *
     * @param message сообщение о ошибке
     * @return ответ с ошибкой
     */
    public static ResponseEntity<RestResponse> prepareErrorResponse(String message, String url) {
        RestResponse response = new RestResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage(message);
        response.setSuccess(Boolean.FALSE);
        response.setUrl(url);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
