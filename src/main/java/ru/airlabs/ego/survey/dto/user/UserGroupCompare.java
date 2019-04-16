package ru.airlabs.ego.survey.dto.user;

import java.io.Serializable;

/**
 * Модель для передачи данных о сравнении групп пользователей с конкретным выбранным пользователем
 *
 * @author Aleksey Gorbachev
 */
public class UserGroupCompare implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Номер блока для графика сравнения участников,
     * определяющий группу сравнения участников,
     * на графике выводится 8 блоков для групп, нумерация от 0 до 7
     */
    private Integer chartColumnNumber;

    /**
     * Кол-во пользователей в группе сравнения
     */
    private Integer usersCount;

    /**
     * Процент совпадения ("похожести") участника с участниками группы сравнения
     */
    private Double compareResult;

    public UserGroupCompare(Integer chartColumnNumber, Integer usersCount, Double compareResult) {
        this.chartColumnNumber = chartColumnNumber;
        this.usersCount = usersCount;
        this.compareResult = compareResult;
    }

    public Integer getChartColumnNumber() {
        return chartColumnNumber;
    }

    public void setChartColumnNumber(Integer chartColumnNumber) {
        this.chartColumnNumber = chartColumnNumber;
    }

    public Integer getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(Integer usersCount) {
        this.usersCount = usersCount;
    }

    public Double getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(Double compareResult) {
        this.compareResult = compareResult;
    }
}
