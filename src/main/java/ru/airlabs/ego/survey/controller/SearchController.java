package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.airlabs.ego.survey.dto.search.SearchUserResult;
import ru.airlabs.ego.survey.security.Authentication;
import ru.airlabs.ego.survey.service.SearchService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Контроллер для реализации поиска в личном кабинете HR
 *
 * @author Aleksey Gorbachev
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    /**
     * Сервис для поиска
     */
    @Autowired
    private SearchService searchService;

    /**
     * Поиск всех пользователей по строке поиска
     *
     * @param search         строка поиска
     * @param authentication текущий авторизованный HR
     * @return список всех результатов поиска по пользователям
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<SearchUserResult> searchAllUsers(@RequestParam(name = "search") String search,
                                                 @AuthenticationPrincipal Authentication authentication) {
        return searchService.searchUsers(authentication.getUser().getId(), search);
    }

    /**
     * Поиск уникальных пользователей по строке поиска (без дублирования пользователей)
     *
     * @param search         строка поиска
     * @param authentication текущий авторизованный HR
     * @return список уникальных результатов поиска по пользователям (без дублирования)
     */
    @RequestMapping(value = "/users/unique", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Set<SearchUserResult> searchUniqueUsers(@RequestParam(name = "search") String search,
                                                   @AuthenticationPrincipal Authentication authentication) {
        return new HashSet<>(searchService.searchUsers(authentication.getUser().getId(), search));
    }

}
