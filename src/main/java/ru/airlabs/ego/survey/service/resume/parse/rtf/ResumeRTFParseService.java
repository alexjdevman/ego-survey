package ru.airlabs.ego.survey.service.resume.parse.rtf;

import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.parse.IResumeParseService;
import ru.airlabs.ego.survey.service.resume.parse.InvalidFormatException;

import java.io.IOException;
import java.util.List;

/**
 * Сервис для парсинга резюме в формате RTF
 *
 * @author Aleksey Gorbachev
 */
public class ResumeRTFParseService implements IResumeParseService {

    /**
     * Список сервисов для парсинга резюме в формате RTF из разных источников (hh.ru, rabota.ua и т.д.)
     */
    private List<IResumeParseService> services;

    public ResumeRTFParseService(List<IResumeParseService> services) {
        this.services = services;
    }

    /**
     * Распарсить резюме в формате RTF
     *
     * @return резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    @Override
    public Resume parse() throws IOException, InvalidFormatException {
        for (IResumeParseService service : services) {  // разбор резюме с помощью сервисов для разных источников
            Resume resume =  service.parse();
            if (resume != null && resume.isRequiredFilled()) {
                return resume;
            }
        }
        return null;
    }
}
