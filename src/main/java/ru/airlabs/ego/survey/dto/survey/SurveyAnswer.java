package ru.airlabs.ego.survey.dto.survey;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.airlabs.ego.survey.dto.survey.metrics.AccelItem;
import ru.airlabs.ego.survey.dto.survey.metrics.Battery;
import ru.airlabs.ego.survey.dto.survey.metrics.Network;
import ru.airlabs.ego.survey.dto.survey.metrics.Swipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Ответ на вопрос опроса
 *
 * @author Roman Kochergin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyAnswer {

    /**
     * Идентификатор вопроса
     */
    private long id;

    /**
     * Ответ на вопрос
     */
    private boolean answer;

    /**
     * Идентификатор пользователя
     */
    private long userId;

    /**
     * Дата открытия вопроса
     */
    @JsonProperty("data_started")
    private String viewDate;

    /**
     * Дата ответа на вопрос
     */
    @JsonProperty("data_end")
    private String answerDate;

    /**
     * Показатели гироскопа
     */
    private List<String> gyro = new ArrayList<>();

    /**
     * Информация о свайпе
     */
    private Swipe swipe;

    /**
     * Показатели акселерометра
     */
    private List<AccelItem> acel = new ArrayList<>();

    /**
     * Уровень заряда батареи
     */
    private Battery battery;

    /**
     * Тип соединения и скорость сети
     */
    private Network network;

    /**
     * История статуса сети
     */
    private List<String> online = new ArrayList<>();

    /**
     * Работа в фоне
     */
    private List<String> hidden = new ArrayList<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SurveyAnswer)) return false;
        SurveyAnswer that = (SurveyAnswer) o;
        return id == that.id && userId == that.userId;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        return result;
    }
}
