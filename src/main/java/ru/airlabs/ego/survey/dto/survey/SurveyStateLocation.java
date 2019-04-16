package ru.airlabs.ego.survey.dto.survey;

import java.io.Serializable;

/**
 * Модель для передачи данных по гео-локации в опросе
 *
 * @author Aleksey Gorbachev
 */
public class SurveyStateLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Долгота
     */
    private Double longitude;

    /**
     * Широта
     */
    private Double latitude;

    public SurveyStateLocation() {
    }

    public SurveyStateLocation(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
