package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.airlabs.ego.core.entity.Survey;
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.survey.SurveyCategory;
import ru.airlabs.ego.survey.dto.survey.SurveyInfo;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.SurveyService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static ru.airlabs.ego.survey.utils.ControllerUtils.*;

/**
 * Контроллер для работы HR с конструктором опросов на UI (создание, редактирование опросов)
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/survey_hr")
public class SurveyAdminController {

    @Autowired
    private SurveyService surveyService;


    /**
     * Получение доступных текущему пользователю опросов
     *
     * @param authentication данные авторизации текущего пользователя
     * @param active признак активности опроса
     * @param locale         текущая локаль
     * @return данные по доступных опросов в JSON
     */
    @RequestMapping(value = "/list",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSurveyList(@RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
                                                                   @AuthenticationPrincipal Authentication authentication,
                                                                   Locale locale) {
        List<Map<String, Object>> surveyList = surveyService.getSurveyDataList(authentication.getUser().getId(), active, locale);
        if (surveyList.isEmpty()) {
            return new ResponseEntity<>(NOT_FOUND);
        } else {
            return new ResponseEntity<>(surveyList, OK);
        }
    }


    /**
     * Получение доступных категорий вопросов
     *
     * @param locale текущая локаль
     * @return список доступных категорий вопросов
     */
    @RequestMapping(value = "/categories",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public List<SurveyCategory> getCategories(Locale locale) {
        return surveyService.getSurveyCategories(locale);
    }


    /**
     * Сохранение опроса с наполнением его категориями вопросов
     *
     * @param surveyId   идентификатор опроса
     * @param categories список категорий вопросов
     * @return результат сохранения опроса в JSON
     */
    @RequestMapping(value = "/save/{surveyId}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> saveSurvey(@PathVariable("surveyId") Long surveyId,
                                                   @RequestBody List<SurveyCategory> categories) {
        surveyService.fillSurveyWithCategories(surveyId, categories);
        return prepareSuccessResponse(format(SURVEY_SAVE_SUCCESS_MESSAGE, surveyId));
    }


    /**
     * Добавление нового опроса
     *
     * @param surveyInfo     данные по опросу (передается только имя)
     * @param authentication данные авторизации текущего пользователя
     * @param locale         текущая локаль
     * @return новый опрос
     */
    @RequestMapping(value = "/add",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public Survey addSurvey(@RequestBody SurveyInfo surveyInfo,
                            @AuthenticationPrincipal Authentication authentication,
                            Locale locale) {
        return surveyService.addSurvey(surveyInfo.getName(), authentication.getUser(), locale);
    }

    /**
     * Редактирование опроса (редактируем только имя)
     *
     * @param surveyInfo     данные по опросу (передается имя + id опроса)
     * @param authentication данные авторизации текущего пользователя
     * @param locale         текущая локаль
     * @return опрос
     */
    @RequestMapping(value = "/update",
            method = RequestMethod.PUT,
            produces = "application/json")
    @ResponseBody
    public Survey updateSurvey(@RequestBody SurveyInfo surveyInfo,
                               @AuthenticationPrincipal Authentication authentication,
                               Locale locale) {
        return surveyService.updateSurvey(surveyInfo, locale);
    }

    /**
     * Изменение статуса опроса
     *
     * @param id идентификатор опроса
     * @return результат смены статуса у опроса
     */
    @RequestMapping(value = "/changeState/{id}",
            method = RequestMethod.POST,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<RestResponse> changeSurveyState(@PathVariable("id") Long id) {
        surveyService.changeSurveyState(id);
        return prepareSuccessResponse(format(SURVEY_STATUS_CHANGE_SUCCESS_MESSAGE, id));
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
