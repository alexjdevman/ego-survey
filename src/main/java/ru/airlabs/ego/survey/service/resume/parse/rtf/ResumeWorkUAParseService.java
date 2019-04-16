package ru.airlabs.ego.survey.service.resume.parse.rtf;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.parse.InvalidFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.core.entity.ResumeData.Type;

/**
 * Сервис парсинга резюме из сайта work.ua на основе формата RTF
 *
 * @author Aleksey Gorbachev
 */
public class ResumeWorkUAParseService extends ResumeRabotaUAParseService {

    /**
     * Конструктор
     *
     * @param inputStream поток данных
     */
    public ResumeWorkUAParseService(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Распарсить файл-резюме в формате DOC
     *
     * @return резюме
     * @throws IOException    ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    @Override
    public Resume parse() throws IOException, InvalidFormatException {
        try (InputStream stream = getInputStream()) {
            HWPFDocument doc = new HWPFDocument(stream);
            WordExtractor we = new WordExtractor(doc);
            String text = we.getText();
            String[] lines = getLines(text);
            Resume resume = buildResumeFromLines(lines);
            return resume;
        } catch (Exception e) {
            throw new InvalidFormatException("Неверный формат данных", e);
        }

    }

    /**
     * Создание модели резюме по данным из файла резюме из сайта work.ua
     *
     * @param lines строки файла
     * @return модель резюме
     */
    @Override
    protected Resume buildResumeFromLines(String[] lines) {
        Resume resume = new Resume(Source.WU, Type.R);
        try {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                line = line.replaceAll("\\t", "").trim();
                if (i == 2) {  // ФИО
                    if (isNotBlank(line)) {
                        fillClientName(line, resume);
                    }
                }
                if (i == 3) {
                    // Должность
                    if (isNotBlank(line)) {
                        resume.setPosition(line);
                    }
                }
                if (line.startsWith("Дата рождения:")) {
                    // Дата рождения
                    resume.setBirthDate(parseBirthDate(line));
                }
                if (line.startsWith("Город:")) {
                    // Город
                    if (isNotBlank(resume.getAddress())) {
                        resume.setAddress(parseCity(line) + " " + resume.getAddress());
                    } else {
                        resume.setAddress(parseCity(line));
                    }
                }
                if (line.startsWith("Адрес:")) {
                    // Адрес
                    if (isNotBlank(resume.getAddress())) {
                        resume.setAddress(resume.getAddress() + " " + parseAddress(line));
                    } else {
                        resume.setAddress(parseAddress(line));
                    }
                }
                if (line.startsWith("Телефон:")) {
                    // Телефон
                    resume.setPhone(parsePhone(line));
                }
                if (line.startsWith("Эл. почта:")) {
                    // Email
                    resume.setEmail(parseEmail(line));
                }
            }
        } catch (Exception err) {
            return null;
        }
        return resume;
    }

    /**
     * Разбор адреса соискателя для резюме
     *
     * @param line строка
     * @return адрес соискателя для резюме
     */
    private String parseAddress(String line) {
        if (isNotBlank(line) && line.startsWith("Адрес:")) {
            return line.replace("Адрес:", "").trim();
        }
        return null;
    }

    /**
     * Разбор города соискателя для резюме
     *
     * @param line строка
     * @return город соискателя для резюме
     */
    private String parseCity(String line) {
        if (isNotBlank(line) && line.startsWith("Город:")) {
            return line.replace("Город:", "").trim();
        }
        return null;
    }

    /**
     * Распарсить телефон
     *
     * @param line строка
     * @return телефон
     */
    private String parsePhone(String line) {
        if (isNotBlank(line) && line.startsWith("Телефон:")) {
            String phone = line.replaceAll("[^\\d]", "");
            if (phone.startsWith("0")) {
                return phone.replaceFirst("0", "380");
            } else {
                return phone;
            }
        }
        return null;
    }

    /**
     * Распарсить email
     *
     * @param line строка
     * @return email
     */
    private String parseEmail(String line) {
        if (isNotBlank(line) && line.startsWith("Эл. почта:")) {
            Matcher m = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(line);
            if (m.find()) {
                return m.group().toLowerCase();
            }
        }
        return null;
    }

    /**
     * Распарсить дату рождения
     *
     * @param line строка
     * @return дата
     */
    private LocalDate parseBirthDate(String line) {
        String birthDateStr = line.replace("Дата рождения:", "").trim();
        String[] date = birthDateStr.split("\\s+");
        if (date.length >= 3) {
            int day = Integer.valueOf(date[0]);
            int year = Integer.valueOf(date[2]);
            switch (date[1]) {
                case "января":
                    return LocalDate.of(year, 1, day);
                case "февраля":
                    return LocalDate.of(year, 2, day);
                case "марта":
                    return LocalDate.of(year, 3, day);
                case "апреля":
                    return LocalDate.of(year, 4, day);
                case "мая":
                    return LocalDate.of(year, 5, day);
                case "июня":
                    return LocalDate.of(year, 6, day);
                case "июля":
                    return LocalDate.of(year, 7, day);
                case "августа":
                    return LocalDate.of(year, 8, day);
                case "сентября":
                    return LocalDate.of(year, 9, day);
                case "октября":
                    return LocalDate.of(year, 10, day);
                case "ноября":
                    return LocalDate.of(year, 11, day);
                case "декабря":
                    return LocalDate.of(year, 12, day);
            }
        }
        return null;
    }

}
