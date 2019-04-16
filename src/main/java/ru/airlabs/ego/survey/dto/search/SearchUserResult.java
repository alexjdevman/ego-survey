package ru.airlabs.ego.survey.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Objects;

/**
 * Модель для передачи результатов поиска по пользователям на UI
 *
 * @author Aleksey Gorbachev
 */
public class SearchUserResult {

    /**
     * Идентификатор пользователя
     */
    private Long userId;

    /**
     * Полное имя пользователя
     */
    private String name;

    /**
     * Email
     */
    private String email;

    /**
     * Телефон
     */
    private String phone;

    /**
     * имя пользователя
     */
    private String firstName;

    /**
     * фамилия пользователя
     */
    private String sureName;

    /**
     * отчество пользователя
     */
    private String parentName;

    /**
     * Пол
     */
    private String sex;

    /**
     * Дата рождения
     */
    @JsonFormat(pattern = "dd.MM.yyyy")
    private Date birthDate;

    /**
     * Город
     */
    private String city;

    /**
     * Описание
     */
    private String description;

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
     * Идентификатор вакансии
     */
    private Long vacancyId;

    /**
     * Название вакансии
     */
    private String vacancyName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSureName() {
        return sureName;
    }

    public void setSureName(String sureName) {
        this.sureName = sureName;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Long getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(Long vacancyId) {
        this.vacancyId = vacancyId;
    }

    public String getVacancyName() {
        return vacancyName;
    }

    public void setVacancyName(String vacancyName) {
        this.vacancyName = vacancyName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SearchUserResult)) return false;
        SearchUserResult that = (SearchUserResult) obj;

        return userId.equals(that.userId) &&
                name.equals(that.name) &&
                (email != null ? email.equals(that.email) : that.email == null) &&
                (phone != null ? phone.equals(that.phone) : that.phone == null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, name, email, phone);
    }
}
