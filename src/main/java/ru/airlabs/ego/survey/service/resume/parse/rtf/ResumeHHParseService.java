package ru.airlabs.ego.survey.service.resume.parse.rtf;

import org.apache.commons.io.IOUtils;
import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.parse.IResumeParseService;
import ru.airlabs.ego.survey.service.resume.parse.InvalidFormatException;
import ru.airlabs.parser.rtf.rpk.parser.RtfStreamSource;
import ru.airlabs.parser.rtf.rpk.text.StringTextConverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.logging.log4j.util.Strings.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.core.entity.ResumeData.Type;


/**
 * Сервис парсинга резюме из HH (HeadHunter) на основе формата RTF
 *
 * @author Roman Kochergin
 */
public class ResumeHHParseService implements IResumeParseService {

    /**
     * Текст заголовка для занимаемой должности в резюме на английском
     */
    private static final String ENGLISH_RESUME_POSITION_TAG = "desired position and salary";

    /**
     * Текст заголовка для занимаемой должности в резюме на русском
     */
    private static final String RUSSIAN_RESUME_POSITION_TAG = "желаемая должность и зарплата";

    /**
     * Последовательности символов, мешающие разбору резюме из HH в формате rtf (регулярное выражение)
     */
    private static final String errorCharSequence = "\\{\\\\cf189[0-9]{9}\\}";

    /**
     * Поток данных
     */
    private InputStream inputStream;

    /**
     * Конструктор
     *
     * @param inputStream поток данных
     */
    public ResumeHHParseService(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Распарсить файл-резюме
     *
     * @return резюме
     * @throws IOException    ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    @Override
    public Resume parse() throws IOException, InvalidFormatException {
        try (InputStream stream = new ByteArrayInputStream(clearBytes(getInputStream()))) {
            StringTextConverter converter = new StringTextConverter();
            converter.convert(new RtfStreamSource(stream));
            String text = converter.getText();
            String[] lines = getLines(text);
            return buildResumeFromLines(lines);
        } catch (Exception e) {
            throw new InvalidFormatException("Неверный формат данных", e);
        }
    }

    /**
     * Убираем из входного потока символы, которые мешают разбору библиотекой rtf-parser-kit
     *
     * @param stream входной поток
     * @return массив байт
     * @throws IOException
     */
    private byte[] clearBytes(InputStream stream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(stream);
        String fileContent = new String(bytes);
        fileContent = fileContent.replaceAll(errorCharSequence, "");
        return fileContent.getBytes();
    }


    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Получение массива строк из файла
     *
     * @param text текст из файла
     * @return массив строк
     */
    protected String[] getLines(String text) {
        // убираем пустые строки
        text = text.replaceAll("(?m)^[ \t]*\r?\n", "");
        return text.split("\\n");
    }

    /**
     * Создание модели резюме по данным из файла
     *
     * @param lines строки файла
     * @return модель резюме
     */
    protected Resume buildResumeFromLines(String[] lines) {
        if (isResumeInEnglish(lines)) {
            return parseEnglish(lines);
        } else {
            return parseRussian(lines);
        }
    }

    /**
     * Проверка на анлийском ли языке резюме
     *
     * @param lines массив строк
     * @return да или нет
     */
    private boolean isResumeInEnglish(String[] lines) {
        for (String line : lines) {
           String formatLine = line.replaceAll(" +", " ").toLowerCase().trim();
           if (formatLine.startsWith(RUSSIAN_RESUME_POSITION_TAG)) {
               return Boolean.FALSE;
           } else if (formatLine.startsWith(ENGLISH_RESUME_POSITION_TAG)) {
               return Boolean.TRUE;
           }
        }
        return Boolean.FALSE;
    }

    /**
     * Парсинг резюме на русском
     *
     * @param lines массив строк
     * @return резюме
     */
    private Resume parseRussian(String[] lines) {
        Resume resume = new Resume(Source.HH, Type.R);
        try {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                line = line.replaceAll("\\t", "").trim();
                if (i == 0) {
                    // ФИО
                    String[] name = line.replaceAll(" +", " ").split(" ");
                    if (name.length > 1) {
                        resume.setLastName(name[0]);
                        resume.setFirstName(name[1]);
                    }
                    if (name.length == 3) {
                        resume.setMiddleName(name[2]);
                    }
                }
                if (i == 1) {
                    // пол
                    if (line.toLowerCase().contains("женщина")) {
                        resume.setSex(Sex.W);
                    } else if (line.toLowerCase().contains("мужчина")) {
                        resume.setSex(Sex.M);
                    }
                    // дата рождения
                    resume.setBirthDate(parseBirthDateRussian(line));
                }
                if (i == 2 || i == 3 || i == 4 || i == 5) {
                    // email
                    if (resume.getEmail() == null) {
                        resume.setEmail(parseEmail(line));
                    }
                    // телефон
                    if (resume.getPhone() == null) {
                        resume.setPhone(parsePhone(line));
                    }
                }
                if (line.toLowerCase().startsWith("проживает:")) {
                    // адрес
                    if (resume.getAddress() == null) {
                        String s = line.replaceFirst("(?i)(?u)проживает:", "");
                        resume.setAddress(s.trim());
                    }
                }
                if (line.replaceAll(" +", " ").toLowerCase().equals(RUSSIAN_RESUME_POSITION_TAG)) {
                    // должность
                    if (resume.getPosition() == null) {
                        String position = "";
                        for (int j = (i + 1); j < lines.length; j++) {
                            String s = lines[j].replaceAll("\\t", "").trim();
                            if (s.toLowerCase().startsWith("занятость:")) {
                                if (position.length() > 0) {
                                    resume.setPosition(position);
                                    break;
                                }
                            }
                            position += (s + "\n");
                        }
                    }
                }
                if ((i == 15 || i == 16 || i == 17) && !line.toLowerCase().contains("опыт работы")) {  // зарплата
                    if (resume.getSalary() == null) {
                        resume.setSalary(parseSalary(line));
                    }
                }
                if (resume.isFullFilled()) {
                    break;
                }
            }
        } catch (Exception err) {   // при ошибке разбора строк возвращаем null
            return null;
        }
        return resume;
    }

