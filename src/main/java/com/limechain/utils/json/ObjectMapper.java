package com.limechain.utils.json;

import lombok.extern.java.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Log
public class ObjectMapper {

    boolean failOnUnknown;

    public ObjectMapper(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    public <T> T mapToClass(String jsonPath, Class<T> clazz) throws IOException {
        Map<String, Object> jsonMap = JsonUtil.parseJson(jsonPath);

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Field field = findField(clazz, key);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(instance, convertValue(field.getType(), value));
                }
            }
            return instance;
        } catch (Exception e) {
            throw new IOException("Failed to map JSON to class", e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (failOnUnknown) {
                throw new NoSuchFieldException("Field " + fieldName + " does not exist in " + clazz.getName());
            } else {
                log.fine("Field " + fieldName + " does not exist in " + clazz.getName());
                return null;
            }
        }
    }

    private static Object convertValue(Class<?> type, Object value) {
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return ((Number) value).intValue();
        } else if (type == Long.class || type == long.class) {
            return ((Number) value).longValue();
        } else if (type == Double.class || type == double.class) {
            return ((Number) value).doubleValue();
        } else if (type == Boolean.class || type == boolean.class) {
            return value;
        } else if (type == String.class) {
            return value.toString();
        } else if (type.isArray()) {
            return convertArray(type.getComponentType(), (List<?>) value);
        }

        throw new RuntimeException("Unsupported field type: " + type);
    }

    private static Object convertArray(Class<?> componentType, List<?> jsonArray) {
        Object array = Array.newInstance(componentType, jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            Array.set(array, i, convertValue(componentType, jsonArray.get(i)));
        }
        return array;
    }
}