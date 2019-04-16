package ru.airlabs.ego.survey.dto.survey;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Модель для инкапсуляции данных по результату прохождения пользователем опроса
 *
 * @author Aleksey Gorbachev
 */
public class SurveyResult {

    /**
     * Общее кол-во принятых ответов (как положительных так и отрицательных)
     */
    public AtomicInteger totalAnswered = new AtomicInteger(0);

    /**
     * Список идентификаторов принятых ответов
     */
    public List<Long> appliedAnswerIds = new CopyOnWriteArrayList<>();

    /**
     * Общее кол-во отвергнутых ответов (как повторы)
     */
    public AtomicInteger totalRepeats = new AtomicInteger(0);

    /**
     * Список идентификаторов отвергнутых (как повторы) ответов
     */
    public List<Long> repeatedAnswerIds = new CopyOnWriteArrayList<>();

}
