package ru.airlabs.ego.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Базовая модель для передачи ошибок клиента из UI
 *
 * @author Aleksey Gorbachev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UIError {

    /**
     * URL, где произошла ошибка
     */
    private String url;

    /**
     * Номер строки, где произошла ошибка
     */
    private Integer lineNumber;

    /**
     * Номер столбца для строки где произошла ошибка
     */
    private Integer colNumber;

    /**
     * Текст ошибки клиента
     */
    private String errorMessage;

    /**
     * Объект ошибки клиента (Error Object)
     */
    private String errorObj;

}
