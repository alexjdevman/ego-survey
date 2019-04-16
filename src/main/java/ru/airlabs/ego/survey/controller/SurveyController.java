package ru.airlabs.ego.survey.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.*;
import ru.airlabs.ego.core.repository.*;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.device.DeviceInfo;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.survey.*;
import ru.airlabs.ego.survey.dto.vacancy.VacancyMethodology;
import ru.airlabs.ego.survey.exception.InvalidCaptchaException;
import ru.airlabs.ego.survey.exception.SmsException;
import ru.airlabs.ego.survey.service.*;
import ru.airlabs.ego.survey.service.SmsService.SmsSendResult;
import ru.airlabs.ego.survey.service.impl.CaptchaService;
import ru.airlabs.ego.survey.utils.GzipUtils;
import ru.airlabs.ego.survey.utils.MapUtils;
import ru.airlabs.ego.survey.utils.PhoneUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;
import static ru.airlabs.ego.survey.utils.EmailUtils.isValid;
import static ru.airlabs.ego.survey.utils.NameUtils.extractFirstNameFromFIO;

/**
 * Контроллер прохождения опросов
 *
 * @author Roman Kochergin
 */
@RestController
@RequestMapping("/survey")
public class SurveyController {

    /**
     * Сервис вакансий
     */
    @Autowired
    private VacancyService vacancyService;

    /**
     * Репозиторий состояния прохождения опроса пользователем
     */
    @Autowired
    private UserSurveyStateRepository userSurveyStateRepository;

    /**
     * Репозиторий пользователя
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий связей пользователей
     */
    @Autowired
    private UserLinkRepository userLinkRepository;

    /**
     * Репозиторий пользователя вакансии
     */
    @Autowired
    private VacancyUserRepository vacancyUserRepository;

    /**
     * Сервис опроса
     */
    @Autowired
    private SurveyService surveyService;

    /**
     * Сервис данных по устройствам пользователей
     */
    @Autowired
    private UserDeviceInfoService deviceInfoService;

    /**
     * Репозиторий организаций
     */
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Репозиторий адресов компаний
     */
    @Autowired
    private OrganizationLocationRepository locationRepository;

    /**
     * Сервис для приглашения участников для прохождения опроса
     */
    @Autowired
    private UserInvitationService userInvitationService;

    /**
     * Сервис для загрузки фотографий пользователей
     */
    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Сервис капчи
     */
    @Autowired
    private CaptchaService captchaService;

    /**
     * Сервис смс
     */
    @Autowired
    private SmsService smsService;

    /**
     * Сервис настроек
     */
    @Autowired
    private AccountSettingsService accountSettingsService;

    /**
     * Сервис работы с пользователями
     */
    @Autowired
    private UserService userService;

    /**
     * Веб-адрес приложения
     */
    @Value("${application.url}")
    private String appUrl;

