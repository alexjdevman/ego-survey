package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.invitation.InvitationStatus;
import ru.airlabs.ego.survey.dto.invitation.InvitationText;
import ru.airlabs.ego.survey.dto.invitation.InvitationType;
import ru.airlabs.ego.survey.dto.survey.SurveyInfo;
import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.dto.vacancy.VacancyForm;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.*;
import ru.airlabs.ego.survey.utils.EmailUtils;
import ru.airlabs.ego.survey.utils.MapUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.OK;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для работы с вакансиями на UI
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/vacancy")
public class VacancyController {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private VacancyDetailService vacancyDetailService;

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private UserService userService;


    /**
     * Получение всех вакансий или исследований
     *
     * @param active         признак активности вакансий или исследований
     * @param vacancy        признак вакансии\исследования
     * @param pageable       настройки пагинации и сортировки
     * @param authentication текущий авторизованный пользователь
     * @return список вакансий и исследований
     */
    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<Vacancy> getVacancyPage(@RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
                                        @RequestParam(value = "vacancy", required = false, defaultValue = "true") Boolean vacancy,
                                        @RequestParam(value = "linked", required = false, defaultValue = "false") Boolean linked,
                                        @PageableDefault(sort = {"dateCreate"}, direction = Sort.Direction.DESC) Pageable pageable,
                                        @AuthenticationPrincipal Authentication authentication) {
        Page<Vacancy> page;
        if (linked) {   // получаем связанные вакансии
            page = vacancyService.getLinkedVacancyPage(authentication.getUser(), active, pageable);
        } else {    // иначе весь список вакансий\исследований
            page = vacancyService.getNotLinkedVacancyPage(authentication.getUser(), active, vacancy, pageable);
        }
        fillVacancyMethodology(page.getContent());
        return page;
    }

