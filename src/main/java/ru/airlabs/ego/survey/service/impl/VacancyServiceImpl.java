package ru.airlabs.ego.survey.service.impl;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.*;
import ru.airlabs.ego.core.repository.*;
import ru.airlabs.ego.survey.dto.invitation.Invitation;
import ru.airlabs.ego.survey.dto.vacancy.VacancyForm;
import ru.airlabs.ego.survey.dto.vacancy.VacancyMethodology;
import ru.airlabs.ego.survey.service.UserInvitationService;
import ru.airlabs.ego.survey.service.UserService;
import ru.airlabs.ego.survey.service.VacancyService;
import ru.airlabs.ego.survey.utils.EmailUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Сервис для работы с вакансиями
 *
 * @author Aleksey Gorbachev
 */
@Service("vacancyService")
@Transactional(readOnly = true)
public class VacancyServiceImpl implements VacancyService {

    /**
     * Кол-во ответов в опросе, необходимое для завершения прохождения опроса лидером
     */
    private static final int LEADER_PASSED_SURVEY_PROGRESS = 96;

    private static final String VACANCY_NOT_FOUND_ERROR = "Не найдена вакансия с id %s";

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий связей пользователей
     */
    @Autowired
    private UserLinkRepository userLinkRepository;

    @Autowired
    private UserSurveyLinkRepository surveyLinkRepository;

    @Autowired
    private UserSurveyStateRepository userSurveyStateRepository;

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Page<Vacancy> getVacancyPage(User user,
                                        Boolean active,
                                        Boolean vacancy,
                                        Pageable pageRequest) {
        return vacancyRepository.findAllByManagerIdAndActiveAndVacancy(user.getId(), active, vacancy, pageRequest);
    }

    /**
     * Получить все не связанные вакансии пользователя, с учетом флага активности,
     * используя настройки постраничного просмотра и сортировки
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии
     * @param vacancy     признак вакансии или исследования
     * @param pageRequest настройки постраничного просмотра и сортировки
     * @return содержимое страницы не связанных вакансий
     */
    @Override
    public Page<Vacancy> getNotLinkedVacancyPage(User user, Boolean active, Boolean vacancy, Pageable pageRequest) {
        return vacancyRepository.getNotLinkedVacancies(user.getId(), active, vacancy, pageRequest);
    }

    /**
     * Получить все не связанные вакансии/исследования пользователя, с учетом флага активности
     *
     * @param user    текущий авторизованный пользователь
     * @param active  признак активности вакансии или исследования
     * @param vacancy признак вакансии или исследования*
     * @return список не связанных вакансий\исследований
     */
    @Override
    public List<Vacancy> getNotLinkedVacancyList(User user, Boolean active, Boolean vacancy) {
        return vacancyRepository.getNotLinkedVacancies(user.getId(), active, vacancy);
    }

    /**
     * Получить все связанные вакансии пользователя, с учетом флага активности,
     * используя настройки постраничного просмотра и сортировки
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии
     * @param pageRequest настройки постраничного просмотра и сортировки
     * @return содержимое страницы связанных вакансий
     */
    @Override
    public Page<Vacancy> getLinkedVacancyPage(User user, Boolean active, Pageable pageRequest) {
        return vacancyRepository.findAllByManagerIdAndActiveAndVacancy(user.getId(), active, Boolean.TRUE, pageRequest);
    }

    /**
     * Получить все связанные вакансии/исследования пользователя, с учетом флага активности
     *
     * @param user        текущий авторизованный пользователь
     * @param active      признак активности вакансии или исследования
     * @return список связанных вакансий\исследований
     */
    @Override
    public List<Vacancy> getLinkedVacancyList(User user, Boolean active) {
        return vacancyRepository.findAllByManagerIdAndActiveAndVacancy(user.getId(), active, Boolean.TRUE);
    }

    @Override
    public Vacancy findById(Long id) {
        Optional<Vacancy> result = vacancyRepository.findById(id);
        return result.orElse(null);
    }

    @Transactional
    @Override
    public Vacancy addVacancy(VacancyForm form, User user) {
        Vacancy vacancy = form.buildVacancy(user);
        return vacancyRepository.save(vacancy);
    }

    @Transactional
    @Override
    public Vacancy updateVacancy(VacancyForm form, User user) {
        final Long vacancyId = form.getId();
        Vacancy vacancy = findById(vacancyId);
        checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, vacancyId);
        form.validateByVacancy(vacancy);

        vacancy.setName(form.getName());
        vacancy.setUrl(form.getUrl());
        vacancy.setSalary(form.getSalary() != null ? form.getSalary() : 0L);
        vacancy.setMobile(form.getMobile());
        vacancy.setSimilarPerson(form.getSimilarPerson());
        vacancy.setBonus(form.getVacancyBonus());
        vacancy.setDateUpdate(new Date());

