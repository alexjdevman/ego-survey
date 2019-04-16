package ru.airlabs.ego.survey.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Утилиты для работы с файлами
 *
 * @author Aleksey Gorbachev
 */

public class FileUtils {

    /**
     * Метод, определяющий кол-во файлов внутри директории (включая субдиректории)
     *
     * @param dir директория
     * @return кол-во файлов
     */
    public static long countFilesInDirectory(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException("Input parameter is not a directory");
        }
        long count;
        try (Stream<Path> paths = Files.walk(Paths.get(dir.getAbsolutePath()))) {
            count = paths.filter(Files::isRegularFile).count();
        }
        return count;
    }
}
