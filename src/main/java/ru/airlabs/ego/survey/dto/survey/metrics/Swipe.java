package ru.airlabs.ego.survey.dto.survey.metrics;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Модель для представления информации о свайпе при отправке ответов по опросу
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Swipe {

    /**
     * Дата начала
     */
    @JsonProperty("d_start")
    @JsonFormat(pattern = "dd.MM.yyyy'T'HH:mm:ss:SSS")
    private Date startDate;

    /**
     * Дата завершения
     */
    @JsonProperty("d_end")
    @JsonFormat(pattern = "dd.MM.yyyy'T'HH:mm:ss:SSS")
    private Date endDate;

    /**
     * Координата x начальной точки
     */
    @JsonProperty("x_s")
    private Integer xStart;

    /**
     * Координата y начальной точки
     */
    @JsonProperty("y_s")
    private Integer yStart;

    /**
     * Координата x конечной точки
     */
    @JsonProperty("x_e")
    private Integer xEnd;

    /**
     * Координата y конечной точки
     */
    @JsonProperty("y_e")
    private Integer yEnd;

}
