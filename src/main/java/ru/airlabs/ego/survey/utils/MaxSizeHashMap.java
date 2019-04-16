package ru.airlabs.ego.survey.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HashMap с ограниченным размером
 *
 * @author Roman Kochergin
 */
public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    public MaxSizeHashMap(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
