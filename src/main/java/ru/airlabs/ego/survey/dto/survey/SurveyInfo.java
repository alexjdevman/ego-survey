package ru.airlabs.ego.survey.dto.survey;

import java.io.Serializable;

/**
 * Модель для передачи данных по опросу на UI
 *
 * @author Aleksey Gorbachev
 */
public class SurveyInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор опроса
     */
    private Long id;

    /**
     * Название опроса
     */
    private String name;

    public SurveyInfo() {
    }

    public SurveyInfo(Long id, String name) {
        this.id = id;
        this.name = name;
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
}
