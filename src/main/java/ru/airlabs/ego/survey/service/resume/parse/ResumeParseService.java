package ru.airlabs.ego.survey.service.resume.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import ru.airlabs.ego.ResumeParser;
import ru.airlabs.ego.Type;
import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.ResumeHtmlModel;
import ru.airlabs.ego.survey.service.resume.parse.rtf.*;
import ru.airlabs.ego.survey.service.resume.parse.xls.ResumeXLSParseService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static ru.airlabs.ego.core.entity.ResumeData.Source;

/**
 * Сервис для парсинга резюме
 *
 * @author Aleksey Gorbachev
 */
public class ResumeParseService implements IResumeListParseService {

    /**
     * Расширение Excel-файлов с резюме
     */
    private static final String EXCEL_FILE_EXTENSION = "xls";

    /**
     * Расширения RTF-файлов с резюме
     */
    private static final String RTF_FILE_EXTENSION = "rtf";
    private static final String DOC_FILE_EXTENSION = "doc";

    /**
     * Полное имя файла с резюме
     */
    private String fileName;

    /**
     * Входной поток данных из файла с резюме
     */
    private InputStream stream;

    public ResumeParseService(String fileName, InputStream stream) {
        this.fileName = fileName;
        this.stream = stream;
    }

    /**
     * Распарсить список резюме
     * (поддерживаемые форматы: RTF, XLS)
     *
     * @return список резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    @Override
    public List<Resume> parse() throws IOException, InvalidFormatException {
        String fileExtension = getExtension(fileName);
        if (fileExtension.equals(EXCEL_FILE_EXTENSION)) {   // формат Excel
            return parseInXLS();
        } else if (fileExtension.equals(RTF_FILE_EXTENSION) || fileExtension.equals(DOC_FILE_EXTENSION)) {    // формат RTF или DOC
            Resume resume = parseRtfOrDoc();
            return resume != null ? asList(resume) : emptyList();
        } else {
            throw new InvalidFormatException("Недопустимый формат файла");
        }
    }

    /**
     * Разбор резюме в формате HTML
     *
     * @param source источник резюме для файла в формате HTML (HH - HeadHunter, FW - FriendWork)
     * @return резюме
     */
    @Override
    public List<Resume> parseInHTML(Source source) throws IOException, InvalidFormatException {
        try {
            String resumeInJson;
            if (source == Source.HH) {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8.name()), Type.HH);
            } else if (source == Source.FW) {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8.name()), Type.FRIEND_WORK);
            } else if (source == Source.JM) {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8.name()), Type.JOB_IN_MOSCOW);
            } else if (source == Source.JA) {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8.name()), Type.JOB_IN_ABAKAN);
            } else if (source == Source.MO) {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, "Cp1251"), Type.JOB_MO);
            } else {
                resumeInJson = ResumeParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8.name()), Type.TRUD_VSEM);
            }
            ResumeHtmlModel resumeHtmlModel = new ObjectMapper().readValue(resumeInJson, ResumeHtmlModel.class);
            Resume resume = resumeHtmlModel.buildResume(source);
            return resume != null ? asList(resume) : emptyList();
        } catch (Exception ex) {
            throw new InvalidFormatException("Неверный формат данных", ex);
        }
    }

    /**
     * Разбор резюме в формате RTF или DOC
     *
     * @return резюме
     * @throws IOException
     * @throws InvalidFormatException
     */
    private Resume parseRtfOrDoc() throws IOException, InvalidFormatException {
        byte[] bytes = IOUtils.toByteArray(stream); // получаем массив байт для раздельного использования потока сервисами
        Resume resume;
        try {   // разбор в формате RTF (hh.ru, superjob.ru, rabota.ua)
            List<IResumeParseService> services = asList(
                    new ResumeHHParseService(new ByteArrayInputStream(bytes)),
                    new ResumeRabotaUAParseService(new ByteArrayInputStream(bytes)),
                    new ResumeSuperjobRUParseService(new ByteArrayInputStream(bytes))); // список сервисов для разбора резюме с hh.ru, superjob.ru, rabota.ua

            resume = new ResumeRTFParseService(services).parse();
        } catch (InvalidFormatException ex) {   // если не удалось в RTF - парсим для work.ua в формате DOC
            resume = new ResumeWorkUAParseService(new ByteArrayInputStream(bytes)).parse();
        }
        return resume;
    }

    /**
     * Разбор резюме в формате XLS
     *
     * @return резюме
     */
    private List<Resume> parseInXLS() throws IOException, InvalidFormatException {
        return new ResumeXLSParseService(stream).parse();
    }

}
