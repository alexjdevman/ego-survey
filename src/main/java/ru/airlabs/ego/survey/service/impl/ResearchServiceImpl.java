package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.Survey;
import ru.airlabs.ego.core.entity.UserLink;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.core.repository.UserLinkRepository;
import ru.airlabs.ego.survey.dto.user.UserCompareResult;
import ru.airlabs.ego.survey.dto.user.UserGroupCompare;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.service.ImageUploadService;
import ru.airlabs.ego.survey.service.ResearchService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static ru.airlabs.ego.survey.utils.SortingUtils.createOrderQueryForCustomSurveyResults;
import static ru.airlabs.ego.survey.utils.SortingUtils.createOrderQueryForVacancyUserDetails;

@Service("researchService")
@Transactional(readOnly = true)
public class ResearchServiceImpl implements ResearchService {

    /**
     * Сервис для работы с загруженными фотографиями пользователей
     */
    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Репозиторий связей пользователей
     */
    @Autowired
    private UserLinkRepository userLinkRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Получение списка участников исследования с процентом их совпадения
     *
     * @param researchId идентификатор исследования
     * @return список участников исследования
     */
    @Override
    public List<UserCompareResult> getUserCompareListForResearch(Long researchId) {
        checkIfResearch(researchId);
        final String queryExp = "select t.id_user, t.compared, z.name, z.email " +
                "from (" +
                "select u.id_user, round(avg(c.compared), 2) as compared " +
                "from v_data v, v_data_user u, users_q_compare c " +
                "where v.id = :researchId  " +
                "and v.id = u.id_v_data " +
                "and u.is_active = 1 " +
                "and v.id_list = c.id_list " +
                "and u.id_user = c.id_user1 " +
                "group by u.id_user " +
                ") t, users z " +
                "where t.id_user = z.id " +
                "order by 2 desc";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("researchId", researchId);
        List<Object[]> records = query.getResultList();
        return records.stream()
                .map(record -> new UserCompareResult(((BigDecimal) record[0]).longValue(),  // userId
                        (String) record[2], // name
                        (String) record[3], // email
                        ((BigDecimal) record[1]).doubleValue()))    //compareResult
                .collect(Collectors.toList());
    }

