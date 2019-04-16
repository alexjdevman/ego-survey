package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.*;
import ru.airlabs.ego.core.repository.UserLinkRepository;
import ru.airlabs.ego.core.repository.UserSurveyStateRepository;
import ru.airlabs.ego.core.repository.VacancyUserRepository;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.service.ImageUploadService;
import ru.airlabs.ego.survey.service.VacancyDetailService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.airlabs.ego.survey.utils.SortingUtils.createOrderQueryForCustomSurveyResults;
import static ru.airlabs.ego.survey.utils.SortingUtils.createOrderQueryForVacancyUserDetails;

/**
 * Сервис для получения подробностей по вакансиям
 *
 * @author Aleksey Gorbachev
 */
@Service("vacancyDetailService")
@Transactional(readOnly = true)
public class VacancyDetailServiceImpl implements VacancyDetailService {

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

    /**
     * Репозиторий состояний прохождения опросов
     */
    @Autowired
    private UserSurveyStateRepository userSurveyStateRepository;

    /**
     * Репозиторий пользователей вакансий
     */
    @Autowired
    private VacancyUserRepository vacancyUserRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Получение откликов по вакансии
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return список данных по откликам
     */
    @Override
    public Page<VacancyUserDetail> getFeedbackForVacancy(Long vacancyId, User currentUser, Pageable pageable) {
        Vacancy vacancy = em.find(Vacancy.class, vacancyId);            // получаем вакансию
        Survey survey = em.find(Survey.class, vacancy.getSurveyId());   // получаем опрос
        Page<VacancyUserDetail> pagingDetails;
        if (survey.getAlgo() == 1) { // получение результатов для опроса "Соционика"
            pagingDetails = getDetails(vacancyId, currentUser.getId(), true, pageable);
            fillWithPhotos(pagingDetails.getContent());
        } else {    // получение результатов для всех прочих опросов
            pagingDetails = getDetailsForCustomSurvey(vacancyId, vacancy.getSurveyId(), pageable);
            fillUserInfoAndPhotos(pagingDetails.getContent(), currentUser.getId());
        }
        return pagingDetails;
    }

    /**
     * Получение приглашений по вакансии
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return список данных по приглашениям
     */
    @Override
    public Page<VacancyUserDetail> getInvitationsForVacancy(Long vacancyId, User currentUser, Pageable pageable) {
        Page<VacancyUserDetail> pagingDetails = getDetails(vacancyId, currentUser.getId(), false, pageable);
        fillWithPhotos(pagingDetails.getContent());
        return pagingDetails;
    }

    /**
     * Получение буфера приглашений по вакансии (приглашения с пустым способом отправки)
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return постраничный сортированный список данных по приглашениям из буфера
     */
    @Override
    public Page<VacancyUserDetail> getBufferInvitationsForVacancy(Long vacancyId, User currentUser, Pageable pageable) {
        return getBufferDetails(vacancyId, currentUser.getId(), pageable);
    }

    /**
     * Получение результатов для пользовательского опроса
     *
     * @param surveyId идентификатор опроса
     * @return список результатов с пагинацией
     */
    private Page<VacancyUserDetail> getDetailsForCustomSurvey(Long vacancyId,
                                                              Long surveyId,
                                                              Pageable pageable) {
        List<VacancyUserDetail> details = new ArrayList<>();
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForCustomSurveyResults(pageable.getSort()) + ") rn, " +
                "q.id_user, " +
                "q.dt_create, " +
                "q.progress, " +
                "q.calculated_parts, " +
                "q.dt_update, " +
                "q.list_result_perc, " +
                "q.list_conflict_perc, " +
                "q.list_avg_resp_sec " +
                "from users_q_list q " +
                "join v_data_user vu on vu.id_user = q.id_user and vu.id_v_data = :vacancyId and vu.is_active = 1 " +
                "where q.id_list = :surveyId and (q.calculated_parts > 0) " +
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("surveyId", surveyId);
        query.setParameter("vacancyId", vacancyId);
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalDetailsCountForCustomSurvey(surveyId, vacancyId);   // общее кол-во результатов

        List<Object[]> results = query.getResultList();
        for (Object[] record : results) {
            VacancyUserDetail detail = new VacancyUserDetail();
            detail.setVacancyId(vacancyId);
            Long userId = ((BigDecimal) record[1]).longValue();
            detail.setUserId(userId);
            detail.setSurveyCreate((Date) record[2]);
            detail.setProgress(((BigDecimal) record[3]).intValue());
            detail.setFinishedParts(((BigDecimal) record[4]).intValue());
            detail.setSurveyUpdate((Date) record[5]);
            if (record[6] != null) {
                detail.setCompareResult(((BigDecimal) record[6]).doubleValue());
            }
            if (record[7] != null) {   // % противоречий в ответах на вопросы
                detail.setConflictAnswerPercent(((BigDecimal) record[7]).intValue());
            }
            if (record[8] != null) {   // Среднее время на ответы в опроснике
                detail.setAverageAnswerTime(((BigDecimal) record[8]).doubleValue());
            }

            details.add(detail);
        }
        Page<VacancyUserDetail> pagingResult = new PageImpl<>(details, pageable, totalCount);
        return pagingResult;
    }

