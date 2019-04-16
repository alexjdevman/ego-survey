package ru.airlabs.ego.survey.service.resume.parse;

import ru.airlabs.ego.survey.service.resume.Resume;

import java.io.IOException;

/**
 * Сервис парсинга резюме
 *
 * @author Roman Kochergin
 */
public interface IResumeParseService {

    /**
     * Распарсить
     *
     * @return резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    Resume parse() throws IOException, InvalidFormatException;

}
