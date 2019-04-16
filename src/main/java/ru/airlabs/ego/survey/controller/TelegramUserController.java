package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.entity.user.UserSocial;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.core.repository.user.UserSocialRepository;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.invitation.InvitationText;
import ru.airlabs.ego.survey.dto.invitation.InvitationType;
import ru.airlabs.ego.survey.dto.settings.AccountSettings;
import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.dto.user.UserRegistration;
import ru.airlabs.ego.survey.security.SocialAuthentication;
import ru.airlabs.ego.survey.service.*;
import ru.airlabs.ego.survey.service.social.TelegramUserService;
import ru.airlabs.ego.survey.utils.MapUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для работы с пользователями Telegram
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/tg")
public class TelegramUserController {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private TelegramUserService telegramUserService;

    @Autowired
    private UserNotificationService notificationService;

    @Autowired
    private AccountSettingsService settingsService;

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialRepository userSocialRepository;


    /**
     * Регистрация нового пользователя Telegram в системе
     *
     * @param registration данные из формы регистрации
     * @return JSON ответ
     */
    @RequestMapping(value = "/registration", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> userTelegramRegistration(@RequestBody UserRegistration registration) {
        User user = telegramUserService.registerOwnerUser(registration);
        notificationService.notifyUserForRegistration(user, "TELEGRAM");
        return prepareSuccessResponse(format(USER_REGISTRATION_SUCCESS_MESSAGE, registration.getEmail()));
    }

    /**
     * Получение постранично всех вакансий или исследований для пользователя Telegram
     *
     * @param active         признак активности вакансий или исследований
     * @param vacancy        признак вакансии\исследования
     * @param pageable       настройки пагинации и сортировки
     * @param authentication текущий авторизованный пользователь Telegram
     * @return постраничный список вакансий и исследований
     */
    @RequestMapping(value = "/vacancy", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<Vacancy> getVacancyPage(@RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
                                        @RequestParam(value = "vacancy", required = false, defaultValue = "true") Boolean vacancy,
                                        @RequestParam(value = "linked", required = false, defaultValue = "false") Boolean linked,
                                        @PageableDefault(sort = {"dateCreate"}, direction = Sort.Direction.DESC) Pageable pageable,
                                        @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        Page<Vacancy> page;
        if (linked) {   // получаем связанные вакансии
            page = vacancyService.getLinkedVacancyPage(ownerUser, active, pageable);
        } else {    // иначе весь список вакансий\исследований
            page = vacancyService.getNotLinkedVacancyPage(ownerUser, active, vacancy, pageable);
        }
        fillVacancyMethodology(page.getContent());
        return page;
    }

    /**
     * Получение всех вакансий или исследований для пользователя Telegram
     *
     * @param active         признак активности вакансий или исследований
     * @param vacancy        признак вакансии\исследования
     * @param authentication текущий авторизованный пользователь Telegram
     * @return список вакансий и исследований
     */
    @RequestMapping(value = "/vacancy/list", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Vacancy> getVacancyList(@RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
                                        @RequestParam(value = "vacancy", required = false, defaultValue = "true") Boolean vacancy,
                                        @RequestParam(value = "linked", required = false, defaultValue = "false") Boolean linked,
                                        @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        List<Vacancy> list;
        if (linked) {   // получаем список связанных вакансий
            list = vacancyService.getLinkedVacancyList(ownerUser, active);
        } else {    // иначе весь список вакансий\исследований
            list = vacancyService.getNotLinkedVacancyList(ownerUser, active, vacancy);
        }
        fillVacancyMethodology(list);
        return list;
    }

    /**
     * Получение настроек ЛК для текущего пользователя Telegram (владельца аккаунта)
     *
     * @param authentication текущий авторизованный пользователь
     * @return настройки ЛК пользователя
     */
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public AccountSettings getAccountSettings(@AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        return settingsService.getAccountSettings(ownerUser);
    }

    /**
     * Сохранение настроек ЛК для текущего пользователя Telegram (владельца аккаунта)
     *
     * @param settings       настройки пользователя
     * @param authentication текущий авторизованный пользователь
     * @return настройки ЛК пользователя
     */
    @RequestMapping(value = "/settings", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> saveAccountSettings(@RequestBody AccountSettings settings,
                                                            @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        settingsService.saveAccountSettings(settings, ownerUser);
        return prepareSuccessResponse("Настройки пользователя успешно сохранены");
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
    @RequestMapping(value = "/vacancy/invite/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> inviteUsers(@PathVariable("vacancyId") Long vacancyId,
                                                    @RequestParam(value = "src", defaultValue = "L") String source,
                                                    @RequestBody Set<UserDetail> userDetails,
                                                    @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        Set<Invitation> inv = new HashSet<>();
        userDetails.forEach(i -> {
            Long receiverId = userService.getOrCreateUser(i.getName(), i.getEmail(), i.getPhone(), ownerUser.getId());
            inv.add(new Invitation(receiverId, source));
        });
        userInvitationService.sendInvitations(inv, ownerUser, vacancyId);
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
    @RequestMapping(value = "/vacancy/inviteInWhatsApp/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inviteUserInWhatsApp(@PathVariable("vacancyId") Long vacancyId,
                                                                    @RequestParam(value = "src", defaultValue = "L") String source,
                                                                    @RequestBody UserDetail userDetail,
                                                                    @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), ownerUser.getId());
        Invitation inv = new Invitation(receiverId, source);
        InvitationText inviteText = userInvitationService.createInvitationText(inv,
                ownerUser,
                vacancyId,
                InvitationType.WHATS_APP);
        return new ResponseEntity<>(MapUtils.<String, Object>builder("inviteText", inviteText.getText()).build(), OK);
    }

    /**
     * Получение текста приглашения на вакансию
     *
     * @param vacancyId      идентификатор вакансии
     * @param source         метод отправки приглашений (по умолчанию L - отправка из ЛК)
     * @param email          признак отправки Email-приглашения (по умолчанию false)
     * @param audio          признак отправки Audio-приглашения (по умолчанию false)
     * @param userDetail     приглашаемый на вакансию
     * @param authentication данные авторизации текущего пользователя
     * @return настройки текущего аккаунта HR
     */
    @RequestMapping(value = "/vacancy/createInvitationText/{vacancyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createInvitationText(@PathVariable("vacancyId") Long vacancyId,
                                                                    @RequestParam(value = "src",
                                                                            required = false,
                                                                            defaultValue = "L") String source,
                                                                    @RequestParam(value = "email",
                                                                            required = false,
                                                                            defaultValue = "false") Boolean email,
                                                                    @RequestParam(value = "audio",
                                                                            required = false,
                                                                            defaultValue = "false") Boolean audio,
                                                                    @RequestBody UserDetail userDetail,
                                                                    @AuthenticationPrincipal SocialAuthentication authentication) {
        // получаем владельца аккаунта Telegram
        User ownerUser = userRepository.findById(authentication.getUser().getOwnerId()).get();
        Long receiverId = userService.getOrCreateUser(userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), ownerUser.getId());
        Invitation inv = new Invitation(receiverId, source);
        InvitationText inviteText = userInvitationService.createInvitationText(inv, // отправляем и получаем текст приглашения WhatsApp
                ownerUser,
                vacancyId,
                InvitationType.WHATS_APP);
        if (email) {
            userInvitationService.createInvitationText(inv, // отправляем Email-приглашения
                    ownerUser,
                    vacancyId,
                    InvitationType.EMAIL);
        }
        if (audio) {
            userInvitationService.createInvitationText(inv, // отправляем аудио-приглашения
                    ownerUser,
                    vacancyId,
                    InvitationType.AUDIO);
        }
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("inviteText", inviteText.getText())
                .add("success", true)
                .build(), OK);
    }

    /**
     * Получение списка пользователей, доступных текущему пользователю-владельцу аккаунта Telegram
     *
     * @param authentication данные авторизации текущего пользователя
     * @param active         признак активности пользователей
     * @return данные по пользователям в JSON
     */
    @RequestMapping(value = "/users/list", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTelegramUserList(@RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
                                                                         @AuthenticationPrincipal SocialAuthentication authentication) {
        return new ResponseEntity<>(telegramUserService.getTelegramUsersForOwner(authentication.getUser().getOwnerId(), active), OK);
    }

    /**
     * Добавление нового пользователя из Telegram
     *
     * @param name           имя пользователя в Telegram
     * @param authentication данные авторизации текущего пользователя
     * @return данные по новому пользователю в JSON
     */
    @RequestMapping(value = "/users/add", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addTelegramUser(@RequestParam(value = "name") String name,
                                                               @AuthenticationPrincipal SocialAuthentication authentication) {
        final String password = telegramUserService.generateTelegramPassword(6);
        UserSocial userSocial = telegramUserService.addTelegramUser(authentication.getUser().getOwnerId(), name, password);
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                // Идентификатор для дальнейшей авторизации через TELEGRAM
                .add("socialUserId", userSocialRepository.findById(userSocial.getId()).get().getSocialUserId())
                .add("password", password)  // Пароль
                .build(), OK);
    }

    /**
     * Деактивация\активация пользователя из Telegram
     *
     * @param socialUserId   идентификатор внешней авторизации пользователя из TELEGRAM
     * @param authentication данные авторизации текущего пользователя
     * @return результат в JSON
     */
    @RequestMapping(value = "/users/deactivate", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> deactivateTelegramUser(@RequestParam(value = "socialUserId") String socialUserId,
                                                               @AuthenticationPrincipal SocialAuthentication authentication) {
        UserSocial userSocial = userSocialRepository.findBySocialUserIdAndOwnerId(socialUserId, authentication.getUser().getOwnerId());
        if (userSocial == null) {
            return new ResponseEntity<>(NOT_FOUND);
        } else {
            userSocial.setActive(!userSocial.getActive());
            userSocialRepository.save(userSocial);
            return prepareSuccessResponse(String.format("Пользователь с идентификатором %s успешно деактивирован", socialUserId));
        }
    }

    /**
     * Удаление пользователя Telegram
     *
     * @param socialUserId   идентификатор внешней авторизации пользователя из TELEGRAM
     * @param authentication данные авторизации текущего пользователя
     * @return результат в JSON
     */
    @RequestMapping(value = "/users/delete", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> deleteTelegramUser(@RequestParam(value = "socialUserId") String socialUserId,
                                                           @AuthenticationPrincipal SocialAuthentication authentication) {
        UserSocial userSocial = userSocialRepository.findBySocialUserIdAndOwnerId(socialUserId, authentication.getUser().getOwnerId());
        if (userSocial == null) {
            return new ResponseEntity<>(NOT_FOUND);
        } else {
            userSocialRepository.delete(userSocial);
            return prepareSuccessResponse(String.format("Пользователь с идентификатором %s успешно удален", socialUserId));
        }
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
