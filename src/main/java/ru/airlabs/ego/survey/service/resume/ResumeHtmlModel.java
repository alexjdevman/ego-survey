package ru.airlabs.ego.survey.service.resume;

import ru.airlabs.ego.core.entity.ResumeData;
import ru.airlabs.ego.model.AdditionalEducation;
import ru.airlabs.ego.model.Education;
import ru.airlabs.ego.model.Examination;
import ru.airlabs.ego.model.Experience;
import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.utils.EmailUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.survey.utils.DateUtils.asLocalDate;
import static ru.airlabs.ego.survey.utils.NameUtils.*;
import static ru.airlabs.ego.survey.utils.PhoneUtils.stripPhone;

/**
 * Модель для разбора данных для резюме из HTML файла
 */
public class ResumeHtmlModel {

    private String vacancy;
    private String salary;
    private String name;
    private String gender;
    private String age;
    private String birthDate;
    private String addressLocality;
    private String metro;
    private String email;
    private String telephone;
    private String contactPreferred;
    private List<String> contacts;
    private String specializationCategory;
    private List<String> positionSpecializations;
    private List<Experience> experiences;
    private List<String> skills;
    private String aboutMe;
    private String education;
    private List<Education> educationsInfo;
    private List<String> languages;
    private List<AdditionalEducation> additionalEducations;
    private List<Examination> examinations;
    private String citizenship;
    private String photoURL;

    public String getVacancy() {
        return vacancy;
    }

    public void setVacancy(String vacancy) {
        this.vacancy = vacancy;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddressLocality() {
        return addressLocality;
    }

    public void setAddressLocality(String addressLocality) {
        this.addressLocality = addressLocality;
    }

    public String getMetro() {
        return metro;
    }

    public void setMetro(String metro) {
        this.metro = metro;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getContactPreferred() {
        return contactPreferred;
    }

    public void setContactPreferred(String contactPreferred) {
        this.contactPreferred = contactPreferred;
    }

    public List<String> getContacts() {
        if (contacts == null) {
            contacts = new ArrayList<>();
        }
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getSpecializationCategory() {
        return specializationCategory;
    }

    public void setSpecializationCategory(String specializationCategory) {
        this.specializationCategory = specializationCategory;
    }

    public List<String> getPositionSpecializations() {
        return positionSpecializations;
    }

    public void setPositionSpecializations(List<String> positionSpecializations) {
        this.positionSpecializations = positionSpecializations;
    }

    public List<Experience> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public List<Education> getEducationsInfo() {
        return educationsInfo;
    }

    public void setEducationsInfo(List<Education> educationsInfo) {
        this.educationsInfo = educationsInfo;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<AdditionalEducation> getAdditionalEducations() {
        return additionalEducations;
    }

    public void setAdditionalEducations(List<AdditionalEducation> additionalEducations) {
        this.additionalEducations = additionalEducations;
    }

    public List<Examination> getExaminations() {
        return examinations;
    }

    public void setExaminations(List<Examination> examinations) {
        this.examinations = examinations;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    /**
     * Создание резюме по данным из HTML файла
     *
     * @return резюме
     */
    public Resume buildResume(Source source) {
        Resume resume = new Resume(source, ResumeData.Type.H);
        resume.setPosition(vacancy);
        if (isNotBlank(salary)) {
            resume.setSalary(parseSalary(salary));
        }
        resume.setFirstName(extractFirstNameFromFIO(name));
        resume.setLastName(extractSurenameFromFIO(name));
        resume.setMiddleName(extractParentnameFromFIO(name));
        resume.setPhone(stripPhone(telephone));
        resume.setEmail(isNotBlank(email) ? parseEmail(email) : getEmailFromContacts());
        resume.setAddress(addressLocality);
        if (isNotBlank(birthDate)) {    // парсим дату рождения
            try {
                resume.setBirthDate(asLocalDate(new SimpleDateFormat("dd.MM.yyyy").parse(birthDate)));
            } catch (ParseException e) {
                resume.setBirthDate(LocalDate.parse(birthDate));
            }
        }
        if (isNotBlank(gender)) {   // определяем пол соискателя
            if (gender.toLowerCase().contains("жен") || gender.toLowerCase().contains("female")) {
                resume.setSex(Sex.W);
            } else if (gender.toLowerCase().contains("муж") || gender.toLowerCase().contains("male")) {
                resume.setSex(Sex.M);
            }
        }
        if (isNotBlank(photoURL)) {
            if (source == Source.HH) {
                resume.setPhotoURL("https://hh.ru" + photoURL);
            } else if (source == Source.JM) {
                resume.setPhotoURL("http://www.jobinmoscow.ru/" + photoURL);
            } else if (source == Source.MO) {
                resume.setPhotoURL("https://www.job-mo.ru" + photoURL);
            } else if (source == Source.TV) {
                resume.setPhotoURL("https://trudvsem.ru" + photoURL);
            } else if (source == Source.JA) {
                resume.setPhotoURL("https://rabota19.ru" + photoURL);
            }
        }
        resume.setHtmlResume(this); // сохраняем все данные из HTML
        return resume;
    }

    /**
     * Получение email из данных резюме
     *
     * @return email соискателя
     */
    public String getEmailFromContacts() {
        String contact = getContactPreferred();
        if (isNotBlank(contact) && EmailUtils.isValid(contact)) {
            return contact;
        }
        for (String con : getContacts()) {
            if (EmailUtils.isValid(con)) {
                return con;
            }
        }
        return null;
    }

    /**
     * Получение email из строки (с учетом того, что в строке может быть несколько имейлов)
     *
     * @param email строка с email
     * @return email
     */
    private String parseEmail(String email) {
        if (isNotBlank(email)) {
            // удаляем лишние пробелы из строки Email
           String emailWithoutSpaces = email.replaceAll("\\s+", "").replaceAll("\u00A0", "");
           if (emailWithoutSpaces.contains(",")) {  // в строке находится несколько Email
                // возвращаем первый валидный Email
               String[] emailz = emailWithoutSpaces.split(",");
               for (String e : emailz) {
                   if (EmailUtils.isValid(e)) {
                       return e;
                   }
               }
           } else {
               if (EmailUtils.isValid(emailWithoutSpaces)) {
                   return emailWithoutSpaces;
               }
           }
        }
        return null;
    }

    private Integer parseSalary(String salaryStr) {
        if (salaryStr.contains("от") && salaryStr.contains("до")) {
            salaryStr = salaryStr
                    .substring(salaryStr.indexOf("до"), salaryStr.length())
                    .replaceAll("[^0-9]+", "");
        } else {
            salaryStr = salaryStr.replaceAll("[^0-9]+", "");
        }
        return isNotBlank(salaryStr) ? parseInt(salaryStr) : null;
    }
}
