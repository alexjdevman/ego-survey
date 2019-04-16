package ru.airlabs.ego.survey.service.resume.parse.rtf;

import ru.airlabs.ego.survey.service.resume.Resume;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.core.entity.ResumeData.Type;

/**
 * Сервис парсинга резюме из сайта rabota.ua на основе формата RTF
 *
 * @author Aleksey Gorbachev
 */
public class ResumeRabotaUAParseService extends ResumeHHParseService {

    /**
     * Индикатор резюме с "SuperJob"
     */
    private static final String SUPERJOB_RESUME_INDICATOR = "superjob";

    /**
     * Конструктор
     *
     * @param inputStream поток данных
     */
    public ResumeRabotaUAParseService(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Создание модели резюме по данным из файла резюме из сайта rabota.ua
     *
     * @param lines строки файла
     * @return модель резюме
     */
    @Override
    protected Resume buildResumeFromLines(String[] lines) {
        Resume resume = new Resume(Source.RB, Type.R);
        if (isSuperJobResume(lines)) return null;   // возвращаем null, если резюме из SuperJob
        int firstLineIndex = detectFirstLineIndex(lines);
        try {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                line = line.replaceAll("\\t", "").trim();
                if (i == firstLineIndex) {
                    // ФИО
                    if (isNotBlank(line)) {
                        fillClientName(line, resume);
                    }
                }
                if (i == firstLineIndex + 1) {
                    // Дата рождения
                    resume.setBirthDate(parseBirthDate(line));
                }
                if (i == firstLineIndex + 2) {
                    // Адрес
                    resume.setAddress(parseAddress(line));
                }
                if (i == firstLineIndex + 4) {
                    // Телефон
                    resume.setPhone(parsePhone(line));
                }
                if (i == firstLineIndex + 5) {
                    // Email
                    resume.setEmail(parseEmail(line));
                }
                if (i == firstLineIndex + 6) {
                    // Должность
                    if (isNotBlank(line)) {
                        resume.setPosition(line);
                        resume.setSalary(parseSalary(line));
                    }
                }
            }

        } catch (Exception err) {   // при ошибке разбора строк возвращаем null
            return null;
        }
        return resume;
    }

    /**
     * Определение номера первой строки резюме, которая подлежит разбору
     *
     * @param lines строки
     * @return номер строки
     */
    private int detectFirstLineIndex(String[] lines) {
        String firstLine = lines[0];
        if (firstLine.contains("rabota.ua")) {  // отбрасываем первую ненужную строку
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Проверка, является ли содержимое резюме из SuperJob
     *
     * @param lines содержимое резюме
     * @return true/false
     */
    private boolean isSuperJobResume(String[] lines) {
        String firstLine = lines[0];
        if (firstLine.replaceAll(" +", " ").toLowerCase().trim().startsWith(SUPERJOB_RESUME_INDICATOR)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Распарсить дату рождения
     *
     * @param line строка
     * @return дата
     */
    private LocalDate parseBirthDate(String line) {
        if (isNotBlank(line) && line.startsWith("Date of birth:")) {
            String text = line.replace("Date of birth:", "").trim();
            String[] date = text.split(" ");
            if (date.length == 3) {
                int day = Integer.valueOf(date[0]);
                int year = Integer.valueOf(date[2]);
                switch (date[1]) {
                    case "янв":
                        return LocalDate.of(year, 1, day);
                    case "фев":
                        return LocalDate.of(year, 2, day);
                    case "мар":
                        return LocalDate.of(year, 3, day);
                    case "апр":
                        return LocalDate.of(year, 4, day);
                    case "май":
                        return LocalDate.of(year, 5, day);
                    case "июн":
                        return LocalDate.of(year, 6, day);
                    case "июл":
                        return LocalDate.of(year, 7, day);
                    case "авг":
                        return LocalDate.of(year, 8, day);
                    case "сен":
                        return LocalDate.of(year, 9, day);
                    case "окт":
                        return LocalDate.of(year, 10, day);
                    case "ноя":
                        return LocalDate.of(year, 11, day);
                    case "дек":
                        return LocalDate.of(year, 12, day);
                }
            }
        }
        return null;
    }

    /**
     * Парсинг имени соискателя
     *
     * @param line строка
     * @param resume резюме
     * @return
     */
    protected void fillClientName(String line, Resume resume) {
        String[] name = line.replaceAll(" +", " ").split(" ");
        if (name.length == 1) {
            resume.setFirstName(name[0]);
        } else if (name.length == 2) {
            resume.setLastName(name[0]);
            resume.setFirstName(name[1]);
        } else if (name.length == 3) {
            resume.setLastName(name[0]);
            resume.setFirstName(name[1]);
            resume.setMiddleName(name[2]);
        }
    }

    /**
     * Разбор адреса соискателя для резюме
     *
     * @param line строка
     * @return адрес соискателя для резюме
     */
    private String parseAddress(String line) {
        if (isNotBlank(line) && line.startsWith("Location:")) {
            return line.replace("Location:", "").trim();
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
        if (isNotBlank(line) && line.startsWith("Phone number(s):")) {
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
        if (isNotBlank(line) && line.startsWith("E-mail:")) {
            Matcher m = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(line);
            if (m.find()) {
                return m.group().toLowerCase();
            }
        }
        return null;
    }


}
