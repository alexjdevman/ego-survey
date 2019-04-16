package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.survey.dto.device.DeviceInfo;
import ru.airlabs.ego.survey.dto.survey.SurveyAnswer;

/**
 * Интерфейс сервиса для работы с данными по устройствам пользователей
 *
 * @author Aleksey Gorbachev
 */
public interface UserDeviceInfoService {

    /**
     * Сохранение данных по устройству пользователя
     *
     * @param vacancyId  идентификатор вакансии
     * @param userId     идентификатор пользователя
     * @param surveyId   идентификатор опроса
     * @param deviceInfo данные по устройству пользователя
     * @param logType    тип записи
     */
    void saveDeviceInfo(Long vacancyId,
                        Long userId,
                        Long surveyId,
                        DeviceInfo deviceInfo,
                        String logType);

    /**
     * Метод проверяющий сохранялась ли информация по устройству пользователя, проходящего опрос
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return true - если информация по устройству сохранялась ранее, иначе - false
     */
    Boolean existsDeviceInfo(Long userId, Long surveyId);
}
