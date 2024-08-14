package com.limechain.utils.json;

import lombok.extern.java.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

@Log
public class JsonSerializer {

    static String serializeToJson(Object object) {
        StringBuilder jsonBuilder = new StringBuilder();
        serializeToJsonInternal(jsonBuilder, object);
        return jsonBuilder.toString();
    }

    private static void serializeToJsonInternal(StringBuilder jsonBuilder, Object object) {
        if (object == null) {
            jsonBuilder.append("null");
        } else if (object instanceof String) {
            jsonBuilder.append("\"").append(object).append("\"");
        } else if (object instanceof Number || object instanceof Boolean) {
            jsonBuilder.append(object);
        } else if (object instanceof byte[]) {
            appendByteArray(jsonBuilder, (byte[]) object);
        } else if (object instanceof List) {
            appendList(jsonBuilder, (List<?>) object);
        } else if (object instanceof Map) {
            appendMap(jsonBuilder, (Map<?, ?>) object);
        } else if (object.getClass().isArray()) {
            appendArray(jsonBuilder, object);
        } else {
            appendObject(jsonBuilder, object);
        }
    }

    private static void appendObject(StringBuilder jsonBuilder, Object object) {
        jsonBuilder.append("{");

        Field[] fields = object.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue = field.get(object);

                jsonBuilder.append("\"").append(fieldName).append("\":");
                serializeToJsonInternal(jsonBuilder, fieldValue);

                if (i < fields.length - 1) {
                    jsonBuilder.append(",");
                }
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            }
        }

        jsonBuilder.append("}");
    }

    private static void appendList(StringBuilder jsonBuilder, List<?> list) {
        jsonBuilder.append("[");
        for (int i = 0; i < list.size(); i++) {
            serializeToJsonInternal(jsonBuilder, list.get(i));
            if (i < list.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
    }

    private static void appendMap(StringBuilder jsonBuilder, Map<?, ?> map) {
        jsonBuilder.append("{");
        Set<?> keys = map.keySet();
        int i = 0;
        for (Object key : keys) {
            jsonBuilder.append("\"").append(key).append("\":");
            serializeToJsonInternal(jsonBuilder, map.get(key));
            if (i < keys.size() - 1) {
                jsonBuilder.append(",");
            }
            i++;
        }
        jsonBuilder.append("}");
    }

    private static void appendArray(StringBuilder jsonBuilder, Object array) {
        jsonBuilder.append("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            serializeToJsonInternal(jsonBuilder, Array.get(array, i));
            if (i < length - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
    }

    private static void appendByteArray(StringBuilder jsonBuilder, byte[] byteArray) {
        String base64 = Base64.getEncoder().encodeToString(byteArray);
        jsonBuilder.append("\"").append(base64).append("\"");
    }
}