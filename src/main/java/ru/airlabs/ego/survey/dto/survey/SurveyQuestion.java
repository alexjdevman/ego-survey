package ru.airlabs.ego.survey.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Вопрос опроса
 *
 * @author Roman Kochergin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyQuestion {

    /**
     * Идентификатор вопроса
     */
    private long id;

    /**
     * Вопрос
     */
    private String name;

    /**
     * Время на ответ, в секундах
     */
    private int answerSeconds;
}
