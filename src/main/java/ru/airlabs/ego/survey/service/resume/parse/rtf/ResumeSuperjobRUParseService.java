package ru.airlabs.ego.survey.service.resume.parse.rtf;

import ru.airlabs.ego.survey.dto.user.Sex;
import ru.airlabs.ego.survey.service.resume.Resume;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.airlabs.ego.core.entity.ResumeData.Source;
import static ru.airlabs.ego.core.entity.ResumeData.Type;

/**
 * Сервис парсинга резюме с сайта superjob.ru на основе формата RTF
 *
 * @author Roman Kochergin
 */
public class ResumeSuperjobRUParseService extends ResumeHHParseService {

    /**
     * Конструктор
     *
     * @param inputStream поток данных
     */
    public ResumeSuperjobRUParseService(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Создание модели резюме по данным из файла
     *
     * @param lines строки файла
     * @return модель резюме
     */
    @Override
    protected Resume buildResumeFromLines(String[] lines) {
        Resume resume = new Resume(Source.SJ, Type.R);
        try {
            for (String line : lines) {
                line = line.replaceAll("\\t", "").trim();
                // ФИО
                if (line.toLowerCase().startsWith("ф.и.о.:")) {
                    line = line.substring("ф.и.о.:".length()).trim();
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
                // пол
                if (line.toLowerCase().startsWith("пол:")) {
                    line = line.substring("пол:".length()).trim();
                    if (line.toLowerCase().equals("женский")) {
                        resume.setSex(Sex.W);
                    } else if (line.toLowerCase().equals("мужской")) {
                        resume.setSex(Sex.M);
                    }
                }
                // дата рождения
                if (line.toLowerCase().startsWith("дата рождения:")) {
                    line = line.substring("дата рождения:".length()).trim();
                    resume.setBirthDate(parseBirthDate(line));
                }
                // email
                if (line.toLowerCase().startsWith("электронная почта:")) {
                    line = line.substring("электронная почта:".length()).trim();
                    resume.setEmail(parseEmail(line));
                }
                // телефон
                if (line.toLowerCase().startsWith("телефон:")) {
                    line = line.substring("телефон:".length()).trim();
                    resume.setPhone(parsePhone(line));
                }
                // адрес
                if (line.toLowerCase().startsWith("город проживания:")) {
                    line = line.substring("город проживания:".length()).trim();
                    resume.setAddress(line);
                }
                // должность
                if (line.toLowerCase().startsWith("желаемая должность:")) {
                    line = line.substring("желаемая должность:".length()).trim();
                    resume.setPosition(line);
                }
                // зарплата
                if (line.toLowerCase().startsWith("зарплата:")) {
                    line = line.substring("зарплата:".length()).trim();
                    line = line.replaceAll("[^\\d]", "");
                    if (line.length() > 0) {
                        resume.setSalary(Integer.valueOf(line));
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
     * Распарсить дату рождения
     *
     * @param s строка
     * @return дата
     */
    private LocalDate parseBirthDate(String s) {
        String[] tokens = s.replaceAll(" +", " ").toLowerCase().split(" ");
        if (tokens.length < 3) {
            return null;
        }
        if (tokens.length > 3) {
            tokens = Arrays.copyOfRange(tokens, 0, 3);
        }
        int day = Integer.valueOf(tokens[0]);
        int year = Integer.valueOf(tokens[2]);
        switch (tokens[1]) {
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
        return null;
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

}