    /**
     * Получение данных по вакансии или исследованию
     *
     * @param id             идентификатор вакансии или исследования
     * @param authentication текущий авторизованный пользователь
     * @return данные по вакансии или исследованию
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public VacancyForm getVacancyById(@PathVariable("id") Long id,
                                      @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), id);
        return vacancyService.prepareVacancyForm(id, authentication.getUser());
    }


    /**
     * Получение доступных опросов для пользователя
     *
     * @param authentication текущий авторизованный пользователь
     * @param locale         текущая локаль
     * @return список доступных опросов
     */
    @RequestMapping(value = "/surveys", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<SurveyInfo> getSurveysForCurrentUser(@AuthenticationPrincipal Authentication authentication,
                                                     Locale locale) {
        return surveyService.getAccessibleSurveyListForUser(authentication.getUser().getId(), Boolean.TRUE, locale);
    }

    /**
     * Получение приглашений пользователей на вакансию или исследование
     *
     * @param vacancyId      идентификатор вакансии или исследования
     * @param pageable       настройки пагинации и сортировки
     * @param authentication текущий авторизованный пользователь
     * @return список приглашений
     */
    @RequestMapping(value = "/user/invitation/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<VacancyUserDetail> getInvitationsForVacancy(@PathVariable("id") Long vacancyId,
                                                            @PageableDefault(sort = {"surveyCreate"}, direction = Sort.Direction.DESC, size = 16) Pageable pageable,
                                                            @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), vacancyId);
        return vacancyDetailService.getInvitationsForVacancy(vacancyId, authentication.getUser(), pageable);
    }

    /**
     * Получение приглашений пользователей из буфера на вакансию (у которых пустой способ отправки)
     *
     * @param vacancyId      идентификатор вакансии или исследования
     * @param pageable       настройки пагинации и сортировки
     * @param authentication текущий авторизованный пользователь
     * @return список приглашений из буфера
     */
    @RequestMapping(value = "/user/invitation/buffer/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<VacancyUserDetail> getBufferInvitationsForVacancy(@PathVariable("id") Long vacancyId,
                                                                  @PageableDefault(sort = {"surveyCreate"}, direction = Sort.Direction.DESC, size = 16) Pageable pageable,
                                                                  @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), vacancyId);
        return vacancyDetailService.getBufferInvitationsForVacancy(vacancyId, authentication.getUser(), pageable);
    }

    /**
     * Получение откликов пользователей на вакансию или исследование
     *
     * @param vacancyId      идентификатор вакансии или исследование
     * @param pageable       настройки пагинации и сортировки
     * @param authentication текущий авторизованный пользователь
     * @return список откликов
     */
    @RequestMapping(value = "/user/feedback/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<VacancyUserDetail> getFeedbackForVacancy(@PathVariable("id") Long vacancyId,
                                                         @PageableDefault(sort = {"surveyUpdate"}, direction = Sort.Direction.DESC, size = 16) Pageable pageable,
                                                         @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), vacancyId);
        return vacancyDetailService.getFeedbackForVacancy(vacancyId, authentication.getUser(), pageable);
    }


    /**
     * Добавление вакансии или исследования
     *
     * @param form           данные по вакансии или исследованию
     * @param authentication данные авторизации текущего пользователя
     * @return вакансия или исследование
     */
    @RequestMapping(value = {"/", ""},
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public Vacancy addVacancy(@RequestBody VacancyForm form,
                              @AuthenticationPrincipal Authentication authentication) {
        Vacancy vacancy = vacancyService.addVacancy(form, authentication.getUser());
        if (isNotBlank(form.getLeaderEmail())) {    // если указан лидер
            // получение или создание пользователя лидера
            Long leaderId = vacancyService.getOrCreateLeaderUser(form.getLeaderEmail(), form.getLeaderName(), authentication.getUser());
            // проставляем лидера для вакансии
            vacancyService.fillLeaderUserInVacancy(vacancy.getId(), leaderId);
            if (EmailUtils.isValid(form.getLeaderEmail())) {    // приглашаем по Email
                vacancyService.sendInvitationToLeader(leaderId, authentication.getUser(), vacancy.getId(), "L");
            } else {    // приглашаем в WhatsApp
                vacancyService.sendInvitationToLeader(leaderId, authentication.getUser(), vacancy.getId(), "L");
            }
        }
        return vacancy;
    }

    /**
     * Редактирование вакансии или исследования
     *
     * @param form           данные по вакансии или исследованию
     * @param authentication данные авторизации текущего пользователя
     * @return вакансия или исследование
     */
    @RequestMapping(value = {"/", ""},
            method = RequestMethod.PUT,
            produces = "application/json")
    @ResponseBody
    public Vacancy updateVacancy(@RequestBody VacancyForm form,
                                 @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), form.getId());
        if (form.getVacancy()) {    // для вакансий
            Vacancy vacancy = vacancyService.updateVacancy(form, authentication.getUser());
            if (isNotBlank(form.getLeaderEmail())) {    // если указан лидер
                // получение или создание пользователя лидера
                Long leaderId = vacancyService.getOrCreateLeaderUser(form.getLeaderEmail(), form.getLeaderName(), authentication.getUser());
                // проставляем лидера для вакансии
                vacancyService.fillLeaderUserInVacancy(vacancy.getId(), leaderId);
                if (EmailUtils.isValid(form.getLeaderEmail())) {    // приглашаем по Email
                    vacancyService.sendInvitationToLeader(leaderId, authentication.getUser(), vacancy.getId(), "L");
                } else {    // приглашаем в WhatsApp
                    vacancyService.sendInvitationToLeader(leaderId, authentication.getUser(), vacancy.getId(), "L");
                }
            }
            return vacancy;
        } else {
            return vacancyService.updateResearch(form, authentication.getUser());
        }
    }

    /**
     * Удаление вакансии или исследования
     *
     * @param id             идентификатор вакансии или исследования
     * @param authentication текущий авторизованный пользователь
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Vacancy> deleteVacancy(@PathVariable("id") Long id,
                                                 @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), id);
        vacancyService.deleteVacancy(id, authentication.getUser());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Деактивация вакансии или исследования (активация для неактивной вакансии)
     *
     * @param id             идентификатор вакансии или исследования
     * @param authentication текущий авторизованный пользователь
     */
    @RequestMapping(value = "/deactivate/{id}", method = RequestMethod.POST)
    public ResponseEntity<RestResponse> deactivateVacancy(@PathVariable("id") Long id,
                                                          @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), id);
        vacancyService.deactivateVacancy(id);
        return prepareSuccessResponse(format(VACANCY_STATUS_CHANGE_SUCCESS_MESSAGE, id));
    }

    /**
     * Отправка приглашений на вакансию
     *
     * @param vacancyId      идентификатор вакансии
     * @param source         метод отправки приглашений
     * @param userDetails    список приглашаемых на вакансию
     * @param authentication данные авторизации текущего пользователя
     * @return результат отправки приглашений
     */
    @RequestMapping(value = "/invite/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> inviteUsers(@PathVariable("vacancyId") Long vacancyId,
                                                    @RequestParam(value = "src", defaultValue = "L") String source,
                                                    @RequestBody Set<UserDetail> userDetails,
                                                    @AuthenticationPrincipal Authentication authentication) {
        Set<Invitation> inv = new HashSet<>();
        userDetails.forEach(i -> {
            Long receiverId = userService.getOrCreateUser(i.getName(), i.getEmail(), i.getPhone(), authentication.getUser().getId());
            inv.add(new Invitation(receiverId, source));
        });
        userInvitationService.sendInvitations(inv, authentication.getUser(), vacancyId);
        return prepareSuccessResponse(USER_INVITATION_SUCCESS_MESSAGE);
    }

    /**
     * Отправка приглашения на вакансию в WhatsApp
     *
     * @param vacancyId      идентификатор вакансии
     * @param source         метод отправки приглашений
     * @param userDetail     приглашаемый на вакансию
     * @param authentication данные авторизации текущего пользователя
     * @return настройки текущего аккаунта HR
     */
    @RequestMapping(value = "/inviteInWhatsApp/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inviteUserInWhatsApp(@PathVariable("vacancyId") Long vacancyId,
                                                                    @RequestParam(value = "src", defaultValue = "L") String source,
                                                                    @RequestBody UserDetail userDetail,
                                                                    @AuthenticationPrincipal Authentication authentication) {
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), authentication.getUser().getId());
        Invitation inv = new Invitation(receiverId, source);
        InvitationText inviteText = userInvitationService.createInvitationText(inv,
                authentication.getUser(),
                vacancyId,
                InvitationType.WHATS_APP);
        return new ResponseEntity<>(MapUtils.<String, Object>builder("inviteText", inviteText.getText()).build(), OK);
    }

    /**
     * Получение текста приглашения на вакансию
     *
     * @param vacancyId      идентификатор вакансии
     * @param source         метод отправки приглашений (по умолчанию L - отправка из ЛК)
     * @param type           тип приглашения (по умолчанию U - Universal - получить текст и как то его потом отправить)
     * @param userDetail     приглашаемый на вакансию
     * @param authentication данные авторизации текущего пользователя
     * @return настройки текущего аккаунта HR
     */
    @RequestMapping(value = "/createInvitationText/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createInvitationText(@PathVariable("vacancyId") Long vacancyId,
                                                                    @RequestParam(value = "src",
                                                                            required = false,
                                                                            defaultValue = "L") String source,
                                                                    @RequestParam(value = "type",
                                                                            required = false,
                                                                            defaultValue = "U") String type,
                                                                    @RequestBody UserDetail userDetail,
                                                                    @AuthenticationPrincipal Authentication authentication) {
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), authentication.getUser().getId());
        Invitation inv = new Invitation(receiverId, source);
        InvitationText inviteText = userInvitationService.createInvitationText(inv, // получаем текст приглашения
                authentication.getUser(),
                vacancyId,
                InvitationType.getInvitationTypeById(type));
        // получаем ссылку на персональный опрос
        String inviteSurveyLink = userInvitationService.getSurveyLinkForUserAndVacancy(receiverId, vacancyId);
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("inviteText", inviteText.getText())
                .add("inviteSurveyLink", inviteSurveyLink)
                .build(), OK);
    }

    /**
     * Установить статус отправки приглашения
     *
     * @param invitationId идентификатор приглашения, @see ru.airlabs.ego.root.dto.invitation.InvitationText#id
     * @param status       статус отправки приглашения
     * @return ответ
     */
    @RequestMapping(value = "/setInvitationStatus/{invitationId}/{status}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> setInvitationStatus(@PathVariable("invitationId") Long invitationId,
                                                 @PathVariable("status") InvitationStatus status) {
        userInvitationService.setInvitationStatus(invitationId, status);
        return new ResponseEntity<>(OK);
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

    /**
     * Заполнение методологии для вакансий и исследований
     *
     * @param vacancies список вакансий и исследований
     */
    private void fillVacancyMethodology(List<Vacancy> vacancies) {
        for (Vacancy vacancy : vacancies) {
            vacancy.setMethodology(vacancyService.detectMethodology(vacancy.getSurveyId()).name());
        }
    }

}
