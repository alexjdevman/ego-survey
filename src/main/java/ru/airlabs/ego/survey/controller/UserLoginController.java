package ru.airlabs.ego.survey.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.user.UserPasswordChange;
import ru.airlabs.ego.survey.dto.user.UserRecovery;
import ru.airlabs.ego.survey.dto.user.UserRegistration;
import ru.airlabs.ego.survey.exception.InvalidCaptchaException;
import ru.airlabs.ego.survey.service.UserLoginService;
import ru.airlabs.ego.survey.service.UserNotificationService;
import ru.airlabs.ego.survey.service.impl.CaptchaService;

import javax.servlet.http.HttpServletRequest;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для регистрации и авторизации пользователей на UI
 *
 * @author Aleksey Gorbachev
 */
@Controller
public class UserLoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginController.class);

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private UserNotificationService notificationService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * Регистрация нового пользователя в системе
     *
     * @param registration данные из формы регистрации
     * @param request      запрос
     * @return JSON ответ
     */
    @RequestMapping(value = "/registration",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> userRegistration(@RequestBody UserRegistration registration,
                                                         HttpServletRequest request) {
        if (!captchaService.validate(registration.getCaptcha(), request.getSession())) {
            throw new InvalidCaptchaException("Неверное значение капчи");
        }
        User user = userLoginService.registerUser(registration);
        notificationService.notifyUserForRegistration(user, null);
        return prepareSuccessResponse(format(USER_REGISTRATION_SUCCESS_MESSAGE, registration.getEmail()));
    }

    /**
     * Подтверждение регистрации пользователя
     *
     * @param email имейл пользователя
     * @param key   ключ пользователя
     * @return JSON ответ
     */
    @RequestMapping(value = "/confirmation",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> confirmRegistration(@RequestParam(value = "email") String email,
                                                            @RequestParam(value = "key") String key) {
        userLoginService.confirmUserRegistration(email, key);
        return prepareSuccessResponse(format(USER_CONFIRMATION_SUCCESS_MESSAGE, email));
    }


    /**
     * Запрос на восстановление пароля пользователя
     *
     * @param recovery имейл пользователя + CAPTCHA код
     * @param request  запрос
     * @return JSON ответ
     */
    @RequestMapping(value = "/password_recovery",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> userPasswordRecovery(@RequestBody UserRecovery recovery,
                                                             HttpServletRequest request) {
        if (!captchaService.validate(recovery.getCaptcha(), request.getSession())) {
            throw new InvalidCaptchaException("Неверное значение капчи");
        }
        notificationService.notifyUserForPasswordRecovery(recovery.getEmail());
        return prepareSuccessResponse(format(USER_RECOVERY_SUCCESS_MESSAGE, recovery.getEmail()));
    }

    /**
     * Изменение пароля пользователя
     *
     * @param passwordChange форма восстановления пароля
     * @return JSON ответ
     */
    @RequestMapping(value = "/password_change",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> userPasswordChange(@RequestBody UserPasswordChange passwordChange) {
        userLoginService.changePassword(passwordChange);
        return prepareSuccessResponse(format(PASSWORD_CHANGE_SUCCESS_MESSAGE, passwordChange.getEmail()));
    }


    /**
     * Переход на страницу изменения пароля
     *
     * @param email имейл пользователя
     * @param key   ключ (md5 hash старого пароля пользователя)
     * @param model модель
     * @return страница изменения пароля
     */
    @RequestMapping(value = "/passwordRecoveryPage", method = RequestMethod.GET)
    public String passwordRecoveryPage(@RequestParam(value = "email") String email,
                                       @RequestParam(value = "key") String key,
                                       Model model) {
        model.addAttribute("email", email);
        model.addAttribute("key", key);
        return "passwordRecovery";
    }


    /**
     * Обработчик ошибок
     *
     * @param request   http запрос
     * @param exception исключение
     * @return информация об ошибке
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<RestResponse> exceptionHandler(HttpServletRequest request, Exception exception) {
        final String message = isNotBlank(exception.getMessage()) ?
                exception.getMessage() :
                exception.getCause().getMessage();
        final String url = request.getRequestURL().toString();
        return prepareErrorResponse(message, url);
    }

}