    /**
     * Получение приглашений или откликов по вакансии для текущего пользователя
     *
     * @param vacancyId     идентификатор вакансии
     * @param currentUserId идентификатор текущего пользователя
     * @param isFeedback    флаг получения откликов (если получаем только фидбеки - прогресс == 288)
     * @param pageable      настройки пагинации и сортировки
     * @return список приглашений или откликов
     */
    private Page<VacancyUserDetail> getDetails(Long vacancyId,
                                               Long currentUserId,
                                               boolean isFeedback,
                                               Pageable pageable) {
        List<VacancyUserDetail> details = new ArrayList<>();
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForVacancyUserDetails(pageable.getSort()) + ") rn, " +
                "v.id, " +
                "u.id as userId, " +
                "ul.email, " +
                "ul.name, " +
                "ul.phone, " +
                "vu.dt_create, " +
                "vu.is_active, " +
                "q.progress, " +
                "q.calculated_parts, " +
                "q.dt_update, " +
                "c.compared, " +
                "q.list_conflict_perc, " +
                "q.list_avg_resp_sec " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data " + (isFeedback ? "and vu.is_active = 1 " : "") +   // для фидбеков только активные участники
                "join users u on vu.id_user = u.id " + (isFeedback ? "and v.id_leader <> u.id " : "") +
                "join users_link ul on u.id = ul.id_user " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and v.id_user = :userId " +
                "and ul.id_parent = :userId " + // исключаем приглашения из буфера
                "and (vu.id_source <> 'B') and ((vu.id_source <> 'S') or (vu.id_source = 'S' and vu.is_active = 1)) " +
                (isFeedback ? "and (q.calculated_parts > 0) " : "") +
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", currentUserId);
        query.setParameter("vacancyId", vacancyId); // устанавливаем настройки пагинации
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalDetailsCount(vacancyId, currentUserId, isFeedback); // общее кол-во результатов

        List<Object[]> results = query.getResultList();
        for (Object[] record : results) {
            VacancyUserDetail detail = new VacancyUserDetail();
            detail.setVacancyId(((BigDecimal) record[1]).longValue());
            detail.setUserId(((BigDecimal) record[2]).longValue());
            detail.setEmail((String) record[3]);
            detail.setName((String) record[4]);
            detail.setPhone((String) record[5]);
            detail.setSurveyCreate((Date) record[6]);
            detail.setActive(((BigDecimal) record[7]).intValue() != 0);

            detail.setProgress(((BigDecimal) record[8]).intValue());
            detail.setFinishedParts(((BigDecimal) record[9]).intValue());
            detail.setSurveyUpdate((Date) record[10]);
            if (record[11] != null) {
                detail.setCompareResult(((BigDecimal) record[11]).doubleValue());
            }
            if (record[12] != null) {   // % противоречий в ответах на вопросы
                detail.setConflictAnswerPercent(((BigDecimal) record[12]).intValue());
            }
            if (record[13] != null) {   // Среднее время на ответы в опроснике
                detail.setAverageAnswerTime(((BigDecimal) record[13]).doubleValue());
            }

            details.add(detail);
        }
        Page<VacancyUserDetail> pagingResult = new PageImpl<VacancyUserDetail>(details, pageable, totalCount);
        return pagingResult;
    }

