package ru.airlabs.ego.survey.controller;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.ResumeData;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.entity.config.EmailConfiguration;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.core.repository.config.EmailConfigurationRepository;
import ru.airlabs.ego.survey.dto.FileContent;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.invitation.InvitationCount;
import ru.airlabs.ego.survey.dto.invitation.InvitationText;
import ru.airlabs.ego.survey.dto.invitation.InvitationType;
import ru.airlabs.ego.survey.dto.resume.ResumeInfo;
import ru.airlabs.ego.survey.dto.resume.ResumeValidationResult;
import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.*;
import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.ResumeService;
import ru.airlabs.ego.survey.service.resume.image.ResumeImageParseService;
import ru.airlabs.ego.survey.service.resume.parse.InvalidFormatException;
import ru.airlabs.ego.survey.service.resume.parse.ResumeParseService;
import ru.airlabs.ego.survey.utils.MapUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.*;
import static ru.airlabs.ego.survey.service.impl.UserBalanceServiceImpl.PHONE_CALL_DEFAULT_COST;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для работы с карточками клиентов на UI
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/users")
public class UserDetailsController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailConfigurationRepository emailConfigurationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Сервис для работы с резюме
     */
    @Autowired
    private ResumeService resumeService;

    /**
     * Сервис для получения изображений пользователей из резюме
     */
    @Autowired
    private ResumeImageParseService resumeImageParseService;

    /**
     * Сервис для работы с аудио-презентациями
     */
    @Autowired
    private AudioPresentationService audioPresentationService;

    /**
     * Сервис для работы с балансом пользователя
     */
    @Autowired
    private UserBalanceService userBalanceService;

    /**
     * Просмотр карточки клиента по вакансии или исследованию
     *
     * @param vacancyId      идентификатор вакансии или исследования
     * @param userId         идентификатор клиента
     * @param authentication текущий авторизованный пользователь
     * @return карточка клиента
     */
    @RequestMapping(value = "/{userId}/{vacancyId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public VacancyUserDetail getUserDetails(@PathVariable("userId") Long userId,
                                            @PathVariable("vacancyId") Long vacancyId,
                                            @AuthenticationPrincipal Authentication authentication) {
        return userDetailsService.getUserDetailsByVacancy(userId, vacancyId, authentication.getUser().getId());
    }

    /**
     * Редактирование карточки клиента
     *
     * @param userId         идентификатор пользователя
     * @param userDetail     данные для редактирования карточки пользователя
     * @param authentication данные авторизации текущего пользователя (HR)
     * @return результат редактирования
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> updateUserDetails(@PathVariable("userId") Long userId,
                                                          @RequestBody UserDetail userDetail,
                                                          @AuthenticationPrincipal Authentication authentication) {
        userDetailsService.updateUserDetails(userId, authentication.getUser().getId(), userDetail);
        return prepareSuccessResponse(format(USER_DETAILS_SAVE_SUCCESS_MESSAGE, userId));
    }

    /**
     * Удаление пользователя из вакансии или исследования
     *
     * @param userId         идентификатор пользователя
     * @param vacancyId      идентификатор вакансии или исследования
     * @param authentication текущий авторизованный пользователь
     */
    @RequestMapping(value = "/{userId}/{vacancyId}", method = RequestMethod.DELETE)
    public ResponseEntity<RestResponse> deleteUserFromVacancy(@PathVariable("userId") Long userId,
                                                              @PathVariable("vacancyId") Long vacancyId,
                                                              @AuthenticationPrincipal Authentication authentication) {
        userDetailsService.deleteUserFromVacancy(userId, vacancyId);
        return prepareSuccessResponse(format(USER_DELETED_FROM_VACANCY_SUCCESS_MESSAGE, userId, vacancyId));
    }

    /**
     * Получение фото пользователя
     *
     * @param userId         идентификатор пользователя
     * @param vacancyId      идентификатор вакансии
     * @param questionId     идентификатор вопрос
     * @param authentication текущий авторизованный пользователь
     * @return содержимое фото пользователя
     */
    @RequestMapping(value = "/u/{userId}/v/{vacancyId}/q/{questionId}/image", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<byte[]> getUserImageForVacancy(@PathVariable("userId") Long userId,
                                                         @PathVariable("vacancyId") Long vacancyId,
                                                         @PathVariable("questionId") Long questionId,
                                                         @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), vacancyId);
        FileContent imageContent = imageUploadService.getUserImageContent(userId, vacancyId, questionId);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.set("Content-Disposition", "inline; filename=" + imageContent.fileName);
        return new ResponseEntity<>(imageContent.content, headers, HttpStatus.OK);
    }

    /**
     * Загрузить файл резюме и привязать его к существующему пользователю вакансии
     *
     * @param vacancyId      идентификатор вакансии
     * @param file           файл
     * @param invite         опция отправлять моментальное приглашение, или нет
     * @param url            ссылка, откуда взято резюме
     * @param html           опция, передается ли файл в формате HTML
     * @param source         опция, которая указывает на источник резюме для файла в формате HTML
     *                       (HH - HeadHunter, FW - FriendWork, JM - Job In Moscow, MO - job-mo.ru,
     *                       TV - trudvsem.ru, JA - rabota19.ru)
     * @param authentication данные авторизации текущего пользователя
     * @return ответ
     */
    @PostMapping(value = "/v/{vacancyId}/uploadResume", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> uploadResume(@PathVariable Long vacancyId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam("invite") Boolean invite,
                                          @RequestParam("url") String url,
                                          @RequestParam(value = "html", required = false, defaultValue = "false") Boolean html,
                                          @RequestParam(value = "htmlSrc", required = false, defaultValue = "HH") String source,
                                          @AuthenticationPrincipal Authentication authentication,
                                          Locale locale) throws IOException {
        final Long managerId = authentication.getUser().getId();
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return processResumeUploadError(managerId, file, url, NOT_FOUND, "Не найдена вакансия");
        }
        if (!vacancy.getManagerId().equals(managerId)) {
            return processResumeUploadError(managerId, file, url, NOT_FOUND, "Нет доступа к вакансии");
        }
        if (file.isEmpty()) {
            return processResumeUploadError(managerId, file, url, NOT_ACCEPTABLE, "Неверный формат файла");
        }
        if (invite) {   // если передан параметр invite=true - проверяем лимит приглашений
            InvitationCount invitationCount = userInvitationService.getInvitationCountForUser(authentication.getUser(), new Date(), "E", null);
            if (invitationCount.invitationOnEmailLimit <= 0) {
                return processResumeUploadError(managerId, file, url, NOT_ACCEPTABLE, "Превышен дневной лимит приглашений");
            }
        }
        List<Resume> resumeList;
        try (InputStream stream = file.getInputStream()) {
            if (!html) {  // передан не HTML
                resumeList = new ResumeParseService(file.getOriginalFilename(), stream).parse();
            } else { // HTML
                resumeList = new ResumeParseService(file.getOriginalFilename(), stream).parseInHTML(ResumeData.Source.valueOf(source));
            }
            if (resumeList.isEmpty() || resumeList.size() > 1) {
                return processResumeUploadError(managerId, file, url, UNPROCESSABLE_ENTITY, "Ошибка парсинга резюме");
            }
            Resume resume = resumeList.get(0);
            // если не заполнены контакты в резюме - возвращаем ошибку
            if (!resume.isContactFilled()) {
                return processResumeUploadError(managerId, file, url, UNPROCESSABLE_ENTITY, RESUME_DATA_NOT_FOUND_MESSAGE);
            }
            final ResumeValidationResult validationResult = resume.validateResume();    // валидируем резюме
            Date birthDate = resume.getBirthDate() != null ?
                    Date.from(resume.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
            // создаем пользователя
            Long userId = userService.getOrCreateUser(resume.getFirstName(), resume.getEmail(), resume.getPhone(),
                    locale.getLanguage().toUpperCase(), resume.getLastName(), resume.getMiddleName(), resume.getSex(),
                    birthDate, resume.getAddress(), null, resume.getSalary(), authentication.getUser().getId());
            if (userId == null) {
                return processResumeUploadError(managerId, file, url, UNPROCESSABLE_ENTITY, "Ошибка при создании нового пользователя");
            }
            // сохраняем резюме
            ResumeData data;
            if (!html) {    // передан не HTML
                data = resumeService.saveResumeFile(vacancyId,
                        userId,
                        file,
                        resume,
                        url);
                // сохраняем фото из резюме
                saveResumeImageFromFile(file, data);
            } else {    // HTML
                data = resumeService.saveResumeFileFromHtml(vacancyId,
                        userId,
                        file,
                        resume,
                        url);
                saveResumeImageFromURL(resume.getPhotoURL(), data);
            }
            // получаем нового пользователя
            Optional<User> user = userRepository.findById(userId);
            if (!user.isPresent()) {
                return processResumeUploadError(managerId, file, url, NOT_FOUND, "Ошибка при получении пользователя");
            }
            EmailConfiguration emailConfiguration = emailConfigurationRepository.findByUserId(managerId);
            String sourceCode;
            if (invite && (validationResult.phoneValid || validationResult.emailValid)) {
                sourceCode = "P";    // sourceCode для приглашений из плагина (если включена опция моментального инвайта)
            } else {                 // приглашение в буфере, если не прошло валидацию
                sourceCode = "B";    // sourceCode для приглашений из плагина (если не включена опция моментального инвайта)
            }
            // создаем и получаем текст приглашения в WhatsApp
            InvitationText inviteText = userInvitationService.createInvitationText(new Invitation(userId, sourceCode),
                    authentication.getUser(),
                    vacancyId,
                    InvitationType.WHATS_APP);
            // определяем, нужно ли слать инвайт на email
            boolean needToSendEmail = invite &&
                    emailConfiguration != null &&
                    "EXTERNAL".equals(emailConfiguration.getSendType());
            if (needToSendEmail) {
                userInvitationService.sendInvitations(Sets.newHashSet(new Invitation(userId, sourceCode)), authentication.getUser(), vacancyId);
            }
            // проверяем необходимость отправки звонка
            boolean needToCall = invite &&
                    audioPresentationService.isAudioPresentationExists(vacancyId) &&
                    (userBalanceService.getUserBalance(authentication.getUser().getId()) - PHONE_CALL_DEFAULT_COST > 0);
            if (needToCall) {   // отправка звонка
                userInvitationService.createInvitationText(new Invitation(userId, sourceCode),
                        authentication.getUser(),
                        vacancyId,
                        InvitationType.AUDIO);
            }

            return new ResponseEntity<>(MapUtils.<String, Object>builder()
                    .add("success", Boolean.TRUE)
                    .add("userName", user.get().getName())
                    .add("email", user.get().getEmail())
                    .add("phone", user.get().getPhone())
                    .add("inviteText", inviteText != null ? inviteText.getText() : null)
                    .add("eventId", inviteText != null ? inviteText.getId() : null)
                    .add("message", validationResult.validationMessage)
                    .build(), OK);
        } catch (InvalidFormatException e) {
            return processResumeUploadError(managerId, file, url, UNPROCESSABLE_ENTITY, "Ошибка парсинга резюме");
        }
    }

    /**
     * Проверка ссылок на резюме
     *
     * @param vacancyId      идентификатор вакансии
     * @param resumeUrls     список ссылок на резюме
     * @param authentication данные авторизации текущего пользователя (HR)
     * @return результат проверки ссылок
     */
    @RequestMapping(value = "/v/{vacancyId}/checkResumeLoaded", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> checkResumeLoaded(@PathVariable("vacancyId") Long vacancyId,
                                               @RequestBody List<String> resumeUrls,
                                               @AuthenticationPrincipal Authentication authentication) {
        Vacancy vacancy = vacancyService.findById(vacancyId);
        if (vacancy == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        if (!vacancy.getManagerId().equals(authentication.getUser().getId())) {
            return new ResponseEntity<>(NOT_FOUND);
        }
        List<ResumeInfo> result = resumeService.checkResumeLoaded(vacancyId, resumeUrls);
        return new ResponseEntity<>(result, OK);
    }

    /**
     * Сохранение изображения из резюме
     *
     * @param file файл с резюме
     * @param data данные из резюме
     * @throws IOException
     */
    private void saveResumeImageFromFile(MultipartFile file, ResumeData data) throws IOException {
        try (InputStream imageStream = new ByteArrayInputStream(file.getBytes())) {
            byte[] resumeImage = resumeImageParseService.getUserImageFromResume(imageStream);
            if (resumeImage.length > 0) {
                imageUploadService.saveUserImageFromResume(data.getUserId(), resumeImage);
            }
        }
    }

    /**
     * Сохранение изображения из резюме для HTML файла
     *
     * @param imageURL URL для фото из резюме
     * @param data     данные из резюме
     * @throws IOException
     */
    private void saveResumeImageFromURL(String imageURL, ResumeData data) throws IOException {
        if (isNotBlank(imageURL)) {
            try {
                URL url = new URL(imageURL);    // проверяем подключение к URL
                URLConnection connection = url.openConnection();
                connection.connect();

                try (InputStream imageStream = url.openStream()) {
                    byte[] resumeImage = IOUtils.toByteArray(imageStream);
                    if (resumeImage.length > 0) {
                        imageUploadService.saveUserImageFromResume(data.getUserId(), resumeImage);
                    }
                }
            } catch (MalformedURLException e) {
                return;
            } catch (IOException e) {
                return;
            }
        }
    }

    /**
     * Обработка ошибки при загрузке резюме
     *
     * @param userId       идентификатор пользователя
     * @param file         файл с резюме
     * @param resumeUrl    урл, откуда взято резюме
     * @param errorCode    код ошибки
     * @param errorMessage сообщение об ошибке
     * @return http-ответ
     */
    private ResponseEntity<?> processResumeUploadError(Long userId,
                                                       MultipartFile file,
                                                       String resumeUrl,
                                                       HttpStatus errorCode,
                                                       String errorMessage) {
        resumeService.saveResumeErrorFile(userId, file, resumeUrl, errorCode);
        if (isNotBlank(errorMessage)) {
            return new ResponseEntity<>(MapUtils.builder("message", errorMessage).build(), errorCode);
        } else {
            return new ResponseEntity<>(errorCode);
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
                (exception.getCause() != null ? exception.getCause().getMessage() : null);
        final String url = request.getRequestURL().toString();
        return prepareErrorResponse(message, url);
    }

}