        return vacancyRepository.save(vacancy);
    }

    @Transactional
    @Override
    public Vacancy addResearch(VacancyForm form, User user) {
        Vacancy research = form.buildResearch(user);
        return vacancyRepository.save(research);
    }

    @Transactional
    @Override
    public Vacancy updateResearch(VacancyForm form, User user) {
        final Long researchId = form.getId();
        Vacancy research = findById(researchId);
        checkNotNull(research, "Не найдено исследование с id " + researchId);
        form.validateByVacancy(research);

        research.setName(form.getName());
        research.setMobile(form.getMobile());
        research.setSimilarPerson(form.getSimilarPerson());
        research.setDateUpdate(new Date());
        return vacancyRepository.save(research);
    }

    @Transactional
    @Override
    public void deleteVacancy(Long id, User user) {
        Vacancy vacancy = findById(id);
        checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, id);
        entityManager.remove(vacancy);
    }

    @Transactional
    @Override
    public void deactivateVacancy(Long id) {
        Vacancy vacancy = findById(id);
        checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, id);
        vacancy.setActive(!vacancy.getActive());
    }

    @Override
    public VacancyForm prepareVacancyForm(Long id, User user) {
        Vacancy vacancy = findById(id);
        checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, id);
        VacancyForm form = new VacancyForm();
        if (vacancy.getVacancy()) {
            form.copyDataFromVacancy(vacancy);
        } else {
            form.copyDataFromResearch(vacancy);
        }
        form.setMethodology(detectMethodology(vacancy.getSurveyId()));
        final Long leaderId = vacancy.getLeaderId();
        if (leaderId != null) {
            UserLink leader = userLinkRepository.findByUserIdAndParentId(leaderId, user.getId());
            checkArgument(leader != null, "Не найдены данные эталонного пользователя с id %s", leaderId);

            form.setLeaderEmail(isNotBlank(leader.getEmail()) ? leader.getEmail() : leader.getPhone());
            form.setLeaderName(leader.getName());
            form.setLeaderPassedSurvey(getLeaderPassedProgress(leaderId, vacancy.getSurveyId()) >= LEADER_PASSED_SURVEY_PROGRESS);
        }
        return form;
    }

    @Override
    public void checkUserAccessForVacancy(User user, Long vacancyId) {
        Vacancy vacancy = findById(vacancyId);
        checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, vacancyId);
        checkArgument(vacancy.getManagerId().equals(user.getId()),
                "У пользователя с идентификатором %s нет доступа к вакансии с идентификатором %s",
                user.getId(),
                vacancyId);
    }

    @Override
    public VacancyMethodology detectMethodology(Long surveyId) {
        if (surveyId.equals(0L)) return VacancyMethodology.EMPTY;   // для пустого опросника - пустая методология
        UserSurveyLink link = surveyLinkRepository.findBySurveyId(surveyId);
        if (link == null) return null;
        return link.getUserId() == 0 ? // если id_user = 0 это портретная методология (общие для всех опросы), иначе категорийная
                VacancyMethodology.PORTRAIT :
                VacancyMethodology.CATEGORY;
    }

    /**
     * Заполнение идентификатора эталонного пользователя для вакансии
     *
     * @param vacancyId идентификатор вакансии
     * @param leaderId  идентификатор эталонного пользователя
     */
    @Transactional
    @Override
    public void fillLeaderUserInVacancy(Long vacancyId,
                                        Long leaderId) {
        if (leaderId != null) {
            Vacancy vacancy = findById(vacancyId);
            checkNotNull(vacancy, VACANCY_NOT_FOUND_ERROR, vacancyId);
            vacancy.setLeaderId(leaderId);
        }
    }

    /**
     * Отправка приглашений пользователю
     *
     * @param leaderId    id приглашаемого пользователя
     * @param currentUser текущий авторизованный пользователь, выполняющий отправку приглашения
     * @param vacancyId   идентификатор вакансии
     * @param inviteType  тип приглашения (откуда происходит приглашение)
     * @return идентификатор лидера
     */
    @Transactional
    @Override
    public void sendInvitationToLeader(Long leaderId,
                                       User currentUser,
                                       Long vacancyId,
                                       String inviteType) {
        Invitation invitation = new Invitation(leaderId, inviteType, Boolean.TRUE);
        userInvitationService.sendInvitations(Sets.newHashSet(invitation), currentUser, vacancyId);
    }

    /**
     * Получение или создание нового пользователя-лидера
     *
     * @param leaderEmailOrPhone email или телефон приглашаемого пользователя лидера
     * @param leaderName         ФИО приглашаемого пользователя лидера
     * @param currentUser        текущий авторизованный пользователь, выполняющий отправку приглашения
     * @return идентификатор лидера
     */
    @Transactional
    @Override
    public Long getOrCreateLeaderUser(String leaderEmailOrPhone,
                                      String leaderName,
                                      User currentUser) {
        if (isNotBlank(leaderEmailOrPhone)) {
            Long leaderId;
            if (EmailUtils.isValid(leaderEmailOrPhone)) {   // если указан email лидера - приглашаем по Email
                leaderId = userService.getOrCreateUser(leaderName, leaderEmailOrPhone, currentUser.getId());
            } else {    // если указан телефон лидера - приглашаем по номеру телефона
                leaderId = userService.getOrCreateUser(leaderName, null, leaderEmailOrPhone, currentUser.getId());
            }
            return leaderId;
        }
        return null;
    }

    /**
     * Получение признака прошел ли эталонный пользователь опрос
     *
     * @param leaderId идентификатор эталонного пользователя
     * @param surveyId идентификатор опроса
     * @return признак, прошел ли эталон опрос
     */
    @Override
    public boolean leaderPassedSurvey(Long leaderId, Long surveyId) {
        if (leaderId != null) {
            return getLeaderPassedProgress(leaderId, surveyId) >= LEADER_PASSED_SURVEY_PROGRESS;
        }
        return false;
    }

    /**
     * Получение кол-ва пройденных вопросов эталонного пользователя
     *
     * @param leaderId идентификатор эталонного пользователя
     * @param surveyId идентификатор опроса
     * @return кол-во пройденных вопросов
     */
    private Integer getLeaderPassedProgress(Long leaderId, Long surveyId) {
        UserSurveyState surveyState = userSurveyStateRepository.findByUserIdAndSurveyId(leaderId, surveyId);
        return surveyState != null ? surveyState.getProgress() : 0;
    }

}
