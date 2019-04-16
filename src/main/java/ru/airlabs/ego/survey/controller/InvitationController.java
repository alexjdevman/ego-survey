package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.invitation.InvitationType;
import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.exception.InvalidCaptchaException;
import ru.airlabs.ego.survey.service.UserInvitationService;
import ru.airlabs.ego.survey.service.UserService;
import ru.airlabs.ego.survey.service.VacancyService;
import ru.airlabs.ego.survey.service.impl.CaptchaService;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static ru.airlabs.ego.survey.utils.ControllerUtils.prepareErrorResponse;
import static ru.airlabs.ego.survey.utils.ControllerUtils.prepareSuccessResponse;

/**
 * Контроллер отправки приглашений
 *
 * @author Aleksey Gorbachev
 */
@RestController
@RequestMapping("/invitation")
public class InvitationController {

    /**
     * Сервис вакансий
     */
    @Autowired
    private VacancyService vacancyService;

    /**
     * Сервис приглашений
     */
    @Autowired
    private UserInvitationService userInvitationService;

    /**
     * Сервис пользователей
     */
    @Autowired
    private UserService userService;

    /**
     * Репозиторий пользователя
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Сервис капчи
     */
    @Autowired
    private CaptchaService captchaService;

    /**
     * Отправка приглашений на вакансию
     *
     * @param vacancyId  идентификатор вакансии
     * @param source     метод отправки приглашений (по умолчанию L - отправка из ЛК)
     * @param userDetail приглашаемый на вакансию
     * @param audio      признак отправки аудио-приглашения
     * @param email      признак отправки email-приглашения
     * @param wa         признак отправки WhatsApp-приглашения
     * @param captcha    капча
     * @param token      токен капчи
     * @return результат
     */
    @RequestMapping(value = "/v/{vacancyId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> sendInvitations(@PathVariable("vacancyId") Long vacancyId,
                                                        @RequestParam(value = "src", required = false, defaultValue = "L") String source,
                                                        @RequestBody UserDetail userDetail,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean audio,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean email,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean wa,
                                                        @RequestParam String captcha,
                                                        @RequestParam String token) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // проверяем капчу
        if (!captchaService.validate(captcha, token)) {
            throw new InvalidCaptchaException("Неверное значение капчи");
        }
        User manager = userRepository.findById(vacancy.getManagerId()).get();
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), manager.getId());
        Invitation inv = new Invitation(receiverId, source);
        if (audio) {
            userInvitationService.createInvitationText(inv, manager, vacancyId, InvitationType.AUDIO);
        }
        if (email) {
            userInvitationService.createInvitationText(inv, manager, vacancyId, InvitationType.EMAIL);
        }
        if (wa) {
            userInvitationService.createInvitationText(inv, manager, vacancyId, InvitationType.WHATS_APP);
        }
        return prepareSuccessResponse("Приглашения успешно отправлены");
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
