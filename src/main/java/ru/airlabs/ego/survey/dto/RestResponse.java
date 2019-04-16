package ru.airlabs.ego.survey.dto;

import java.io.Serializable;

/**
 * Модель для передачи данных о результатах выполнения REST запросов на UI
 *
 * @author Aleksey Gorbachev
 */
public class RestResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Код HTTP статуса
     */
    private int statusCode;

    /**
     * Флаг успешного выполнения операции
     */
    private boolean success;

    /**
     * Сообщение об ошибке
     */
    private String message;

    /**
     * URL, по которому была получена ошибка
     */
    private String url;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
