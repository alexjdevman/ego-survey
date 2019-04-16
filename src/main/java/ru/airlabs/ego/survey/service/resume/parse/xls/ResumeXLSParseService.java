package ru.airlabs.ego.survey.service.resume.parse.xls;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import ru.airlabs.ego.core.entity.ResumeData;
import ru.airlabs.ego.survey.service.resume.Resume;
import ru.airlabs.ego.survey.service.resume.parse.IResumeListParseService;
import ru.airlabs.ego.survey.service.resume.parse.InvalidFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.core.entity.ResumeData.Source;

/**
 * Сервис для парсинга данных для резюме в формате XLS
 *
 * @author Aleksey Gorbachev
 */
public class ResumeXLSParseService implements IResumeListParseService {

    /**
     * Поток данных
     */
    private InputStream inputStream;

    /**
     * Номер таблицы данных в excel документе
     */
    private static final Integer workSheetNumber = 0;

    /**
     * Номер строки, где расположены данные для разбора
     */
    private static final Integer dataRowStartNumber = 0;

    /**
     * Конструктор
     *
     * @param inputStream поток данных
     */
    public ResumeXLSParseService(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Распарсить данные для резюме в формате XLS
     *
     * @return резюме
     * @throws IOException            ошибка ввода-вывода
     * @throws InvalidFormatException ошибка неверного формата данных
     */
    @Override
    public List<Resume> parse() throws IOException, InvalidFormatException {
        try (InputStream stream = getInputStream()) {
            List<Resume> resumeList = new LinkedList<>();
            HSSFWorkbook workbook = new HSSFWorkbook(stream);
            Sheet sheet = workbook.getSheetAt(workSheetNumber);
            for (int rowIndex = dataRowStartNumber; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                final Row row = sheet.getRow(rowIndex);
                if (!isRowEmpty(row)) {
                    resumeList.add(getResumeFromRow(row));
                }
            }
            return resumeList;
        } catch (IOException io) {
            throw io;
        } catch (Exception e) {
            throw new InvalidFormatException("Неверный формат данных", e);
        }
    }

    @Override
    public List<Resume> parseInHTML(Source source) throws IOException, InvalidFormatException {
        throw new UnsupportedOperationException("Не поддерживается для Excel парсера");
    }

    private Resume getResumeFromRow(Row row) {
        Resume resume = new Resume(Source.UN,  ResumeData.Type.E);
        String userFullName = getStringCellValue(row, 0);
        String[] name = userFullName.replaceAll(" +", " ").split(" ");
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

        resume.setEmail(replaceAllSpaces(getStringCellValue(row, 1)));
        resume.setPhone(formatPhoneNumber(getStringCellValue(row, 2)));
        String address = getStringCellValue(row, 3);
        resume.setAddress(isNotBlank(address) ? address.trim() : null);
        resume.setPosition(getPositionFromRow(row));

        String salary = getStringCellValue(row, 5);
        resume.setSalary(isNotBlank(salary) ? Integer.valueOf(salary) : null);
        return resume;
    }

    private String getStringCellValue(Row row, int cellNumber) {
        final Cell cell = row.getCell(cellNumber);
        String result;
        if (cell != null) {
            int cellType = cell.getCellType();
            switch (cellType) {
                case Cell.CELL_TYPE_BLANK:
                    result = "";
                    break;
                case Cell.CELL_TYPE_STRING:
                    result = cell.getRichStringCellValue().getString();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    NumberFormat numberFormat = DecimalFormat.getInstance();
                    numberFormat.setGroupingUsed(Boolean.FALSE);
                    result = (numberFormat.format(cell.getNumericCellValue()));
                    break;
                default:
                    result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    private boolean isRowEmpty(Row r) {
        int lastColumn = Math.max(r.getLastCellNum(), 0);

        for (int cn = 0; cn < lastColumn; cn++) {
            Cell c = r.getCell(cn, Row.RETURN_BLANK_AS_NULL);
            if (c != null) {
                return false;
            }
        }
        return true;
    }

    private String formatPhoneNumber(String line) {
        if (isNotBlank(line)) {
           return line.replaceAll("[^\\d]", "").trim();
        }
        return null;
    }

    /**
     * Получение должности для резюме
     *
     * @param row строка в Excel
     * @return должность для резюме
     */
    private String getPositionFromRow(Row row) {
        String positionName = getStringCellValue(row, 4);
        String positionPlace = getStringCellValue(row, 6);
        if (isNotBlank(positionName)) {
            String position = positionName + (isNotBlank(positionPlace) ? " " + positionPlace : "");
            return position;
        } else {
            return null;
        }
    }

    /**
     * Удаление всех пробелов из строкового значения (с учетом специального символа &nbsp;)
     *
     * @param value строковое значение
     * @return преобразованная строка
     */
    private String replaceAllSpaces(String value) {
        if (isNotBlank(value)) {
            return value.replaceAll("\\s+", "").replaceAll("\u00A0", "");
        }
        return null;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
