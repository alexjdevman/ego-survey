package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.survey.dto.FileContent;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.settings.AccountSettings;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.AccountSettingsService;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для работы с настройками личного кабинета HR на UI
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/settings")
public class AccountSettingsController {

    @Autowired
    private AccountSettingsService settingsService;

    /**
     * Получение настроек ЛК для текущего пользователя
     *
     * @param authentication текущий авторизованный пользователь
     * @return настройки ЛК пользователя
     */
    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public AccountSettings getSettings(@AuthenticationPrincipal Authentication authentication) {
        return settingsService.getAccountSettings(authentication.getUser());
    }

    /**
     * Сохранение настроек ЛК для текущего пользователя
     *
     * @param settings       настройки пользователя
     * @param authentication текущий авторизованный пользователь
     * @return настройки ЛК пользователя
     */
    @RequestMapping(value = {"/", ""}, method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> saveSettings(@RequestBody AccountSettings settings,
                                                     @AuthenticationPrincipal Authentication authentication) {
        settingsService.saveAccountSettings(settings, authentication.getUser());
        return prepareSuccessResponse("Настройки пользователя успешно сохранены");
    }

    /**
     * Получение фото HR компании
     *
     * @param companyId идентификатор компании
     * @param preview   флаг превью (уменьшенное изображение)
     * @return содержимое фото HR компании
     */
    @RequestMapping(value = "/company/image/{companyId}/{preview}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<byte[]> getCompanyImage(@PathVariable("companyId") Long companyId,
                                                  @PathVariable("preview") boolean preview) {
        FileContent imageContent = settingsService.getCompanyImage(companyId, preview);
        if (imageContent.content == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("Content-Disposition", "inline; filename=" + imageContent.fileName);
        return new ResponseEntity<>(imageContent.content, headers, HttpStatus.OK);
    }

    /**
     * Загрузка фото HR компании
     *
     * @param companyId      идентификатор компании
     * @param file           файл с фото HR компании
     * @param authentication текущий авторизованный пользователь
     * @return
     */
    @RequestMapping(value = "/company/saveImage/{companyId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> saveCompanyImage(@PathVariable("companyId") Long companyId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @AuthenticationPrincipal Authentication authentication) {
        settingsService.saveCompanyImage(companyId, file);
        return prepareSuccessResponse(COMPANY_HR_IMAGE_UPLOAD_SUCCESS_MESSAGE);
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
