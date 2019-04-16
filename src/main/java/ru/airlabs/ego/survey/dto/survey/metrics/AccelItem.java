package ru.airlabs.ego.survey.dto.survey.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель для представления информации о показателях акселерометра
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccelItem {

    private String accel;

    private String rotrate;
}
