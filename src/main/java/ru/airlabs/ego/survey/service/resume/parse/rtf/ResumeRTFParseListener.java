package ru.airlabs.ego.survey.service.resume.parse.rtf;

import ru.airlabs.parser.rtf.rpk.parser.RtfListenerAdaptor;

import java.util.Arrays;

/**
 * Слушатель событий для парсинга резюме в формате RTF
 *
 * @author Aleksey Gorbachev
 */
public class ResumeRTFParseListener extends RtfListenerAdaptor {

    /**
     * Содержимое файла
     */
    private byte[] binaryContent;

    public ResumeRTFParseListener() {
        binaryContent = new byte[0];
    }

    /**
     * Обработка бинарных данных
     *
     * @param data массив бинарных байтов
     */
    @Override
    public void processBinaryBytes(byte[] data) {
        binaryContent = Arrays.copyOf(data, data.length);
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }
}
