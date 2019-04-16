package ru.airlabs.ego.survey.service.resume.parse;

import ru.airlabs.ego.survey.service.resume.Resume;

import java.io.IOException;
import java.util.List;

import static ru.airlabs.ego.core.entity.ResumeData.Source;

/**
 * Сервис парсинга списков резюме
 *
 * @author Aleksey Gorbachev
 */
public interface IResumeListParseService {

    /**
     * Распарсить файл в список резюме
     *
     * @return список резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    List<Resume> parse() throws IOException, InvalidFormatException;

    /**
     * Распарсить HTMl файл в список резюме
     *
     * @param source источник резюме для файла в формате HTML (HH - HeadHunter, FW - FriendWork)
     *
     * @return список резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    List<Resume> parseInHTML(Source source) throws IOException, InvalidFormatException;

}
