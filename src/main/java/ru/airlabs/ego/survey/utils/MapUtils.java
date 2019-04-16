package ru.airlabs.ego.survey.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Anton Rudenko.
 */
public class MapUtils {

    public static Map<Object,Object> from(Object... values) {
        if ((values.length % 2) != 0) throw new RuntimeException("Number of params must be even");
        Map<Object, Object> result = new HashMap<>();

        for (int i = 0; i < values.length; i=i+2) {
            result.put(values[i], values[i+1]);
        }

        return result;
    }

    public static <K,V> MapBuilder<K,V> builder() {
        return new MapBuilder<>();
    }

    public static MapBuilder<String,Object> commonBuilder(String key, Object value) {
        return MapUtils.<String,Object>builder().add(key, value);
    }

    public static <K,V> MapBuilder<K,V> builder(K k, V v) {
        return MapUtils.<K,V>builder().add(k,v);
    }

    public static class MapBuilder<K,V> {
        Map<K,V> map = new HashMap<>();

        public MapBuilder<K,V> add(K k, V v) {
            map.put(k,v);
            return this;
        }

        public Map<K,V> build() {
            return map;
        }
    }

    public static <K,V> Map<K,V> filterMap(Map<K,V> map, Predicate<? super V> predicate) {
        Map<K,V> result = new HashMap<>();
        map.forEach( (k, v) -> {
            if (predicate.test(v)) result.put(k,v);
        });
        return result;
    }
}
