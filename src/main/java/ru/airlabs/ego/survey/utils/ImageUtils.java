package ru.airlabs.ego.survey.utils;

import org.imgscalr.Scalr;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Утилиты для обработки изображений
 *
 * @author Aleksey Gorbachev
 */
public class ImageUtils {

    private static final String JPG_FORMAT_NAME = "jpg";

    /**
     * Получение изображения из входного потока
     *
     * @param stream входной поток
     * @return изображение
     */
    public static BufferedImage readImage(InputStream stream) throws IOException {
        BufferedImage image = ImageIO.read(stream);
        checkNotNull(image, "Файл не является изображением. Проверьте формат файла");
        return image;
    }

    /**
     * Масштабирование изображения до нужного размера по большей стороне
     * (меньшая сторона изображения масштабируется в пропорции)
     *
     * @param image   исходное изображение
     * @param maxSize максимальное значение большего измерения (в px)
     * @return масштабированное изображение
     */
    public static BufferedImage scaleImage(BufferedImage image, int maxSize) throws Exception {
        int maxDimensionSize = Math.max(image.getWidth(), image.getHeight());
        if (maxDimensionSize <= maxSize) {
            return image;
        }
        BufferedImage scaledImage = Scalr.resize(image, maxSize);
        return scaledImage;
    }

    /**
     * Масштабирование изображения к одному квадратному измерению
     * (ширина и высота изображения будут одинаковые)
     *
     * @param image изображение
     * @param size  размер изображения (ширина и высота)
     * @return масштабированное изображение
     */
    public static BufferedImage scaleImageToSquare(BufferedImage image, int size) {
         int maxDimensionSize = Math.max(image.getWidth(), image.getHeight());
         if (maxDimensionSize <= size) {
         return image;
         }
         BufferedImage scaledImage = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, size, size);
         return scaledImage;
    }

    /**
     * Оптимизация изображения в нужном качестве в формате JPG и запись в файл
     *
     * @param image    исходное изображение
     * @param destFile результирующий файл с оптимизированным изображением
     * @param quality  качество
     */
    public static void compressAndWriteImageInJPG(BufferedImage image, File destFile, float quality) throws IOException {
        ImageWriter writer = null;
        try (OutputStream os = new FileOutputStream(destFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPG_FORMAT_NAME);
            if (!writers.hasNext())
                throw new IllegalStateException("No writers found");
            writer = writers.next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            if (writer != null) {
                writer.dispose();
            }
        }
    }

}
