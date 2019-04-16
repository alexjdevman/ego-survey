package ru.airlabs.ego.survey.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

/**
 * Утилиты для работы с форматом gzip
 *
 * @author Roman Kochergin
 */
public class GzipUtils {

    /**
     * Сжать в gzip
     *
     * @param stringToCompress строка
     * @return массив байт
     */
    public static byte[] compress(final String stringToCompress) {
        if (isNull(stringToCompress) || stringToCompress.length() == 0) {
            return null;
        }
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final GZIPOutputStream gzipOutput = new GZIPOutputStream(baos)) {
            gzipOutput.write(stringToCompress.getBytes(UTF_8));
            gzipOutput.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while compression!", e);
        }
    }

    /**
     * Расжать
     *
     * @param compressed массив байт
     * @return строка
     */
    public static String decompress(final byte[] compressed) {
        if (isNull(compressed) || compressed.length == 0) {
            return null;
        }
        try (final GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(compressed));
             final StringWriter stringWriter = new StringWriter()) {
            IOUtils.copy(gzipInput, stringWriter, UTF_8);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while decompression!", e);
        }
    }

}
