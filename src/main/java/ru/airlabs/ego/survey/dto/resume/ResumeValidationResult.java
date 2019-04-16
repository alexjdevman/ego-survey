package ru.airlabs.ego.survey.dto.resume;

/**
 * Модель для валидации данных в резюме
 *
 * @author Aleksey Gorbachev
 */
public class ResumeValidationResult {

    /**
     * Признак валидности Email в резюме
     */
    public boolean emailValid = true;

    /**
     * Признак валидности телефона в резюме
     */
    public boolean phoneValid = true;

    /**
     * Сообщение
     */
    public String validationMessage;
}
