package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.service.SubscriptionService;
import ru.airlabs.ego.survey.service.VacancyService;
import ru.airlabs.ego.survey.utils.EmailUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер подписок
 *
 * @author Roman Kochergin
 */
@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    /**
     * Сервис вакансий
     */
    @Autowired
    private VacancyService vacancyService;

    /**
     * Сервис для работы с подписками
     */
    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Репозиторий пользователя
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Отписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   почта
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/unsubscribe/{eventId}/{userId}/{email}", produces = "application/json")
    public ResponseEntity<RestResponse> unsubscribe(@PathVariable Long eventId,
                                                    @PathVariable Long userId,
                                                    @PathVariable String email) {
        // проверяем почту
        if (!EmailUtils.isValid(email)) {
            return new ResponseEntity<>(NOT_ACCEPTABLE);
        }
        // отписываемся
        subscriptionService.unsubscribe(eventId, userId, email);
        return prepareSuccessResponse(format(USER_UNSUBSCRIBED_SUCCESS_MESSAGE, userId, email, eventId));
    }

    /**
     * Подписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   почта
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/subscribe/{eventId}/{userId}/{email}", produces = "application/json")
    public ResponseEntity<RestResponse> subscribe(@PathVariable Long eventId,
                                                  @PathVariable Long userId,
                                                  @PathVariable String email) {
        // проверяем почту
        if (!EmailUtils.isValid(email)) {
            return new ResponseEntity<>(NOT_ACCEPTABLE);
        }
        // подписываемся
        subscriptionService.subscribe(eventId, userId, email);
        return prepareSuccessResponse(format(USER_SUBSCRIBED_SUCCESS_MESSAGE, userId, email, eventId));
    }

    /**
     * Согласие или отказ на приглашение по вакансии
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @param sc        соль (доп. параметр для проверки пользователя)
     * @param response  состояние приглашения пользователя
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/invitation/response/{vacancyId}/{userId}", produces = "application/json")
    public ResponseEntity<RestResponse> invitationResponse(@PathVariable Long vacancyId,
                                                           @PathVariable Long userId,
                                                           @RequestParam String sc,
                                                           @RequestParam Integer response) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(userId);
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        if (!user.get().getSalt().equalsIgnoreCase(sc)) { // проверяем соль
            return new ResponseEntity<>(NOT_FOUND);
        }
        // устанавливаем статус приглашения на вакансию
        subscriptionService.setInvitationResponse(vacancyId, userId, response);
        return prepareSuccessResponse(format(USER_INVITATION_RESPONSE_SUCCESS_MESSAGE, userId, vacancyId));
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
