package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.survey.dto.user.Sex;

import java.util.Date;

/**
 * Интерфейс сервиса работы с пользователями
 *
 * @author Roman Kochergin
 */
public interface UserService {

    /**
     * Получить или создать пользователя
     *
     * @param name         имя
     * @param email        email
     * @param phone        телефон
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    Long getOrCreateUser(String name, String email, String phone, Long parentUserId);

    /**
     * Получить или создать пользователя
     *
     * @param name         имя
     * @param email        email
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    Long getOrCreateUser(String name, String email, Long parentUserId);

    /**
     * Получить или создать пользователя
     *
     * @param name         имя
     * @param email        email
     * @param phone        телефон
     * @param locale       локаль
     * @param lastName     фамилия
     * @param middleName   отчество
     * @param sex          пол
     * @param birthDate    дата рождения
     * @param city         город
     * @param description  описание
     * @param salary       зарплата
     * @param parentUserId идентификатор пользователя, создающего нового пользователя
     * @return идентификатор пользователя
     */
    Long getOrCreateUser(String name, String email, String phone,
                         String locale, String lastName, String middleName,
                         Sex sex, Date birthDate, String city, String description,
                         Integer salary, Long parentUserId);

}
