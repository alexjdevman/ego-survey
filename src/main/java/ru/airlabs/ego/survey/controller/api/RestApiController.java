package ru.airlabs.ego.survey.controller.api;

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
import ru.airlabs.ego.survey.service.UserInvitationService;
import ru.airlabs.ego.survey.service.UserService;
import ru.airlabs.ego.survey.service.VacancyService;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static ru.airlabs.ego.survey.utils.ControllerUtils.prepareErrorResponse;
import static ru.airlabs.ego.survey.utils.ControllerUtils.prepareSuccessResponse;

/**
 * Контроллер для внешнего REST API
 *
 * @author Aleksey Gorbachev
 */
@RestController
@RequestMapping("/api")
public class RestApiController {

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
     * Отправка приглашений на вакансию
     *
     * @param vacancyId  идентификатор вакансии
     * @param source     метод отправки приглашений (по умолчанию D - отправка извне)
     * @param userDetail приглашаемый на вакансию
     * @param audio      признак отправки аудио-приглашения
     * @param email      признак отправки email-приглашения
     * @param wa         признак отправки WhatsApp-приглашения
     * @param key        API ключ
     * @return результат
     */
    @RequestMapping(value = "/invitation/v/{vacancyId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> sendInvitations(@PathVariable("vacancyId") Long vacancyId,
                                                        @RequestParam(value = "src", required = false, defaultValue = "D") String source,
                                                        @RequestBody UserDetail userDetail,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean audio,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean email,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean wa,
                                                        @RequestParam String key) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя - владельца вакансии
        User user = userRepository.findByApiKey(key);
        if (user == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        if (!user.getId().equals(vacancy.getManagerId())) {
            return new ResponseEntity<>(NOT_ACCEPTABLE);
        }
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), user.getId());
        Invitation inv = new Invitation(receiverId, source);
        if (audio) {
            userInvitationService.createInvitationText(inv, user, vacancyId, InvitationType.AUDIO);
        }
        if (email) {
            userInvitationService.createInvitationText(inv, user, vacancyId, InvitationType.EMAIL);
        }
        if (wa) {
            userInvitationService.createInvitationText(inv, user, vacancyId, InvitationType.WHATS_APP);
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
