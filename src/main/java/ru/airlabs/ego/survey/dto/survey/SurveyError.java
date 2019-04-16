package ru.airlabs.ego.survey.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.airlabs.ego.survey.dto.UIError;

/**
 * Модель для передачи ошибок клиента из UI, возникающих при прохождении опроса
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyError {

    /**
     * Ошибка клиента
     */
    private UIError error;

    /**
     * Юзерагент
     */
    private String agent;

    /**
     * Идентификатор состояния прохождения опроса пользователем
     */
    private Long surveyStateId;

}
