package ru.airlabs.ego.survey.dto.vacancy;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.airlabs.ego.survey.dto.survey.TremorLevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Модель для передачи данных о приглашениях и откликах пользователей по вакансии на UI
 *
 * @author Aleksey Gorbachev
 */
public class VacancyUserDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор вакансии
     */
    private Long vacancyId;

    /**
     * Идентификатор пользователя
     */
    private Long userId;

    /**
     * Email участника
     */
    private String email;

    /**
     * ФИО участника
     */
    private String name;

    /**
     * Телефон участника
     */
    private String phone;

    /**
     * Дата отправки приглашения на опрос
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date surveyCreate;

    /**
     * Дата последнего ответа в опросе
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date surveyUpdate;

    /**
     * Текущее кол-во ответов в опросе
     */
    private Integer progress;

    /**
     * Кол-во пройденных частей опроса
     */
    private Integer finishedParts;

    /**
     * Процент совпадения с эталоном
     */
    private Double compareResult;

    /**
     * Список ссылок на загруженные фотографии пользователя (заполняется для откликов)
     */
    private List<String> images = new ArrayList<>();

    /**
     * Степень волнения участника, проходившего опрос
     */
    private TremorLevel tremorLevel;

    /**
     * Признак активности участника в вакансии
     */
    private Boolean active;

    /**
     * Среднее время на ответы в опроснике
     */
    private Double averageAnswerTime;

    /**
     * % противоречий в ответах на вопросы
     */
    private Integer conflictAnswerPercent;

    public Long getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(Long vacancyId) {
        this.vacancyId = vacancyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getSurveyCreate() {
        return surveyCreate;
    }

    public void setSurveyCreate(Date surveyCreate) {
        this.surveyCreate = surveyCreate;
    }

    public Date getSurveyUpdate() {
        return surveyUpdate;
    }

    public void setSurveyUpdate(Date surveyUpdate) {
        this.surveyUpdate = surveyUpdate;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getFinishedParts() {
        return finishedParts;
    }

    public void setFinishedParts(Integer finishedParts) {
        this.finishedParts = finishedParts;
    }

    public Double getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(Double compareResult) {
        this.compareResult = compareResult;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public TremorLevel getTremorLevel() {
        return tremorLevel;
    }

    public void setTremorLevel(TremorLevel tremorLevel) {
        this.tremorLevel = tremorLevel;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getAverageAnswerTime() {
        return averageAnswerTime;
    }

    public void setAverageAnswerTime(Double averageAnswerTime) {
        this.averageAnswerTime = averageAnswerTime;
    }

    public Integer getConflictAnswerPercent() {
        return conflictAnswerPercent;
    }

    public void setConflictAnswerPercent(Integer conflictAnswerPercent) {
        this.conflictAnswerPercent = conflictAnswerPercent;
    }
}
