package ru.airlabs.ego.survey.service.resume;

import ru.airlabs.ego.survey.dto.resume.ResumeValidationResult;
import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.utils.EmailUtils;

import java.time.LocalDate;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.core.entity.ResumeData.Type;

/**
 * Резюме
 *
 * @author Roman Kochergin
 */
public class Resume {

    /**
     * Имя
     */
    private String firstName;

    /**
     * Фамилия
     */
    private String lastName;

    /**
     * Отчество
     */
    private String middleName;

    /**
     * Пол
     */
    private Sex sex;

    /**
     * Дата рождения
     */
    private LocalDate birthDate;

    /**
     * Адрес электронной почты
     */
    private String email;

    /**
     * Телефон
     */
    private String phone;

    /**
     * Адрес
     */
    private String address;

    /**
     * Должность
     */
    private String position;

    /**
     * Зарплата
     */
    private Integer salary;

    /**
     * URL фото из резюме (для HTML файлов)
     */
    private String photoURL;

    /**
     * Источник резюме
     */
    private Source source;

    /**
     * Тип данных
     */
    private Type type;

    /**
     * Все данные из исходного файла резюме (для резюме в формате HTML)
     */
    private ResumeHtmlModel htmlResume;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ResumeHtmlModel getHtmlResume() {
        return htmlResume;
    }

    public void setHtmlResume(ResumeHtmlModel htmlResume) {
        this.htmlResume = htmlResume;
    }

    public Resume(Source source, Type type) {
        this.source = source;
        this.type = type;
    }

    public Resume() {
    }


    /**
     * Заполнены ли минимально необходимые поля в резюме
     *
     * @return да или нет
     */
    public boolean isRequiredFilled() {
        return isNotBlank(firstName) && isNotBlank(position);
    }

    /**
     * Заполнены ли контактные поля (email или телефон) в резюме
     *
     * @return да или нет
     */
    public boolean isContactFilled() {
        return isNotBlank(email) || isNotBlank(phone);
    }

    /**
     * Валидация резюме
     *
     * @return результат валидации
     */
    public ResumeValidationResult validateResume() {
        ResumeValidationResult result = new ResumeValidationResult();
        StringBuilder messageBuilder = new StringBuilder("");
        if (!isRussianPhone(phone)) {
            result.phoneValid = false;
            messageBuilder.append("Номер телефона соискателя не соответствует формату, ");
        }
        if (isBlank(email)) {
            result.emailValid = false;
            messageBuilder.append("Email соискателя не заполнен, ");
        } else {
            if (!EmailUtils.isValid(email)) {
                result.emailValid = false;
                messageBuilder.append("Email соискателя не валиден, ");
            }
        }
        final String message = messageBuilder.length() > 0 ?
                messageBuilder.toString().substring(0, messageBuilder.length() - 2) :
                messageBuilder.toString();
        result.validationMessage = message;
        return result;
    }

    /**
     * Проверка номера телефона, является ли он российским
     *
     * @param phone номер телефона
     * @return true/false
     */
    private boolean isRussianPhone(String phone) {
        if (isBlank(phone)) return Boolean.FALSE;
        return phone.length() == 11 && (phone.startsWith("79") || phone.startsWith("89"));
    }

    /**
     * Полностью ли заполнено резюме
     *
     * @return да или нет
     */
    public boolean isFullFilled() {
        return firstName != null &&
                lastName != null &&
                middleName != null &&
                sex != null &&
                birthDate != null &&
                email != null &&
                phone != null &&
                address != null &&
                position != null &&
                salary != null;
    }

    /**
     * Пустое ли резюме
     *
     * @return да или нет
     */
    public boolean isEmpty() {
        return firstName == null &&
                lastName == null &&
                middleName == null &&
                sex == null &&
                birthDate == null &&
                email == null &&
                phone == null &&
                address == null &&
                position == null;
    }

    @Override
    public String toString() {
        return "Resume{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", sex=" + sex +
                ", birthDate=" + birthDate +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", position='" + position + '\'' +
                ", source='" + source + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
