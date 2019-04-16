package ru.airlabs.ego.survey.service.resume.image;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Picture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.airlabs.ego.survey.service.resume.parse.rtf.ResumeRTFParseListener;
import ru.airlabs.parser.rtf.rpk.parser.IRtfParser;
import ru.airlabs.parser.rtf.rpk.parser.IRtfSource;
import ru.airlabs.parser.rtf.rpk.parser.RtfStreamSource;
import ru.airlabs.parser.rtf.rpk.parser.standard.StandardRtfParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Реализация сервиса для получения изображений пользователй из резюме
 *
 * @author Aleksey Gorbachev
 */
@Service("resumeImageParseService")
public class ResumeImageParseServiceImpl implements ResumeImageParseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeImageParseServiceImpl.class);

    /**
     * Массив начальных символов, идентифицирующие формат RTF у файла
     */
    private static final char[] rtfHeaderBytes = {'{', '\\', 'r', 't', 'f', '1'};

    /**
     * Строковый маркер начала блока содержимого JPEG изображения для резюме из HH.ru
     */
    public static final String HH_IMAGE_JPEG_CONTENT_MARKER = "{\\pict\\jpegblip\\";
    /**
     * Строковый маркер начала блока содержимого PNG изображения для резюме из HH.ru
     */
    public static final String HH_IMAGE_PNG_CONTENT_MARKER = "{\\pict\\pngblip\\";

    /**
     * Строковый маркер начала блока содержимого JPEG изображения для резюме из Rabota.ua
     */
    public static final String RABOTA_UA_IMAGE_JPEG_CONTENT_MARKER = "\\jpegblip";

    /**
     * Паттерн для определения, является ли строка в кодировке HEX
     */
    public static final String HEX_STRING_PATTERN = "-?[0-9a-fA-F]+";


    @Override
    public byte[] getUserImageFromResume(InputStream stream) {
        try {
            byte[] bytes = IOUtils.toByteArray(stream);
            if (isRTFFile(bytes)) { // если файл в формате RTF
                return getImageFromRTF(bytes);
            } else {    // если файл в формате DOC
                return getImageFromDOC(bytes);
            }
        } catch (IOException err) {
            LOGGER.error(err.getMessage(), err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Получение фото пользователя из резюме в формате RTF
     *
     * @param bytes содержимое файла в массиве байт
     * @return содержимое фото
     */
    private byte[] getImageFromRTF(byte[] bytes) throws IOException {
        String hexImageStr = getHexImageStringForHH(bytes); // получаем HEX строку для HH.RU
        if (isBlank(hexImageStr)) { // если для HH не получилось - пытаемся получить HEX строку для Rabota.ua
            hexImageStr = getHexImageStringForRabotaUA(bytes);
        }
        if (isNotBlank(hexImageStr) && !isHexString(hexImageStr)) {    // если изображение закодировано не в HEX строке
            return getBinaryContentFromRTF(bytes);
        }
        if (isNotBlank(hexImageStr)) {  // получение изображения из HEX строки
            return hexStringToByteArray(hexImageStr);
        } else {
            return new byte[0];
        }
    }

    /**
     * Получение фото пользователя из резюме в формате DOC (из Work.ua)
     *
     * @param bytes содержимое файла в массиве байт
     * @return содержимое фото
     */
    private byte[] getImageFromDOC(byte[] bytes) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            HWPFDocument doc = new HWPFDocument(stream);
            List<Picture> pics = doc.getPicturesTable().getAllPictures();
            if (pics.size() > 2) {   // 2 изображения логотипов в резюме Work.ua есть всегда
                Picture picture = pics.get(pics.size() - 1);    // берем последнее изображение в списке
                return picture.getContent();
            }
            return new byte[0];
        }
    }

    /**
     * Получение изображения пользователя в закодированной HEX-строке для резюме из сайта HH.RU
     *
     * @param bytes данные файла в массиве байт
     * @return закодированная HEX-строка, содержащая изображение пользователя
     * @throws IOException
     */
    private String getHexImageStringForHH(byte[] bytes) throws IOException {
        String fileContent = new String(bytes);
        int indexOfJPEGImageContent = fileContent.lastIndexOf(HH_IMAGE_JPEG_CONTENT_MARKER);
        int indexOfPNGImageContent = getIndexOfPNGImageContent(fileContent);

        int indexOfImageContent = indexOfJPEGImageContent != -1 ? indexOfJPEGImageContent : indexOfPNGImageContent;
        if (indexOfImageContent != -1) {
            String imageFileContent = fileContent.substring(indexOfImageContent, fileContent.length());
            int newLineIndex = imageFileContent.indexOf('\n');
            int closeBracketIndex = imageFileContent.indexOf('}');

            return imageFileContent.substring(newLineIndex + 1, closeBracketIndex);
        }
        return null;
    }

    /**
     * Получение изображения пользователя в закодированной HEX-строке для резюме из сайта Rabota.ua
     *
     * @param bytes данные файла в массиве байт
     * @return закодированная HEX-строка, содержащая изображение пользователя
     * @throws IOException
     */
    private String getHexImageStringForRabotaUA(byte[] bytes) throws IOException {
        String fileContent = new String(bytes);
        int indexOfImageContent = fileContent.lastIndexOf(RABOTA_UA_IMAGE_JPEG_CONTENT_MARKER);

        if (indexOfImageContent != -1) {
            String imageFileContent = fileContent.substring(indexOfImageContent, fileContent.length());
            int newLineIndex = imageFileContent.indexOf('\n');
            imageFileContent = imageFileContent.substring(newLineIndex);
            int closeBracketIndex = imageFileContent.indexOf('}');
            if (closeBracketIndex != -1) {
                return imageFileContent.substring(1, closeBracketIndex).replaceAll("\r\n", "");
            }
        }
        return null;
    }

    /**
     * Получение индекса начала HEX-строки для PNG изображения
     * (получаем последнее PNG-изображение из файла, поскольку первое - это логотип HH.RU)
     *
     * @param fileContent строка содержимого файла
     * @return индекс начала HEX-строки для PNG изображения
     */
    private int getIndexOfPNGImageContent(String fileContent) {
        int lastPNGIndex = fileContent.lastIndexOf(HH_IMAGE_PNG_CONTENT_MARKER);
        int firstPNGIndex = fileContent.indexOf(HH_IMAGE_PNG_CONTENT_MARKER);
        if (lastPNGIndex != -1 && lastPNGIndex != firstPNGIndex) {  // пропускаем первое PNG-изображение
            return lastPNGIndex;
        }
        return -1;
    }

    /**
     * Преобразовать закодированную  HEX-строку в массив байт
     *
     * @param str закодированная HEX-строку
     * @return массив байт
     */
    private byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Получение изображения в виде массива бинарных байт
     *
     * @param bytes исходный файл в массиве байт
     * @return массив байт
     * @throws IOException
     */
    private byte[] getBinaryContentFromRTF(byte[] bytes) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            IRtfSource source = new RtfStreamSource(stream);
            IRtfParser parser = new StandardRtfParser();
            ResumeRTFParseListener listener = new ResumeRTFParseListener();
            parser.parse(source, listener);
            return listener.getBinaryContent();
        }
    }

    /**
     * Является ли строка в кодировке HEX
     *
     * @param string строка
     * @return true, если строка в кодировке HEX
     */
    private boolean isHexString(String string) {
        return string.matches(HEX_STRING_PATTERN);
    }

    /**
     * Определение, является ли файл в формате RTF
     *
     * @param bytes содержимое файла в массиве байт
     * @return true, если в формате RTF
     */
    private boolean isRTFFile(byte[] bytes) {
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        for (int i = 0; i < rtfHeaderBytes.length; i++) {
            if (rtfHeaderBytes[i] != chars[i]) {
                return false;
            }
        }
        return true;
    }

}