    /**
     * Получение приглашений из буфера по вакансии для текущего пользователя
     *
     * @param vacancyId     айди вакансии
     * @param currentUserId айди текущего пользователя
     * @param pageable      настройки пагинации
     * @return список приглашений из буфера
     */
    private Page<VacancyUserDetail> getBufferDetails(Long vacancyId,
                                                     Long currentUserId,
                                                     Pageable pageable) {
        List<VacancyUserDetail> details = new ArrayList<>();
        final String queryExp = "select s.* from (" +
                "select row_number() over (" + createOrderQueryForVacancyUserDetails(pageable.getSort()) + ") rn, " +
                "v.id, " +
                "u.id as userId, " +
                "ul.email, " +
                "ul.name, " +
                "ul.phone, " +
                "vu.dt_create, " +
                "vu.is_active, " +
                "q.progress, " +
                "q.calculated_parts, " +
                "q.dt_update, " +
                "c.compared, " +
                "q.list_conflict_perc, " +
                "q.list_avg_resp_sec " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data and vu.is_active = 1 " + // получаем только активные приглашения из буфера
                "join users u on vu.id_user = u.id " +
                "join users_link ul on u.id = ul.id_user " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and v.id_user = :userId " +
                "and ul.id_parent = :userId " +
                "and vu.id_source = 'B'" + // получаем приглашения из буфера
                ") s where s.rn > :fromRow and s.rn <= :toRow";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", currentUserId);
        query.setParameter("vacancyId", vacancyId); // устанавливаем настройки пагинации
        query.setParameter("fromRow", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("toRow", pageable.getPageNumber() * pageable.getPageSize() + pageable.getPageSize());

        final long totalCount = getTotalBufferDetailsCount(vacancyId, currentUserId); // общее кол-во приглашений

        List<Object[]> results = query.getResultList();
        for (Object[] record : results) {
            VacancyUserDetail detail = new VacancyUserDetail();
            detail.setVacancyId(((BigDecimal) record[1]).longValue());
            detail.setUserId(((BigDecimal) record[2]).longValue());
            detail.setEmail((String) record[3]);
            detail.setName((String) record[4]);
            detail.setPhone((String) record[5]);
            detail.setSurveyCreate((Date) record[6]);
            detail.setActive(((BigDecimal) record[7]).intValue() != 0);

            detail.setProgress(((BigDecimal) record[8]).intValue());
            detail.setFinishedParts(((BigDecimal) record[9]).intValue());
            detail.setSurveyUpdate((Date) record[10]);
            if (record[11] != null) {
                detail.setCompareResult(((BigDecimal) record[11]).doubleValue());
            }
            if (record[12] != null) {   // % противоречий в ответах на вопросы
                detail.setConflictAnswerPercent(((BigDecimal) record[12]).intValue());
            }
            if (record[13] != null) {   // Среднее время на ответы в опроснике
                detail.setAverageAnswerTime(((BigDecimal) record[13]).doubleValue());
            }

            details.add(detail);
        }
        Page<VacancyUserDetail> pagingResult = new PageImpl<VacancyUserDetail>(details, pageable, totalCount);
        return pagingResult;
    }

    /**
     * Получение общего кол-ва откликов или приглашений по вакансии
     *
     * @param vacancyId     идентификатор вакансии
     * @param currentUserId идентификатор текущего пользователя
     * @param isFeedback    флаг получения откликов (если получаем только фидбеки - прогресс == 288)
     * @return
     */
    private long getTotalDetailsCount(Long vacancyId,
                                      Long currentUserId,
                                      boolean isFeedback) {
        final String queryExp = "select count(*) " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data " + (isFeedback ? "and vu.is_active = 1 " : "") +
                "join users u on vu.id_user = u.id " + (isFeedback ? "and v.id_leader <> u.id " : "") +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and v.id_user = :userId " +
                "and (vu.id_source <> 'B') and ((vu.id_source <> 'S') or (vu.id_source = 'S' and vu.is_active = 1)) " +
                (isFeedback ? "and (q.calculated_parts > 0) " : "");
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", currentUserId);
        query.setParameter("vacancyId", vacancyId);
        return ((BigDecimal) query.getSingleResult()).longValue();
    }

    /**
     * Получение общего кол-ва приглашений из буфера по вакансии
     *
     * @param vacancyId     идентификатор вакансии
     * @param currentUserId идентификатор текущего пользователя
     * @return общее кол-во приглашений из буфера
     */
    private long getTotalBufferDetailsCount(Long vacancyId,
                                            Long currentUserId) {
        final String queryExp = "select count(*) " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data and vu.is_active = 1 " +
                "join users u on vu.id_user = u.id " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and v.id_user = :userId " +
                "and vu.id_source = 'B'";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", currentUserId);
        query.setParameter("vacancyId", vacancyId);
        return ((BigDecimal) query.getSingleResult()).longValue();
    }

    /**
     * Получение общего кол-ва результатов для пользовательского опроса
     *
     * @param surveyId  идентификатор опроса
     * @param vacancyId идентификатор вакансии
     * @return общее кол-во результатов
     */
    private long getTotalDetailsCountForCustomSurvey(Long surveyId, Long vacancyId) {
        final String queryExp = "select count(*) " +
                "from users_q_list q " +
                "join v_data_user vu on vu.id_user = q.id_user and vu.id_v_data = :vacancyId and vu.is_active = 1 " +
                "where q.id_list = :surveyId and (q.progress BETWEEN 96 and 288)";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("surveyId", surveyId);
        query.setParameter("vacancyId", vacancyId);
        return ((BigDecimal) query.getSingleResult()).longValue();
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
            VacancyUser vacancyUser = vacancyUserRepository.findByUserIdAndVacancyId(detail.getUserId(), detail.getVacancyId());
            detail.getImages().addAll(imagePaths);
            detail.setEmail(user.getEmail());
            detail.setName(user.getName());
            detail.setPhone(user.getPhone());
            detail.setActive(vacancyUser.getActive());
        }
    }

}
