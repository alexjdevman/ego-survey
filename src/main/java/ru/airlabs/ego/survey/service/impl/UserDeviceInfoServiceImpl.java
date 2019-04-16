package ru.airlabs.ego.survey.service.impl;

import net.sf.uadetector.ReadableDeviceCategory;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.device.UserDeviceInfo;
import ru.airlabs.ego.core.repository.device.UserDeviceInfoRepository;
import ru.airlabs.ego.survey.dto.device.DeviceInfo;
import ru.airlabs.ego.survey.service.UserDeviceInfoService;

import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service("userDeviceInfoService")
@Transactional(readOnly = true)
public class UserDeviceInfoServiceImpl implements UserDeviceInfoService {

    /**
     * Тип устройства (Desktop)
     */
    private static final String DESKTOP_DEVICE_TYPE = "D";

    /**
     * Тип устройства (Mobile)
     */
    private static final String MOBILE_DEVICE_TYPE = "M";

    /**
     * Репозиторий данных по устройству клиента
     */
    @Autowired
    private UserDeviceInfoRepository deviceInfoRepository;

    /**
     * Сохранение данных по устройству пользователя
     *
     * @param vacancyId   идентификатор вакансии
     * @param userId      идентификатор пользователя
     * @param surveyId    идентификатор опроса
     * @param deviceInfo  данные по устройству пользователя
     * @param logType     тип записи
     */
    @Transactional
    @Override
    public void saveDeviceInfo(Long vacancyId,
                               Long userId,
                               Long surveyId,
                               DeviceInfo deviceInfo,
                               String logType) {

        UserDeviceInfo info = new UserDeviceInfo();
        info.setUserId(userId);
        info.setSurveyId(surveyId);
        info.setVacancyId(vacancyId);
        info.setInfoDate(new Date());
        info.setScreen(deviceInfo.getScreen());
        info.setCookieId(deviceInfo.getFingerprint());
        info.setReferrer(isNotBlank(deviceInfo.getReferrer()) ? deviceInfo.getReferrer() : null);
        info.setLogType(logType);

        if (isNotBlank(deviceInfo.getAgent())) {    // делаем разбор юзерагента
            UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();
            ReadableUserAgent agent = parser.parse(deviceInfo.getAgent());

            info.setDevice(agent.getName());
            info.setOs(detectOperationSystemWithVersionFromAgent(agent));
            info.setAgent(deviceInfo.getAgent());
            info.setDeviceType(detectDeviceTypeFromAgent(agent));
        }
        deviceInfoRepository.save(info);
    }

    /**
     * Метод проверяющий сохранялась ли информация по устройству пользователя, проходящего опрос
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return true - если информация по устройству сохранялась ранее, иначе - false
     */
    @Override
    public Boolean existsDeviceInfo(Long userId, Long surveyId) {
        return deviceInfoRepository.findAllByUserIdAndSurveyId(userId, surveyId).size() > 0;
    }

    /**
     * Определение типа устройства по значению юзерагента
     *
     * @param userAgent юзерагент
     * @return тип устройства
     */
    private String detectDeviceTypeFromAgent(ReadableUserAgent userAgent) {
        ReadableDeviceCategory.Category category = userAgent.getDeviceCategory().getCategory();
        if (category == ReadableDeviceCategory.Category.UNKNOWN) return null;
        if (category != ReadableDeviceCategory.Category.PERSONAL_COMPUTER) {
            return MOBILE_DEVICE_TYPE;
        } else {
            return DESKTOP_DEVICE_TYPE;
        }
    }

    /**
     * Определение операционной системы и версии по значению юзерагента
     *
     * @param userAgent юзерагент
     * @return тип операционной системы + номер версии
     */
    private String detectOperationSystemWithVersionFromAgent(ReadableUserAgent userAgent) {
        return userAgent.getOperatingSystem().getName() + " " +
                userAgent.getOperatingSystem().getVersionNumber().toVersionString();
    }

}
