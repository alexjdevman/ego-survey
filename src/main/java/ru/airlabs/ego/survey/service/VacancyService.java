package ru.airlabs.ego.survey.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.survey.dto.vacancy.VacancyForm;
import ru.airlabs.ego.survey.dto.vacancy.VacancyMethodology;

import java.util.List;

/**
 * Интерфейс сервиса для работы с вакансиями
 *
 * @author Aleksey Gorbachev
 */
public interface VacancyService {

    /**
     * Получить все вакансии или исследования пользователя, с учетом флага активности,
     * используя настройки постраничного просмотра и сортировки
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии\исследования (false для архивных вакансий)
     * @param vacancy     признак вакансии или исследования
     * @param pageRequest настройки постраничного просмотра и сортировки
     * @return содержимое страницы вакансий и исследований
     */
    Page<Vacancy> getVacancyPage(User user,
                                 Boolean active,
                                 Boolean vacancy,
                                 Pageable pageRequest);


    /**
     * Получить все не связанные вакансии/исследования пользователя, с учетом флага активности,
     * используя настройки постраничного просмотра и сортировки
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии или исследования
     * @param vacancy     признак вакансии или исследования*
     * @param pageRequest настройки постраничного просмотра и сортировки
     * @return содержимое страницы не связанных вакансий\исследований
     */
    Page<Vacancy> getNotLinkedVacancyPage(User user,
                                          Boolean active,
                                          Boolean vacancy,
                                          Pageable pageRequest);

    /**
     * Получить все не связанные вакансии/исследования пользователя, с учетом флага активности
     *
     * @param user    текущий авторизованный пользователь
     * @param active  признак активности вакансии или исследования
     * @param vacancy признак вакансии или исследования*
     * @return список не связанных вакансий\исследований
     */
    List<Vacancy> getNotLinkedVacancyList(User user,
                                          Boolean active,
                                          Boolean vacancy);

    /**
     * Получить все связанные вакансии/исследования пользователя, с учетом флага активности,
     * используя настройки постраничного просмотра и сортировки
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии или исследования
     * @param pageRequest настройки постраничного просмотра и сортировки
     * @return содержимое страницы связанных вакансий\исследований
     */
    Page<Vacancy> getLinkedVacancyPage(User user,
                                       Boolean active,
                                       Pageable pageRequest);

    /**
     * Получить все связанные вакансии/исследования пользователя, с учетом флага активности
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии или исследования
     * @return список связанных вакансий\исследований
     */
    List<Vacancy> getLinkedVacancyList(User user,
                                       Boolean active);

    /**
     * Получить вакансию или исследование по идентификатору
     *
     * @param id идентификатор вакансии или исследования
     * @return вакансия или исследование
     */
    Vacancy findById(Long id);

    /**
     * Создать вакансию
     *
     * @param form модель вакансии на UI
     * @param user пользователь, создающий вакансию
     * @return созданная вакансия
     */
    Vacancy addVacancy(VacancyForm form, User user);

    /**
     * Редактировать вакансию
     *
     * @param form модель вакансии на UI
     * @param user пользователь, сохраняющий вакансию
     * @return сохраненная вакансия
     */
    Vacancy updateVacancy(VacancyForm form, User user);

    /**
     * Создать исследование
     *
     * @param form модель исследования на UI
     * @param user пользователь, создающий исследование
     * @return созданное исследование
     */
    Vacancy addResearch(VacancyForm form, User user);

    /**
     * Редактировать исследование
     *
     * @param form модель исследования на UI
     * @param user пользователь, сохраняющий исследование
     * @return сохраненное исследование
     */
    Vacancy updateResearch(VacancyForm form, User user);

    /**
     * Удаление вакансии или исследования
     *
     * @param id   идентификатор вакансии или исследования
     * @param user текущий авторизованный пользователь
     */
    void deleteVacancy(Long id, User user);

    /**
     * Деактивировать вакансию (активация для деактивированной вакансии)
     *
     * @param id идентификатор вакансии
     */
    void deactivateVacancy(Long id);

    /**
     * Создание формы по данным вакансии или исследования для UI
     *
     * @param id   идентификатор вакансии или исследования
     * @param user текущий авторизованный пользователь
     * @return модель данных для вакансии или исследования
     */
    VacancyForm prepareVacancyForm(Long id, User user);

    /**
     * Проверка доступности вакансии пользователю
     *
     * @param user      текущий авторизованный пользователь
     * @param vacancyId идентификатор вакансии
     */
    void checkUserAccessForVacancy(User user, Long vacancyId);

    /**
     * Определение методологии вакансии по идентификатору опроса
     *
     * @param surveyId идентификатор опроса, с которым связана вакансия
     * @return методология вакансии
     */
    VacancyMethodology detectMethodology(Long surveyId);

    /**
     * Заполнение идентификатора эталонного пользователя для вакансии
     *
     * @param vacancyId идентификатор вакансия
     * @param leaderId  идентификатор эталонного пользователя
     */
    void fillLeaderUserInVacancy(Long vacancyId,
                                 Long leaderId);


    /**
     * Отправка приглашений пользователю
     *
     * @param leaderId    id приглашаемого пользователя
     * @param currentUser текущий авторизованный пользователь, выполняющий отправку приглашения
     * @param vacancyId   идентификатор вакансии
     * @param inviteType  тип приглашения (E - email, W - WhatsApp)
     * @return идентификатор лидера
     */
    void sendInvitationToLeader(Long leaderId,
                                User currentUser,
                                Long vacancyId,
                                String inviteType);

    /**
     * Получение или создание нового пользователя-лидера
     *
     * @param leaderEmailOrPhone email или телефон приглашаемого пользователя лидера
     * @param leaderName         ФИО приглашаемого пользователя лидера
     * @param currentUser        текущий авторизованный пользователь, выполняющий отправку приглашения
     * @return идентификатор лидера
     */
    Long getOrCreateLeaderUser(String leaderEmailOrPhone,
                               String leaderName,
                               User currentUser);

    /**
     * Получение признака, прошел ли эталонный пользователь опрос
     *
     * @param leaderId идентификатор эталонного пользователя
     * @param surveyId идентификатор опроса
     * @return признак, прошел ли эталон опрос
     */
    boolean leaderPassedSurvey(Long leaderId, Long surveyId);

}