    /**
     * Отправить ссылку на прохождение опроса по смс
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param phone         телефон
     * @param captcha       капча
     * @param token         токен капчи
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/v/{vacancyId}/{surveyStateId}/sms/sendSurveyLink", produces = "application/json")
    public ResponseEntity<RestResponse> sendSurveyLinkBySms(@PathVariable Long vacancyId,
                                                            @PathVariable Long surveyStateId,
                                                            @RequestParam String phone,
                                                            @RequestParam String captcha,
                                                            @RequestParam String token) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // проверяем телефон
        if (!PhoneUtils.isPhoneNumber(phone) || !PhoneUtils.isPhoneMobile(phone)) {
            throw new InvalidCaptchaException("Некорректный номер мобильного телефона");
        }
        // проверяем капчу
        if (!captchaService.validate(captcha, token)) {
            throw new InvalidCaptchaException("Неверное значение капчи");
        }
        // пытаемся отправить смс
        SmsSendResult result = smsService.send(vacancy.getManagerId(), state.get().getUserId(),
                "WARN_DESKTOP", phone, vacancyId, state.get().getSurveyId());
        // обрабатываем результат
        switch (result) {
            case OK:
                return prepareSuccessResponse(SMS_SURVEY_LINK_SENT_SUCCESS_MESSAGE);
            case PHONE_ERROR:
                throw new SmsException("Не задан номер телефона");
            case RECEIVER_ERROR:
                throw new SmsException("Получатель не найден");
            case SPAM_FILTER_ERROR:
                throw new SmsException("Сообщение не прошло спам-фильтр");
            case TEMPLATE_ERROR:
                throw new SmsException("Передан неподдерживаемый шаблон");
            case PARAMS_ERROR:
                throw new SmsException("Ошибка при заполнении параметров шаблона сообщения");
            case EMPTY_ERROR:
                throw new SmsException("Результирующее сообщение пустое");
            case ERROR:
                throw new SmsException("Ошибка при отправке сообщения");
            default:
                throw new SmsException("Неизвестный статус отправки смс-сообщения");
        }
    }

    /**
     * Получить информацию об опросе по вакансии и статусу прохождения опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param sc            соль (доп. параметр для проверки факта прохождение пользователем опроса)
     * @param locale        локаль
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{vacancyId}/{surveyStateId}", produces = "application/json")
    public ResponseEntity<Map> getSurveyDetailsByVacancyAndState(@PathVariable Long vacancyId,
                                                                 @PathVariable Long surveyStateId,
                                                                 @RequestParam String sc,
                                                                 Locale locale) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        final VacancyMethodology methodology = vacancyService.detectMethodology(vacancy.getSurveyId());
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        if (!user.get().getSalt().equalsIgnoreCase(sc)) { // проверяем соль
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем данные пользователя
        UserLink userLink = userLinkRepository.findByUserIdAndParentId(user.get().getId(), vacancy.getManagerId());
        if (userLink == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя вакансии (для определния типа приглашения пользователя)
        VacancyUser vacancyUser = vacancyUserRepository.findByUserIdAndVacancyId(user.get().getId(), vacancyId);
        if (vacancyUser == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // находим организацию
        Organization organization = organizationRepository.findByManagerId(vacancy.getManagerId());
        // получаем адрес организации
        String address = null;
        boolean imageExists = false;
        if (organization != null) {
            OrganizationLocation orgLocation = locationRepository.findByManagerIdAndCompanyId(organization.getManagerId(),
                    organization.getId());

            address = orgLocation != null ? orgLocation.getTitle() : null;
            imageExists = accountSettingsService.isCompanyImageExists(organization.getId());
        }

        // признак того, что участник проходит опрос впервые
        final Boolean firstVisit = !deviceInfoService.existsDeviceInfo(user.get().getId(), state.get().getSurveyId());
        // возвращаем результат
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("user", MapUtils.<String, Object>builder()
                        .add("id", user.get().getId())
                        .add("name", isNotBlank(userLink.getFirstName()) ?
                                userLink.getFirstName() : extractFirstNameFromFIO(userLink.getName()))
                        .add("phone", isNotBlank(user.get().getPhone()))
                        .add("email", isNotBlank(user.get().getEmail()))
                        .build())
                .add("organization", MapUtils.<String, Object>builder()
                        .add("id", organization != null ? organization.getId() : null)
                        .add("name", organization != null ? organization.getName() : null)
                        .add("description", organization != null ? organization.getDescription() : null)
                        .add("address", address)
                        .add("vacancyName", vacancy.getName())
                        .add("vacancySalary", vacancy.getSalary())
                        .add("vacancyBonus", vacancy.getBonus())
                        .add("image", imageExists ? format(GET_COMPANY_HR_IMAGE_REST_ADDRESS, appUrl, organization.getId(), Boolean.TRUE.toString()) : null)
                        .build())
                .add("survey", MapUtils.<String, Object>builder()
                        .add("id", state.get().getSurveyId())
                        .add("vacancy", vacancy.getVacancy())
                        .add("methodology", methodology)
                        .build())
                .add("mobileOnly", vacancy.getMobile())
                .add("firstVisit", firstVisit)
                .add("leader", user.get().getId().equals(vacancy.getLeaderId()))
                .add("inviteSource", vacancyUser.getSourceCode())
                .build(), OK);
    }

    /**
     * Получить список вопросов по вакансии и статусу прохождения опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param sc            соль (доп. параметр для проверки факта прохождение пользователем опроса)
     * @param max           макс. кол-во сущностей
     * @param locale        локаль
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{vacancyId}/{surveyStateId}/questions", produces = "application/json")
    public ResponseEntity<Map> getQuestionsByVacancyAndState(@PathVariable Long vacancyId,
                                                             @PathVariable Long surveyStateId,
                                                             @RequestParam String sc,
                                                             @RequestParam(required = false) Integer max,
                                                             Locale locale) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        if (!user.get().getSalt().equalsIgnoreCase(sc)) { // проверяем соль
            return new ResponseEntity<>(NOT_FOUND);
        }

        // получаем список неотвеченных вопросов
        List<SurveyQuestion> questions =
                surveyService.getUnansweredSurveyQuestions(state.get().getUserId(), state.get().getSurveyId(), locale, max);
        // возвращаем результат
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("questions", questions)
                .build(), OK);
    }

    /**
     * Получение количества вопросов и общее время на интервью на старте
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{surveyStateId}/questions/countWithTime", produces = "application/json")
    public ResponseEntity<Map> getRemainsQuestionsWithTime(@PathVariable Long surveyStateId) {
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем количество вопросов и время
        Map<String, Object> countWithTime = surveyService.getRemainsQuestionsWithTime(state.get().getUserId(), state.get().getSurveyId());
        // возвращаем результат
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("count", countWithTime.get("PCOUNT"))
                .add("time", countWithTime.get("PANSWER_SEC"))
                .build(), OK);
    }

    /**
     * Получение количества оставшихся вопросов
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{surveyStateId}/questions/count", produces = "application/json")
    public ResponseEntity<Map> getRemainsQuestionsByState(@PathVariable Long surveyStateId) {
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем количество вопросов
        Integer questionsCount = surveyService.getRemainsQuestionsCount(state.get().getUserId(), state.get().getSurveyId());
        // возвращаем результат
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("count", questionsCount)
                .build(), OK);
    }

    /**
     * Принять ответы на вопросы по вакансии и статусу прохождения опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param answers       список ответов
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/v/{vacancyId}/{surveyStateId}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> applyAnswersByVacancyAndState(@PathVariable Long vacancyId,
                                                                             @PathVariable Long surveyStateId,
                                                                             @RequestParam(required = false) String phone,
                                                                             @RequestParam(required = false) String email,
                                                                             @RequestBody Collection<SurveyAnswer> answers) {

        return processApplyAnswersByVacancyAndState(vacancyId, surveyStateId, phone, email, answers);
    }

    /**
     * Принять ответы на вопросы по вакансии и статусу прохождения опроса, тело запроса передается в сжатом виде (gzip)
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param body          список ответов
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/v/{vacancyId}/{surveyStateId}/compressed", produces = "application/json")
    public ResponseEntity<Map<String, Object>> applyAnswersByVacancyAndState(@PathVariable Long vacancyId,
                                                                             @PathVariable Long surveyStateId,
                                                                             @RequestParam(required = false) String phone,
                                                                             @RequestParam(required = false) String email,
                                                                             @RequestBody byte[] body, HttpServletRequest request) {
        if (!"gzip".equals(request.getHeader("Content-Encoding"))) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        try {
            String s = GzipUtils.decompress(Base64.getDecoder().decode(body));
            Collection<SurveyAnswer> answers = new ObjectMapper().readValue(s, new TypeReference<Collection<SurveyAnswer>>() {
            });
            return processApplyAnswersByVacancyAndState(vacancyId, surveyStateId, phone, email, answers);
        } catch (Exception e) {
            return new ResponseEntity<>(NOT_ACCEPTABLE);
        }
    }

    /**
     * Сохранение информации об устройстве перед прохождением опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param deviceInfo    данные об устройстве пользователя
     * @return результат сохранения в JSON
     */
    @CrossOrigin
    @RequestMapping(value = "/v/{vacancyId}/saveDeviceInfo", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<RestResponse> saveUserDeviceInfo(@PathVariable("vacancyId") Long vacancyId,
                                                           @RequestParam(value = "surveyStateId",
                                                                   required = false,
                                                                   defaultValue = "0") Long surveyStateId,
                                                           @RequestBody DeviceInfo deviceInfo) {
        // получаем состояние прохождения опроса пользователем
        UserSurveyState surveyState;
        if (!surveyStateId.equals(0L)) {   // если в запросе указан параметр surveyStateId
            Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
            if (!state.isPresent()) {
                return new ResponseEntity<>(NOT_FOUND);
            }
            surveyState = state.get();
        } else {
            surveyState = null;
        }
        deviceInfoService.saveDeviceInfo(vacancyId,
                surveyState != null ? surveyState.getUserId() : null,
                surveyState != null ? surveyState.getSurveyId() : null,
                deviceInfo, "V");
        return prepareSuccessResponse(DEVICE_INFO_SAVE_SUCCESS_MESSAGE);
    }

    /**
     * Сохранение фотографии пользователя во время прохождения опроса
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param questionId    идентификатор текущего вопроса
     * @param file          загружаемый файл с фотографией пользователя
     * @return результат сохранения в JSON
     */
    @CrossOrigin
    @RequestMapping(value = "/s/{surveyStateId}/q/{questionId}/saveImage", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<RestResponse> saveUserImage(@PathVariable("surveyStateId") Long surveyStateId,
                                                      @PathVariable("questionId") Long questionId,
                                                      @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        final Long surveyId = state.get().getSurveyId();
        checkArgument(surveyService.isSurveyContainsQuestion(surveyId, questionId),
                format("Опрос с id %s не содержит вопрос с id %s", surveyId, questionId));
        imageUploadService.saveUserImage(state.get().getUserId(), questionId, file);
        return prepareSuccessResponse(USER_IMAGE_UPLOAD_SUCCESS_MESSAGE);
    }


    /**
     * Обработать ответы на вопросы по вакансии и статусу прохождения опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param phone         телефон
     * @param answers       список ответов
     * @return ответ
     */
    private ResponseEntity<Map<String, Object>> processApplyAnswersByVacancyAndState(Long vacancyId,
                                                                                     Long surveyStateId,
                                                                                     String phone,
                                                                                     String email,
                                                                                     Collection<SurveyAnswer> answers) {
        // проверяем наличие вакансии
        if (vacancyService.findById(vacancyId) == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // получаем пользователя
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        User u = user.get();
        boolean userChanges = false;
        // проверяем телефон
        if (isNotBlank(phone)) {
            if (PhoneUtils.isOnlyDigitsInPhone(phone)) {
                u.setPhone(phone);
                userChanges = true;
            } else {
                return new ResponseEntity<>(NOT_FOUND);
            }
        }
        // проверяем email
        if (isNotBlank(email) && isValid(email)) {
            u.setEmail(email);
            userChanges = true;
        }
        if (userChanges) userRepository.save(u);    // сохраняем изменения для пользователя

        // отвечаем на вопросы
        SurveyResult surveyResult = surveyService.answerSurveyQuestions(answers, surveyStateId);
        // отдаем ответ
        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("applied", surveyResult.totalAnswered.get())
                .add("applied_id", surveyResult.appliedAnswerIds)
                .add("repeats", surveyResult.totalRepeats.get())
                .add("repeats_id", surveyResult.repeatedAnswerIds)
                .build(), OK);
    }

    /**
     * Получение результата опроса пользователя
     * (на сколько % он соответствует эталону)
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @return результат опроса пользователя
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{vacancyId}/{surveyStateId}/compareWithLeader", produces = "application/json")
    public ResponseEntity<Map<String, Object>> compareUserWithLeader(@PathVariable Long vacancyId,
                                                                     @PathVariable Long surveyStateId) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        final VacancyMethodology methodology = vacancyService.detectMethodology(vacancy.getSurveyId());
        // получаем состояние прохождения опроса пользователем
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        Optional<User> user = userRepository.findById(state.get().getUserId());
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        Survey survey = surveyService.findById(state.get().getSurveyId());
        final boolean leaderPassedSurvey = vacancyService.leaderPassedSurvey(vacancy.getLeaderId(), survey.getId());
        final boolean needToWait = !(vacancy.getVacancy() && methodology == VacancyMethodology.PORTRAIT && !leaderPassedSurvey);
        final Double compareResult;
        if (survey.getAlgo() == 1) { // получение результатов для опроса "Соционика"
            compareResult = surveyService.compareUserWithLeaderBySurvey(user.get().getId(),
                    vacancy.getLeaderId(),
                    state.get().getSurveyId());
        } else { // получение результатов для всех прочих опросов
            compareResult = state.get().getResultPercent() != null ? state.get().getResultPercent().doubleValue() : null;
        }

        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("compareResult", compareResult)
                .add("similarPerson", vacancy.getSimilarPerson())
                .add("final", state.get().getCalculatedParts() >= 3 && compareResult != null)
                .add("needToWait", needToWait)
                .build(), OK);
    }

    /**
     * Получить детальную информацию по вакансии
     *
     * @param vacancyId идентификатор вакансии
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = GET, value = "/v/{vacancyId}", produces = "application/json")
    public ResponseEntity<Map> getVacancyDetails(@PathVariable Long vacancyId) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        final VacancyMethodology methodology = vacancyService.detectMethodology(vacancy.getSurveyId());
        // находим организацию
        Organization organization = organizationRepository.findByManagerId(vacancy.getManagerId());
        if (organization != null) {
            final boolean imageExists = accountSettingsService.isCompanyImageExists(organization.getId());
            // возвращаем результат
            return new ResponseEntity<>(MapUtils.<String, Object>builder()
                    .add("organization", MapUtils.<String, Object>builder()
                            .add("id", organization.getId())
                            .add("name", organization.getName())
                            .add("description", organization.getDescription())
                            .add("vacancyName", vacancy.getName())
                            .add("vacancySalary", vacancy.getSalary())
                            .add("vacancyBonus", vacancy.getBonus())
                            .add("vacancy", vacancy.getVacancy())
                            .add("methodology", methodology)
                            .add("image", imageExists ? format(GET_COMPANY_HR_IMAGE_REST_ADDRESS, appUrl, organization.getId(), Boolean.TRUE.toString()) : null)
                            .build())
                    .build(), OK);
        } else {
            return new ResponseEntity<>(MapUtils.builder("organization", null).build(), OK);
        }
    }

    /**
     * Пригласить на опрос
     *
     * @param vacancyId  идентификатор вакансии
     * @param name       имя пользователя
     * @param email      почта
     * @param deviceInfo данные об устройстве пользователя
     * @param locale     текущая локаль
     * @return ответ
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/v/{vacancyId}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> inviteForSurvey(@PathVariable Long vacancyId,
                                                               @RequestParam String name,
                                                               @RequestParam String email,
                                                               @RequestBody DeviceInfo deviceInfo,
                                                               Locale locale) {
        // проверяем наличие вакансии
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        // проверяем почту
        if (!isValid(email)) {
            return new ResponseEntity<>(NOT_ACCEPTABLE);
        }
        User manager = userRepository.findById(vacancy.getManagerId()).orElse(null);
        if (manager == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        Long receiverId = userService.getOrCreateUser(name, email, null);
        // получаем состояние прохождения опроса пользователем
        UserSurveyState state = userSurveyStateRepository.findByUserIdAndSurveyId(receiverId, vacancy.getSurveyId());
        // получаем пользователя
        Optional<User> user = userRepository.findById(receiverId);
        if (!user.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        Invitation invitation = new Invitation(receiverId, "D");
        // отправляем приглашение
        userInvitationService.sendInvitations(Sets.newHashSet(invitation), manager, vacancyId);
        // сохраняем информацию об устройстве
        deviceInfoService.saveDeviceInfo(vacancyId, receiverId, vacancy.getSurveyId(), deviceInfo, "I");
        // получаем список неотвеченных вопросов
        List<SurveyQuestion> questions =
                surveyService.getUnansweredSurveyQuestions(receiverId, vacancy.getSurveyId(), locale, null);
        // признак того, что участник уже проходил опрос
        final boolean passedAlready = state != null && questions.isEmpty() && state.getCalculatedParts() >= 3;

        return new ResponseEntity<>(MapUtils.<String, Object>builder()
                .add("passedAlready", passedAlready)
                .add("surveyResultUrl", passedAlready ? buildSurveyResultUrl(vacancyId, state.getId(), user.get().getSalt()) : null)
                .build(), OK);
    }


    /**
     * Сохранение ошибки клиента при прохождении опроса
     *
     * @param surveyError модель ошибки прохождения опроса
     * @return ответ с результатом сохранения ошибки клиента
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/saveError", produces = "application/json")
    public ResponseEntity<RestResponse> saveSurveyClientError(@RequestBody SurveyError surveyError) {
        Long userId = null;
        Long surveyId = null;
        final Long surveyStateId = surveyError.getSurveyStateId();
        if (surveyStateId != null) {
            // получаем состояние прохождения опроса пользователем
            Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
            if (!state.isPresent()) {
                return new ResponseEntity<>(NOT_FOUND);
            }
            userId = state.get().getUserId();
            surveyId = state.get().getSurveyId();
        }
        surveyService.saveSurveyError(userId, surveyId, surveyError);
        return prepareSuccessResponse(format(CLIENT_ERROR_SAVE_SUCCESS_MESSAGE, userId));
    }


    /**
     * Сохранение данных по геолокации опроса
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param location      данные по геолокации (широта + долгота)
     */
    @CrossOrigin
    @RequestMapping(method = POST, value = "/{surveyStateId}/saveLocation", produces = "application/json")
    public ResponseEntity<RestResponse> saveSurveyStateLocation(@PathVariable Long surveyStateId,
                                                                @RequestBody SurveyStateLocation location) {
        Optional<UserSurveyState> state = userSurveyStateRepository.findById(surveyStateId);
        if (!state.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        surveyService.saveSurveyLocation(state.get().getId(), location);
        return prepareSuccessResponse(format(SURVEY_STATE_LOCATION_SUCCESS_MESSAGE, surveyStateId));
    }

    /**
     * Получение данных по глобальным параметрам пользователей
     *
     * @return данные по глобальным параметрам пользователей
     */
    @RequestMapping(value = "/globalUserScore", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public GlobalUserScore getGlobalUserScore() {
        return surveyService.getGlobalUserScore();
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
     * Формирование ссылки для просмотра результатов опроса
     *
     * @param vacancyId     идентификатор вакансии
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param salt          соль
     * @return ссылка для просмотра результатов опроса
     */
    private String buildSurveyResultUrl(Long vacancyId, Long surveyStateId, String salt) {
        return "/" + String.valueOf(vacancyId) + "/" + String.valueOf(surveyStateId) + "?sc=" + salt;
    }

}