    /**
     * Парсинг резюме на английском
     *
     * @param lines массив строк
     * @return резюме
     */
    private Resume parseEnglish(String[] lines) {
        Resume resume = new Resume(Source.HH, Type.R);
        try {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                line = line.replaceAll("\\t", "").trim();
                if (i == 0) {
                    // ФИО
                    String[] name = line.replaceAll(" +", " ").split(" ");
                    if (name.length > 1) {
                        resume.setLastName(name[0]);
                        resume.setFirstName(name[1]);
                    }
                    if (name.length == 3) {
                        resume.setMiddleName(name[2]);
                    }
                }
                if (i == 1) {
                    // пол
                    if (line.toLowerCase().contains("female")) {
                        resume.setSex(Sex.W);
                    } else if (line.toLowerCase().contains("male")) {
                        resume.setSex(Sex.M);
                    }
                    // дата рождения
                    resume.setBirthDate(parseBirthDateEnglish(line));
                }
                if (i == 2 || i == 3 || i == 4 || i == 5) {
                    // email
                    if (resume.getEmail() == null) {
                        resume.setEmail(parseEmail(line));
                    }
                    // телефон
                    if (resume.getPhone() == null) {
                        resume.setPhone(parsePhone(line));
                    }
                }
                if (line.toLowerCase().startsWith("reside in:")) {
                    // адрес
                    if (resume.getAddress() == null) {
                        String s = line.replaceFirst("(?i)(?u)reside in:", "");
                        resume.setAddress(s.trim());
                    }
                }
                if (line.replaceAll(" +", " ").toLowerCase().equals(ENGLISH_RESUME_POSITION_TAG)) {
                    // должность
                    if (resume.getPosition() == null) {
                        String position = "";
                        for (int j = (i + 1); j < lines.length; j++) {
                            String s = lines[j].replaceAll("\\t", "").trim();
                            if (s.toLowerCase().startsWith("employment:")) {
                                if (position.length() > 0) {
                                    resume.setPosition(position);
                                    break;
                                }
                            }
                            position += (s + "\n");
                        }
                    }
                }
                if ((i == 15 || i == 16 || i == 17) && !line.toLowerCase().contains("work experience")) {  // зарплата
                    if (resume.getSalary() == null) {
                        resume.setSalary(parseSalary(line));
                    }
                }
                if (resume.isFullFilled()) {
                    break;
                }
            }
        } catch (Exception err) {   // при ошибке разбора строк возвращаем null
            return null;
        }
        return resume;
    }

    /**
     * Распарсить дату рождения на русском
     *
     * @param s строка
     * @return дата
     */
    private LocalDate parseBirthDateRussian(String s) {
        String[] tokens = s.split(",");
        for (String token : tokens) {
            String text = null;
            if (token.toLowerCase().contains("родилась")) {
                text = token.replace("родилась", "").trim().replaceAll(" +", " ").toLowerCase();
            } else if (token.toLowerCase().contains("родился")) {
                text = token.replace("родился", "").trim().replaceAll(" +", " ").toLowerCase();
            }
            if (text != null) {
                String[] date = text.split(" ");
                if (date.length == 3) {
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
            }
        }
        return null;
    }

    /**
     * Распарсить дату рождения на английском
     *
     * @param s строка
     * @return дата
     */
    private LocalDate parseBirthDateEnglish(String s) {
        String[] tokens = s.split(",");
        for (String token : tokens) {
            String text = null;
            if (token.toLowerCase().contains("born on")) {
                text = token.replace("born on", "").trim().replaceAll(" +", " ").toLowerCase();
            }
            if (text != null) {
                String[] date = text.split(" ");
                if (date.length == 3) {
                    int day = Integer.valueOf(date[0]);
                    int year = Integer.valueOf(date[2]);
                    switch (date[1]) {
                        case "january":
                            return LocalDate.of(year, 1, day);
                        case "february":
                            return LocalDate.of(year, 2, day);
                        case "march":
                            return LocalDate.of(year, 3, day);
                        case "april":
                            return LocalDate.of(year, 4, day);
                        case "may":
                            return LocalDate.of(year, 5, day);
                        case "june":
                            return LocalDate.of(year, 6, day);
                        case "july":
                            return LocalDate.of(year, 7, day);
                        case "august":
                            return LocalDate.of(year, 8, day);
                        case "september":
                            return LocalDate.of(year, 9, day);
                        case "october":
                            return LocalDate.of(year, 10, day);
                        case "november":
                            return LocalDate.of(year, 11, day);
                        case "december":
                            return LocalDate.of(year, 12, day);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Распарсить email
     *
     * @param s строка
     * @return email
     */
    private String parseEmail(String s) {
        Matcher m = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(s);
        if (m.find()) {
            return m.group().toLowerCase();
        }
        return null;
    }

    /**
     * Распарсить телефон
     *
     * @param s строка
     * @return телефон
     */
    private String parsePhone(String s) {
        if (s.contains("—")) {  // если в строке 2 номера разделенных символом '—' то получаем первый номер
            s = s.split("—")[0];
        }
        int idx = findFirstLetterPosition(s);
        if (idx == -1) {
            s = s.replaceAll("[^\\d]", "");
        } else {
            s = s.substring(0, idx);
            s = s.replaceAll("[^\\d]", "");
        }
        if (s.length() == 11) {
            if (s.startsWith("8")) {
                s = s.replaceFirst("8", "7");
            }
            if (s.startsWith("7")) {
                return s;
            }
        } else if (s.length() == 13 && s.startsWith("007")) {
            return s.replaceFirst("007", "7");
        }
        if (isNotBlank(s)) {
            return s;
        } else {
            return null;
        }
    }

    /**
     * Найти позицию первой буквы в строке
     *
     * @param input строка
     * @return позиция
     */
    private int findFirstLetterPosition(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (Character.isLetter(input.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Получение зарплаты из резюме
     *
     * @param line строка с зарплатой из резюме
     * @return зарплата пользователя
     */
    protected Integer parseSalary(String line) {
        if (isNotBlank(line)) {
            String salaryValue = line.replaceAll("[^0-9]+","");
            if (isNotBlank(salaryValue) && salaryValue.length() <= 7) {
                return Integer.parseInt(salaryValue);
            }
        }
        return null;
    }

}
