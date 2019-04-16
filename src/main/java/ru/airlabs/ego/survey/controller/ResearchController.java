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
import ru.airlabs.ego.survey.dto.RestResponse;
import ru.airlabs.ego.survey.dto.user.UserCompareResult;
import ru.airlabs.ego.survey.dto.user.UserGroupCompare;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.ResearchService;
import ru.airlabs.ego.survey.service.VacancyService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.survey.utils.ControllerUtils.prepareErrorResponse;

/**
 * Контроллер для работы с исследованиями в ЛК HR
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/research")
public class ResearchController {

    /**
     * Сервис вакансий
     */
    @Autowired
    private VacancyService vacancyService;

    /**
     * Сервис исследований
     */
    @Autowired
    private ResearchService researchService;

    /**
     * Получение списка участников исследования с процентом их совпадения
     *
     * @param researchId     идентификатор исследования
     * @param authentication текущий авторизованный пользователь
     * @return список участников исследования (в порядке "похожести" участника с остальными)
     */
    @RequestMapping(value = "/{researchId}/user/list", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<UserCompareResult> getUserCompareListForResearch(@PathVariable("researchId") Long researchId,
                                                                 @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), researchId);
        return researchService.getUserCompareListForResearch(researchId);
    }

    /**
     * Получение данных о сравнении групп пользователей с конкретным выбранным пользователем
     *
     * @param researchId     идентификатор исследования
     * @param userId         идентификатор участника
     * @param authentication текущий авторизованный пользователь
     * @return данные о сравнении групп пользователей
     */
    @RequestMapping(value = "/{researchId}/user/{userId}/compare", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<UserGroupCompare> getGroupCompareWithUser(@PathVariable("researchId") Long researchId,
                                                          @PathVariable("userId") Long userId,
                                                          @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), researchId);
        return researchService.getGroupCompareWithUser(researchId, userId);
    }

    /**
     * Получение данных о сравнении групп пользователей для категорийного исследования ("Поиск лидера")
     *
     * @param researchId     идентификатор исследования
     * @param authentication текущий авторизованный пользователь
     * @return данные о сравнении групп пользователей
     */
    @RequestMapping(value = "/{researchId}/category/compare", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<UserGroupCompare> getGroupCompareForCategoryResearch(@PathVariable("researchId") Long researchId,
                                                                     @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), researchId);
        return researchService.getGroupCompareForCategoryResearch(researchId, authentication.getUser().getId());
    }

    /**
     * Просмотр подробностей столбца сравнения в рамках конкретного диапазона совпадения для категорийного исследования ("Поиск лидера")
     *
     * @param researchId        идентификатор исследования
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @param authentication    текущий авторизованный пользователь
     * @return список данных сравнения участников опроса
     */
    @RequestMapping(value = "/{researchId}/category/compare/details", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<VacancyUserDetail> getGroupCompareCategoryDetails(@PathVariable("researchId") Long researchId,
                                                                  @RequestParam(value = "chartColumnNumber") Integer chartColumnNumber,
                                                                  @PageableDefault(sort = {"surveyUpdate"}, direction = Sort.Direction.DESC) Pageable pageable,
                                                                  @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), researchId);
        return researchService.getGroupCompareCategoryDetails(researchId,
                authentication.getUser().getId(),
                chartColumnNumber,
                pageable);
    }

    /**
     * Просмотр подробностей столбца сравнения в рамках конкретного диапазона совпадения
     *
     * @param researchId        идентификатор исследования
     * @param userId            идентификатор участника
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @param authentication    текущий авторизованный пользователь
     * @return список данных сравнения участников опроса
     */
    @RequestMapping(value = "/{researchId}/user/{userId}/compare/details", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<VacancyUserDetail> getGroupCompareUserDetails(@PathVariable("researchId") Long researchId,
                                                              @PathVariable("userId") Long userId,
                                                              @RequestParam(value = "chartColumnNumber") Integer chartColumnNumber,
                                                              @PageableDefault(sort = {"surveyCreate"}, direction = Sort.Direction.DESC) Pageable pageable,
                                                              @AuthenticationPrincipal Authentication authentication) {
        vacancyService.checkUserAccessForVacancy(authentication.getUser(), researchId);
        return researchService.getGroupCompareUserDetails(researchId,
                userId,
                authentication.getUser().getId(),
                chartColumnNumber,
                pageable);
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
