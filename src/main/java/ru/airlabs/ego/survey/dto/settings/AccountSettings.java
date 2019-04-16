package ru.airlabs.ego.survey.dto.settings;

import ru.airlabs.ego.core.entity.User;

import java.io.Serializable;

/**
 * Модель для работы с настройками личного кабинета HR на UI
 *
 * @author Aleksey Gorbachev
 */
public class AccountSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор организации
     */
    private Long organizationId;

    /**
     * ФИО HR-а
     */
    private String managerName;

    /**
     * Контактный телефон
     */
    private String phone;

    /**
     * Сайт компании
     */
    private String site;

    /**
     * Email HR-а
     */
    private String managerEmail;

    /**
     * Адрес компании (географический)
     */
    private String address;

    /**
     * Название компании
     */
    private String companyName;

    /**
     * Описание компании
     */
    private String companyDescription;

    /**
     * Ссылка на логотип компании
     */
    private String companyLogo;

    /**
     * Подписка на email рассылку
     */
    private Boolean emailSubscription;

    public AccountSettings() {
    }

    public AccountSettings(User user) {
        this.managerEmail = user.getEmail();
        this.managerName = user.getName();
        this.emailSubscription = user.getSubscribed();
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public String getCompanyLogo() {
        return companyLogo;
    }

    public void setCompanyLogo(String companyLogo) {
        this.companyLogo = companyLogo;
    }

    public Boolean isEmailSubscription() {
        return emailSubscription;
    }

    public void setEmailSubscription(Boolean emailSubscription) {
        this.emailSubscription = emailSubscription;
    }
}
