package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.airlabs.ego.survey.service.AudioPresentationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Сервис для работы с аудио-презентациями
 *
 * @author Aleksey Gorbachev
 */
@Service("audioPresentationService")
public class AudioPresentationServiceImpl implements AudioPresentationService {

    /**
     * Репозиторий аудио-файлов
     */
    @Value("${audio.presentation.repository}")
    private String basicPath;

    /**
     * Максимальное кол-во файлов в директории
     */
    private static final long filesPerDirectory = 20000L;

    /**
     * Путь к смикшированному файлу
     *
     * @param vacancyId - идентификатор вакансии
     * @return путь
     */
    @Override
    public String getMixPath(Long vacancyId) {
        return Paths.get(basicPath, "mix", String.valueOf(vacancyId / filesPerDirectory), String.valueOf(vacancyId) + ".mp3").toString();
    }

    /**
     * Проверка, создан ли для вакансии аудио-стикер
     *
     * @param vacancyId - идентификатор вакансии
     * @return true/false
     */
    @Override
    public boolean isAudioPresentationExists(Long vacancyId) {
        Path path = Paths.get(getMixPath(vacancyId));
        return Files.exists(path);
    }
}
