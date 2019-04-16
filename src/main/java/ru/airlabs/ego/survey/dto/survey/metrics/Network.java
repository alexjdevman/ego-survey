package ru.airlabs.ego.survey.dto.survey.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель для представления информации о сети при отправке ответов по опросу
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Network {

    /**
     * Тип соединения
     */
    private String type;

    private String effectiveType;

    /**
     * Скорость сети
     */
    private Integer downlinkMax;
}
