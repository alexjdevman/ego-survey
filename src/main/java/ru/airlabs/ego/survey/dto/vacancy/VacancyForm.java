package ru.airlabs.ego.survey.dto.vacancy;

import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;

import java.io.Serializable;
import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Модель для представления данных по вакансии или исследованию на UI
 *
 * @author Aleksey Gorbachev
 */
public class VacancyForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор вакансии или исследования
     */
    private Long id;

    /**
     * Наименование вакансии или исследования
     */
    private String name;

    /**
     * Внешняя ссылка на вакансию
     */
    private String url;

    /**
     * Зарплата
     */
    private Long salary;

    /**
     * Email эталонного участника
     */
    private String leaderEmail;

    /**
     * ФИО эталонного участника
     */
    private String leaderName;

    /**
     * Идентификатор опроса
     */
    private Long surveyId;

    /**
     * Методология
     */
    private VacancyMethodology methodology;

    /**
     * Фамилия известного человека, с которым ассоциируется эталон
     */
    private String similarPerson;

    /**
     * Флаг вакансии или исследования (== false для исследований)
     */
    private Boolean vacancy = true;

    /**
     * Флаг признака прохождения опроса "Проходить только на мобильнике"
     */
    private Boolean mobile = false;

    /**
     * Признак, прошел ли опрос эталонный пользователь
     */
    private Boolean leaderPassedSurvey;

    /**
     * Бонус
     */
    private Long vacancyBonus;

    public VacancyForm() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSalary() {
        return salary;
    }

    public void setSalary(Long salary) {
        this.salary = salary;
    }

    public String getLeaderEmail() {
        return leaderEmail;
    }

    public void setLeaderEmail(String leaderEmail) {
        this.leaderEmail = leaderEmail;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public VacancyMethodology getMethodology() {
        return methodology;
    }

    public void setMethodology(VacancyMethodology methodology) {
        this.methodology = methodology;
    }

    public String getSimilarPerson() {
        return similarPerson;
    }

    public void setSimilarPerson(String similarPerson) {
        this.similarPerson = similarPerson;
    }

    public Boolean getVacancy() {
        return vacancy;
    }

    public void setVacancy(Boolean vacancy) {
        this.vacancy = vacancy;
    }

    public Boolean getMobile() {
        return mobile;
    }

    public void setMobile(Boolean mobile) {
        this.mobile = mobile;
    }

    public Boolean getLeaderPassedSurvey() {
        return leaderPassedSurvey;
    }

    public void setLeaderPassedSurvey(Boolean leaderPassedSurvey) {
        this.leaderPassedSurvey = leaderPassedSurvey;
    }

    public Long getVacancyBonus() {
        return vacancyBonus;
    }

    public void setVacancyBonus(Long vacancyBonus) {
        this.vacancyBonus = vacancyBonus;
    }

    /**
     * Создание вакансии по данным из UI
     *
     * @param hrUser HR (текущий пользователь, создающий вакансию в ЛК)
     * @return вакансия
     */
    public Vacancy buildVacancy(User hrUser) {
        Vacancy vacancy = new Vacancy();
        vacancy.setName(name);
        vacancy.setUrl(url);
        vacancy.setSalary(salary != null ? salary : 0L);
        vacancy.setManagerId(hrUser != null ? hrUser.getId() : 0);  // если нет текущего пользователя - используется системный с id == 0
        vacancy.setDateCreate(new Date());
        vacancy.setSurveyId(surveyId);
        vacancy.setVacancy(Boolean.TRUE);
        vacancy.setMobile(mobile);
        vacancy.setSimilarPerson(similarPerson);
        vacancy.setBonus(vacancyBonus);
        return vacancy;
    }

    /**
     * Создание исследования по данным из UI
     *
     * @param hrUser HR (текущий пользователь, создающий исследование в ЛК)
     * @return исследование
     */
    public Vacancy buildResearch(User hrUser) {
        Vacancy research = new Vacancy();
        research.setName(name);
        research.setManagerId(hrUser != null ? hrUser.getId() : 0);  // если нет текущего пользователя - используется системный с id == 0
        research.setDateCreate(new Date());
        research.setSurveyId(surveyId);
        research.setVacancy(Boolean.FALSE);
        research.setMobile(mobile);
        research.setSimilarPerson(similarPerson);
        return research;
    }

    /**
     * Заполнение формы из вакансии
     *
     * @param vacancy вакансия
     */
    public void copyDataFromVacancy(Vacancy vacancy) {
        setId(vacancy.getId());
        setName(vacancy.getName());
        setUrl(vacancy.getUrl());
        setSalary(vacancy.getSalary());
        setSurveyId(vacancy.getSurveyId());
        setMobile(vacancy.getMobile());
        setSimilarPerson(vacancy.getSimilarPerson());
        setVacancyBonus(vacancy.getBonus());
    }

    /**
     * Заполнение формы из исследования
     *
     * @param vacancy исследование
     */
    public void copyDataFromResearch(Vacancy vacancy) {
        setId(vacancy.getId());
        setName(vacancy.getName());
        setSurveyId(vacancy.getSurveyId());
        setVacancy(vacancy.getVacancy());
        setMobile(vacancy.getMobile());
        setSimilarPerson(vacancy.getSimilarPerson());
    }

    /**
     * Проверка правильности заполнения формы для вакансии/исследования
     *
     * @param vacancy вакансия или исследование
     */
    public void validateByVacancy(Vacancy vacancy) {
        final boolean isVacancy = vacancy.getVacancy();
        checkArgument(this.vacancy.booleanValue() == isVacancy, // проверка на соответствие флага вакансии или исследования
                "Неправильно заполнен признак " + (isVacancy ? "вакансии" : "исследования"));
    }

}
