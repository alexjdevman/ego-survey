package ru.airlabs.ego.survey.dto.survey;

import java.io.Serializable;

/**
 * Модель для работы с категориями опросов на UI
 *
 * @author Aleksey Gorbachev
 */
public class SurveyCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор категории
     */
    private Long id;

    /**
     * Название категории
     */
    private String name;

    /**
     * Порядковый номер категории
     */
    private Integer chainNumber;

    public SurveyCategory(Long id, String name, Integer chainNumber) {
        this.id = id;
        this.name = name;
        this.chainNumber = chainNumber;
    }

    public SurveyCategory(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public SurveyCategory() {
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

    public Integer getChainNumber() {
        return chainNumber;
    }

    public void setChainNumber(Integer chainNumber) {
        this.chainNumber = chainNumber;
    }
}