    /**
     * Получение данных о сравнении групп пользователей с конкретным выбранным пользователем
     *
     * @param researchId идентификатор исследования
     * @param userId     идентификатор пользователя
     * @return данные о сравнении групп пользователей
     */
    @Override
    public List<UserGroupCompare> getGroupCompareWithUser(Long researchId, Long userId) {
        checkIfResearch(researchId);
        final String queryExp = "select trunc((c.compared - 0.001)/(100/8)) as column_num, count(1) cnt_users, round(avg(c.compared), 2) as avg_compared " +
                "from v_data v, v_data_user u, users_q_compare c " +
                "where v.id = :researchId " +
                "and v.id = u.id_v_data " +
                "and u.is_active = 1 " +
                "and v.id_list = c.id_list " +
                "and u.id_user = c.id_user2 " +
                "and c.id_user1 = :userId " +
                // выражение (c.compared - 0.001)/(100/8) обеспечивает попадание в 8 диапазонов результатов с граничными значениями сравнения (100% и 0)
                "group by trunc((c.compared - 0.001)/(100/8)) " +
                "order by 3 desc";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("researchId", researchId);
        query.setParameter("userId", userId);
        List<Object[]> records = query.getResultList();
        return records.stream()
                .map(record -> new UserGroupCompare(((BigDecimal) record[0]).intValue(),
                        ((BigDecimal) record[1]).intValue(),
                        ((BigDecimal) record[2]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * Получение данных о сравнении групп пользователей для категорийного исследования ("Поиск лидера")
     *
     * @param researchId    идентификатор исследования
     * @param currentUserId идентификатор пользователя проводящего исследование
     * @return данные о сравнении групп пользователей для категорийного исследования
     */
    @Override
    public List<UserGroupCompare> getGroupCompareForCategoryResearch(Long researchId, Long currentUserId) {
        Vacancy research = em.find(Vacancy.class, researchId);            // получаем исследование
        Survey survey = em.find(Survey.class, research.getSurveyId());   // получаем опрос
        List<UserGroupCompare> result;
        if (survey.getAlgo() == 1) { // получение результатов для опроса "Соционика"
            result = getCategoryGroupForSocionics(researchId, currentUserId);
        } else {
            result = getCategoryGroupForCustomSurvey(researchId, research.getSurveyId());
        }
        return result;
    }

    /**
     * Получение подробностей столбца сравнения групп пользователей для категорийного исследования ("Поиск лидера")
     *
     * @param researchId        идентификатор исследования
     * @param currentUserId     идентификатор пользователя проводящего исследование
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @return список данных с возможностью пагинации
     */
    @Override
    public Page<VacancyUserDetail> getGroupCompareCategoryDetails(Long researchId,
                                                                  Long currentUserId,
                                                                  Integer chartColumnNumber,
                                                                  Pageable pageable) {
        Vacancy research = em.find(Vacancy.class, researchId);            // получаем исследование
        Survey survey = em.find(Survey.class, research.getSurveyId());   // получаем опрос
        Page<VacancyUserDetail> details;
        if (survey.getAlgo() == 1) { // получение результатов для опроса "Соционика"
            details = getCategoryDetailsForSocionics(researchId,
                    currentUserId,
                    chartColumnNumber,
                    survey.getId(),
                    pageable);
        } else {
            details = getCategoryDetailsForCustomSurvey(researchId,
                    currentUserId,
                    chartColumnNumber,
                    survey.getId(),
                    pageable);
        }
        return details;
    }


    /**
     * Получение подробностей столбца сравнения в рамках конкретного диапазона совпадения
     *
     * @param researchId        идентификатор исследования
     * @param userId            идентификатор участника для сравнения
     * @param currentUserId     идентификатор пользователя проводящего исследование
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @return список данных сравнения участников опроса с возможностью пагинации
     */
    @Override
    public Page<VacancyUserDetail> getGroupCompareUserDetails(Long researchId,
                                                              Long userId,
                                                              Long currentUserId,
                                                              Integer chartColumnNumber,
                                                              Pageable pageable) {
        checkIfResearch(researchId);
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForVacancyUserDetails(pageable.getSort()) + ") rn, " +
                "v.id, u.id as userId, ul.email, ul.name, ul.phone, vu.dt_create, q.progress, q.calculated_parts, q.dt_update, c.compared " +
                "from v_data v, v_data_user vu, users_q_compare c, users_q_list q, users u, users_link ul " +
                "where v.id = :researchId " +
                "and v.id = vu.id_v_data " +
                "and v.id_list = c.id_list " +
                "and vu.is_active = 1 " +
                "and vu.id_user = u.id " +
                "and u.id = ul.id_user " +
                "and ul.id_parent = :currentUserId " +
                "and vu.id_user = c.id_user2 " +
                "and v.id_list = q.id_list and vu.id_user = q.id_user " +
                "and c.id_user1 = :userId " +
                // выражение (c.compared - 0.001)/(100/8) обеспечивает попадание в 8 диапазонов результатов с граничными значениями сравнения (100% и 0)
                "and trunc((c.compared - 0.001)/(100/8)) = :chartColumnNumber " +
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", userId);
        query.setParameter("currentUserId", currentUserId);
        query.setParameter("researchId", researchId);
        query.setParameter("chartColumnNumber", chartColumnNumber);
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalUserDetailsCount(researchId, userId, currentUserId, chartColumnNumber);

        List<Object[]> results = query.getResultList();
        List<VacancyUserDetail> details = results.stream()
                .map(record -> {
                    VacancyUserDetail detail = new VacancyUserDetail();
                    detail.setVacancyId(((BigDecimal) record[1]).longValue());
                    detail.setUserId(((BigDecimal) record[2]).longValue());
                    detail.setEmail((String) record[3]);
                    detail.setName((String) record[4]);
                    detail.setPhone((String) record[5]);
                    detail.setSurveyCreate((Date) record[6]);
                    detail.setProgress(((BigDecimal) record[7]).intValue());
                    detail.setFinishedParts(((BigDecimal) record[8]).intValue());
                    detail.setSurveyUpdate((Date) record[9]);
                    detail.setCompareResult(((BigDecimal) record[10]).doubleValue());
                    return detail;
                })
                .collect(Collectors.toList());
        fillWithPhotos(details);
        Page<VacancyUserDetail> page = new PageImpl<>(details, pageable, totalCount);
        return page;
    }

    private Page<VacancyUserDetail> getCategoryDetailsForSocionics(Long researchId,
                                                                   Long currentUserId,
                                                                   Integer chartColumnNumber,
                                                                   Long surveyId,
                                                                   Pageable pageable) {
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForVacancyUserDetails(pageable.getSort()) + ") rn, " +
                "v.id, u.id as userId, ul.email, ul.name, ul.phone, vu.dt_create, q.progress, q.calculated_parts, q.dt_update, c.compared " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data and vu.is_active = 1 " +
                "join users u on vu.id_user = u.id and v.id_leader <> u.id " +
                "join users_link ul on u.id = ul.id_user " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :researchId " +
                "and v.id_user = :userId " +
                "and ul.id_parent = :userId " +
                "and vu.is_employee = 1 " +
                "and (q.calculated_parts > 0) " +
                // выражение (c.compared - 0.001)/(100/8) обеспечивает попадание в 8 диапазонов результатов с граничными значениями сравнения (100% и 0)
                "and trunc((c.compared - 0.001)/(100/8)) = :chartColumnNumber " +
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("researchId", researchId);
        query.setParameter("userId", currentUserId);
        query.setParameter("chartColumnNumber", chartColumnNumber);
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalCategoryDetailsCount(researchId, currentUserId, chartColumnNumber, surveyId);

        List<Object[]> results = query.getResultList();
        List<VacancyUserDetail> details = results.stream()
                .map(record -> {
                    VacancyUserDetail detail = new VacancyUserDetail();
                    detail.setVacancyId(((BigDecimal) record[1]).longValue());
                    detail.setUserId(((BigDecimal) record[2]).longValue());
                    detail.setEmail((String) record[3]);
                    detail.setName((String) record[4]);
                    detail.setPhone((String) record[5]);
                    detail.setSurveyCreate((Date) record[6]);
                    detail.setProgress(((BigDecimal) record[7]).intValue());
                    detail.setFinishedParts(((BigDecimal) record[8]).intValue());
                    detail.setSurveyUpdate((Date) record[9]);
                    detail.setCompareResult(((BigDecimal) record[10]).doubleValue());
                    return detail;
                })
                .collect(Collectors.toList());
        fillWithPhotos(details);
        Page<VacancyUserDetail> page = new PageImpl<>(details, pageable, totalCount);
        return page;
    }

    private Page<VacancyUserDetail> getCategoryDetailsForCustomSurvey(Long researchId,
                                                                      Long currentUserId,
                                                                      Integer chartColumnNumber,
                                                                      Long surveyId,
                                                                      Pageable pageable) {
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForCustomSurveyResults(pageable.getSort()) + ") rn, " +
                "q.id_user, q.progress, q.calculated_parts, q.dt_create, q.dt_update, q.list_result_perc " +
                "from users_q_list q " +
                "join v_data_user vu on vu.id_user = q.id_user and vu.id_v_data = :researchId and vu.is_active = 1 " +
                "where q.id_list = :surveyId and (q.calculated_parts > 0) and q.list_result_perc is not null " +
                "and trunc((q.list_result_perc - 0.001)/(100/8)) = :chartColumnNumber " +
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("researchId", researchId);
        query.setParameter("surveyId", surveyId);
        query.setParameter("chartColumnNumber", chartColumnNumber);
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalCategoryDetailsCount(researchId, currentUserId, chartColumnNumber, surveyId);

        List<Object[]> results = query.getResultList();
        List<VacancyUserDetail> details = results.stream()
                .map(record -> {
                    VacancyUserDetail detail = new VacancyUserDetail();
                    detail.setVacancyId(researchId);
                    detail.setUserId(((BigDecimal) record[1]).longValue());
                    detail.setProgress(((BigDecimal) record[2]).intValue());
                    detail.setFinishedParts(((BigDecimal) record[3]).intValue());
                    detail.setSurveyCreate((Date) record[4]);
                    detail.setSurveyUpdate((Date) record[5]);
                    detail.setCompareResult(((BigDecimal) record[6]).doubleValue());
                    return detail;
                })
                .collect(Collectors.toList());
        fillUserInfoAndPhotos(details, currentUserId);
        Page<VacancyUserDetail> page = new PageImpl<>(details, pageable, totalCount);
        return page;
    }

    /**
     * Получение данных о сравнении групп пользователей для категорийного исследования для опроса Соционика
     *
     * @param researchId    идентификатор исследования
     * @param currentUserId идентификатор пользователя проводящего исследование
     * @return данные о сравнении групп пользователей для категорийного исследования для опроса Соционика
     */
    private List<UserGroupCompare> getCategoryGroupForSocionics(Long researchId, Long currentUserId) {
        final String queryExp = "select trunc((c.compared - 0.001)/(100/8)) as column_num, count(1) cnt_users, round(avg(c.compared), 2) as avg_compared " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data and vu.is_active = 1 " +
                "join users u on vu.id_user = u.id and v.id_leader <> u.id " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and v.id_user = :userId " +
                "and vu.is_employee = 1 " +
                "and (q.calculated_parts > 0) " +
                // выражение (c.compared - 0.001)/(100/8) обеспечивает попадание в 8 диапазонов результатов с граничными значениями сравнения (100% и 0)
                "group by trunc((c.compared - 0.001)/(100/8)) " +
                "order by 3 desc";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", currentUserId);
        query.setParameter("vacancyId", researchId);
        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(record -> new UserGroupCompare(((BigDecimal) record[0]).intValue(),
                        ((BigDecimal) record[1]).intValue(),
                        ((BigDecimal) record[2]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * Получение данных о сравнении групп пользователей для категорийного исследования для пользовательского опроса
     *
     * @param researchId идентификатор исследования
     * @param surveyId идентификатор опроса
     * @return данные о сравнении групп пользователей для категорийного исследования для пользовательского опроса
     */
    private List<UserGroupCompare> getCategoryGroupForCustomSurvey(Long researchId, Long surveyId) {
        final String queryExp = "select trunc((q.list_result_perc - 0.001)/(100/8)) as column_num, count(1) cnt_users, round(avg(q.list_result_perc), 2) as avg_compared " +
                "from users_q_list q " +
                "join v_data_user vu on vu.id_user = q.id_user and vu.id_v_data = :researchId and vu.is_active = 1 " +
                "where q.id_list = :surveyId and (q.calculated_parts > 0) and q.list_result_perc is not null " +
                "group by trunc((q.list_result_perc - 0.001)/(100/8)) " +
                "order by 3 desc";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("surveyId", surveyId);
        query.setParameter("researchId", researchId);
        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(record -> new UserGroupCompare(((BigDecimal) record[0]).intValue(),
                        ((BigDecimal) record[1]).intValue(),
                        ((BigDecimal) record[2]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * Получение общего кол-ва записей для подробностей сравнения участников
     *
     * @param researchId        идентификатор исследования
     * @param userId            идентификатор участника для сравнения
     * @param currentUserId     идентификатор пользователя проводящего исследование
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @return кол-во записей входящих в требуемый диапазон
     */
    private long getTotalUserDetailsCount(Long researchId,
                                          Long userId,
                                          Long currentUserId,
                                          Integer chartColumnNumber) {
        final String queryExp = "select count(*) " +
                "from v_data v, v_data_user vu, users_q_compare c, users_q_list q, users u, users_link ul " +
                "where v.id = :researchId " +
                "and v.id = vu.id_v_data " +
                "and v.id_list = c.id_list " +
                "and vu.is_active = 1 " +
                "and vu.id_user = u.id " +
                "and u.id = ul.id_user " +
                "and ul.id_parent = :currentUserId " +
                "and vu.id_user = c.id_user2 " +
                "and v.id_list = q.id_list and vu.id_user = q.id_user " +
                "and c.id_user1 = :userId " +
                "and trunc((c.compared - 0.001)/(100/8)) = :chartColumnNumber ";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", userId);
        query.setParameter("currentUserId", currentUserId);
        query.setParameter("researchId", researchId);
        query.setParameter("chartColumnNumber", chartColumnNumber);
        return ((BigDecimal) query.getSingleResult()).longValue();
    }

    /**
     * Получение общего кол-ва записей для подробностей сравнения участников для категорийного исследования
     *
     * @param researchId        идентификатор исследования
     * @param currentUserId     идентификатор пользователя, проводящего исследование
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param surveyId          идентификатор опроса
     * @return кол-во записей входящих в требуемый диапазон
     */
    private long getTotalCategoryDetailsCount(Long researchId,
                                              Long currentUserId,
                                              Integer chartColumnNumber,
                                              Long surveyId) {
        final String queryExp;
        if (surveyId == 1L) { // для опроса "Соционика"
            queryExp = "select count(*) " +
                    "from v_data v " +
                    "join v_data_user vu on v.id = vu.id_v_data and vu.is_active = 1 " +
                    "join users u on vu.id_user = u.id and v.id_leader <> u.id " +
                    "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                    "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                    "where v.id = :researchId " +
                    "and v.id_user = :userId " +
                    "and vu.is_employee = 1 " +
                    "and (q.calculated_parts > 0) " +
                    "and trunc((c.compared - 0.001)/(100/8)) = :chartColumnNumber ";
        } else {
            queryExp = "select count(*) " +
                    "from users_q_list q " +
                    "join v_data_user vu on vu.id_user = q.id_user and vu.id_v_data = :researchId and vu.is_active = 1 " +
                    "where q.id_list = :surveyId and (q.calculated_parts > 0) and q.list_result_perc is not null " +
                    "and trunc((q.list_result_perc - 0.001)/(100/8)) = :chartColumnNumber ";
        }
        Query query = em.createNativeQuery(queryExp);
        if (surveyId == 1L) {
            query.setParameter("userId", currentUserId);
            query.setParameter("researchId", researchId);
            query.setParameter("chartColumnNumber", chartColumnNumber);
        } else {
            query.setParameter("researchId", researchId);
            query.setParameter("surveyId", surveyId);
            query.setParameter("chartColumnNumber", chartColumnNumber);
        }
        return ((BigDecimal) query.getSingleResult()).longValue();
    }

    /**
     * Метод проверки, является ли сущность исследованием
     *
     * @param id идентификатор вакансии или исследования
     */
    private void checkIfResearch(Long id) {
        Vacancy vacancy = em.find(Vacancy.class, id);
        checkArgument(!vacancy.getVacancy(), "Вакансия с id %s не является исследованием", id);
    }

    /**
     * Заполнение списка ссылок на загруженные фотографии для откликов и приглашений
     *
     * @param details отклики и приглашения
     */
    private void fillWithPhotos(List<VacancyUserDetail> details) {
        for (VacancyUserDetail detail : details) {
            List<String> imagePaths = imageUploadService.getImagesForUserAndVacancy(detail.getUserId(), detail.getVacancyId());
            detail.getImages().addAll(imagePaths);
        }
    }

    private void fillUserInfoAndPhotos(List<VacancyUserDetail> details, Long currentUserId) {
        for (VacancyUserDetail detail : details) {
            UserLink user = userLinkRepository.findByUserIdAndParentId(detail.getUserId(), currentUserId);

            List<String> imagePaths = imageUploadService.getImagesForUserAndVacancy(detail.getUserId(), detail.getVacancyId());
            detail.getImages().addAll(imagePaths);
            detail.setEmail(user.getEmail());
            detail.setName(user.getName());
            detail.setPhone(user.getPhone());
        }
    }

}
