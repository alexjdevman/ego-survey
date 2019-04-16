package ru.airlabs.ego.survey.service;

/**
 * Интерфейс для работы с балансом пользователя
 *
 * @author Aleksey Gorbachev
 */
public interface UserBalanceService {

    /**
     * Получение текущего баланса пользователя
     *
     * @param userId идентификатор пользователя
     * @return текущий баланс пользователя
     */
    Double getUserBalance(Long userId);

}
