package ru.airlabs.ego.survey.dto.survey.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель для представления информации об уровне заряда батареи при отправке ответов по опросу
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Battery {

    /**
     * Уровень заряда батареи
     */
    private Integer charge;

    private Boolean charging;
}
