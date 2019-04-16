package ru.airlabs.ego.survey.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.airlabs.ego.core.entity.*;
import ru.airlabs.ego.core.entity.user.UserCompare;
import ru.airlabs.ego.core.entity.util.ClientError;
import ru.airlabs.ego.core.repository.*;
import ru.airlabs.ego.core.repository.user.UserCompareRepository;
import ru.airlabs.ego.core.repository.util.ClientErrorRepository;
import ru.airlabs.ego.survey.dto.UIError;
import ru.airlabs.ego.survey.dto.survey.*;
import ru.airlabs.ego.survey.service.ImageUploadService;
import ru.airlabs.ego.survey.service.QuestionChainService;
import ru.airlabs.ego.survey.service.SurveyService;
import ru.airlabs.ego.survey.utils.MapUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Сервис для работы с опросами
 *
 * @author Aleksey Gorbachev
 */
@Service("surveyService")
@Transactional(readOnly = true)
public class SurveyServiceImpl implements SurveyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyServiceImpl.class);

    /**
     * Список идентификаторов стандартных категорий вопросов,
     * которые исключаются из раздела "Конструктор опросов" при показе на UI
     */
    private static final List<Long> SURVEY_CATEGORY_ID_IGNORE_LIST = Arrays.asList(1L, 2L, 3L, 4L);

    /**
     * Идентификатор стандартного опроса "Соционика"
     */
    private static final Long SURVEY_SOCIONICS_ID = 1L;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager em;

    /**
     * Сервис для работы с фотографиями пользователей
     */
    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Сервис для работы с цепочками вопросов
     */
    @Autowired
    private QuestionChainService questionChainService;

    /**
     * Репозиторий категории вопросов
     */
    @Autowired
    private QuestionCategoryRepository questionCategoryRepository;

    /**
     * Репозиторий локали категории вопросов
     */
    @Autowired
    private QuestionCategoryLocaleRepository questionCategoryLocaleRepository;

    /**
     * Репозиторий опросов
     */
    @Autowired
    private SurveyRepository surveyRepository;

    /**
     * Репозиторий локали опросов
     */
    @Autowired
    private SurveyLocaleRepository surveyLocaleRepository;

    /**
     * Репозиторий категорий опросов
     */
    @Autowired
    private SurveyTypeRepository surveyTypeRepository;

    /**
     * Репозиторий связи опросов с пользователями
     */
    @Autowired
    private UserSurveyLinkRepository surveyLinkRepository;

    /**
     * Репозиторий вопросов
     */
    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Репозиторий ответа на вопрос
     */
    @Autowired
    private QuestionAnswerRepository questionAnswerRepository;

    /**
     * Репозиторий состояния прохождения опроса пользователем
     */
    @Autowired
    private UserSurveyStateRepository userSurveyStateRepository;

    /**
     * Репозиторий сравнений пользователей
     */
    @Autowired
    private UserCompareRepository userCompareRepository;

    /**
     * Репозиторий ошибок клиента
     */
    @Autowired
    private ClientErrorRepository clientErrorRepository;

    /**
     * Движок управления транзакциями
     */
    private TransactionTemplate transactionTemplate;

    /**
     * Устпновить менеджер транзакций
     *
     * @param platformTransactionManager менеджер транзакций
     */
    @Autowired
    public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @Override
    public Survey findById(Long surveyId) {
        Optional<Survey> result = surveyRepository.findById(surveyId);
        return result.orElse(null);
    }

    /**
     * Создание нового опроса
     *
     * @param name        имя опроса
     * @param currentUser текущий пользователь, создающий опрос
     * @param locale      текущая локаль
     * @return новый опрос
     */
    @Transactional
    @Override
    public Survey addSurvey(String name, User currentUser, Locale locale) {
        Survey survey = surveyRepository.save(new Survey());
        survey.setActive(Boolean.TRUE);
        SurveyLocale surveyLocale = new SurveyLocale();
        surveyLocale.setName(name);
        surveyLocale.setSurveyId(survey.getId());
        surveyLocale.setLocaleId(locale.getLanguage().toUpperCase());
        surveyLocaleRepository.save(surveyLocale);

        UserSurveyLink surveyLink = new UserSurveyLink();
        surveyLink.setSurveyId(survey.getId());
        surveyLink.setUserId(currentUser.getId());
        surveyLinkRepository.save(surveyLink);
        return survey;
    }

    /**
     * Редактирование опроса
     *
     * @param surveyInfo данные по опросу
     * @param locale     текущая локаль
     * @return опрос
     */
    @Transactional
    @Override
    public Survey updateSurvey(SurveyInfo surveyInfo, Locale locale) {
        final Long surveyId = surveyInfo.getId();
        checkNotNull(surveyId, "Не заполнен идентификатор опроса");
        final String localeId = locale.getLanguage().toUpperCase();
        Survey survey = em.find(Survey.class, surveyId);
        checkNotNull(survey, "Не найден опрос с id %s", surveyId);
        SurveyLocale surveyLocale = surveyLocaleRepository.findBySurveyIdAndLocaleId(surveyId, localeId);
        checkNotNull(surveyLocale, "Не найдена локаль для опроса с id %s", surveyId);
        if (isNotBlank(surveyInfo.getName())) {
            surveyLocale.setName(surveyInfo.getName());
        }
        return survey;
    }

    @Transactional
    @Override
    public void fillSurveyWithCategories(Long surveyId, List<SurveyCategory> categories) {
        checkArgument(isSurveyEditable(surveyId),
                "Нельзя наполнять и менять категории опроса с id %s", surveyId);
        checkArgument(!categories.isEmpty(), "Список категорий опроса пустой");
        checkSurveyCategoriesForDuplicates(categories);

        List<SurveyType> surveyTypes = surveyTypeRepository.findAllBySurveyId(surveyId);
        surveyTypeRepository.deleteInBatch(surveyTypes);    // обновляем категории для опроса
        for (SurveyCategory category : categories) {
            final Long questionCategoryId = category.getId();
            checkArgument(questionCategoryRepository.existsById(questionCategoryId),
                    "Категории вопросов с id %s не существует", questionCategoryId);

            SurveyType surveyType = new SurveyType();
            surveyType.setSurveyId(surveyId);
            surveyType.setQuestionCategoryId(questionCategoryId);
            surveyType.setChainNumber(category.getChainNumber());
            surveyTypeRepository.save(surveyType);
        }
        questionChainService.fillQuestionChainForSurvey(surveyId);  // формируем цепочки вопросов для опроса
    }

    @Override
    public List<SurveyCategory> getSurveyCategories(Locale locale) {
        final String localeLanguage = locale.getLanguage().toUpperCase();
        final String queryExp = "select t.id, l.name " +
                "from q_item_type t, q_item_type_locale l " +
                "where t.is_active = 1 " +
                "and t.id = l.id_item_type " +
                "and l.locale = :locale " +
                // Не показываем стандартные 4 категории: Логику, Рационализм, Экстраверсию, Сенсорику.
                // Они не доступны для создания кастомных опросов.
                "and t.id not in :ignoreList " +
                "order by 1";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("locale", localeLanguage);
        query.setParameter("ignoreList", SURVEY_CATEGORY_ID_IGNORE_LIST);
        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(record -> new SurveyCategory(((BigDecimal) record[0]).longValue(), (String) record[1]))
                .collect(Collectors.toList());
    }

    /**
     * Получение данных по доступным опросам для пользователя
     *
     * @param userId идентификатор текущего пользователя
     * @param active признак активности опроса
     * @param locale текущая локаль
     * @return данные по доступным опросам
     */
    @Override
    public List<Map<String, Object>> getSurveyDataList(Long userId, Boolean active, Locale locale) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SurveyInfo> surveyInfoList = getAccessibleSurveyListForUser(userId, active, locale);
        for (SurveyInfo surveyInfo : surveyInfoList) {
            final Long surveyId = surveyInfo.getId();
            if (surveyId != SURVEY_SOCIONICS_ID) {  // стандартный опрос "Соционика" не показываем
                boolean isEditable = isSurveyEditable(surveyId);
                List<SurveyCategory> categories = getCategoriesForSurvey(surveyId, locale);
                Map<String, Object> map = MapUtils.<String, Object>builder()
                        .add("surveyId", surveyId)
                        .add("surveyName", surveyInfo.getName())
                        .add("isEditable", isEditable)
                        .add("categories", categories)
                        .build();

                result.add(map);

            }
        }
        return result;
    }

    @Override
    public boolean isSurveyEditable(Long surveyId) {
        // если опрос есть хотя бы в одной вакансии или исследовании, то редактировать его категории нельзя
        Query query = em.createNativeQuery("select count(1) from v_data v where v.id_list = :surveyId");
        query.setParameter("surveyId", surveyId);
        BigDecimal count = (BigDecimal) query.getSingleResult();
        return (count.intValue() == 0);
    }

    @Override
    public List<SurveyInfo> getAccessibleSurveyListForUser(Long userId, Boolean active, Locale locale) {
        final String localeLanguage = locale.getLanguage().toUpperCase();
        final String queryExp = "select uql.id_list, l.name " +
                "from users_q_list_link uql " +
                "join q_list q on q.id = uql.id_list " +
                "join q_list_locale l on q.id = l.id_list and l.locale = :locale " +
                "where (uql.id_user = :userId or uql.id_user = 0) " +
                "and q.is_active = :active " +
                "order by l.name";
        Query query = em.createNativeQuery(queryExp);
        query.setParameter("userId", userId);
        query.setParameter("active", active);
        query.setParameter("locale", localeLanguage);
        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(arr -> new SurveyInfo(((BigDecimal) arr[0]).longValue(), (String) arr[1]))
                .collect(Collectors.toList());
    }

    /**
     * Получить список неотвеченных вопросов
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @param locale   локаль
     * @param max      макс. кол-во вопросов
     * @return список
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<SurveyQuestion> getUnansweredSurveyQuestions(Long userId, Long surveyId, Locale locale, Integer max) {
        final String language = locale.getLanguage().toUpperCase();
        Map<String, Object> result = new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_SURVEY_PKG")
                .withProcedureName("GET_QUESTIONS")
                .declareParameters(
                        new SqlOutParameter("CUR", OracleTypes.CURSOR, (rs, i) ->
                                new SurveyQuestion(rs.getLong("ID"),
                                        rs.getString("NAME"),
                                        rs.getInt("ANSWER_SEC"))),
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_LIST", Types.BIGINT),
                        new SqlParameter("PLOCALE", Types.VARCHAR),
                        new SqlParameter("PCOUNT", Types.INTEGER))
                .execute(new MapSqlParameterSource()
                        .addValue("PID_USER", userId)
                        .addValue("PID_LIST", surveyId)
                        .addValue("PLOCALE", language)
                        .addValue("PCOUNT", max));

        return (List<SurveyQuestion>) result.get("CUR");
    }

    /**
     * Получение количества оставшихся вопросов и общее время на интервью на старте
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return количество оставшихся вопросов и общее время на интервью на старте
     */
    @Override
    public Map<String, Object> getRemainsQuestionsWithTime(Long userId, Long surveyId) {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_SURVEY_PKG")
                .withProcedureName("GET_QUESTIONS_REMAINS_SEC")
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_LIST", Types.BIGINT),
                        new SqlOutParameter("PCOUNT", Types.BIGINT),
                        new SqlOutParameter("PANSWER_SEC", Types.BIGINT))
                .execute(new MapSqlParameterSource()
                        .addValue("PID_USER", userId)
                        .addValue("PID_LIST", surveyId));
    }

    /**
     * Получение количества оставшихся вопросов
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return количество оставшихся вопросов
     */
    @Override
    public Integer getRemainsQuestionsCount(Long userId, Long surveyId) {
        BigDecimal result = new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_SURVEY_PKG")
                .withFunctionName("GET_QUESTIONS_REMAINS")
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_LIST", Types.BIGINT))
                .executeFunction(BigDecimal.class, new MapSqlParameterSource()
                        .addValue("PID_USER", userId)
                        .addValue("PID_LIST", surveyId));

        return result != null ? result.intValue() : null;
    }

    /**
     * Ответить на вопросы
     *
     * @param answers           ответы на вопросы
     * @param userSurveyStateId идентификатор состояния прохождения вопроса
     * @return количество принятых к ответу вопросов
     */
    @Override
    public SurveyResult answerSurveyQuestions(Collection<SurveyAnswer> answers, Long userSurveyStateId) {
        SurveyResult surveyResult = new SurveyResult();
        UserSurveyState state = userSurveyStateRepository.findById(userSurveyStateId).orElse(null);
        if (state == null) {
            return surveyResult;
        }
        answers = new HashSet<>(answers);
        answers.stream()
                .filter(a -> state.getUserId().equals(a.getUserId()))
                .parallel()
                .forEach(answer -> {
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transactionTemplate.execute(status -> {
                        try {
                            long result = answerSurveyQuestion(answer);
                            if (result > 0) {
                                QuestionAnswer a = questionAnswerRepository.getOne(result);
                                checkQuestionAnswerForUploadImage(a);
                                questionAnswerRepository.save(a);
                                surveyResult.totalAnswered.getAndIncrement();
                                surveyResult.appliedAnswerIds.add(answer.getId());
                            } else if (result == 0) {
                                surveyResult.totalRepeats.getAndIncrement();
                                surveyResult.repeatedAnswerIds.add(answer.getId());
                            } else {
                                LOGGER.error("Survey answer DB error at: {}", answer);
                            }
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                        return status;
                    });
                });
//        for (SurveyAnswer answer : answers) {
//            if (!state.getUserId().equals(answer.getUserId())) {
//                continue;
//            }
//            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//            transactionTemplate.execute(status -> {
//                try {
//                    long result = answerSurveyQuestion(answer);
//                    if (result > 0) {
//                        QuestionAnswer a = questionAnswerRepository.getOne(result);
//                        checkQuestionAnswerForUploadImage(a);
//                        questionAnswerRepository.save(a);
//                        surveyResult.totalAnswered++;
//                        surveyResult.appliedAnswerIds.add(answer.getId());
//                    } else if (result == 0) {
//                        surveyResult.totalRepeats++;
//                        surveyResult.repeatedAnswerIds.add(answer.getId());
//                    } else {
//                        LOGGER.error("Survey answer DB error at: {}", answer);
//                    }
//                } catch (Exception e) {
//                    LOGGER.error(e.getMessage(), e);
//                    throw new RuntimeException(e);
//                }
//                return status;
//            });
//        }
        return surveyResult;
    }

    /**
     * Проверка, загружалось ли фото для ответа на вопрос
     * (если фото ранее загружалось, то выставляется признак "Фото загружено" для ответа на вопрос)
     *
     * @param answer ответ на вопрос
     */
    private void checkQuestionAnswerForUploadImage(QuestionAnswer answer) {
        if (imageUploadService.isImageUploadForQuestionAnswer(answer)) {
            answer.setPhoto(Boolean.TRUE);
        }
    }

    /**
     * Проверка, содержит ли опрос данный вопрос
     *
     * @param surveyId   идентификатор опроса
     * @param questionId идентификатор вопроса
     * @return true - если содержит, иначе false
     */
    @Override
    public boolean isSurveyContainsQuestion(Long surveyId, Long questionId) {
        Optional<Question> question = questionRepository.findById(questionId);
        if (!question.isPresent()) {
            return Boolean.FALSE;
        }
        List<SurveyType> surveyTypes = surveyTypeRepository.findAllBySurveyId(surveyId);
        Optional<SurveyType> surveyType = surveyTypes.stream()
                .filter(s -> Objects.equals(s.getQuestionCategoryId(), question.get().getQuestionCategoryId()))
                .findFirst();
        return surveyType.isPresent();
    }

    /**
     * Изменение статуса опроса
     *
     * @param surveyId идентификатор опроса
     */
    @Transactional
    @Override
    public void changeSurveyState(Long surveyId) {
        Survey survey = em.find(Survey.class, surveyId);
        checkNotNull(survey, "Не найден опрос с id %s", surveyId);
        survey.setActive(!survey.getActive());
    }

    /**
     * Получение результата опроса пользователя
     * (на сколько % он соответствует эталону)
     *
     * @param userId   идентификатор пользователя
     * @param leaderId идентификатор эталонного сотрудника
     * @param surveyId идентификатор опроса
     * @return результат опроса пользователя (% схожести с эталоном)
     */
    @Override
    public Double compareUserWithLeaderBySurvey(Long userId, Long leaderId, Long surveyId) {
        UserCompare userCompare = userCompareRepository.findBySurveyIdAndFirstUserIdAndSecondUserId(surveyId, userId, leaderId);
        if (userCompare == null) {
            userCompare = userCompareRepository.findBySurveyIdAndFirstUserIdAndSecondUserId(surveyId, leaderId, userId);
        }
        return userCompare != null ? userCompare.getCompareResult() : null;
    }

    /**
     * Сохранение ошибки клиента при прохождении опроса
     *
     * @param userId      идентификатор пользователя, проходящего опрос
     * @param surveyId    идентификатор опроса
     * @param surveyError модель с текстом ошибки и юзерагентом
     */
    @Transactional
    @Override
    public void saveSurveyError(Long userId, Long surveyId, SurveyError surveyError) {
        try {
            ClientError clientError = new ClientError();
            clientError.setUserId(userId);
            clientError.setSurveyId(surveyId);
            UIError error = surveyError.getError();
            clientError.setAgent(surveyError.getAgent());
            clientError.setDescription(new ObjectMapper().writeValueAsString(error));
            clientError.setDate(new Date());
            clientErrorRepository.save(clientError);
        } catch (JsonProcessingException ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Сохранение данных по геолокации опроса
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param location      данные по геолокации (широта + долгота)
     */
    @Transactional
    @Override
    public void saveSurveyLocation(Long surveyStateId, SurveyStateLocation location) {
        UserSurveyState state = userSurveyStateRepository.findById(surveyStateId).get();
        state.setLongitude(location.getLongitude());
        state.setLatitude(location.getLatitude());
    }

    /**
     * Получение данных по глобальным параметрам пользователей (среднее время и глобальный % противоречий)
     *
     * @return глобальные параметры пользователей
     */
    @Override
    public GlobalUserScore getGlobalUserScore() {
        GlobalUserScore score = new GlobalUserScore();
        final String queryExp = "select p.* from GLOBAL_PARAM p";
        Query query = em.createNativeQuery(queryExp);
        List<Object[]> results = query.getResultList();
        for (Object[] arr : results) {
            if ("LIST_AVG_CONFLICT_PERC".equals(arr[0])) {
                score.setConflictAnswerPercent(Integer.parseInt((String) arr[1]));
            }
            if ("LIST_AVG_RESP_SEC".equals(arr[0])) {
                score.setAverageAnswerTime(Double.parseDouble((String) arr[1]));
            }
        }
        return score;
    }

    /**
     * Получение списка категорий для опроса (для UI)
     *
     * @param surveyId идентификатор опроса
     * @param locale   текущая локаль
     * @return список категорий
     */
    private List<SurveyCategory> getCategoriesForSurvey(Long surveyId, Locale locale) {
        final String localeId = locale.getLanguage().toUpperCase();
        List<SurveyType> surveyTypes = surveyTypeRepository.findAllBySurveyId(surveyId);
        return surveyTypes.stream()
                .sorted(Comparator.comparing(SurveyType::getChainNumber))
                .map(surveyType -> {
                    QuestionCategoryLocale questionCategoryLocale = questionCategoryLocaleRepository
                            .findByQuestionCategoryIdAndLocaleId(surveyType.getQuestionCategoryId(), localeId);
                    return new SurveyCategory(surveyType.getQuestionCategoryId(),
                            questionCategoryLocale.getName(),
                            surveyType.getChainNumber());
                })
                .collect(Collectors.toList());
    }

    /**
     * Проверяем список категорий вопросов на наличие дубликатов
     * и одинаковых порядковых номеров категорий
     *
     * @param categories список категорий вопросов
     */
    private void checkSurveyCategoriesForDuplicates(List<SurveyCategory> categories) {
        final Set<Long> categoryIds = categories.stream()
                .map(SurveyCategory::getId)
                .collect(Collectors.toSet());
        checkArgument(categoryIds.size() == categories.size(),
                "Список категорий опроса содержит одинаковые категории");
        final Set<Integer> chainNumbers = categories.stream()
                .map(SurveyCategory::getChainNumber)
                .collect(Collectors.toSet());
        checkArgument(chainNumbers.size() == categories.size(),
                "Список категорий опроса содержит одинаковые порядковые номера категорий");
    }

    /**
     * Сохранить ответ на вопрос
     *
     * @param answer ответ
     * @return > 0 - идентификатор сохраненной записи
     * = 0 - ответ уже был принят заранее
     * = -1 - ошибка сохранения в БД
     * @throws ParseException
     * @throws JsonProcessingException
     */
    private long answerSurveyQuestion(SurveyAnswer answer) throws ParseException, JsonProcessingException {
        return new SimpleJdbcCall(dataSource)
                .withCatalogName("Q_SURVEY_PKG")
                .withFunctionName("SET_ANSWER")
                .declareParameters(
                        new SqlParameter("PID_USER", Types.BIGINT),
                        new SqlParameter("PID_ITEM", Types.BIGINT),
                        new SqlParameter("PANSWER", Types.INTEGER),
                        new SqlParameter("PDT_VIEW", Types.TIMESTAMP),
                        new SqlParameter("PDT_ANSWER", Types.TIMESTAMP),
                        new SqlParameter("PJSON", Types.CLOB))
                .executeFunction(BigDecimal.class, new MapSqlParameterSource()
                        .addValue("PID_USER", answer.getUserId())
                        .addValue("PID_ITEM", answer.getId())
                        .addValue("PANSWER", answer.isAnswer() ? 1 : 0)
                        .addValue("PDT_VIEW", isNotBlank(answer.getViewDate()) ? new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss:SSS").parse(answer.getViewDate()) : null)
                        .addValue("PDT_ANSWER", isNotBlank(answer.getAnswerDate()) ? new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss:SSS").parse(answer.getAnswerDate()) : null)
                        .addValue("PJSON", new ObjectMapper().writeValueAsString(answer))).longValue();
    }
}
