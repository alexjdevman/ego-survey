package ru.airlabs.ego.survey.dto.survey;

import java.io.Serializable;

/**
 * Модель для передачи данных по глобальным параметрам пользователей (среднее время и глобальный % противоречий)
 *
 * @author Aleksey Gorbachev
 */
public class GlobalUserScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Средний % противоречий в ответах на вопросы для всех пользователй
     */
    private Integer conflictAnswerPercent;

    /**
     * Среднее время на ответы в опроснике для всех пользователей
     */
    private Double averageAnswerTime;

    public Integer getConflictAnswerPercent() {
        return conflictAnswerPercent;
    }

    public void setConflictAnswerPercent(Integer conflictAnswerPercent) {
        this.conflictAnswerPercent = conflictAnswerPercent;
    }

    public Double getAverageAnswerTime() {
        return averageAnswerTime;
    }

    public void setAverageAnswerTime(Double averageAnswerTime) {
        this.averageAnswerTime = averageAnswerTime;
    }
}
