package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.UserLink;
import ru.airlabs.ego.core.entity.VacancyUser;
import ru.airlabs.ego.core.repository.UserLinkRepository;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.core.repository.VacancyUserRepository;
import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;
import ru.airlabs.ego.survey.service.ImageUploadService;
import ru.airlabs.ego.survey.service.UserDetailsService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Сервис для работы с данными пользователей
 *
 * @author Aleksey Gorbachev
 */
@Service("userProfileService")
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * Сервис для работы с загруженными фотографиями пользователей
     */
    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Репозиторий пользователей
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий связей пользователей
     */
    @Autowired
    private UserLinkRepository userLinkRepository;

    /**
     * Репозиторий пользователей вакансий
     */
    @Autowired
    private VacancyUserRepository vacancyUserRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Получение карточки клиента
     *
     * @param userId           идентификатор клиента
     * @param vacancyId        идентификатор вакансии
     * @param currentManagerId идентификатор текущего авторизованного пользователя (HR)
     * @return данные по карточке клиента
     */
    @Override
    public VacancyUserDetail getUserDetailsByVacancy(Long userId, Long vacancyId, Long currentManagerId) {
        final String queryExp = "select v.id, " +
                "ul.email, " +
                "ul.name, " +
                "ul.phone, " +
                "vu.dt_create, " +
                "q.progress, " +
                "trunc(q.progress/(288/3)) as finishedParts, " +
                "q.dt_update, " +
                "c.compared, " +
                "q.list_conflict_perc, " +
                "q.list_avg_resp_sec " +
                "from v_data v " +
                "join v_data_user vu on v.id = vu.id_v_data " +
                "join users u on vu.id_user = u.id " +
                "join users_link ul on u.id = ul.id_user " +
                "join users_q_list q on v.id_list = q.id_list and vu.id_user = q.id_user " +
                "left join users_q_compare c on q.id_user = c.id_user2 and v.id_leader = c.id_user1 and q.id_list = c.id_list " +
                "where v.id = :vacancyId " +
                "and u.id = :userId " +
                "and ul.id_parent = :currentManagerId " +
                "and v.id_user = :currentManagerId " +
                "order by vu.dt_create desc, q.dt_update desc, c.compared desc nulls last";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("currentManagerId", currentManagerId);
        query.setParameter("userId", userId);
        query.setParameter("vacancyId", vacancyId);
        List<Object[]> results = query.getResultList();
        checkArgument(!results.isEmpty(), "Не найдены данные клиента с id %s для вакансии с id %s", userId, vacancyId);
        Object[] record = results.get(0);

        VacancyUserDetail detail = new VacancyUserDetail();
        detail.setVacancyId(((BigDecimal) record[0]).longValue());
        detail.setEmail((String) record[1]);
        detail.setName((String) record[2]);
        detail.setPhone((String) record[3]);
        detail.setSurveyCreate((Date) record[4]);
        detail.setProgress(((BigDecimal) record[5]).intValue());
        detail.setFinishedParts(((BigDecimal) record[6]).intValue());
        detail.setSurveyUpdate((Date) record[7]);
        if (record[8] != null) {
            detail.setCompareResult(((BigDecimal) record[8]).doubleValue());
        }
        if (record[9] != null) {   // % противоречий в ответах на вопросы
            detail.setConflictAnswerPercent(((BigDecimal) record[9]).intValue());
        }
        if (record[10] != null) {   // Среднее время на ответы в опроснике
            detail.setAverageAnswerTime(((BigDecimal) record[10]).doubleValue());
        }

        fillWithPhotos(detail);
        return detail;
    }

    /**
     * Редактирование данных пользователя
     *
     * @param userId        идентификатор пользователя
     * @param currentUserId идентификатор текущего пользователя
     * @param userDetail    данные из формы редактирования пользователя
     */
    @Transactional
    @Override
    public void updateUserDetails(Long userId,
                                  Long currentUserId,
                                  UserDetail userDetail) {
        UserLink user = userLinkRepository.findByUserIdAndParentId(userId, currentUserId);
        checkArgument(user != null, "Не найдены данные пользователя с id %s", userId);
        userDetail.validate();

        final String email = userDetail.getEmail();
        // если меняется email
        if (isNotBlank(email) && !email.equals(user.getEmail())) {   // проверка существования пользователя с таким же email
            checkArgument(userRepository.findByEmail(email) == null,
                    "Пользователь с email %s уже существует в системе", email);
            user.setEmail(email);
        }
        user.setName(userDetail.getName());
        user.setPhone(userDetail.getPhone());
    }

    /**
     * Удаление (деактивация) пользователя из вакансии или исследования
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии или исследования
     */
    @Transactional
    @Override
    public void deleteUserFromVacancy(Long userId, Long vacancyId) {
        VacancyUser vacancyUser = vacancyUserRepository.findByUserIdAndVacancyId(userId, vacancyId);
        checkNotNull(vacancyUser, "Не найден пользователь с id %s для вакансии с id %s", userId, vacancyId);
        vacancyUser.setActive(Boolean.FALSE);
    }

    private void fillWithPhotos(VacancyUserDetail detail) {
        List<String> imagePaths = imageUploadService.getImagesForUserAndVacancy(detail.getUserId(), detail.getVacancyId());
        detail.getImages().addAll(imagePaths);
    }
}
