package ru.airlabs.ego.survey.dto.device;

import java.io.Serializable;

/**
 * Модель для передачи данных по устройству пользователя из UI
 *
 * @author Aleksey Gorbachev
 */
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Разрешение экрана
     */
    private String screen;

    /**
     * Юзерагент
     */
    private String agent;

    /**
     * UID cookie
     */
    private String fingerprint;

    /**
     * Источник перехода по ссылке
     */
    private String referrer;


    public DeviceInfo() {
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}
