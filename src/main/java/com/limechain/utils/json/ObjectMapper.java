package com.limechain.utils.json;

import lombok.extern.java.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Log
public class ObjectMapper {

    private final boolean failOnUnknownField;

    public ObjectMapper(boolean failOnUnknownField) {
        this.failOnUnknownField = failOnUnknownField;
    }

    @SuppressWarnings("unchecked")
    public <T> T mapToClass(String jsonString, Class<T> clazz) {
        Object parsed = JsonUtil.parseJson(jsonString);

        if (isPrimitiveOrWrapper(clazz) || clazz == String.class || clazz.isArray() || clazz == byte[].class) {
            return convertValue(clazz, parsed);
        }

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) parsed).entrySet()) {
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
            throw new RuntimeException("Failed to map JSON to class", e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (failOnUnknownField) {
                throw new IllegalStateException("Field " + fieldName + " does not exist in " + clazz.getName());
            } else {
                log.log(Level.FINE, "Field " + fieldName + " does not exist in " + clazz.getName());
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Class<T> type, Object value) {
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        } else if (type == Integer.class || type == int.class) {
            return handleWholeNumber(value, (long) Integer.MIN_VALUE, (long) Integer.MIN_VALUE);
        } else if (type == Long.class || type == long.class) {
            return handleWholeNumber(value, Long.MIN_VALUE, Long.MAX_VALUE);
        } else if (type == Double.class || type == double.class) {
            BigDecimal bigDecimalValue = new BigDecimal((String) value);
            double doubleValue = bigDecimalValue.doubleValue();
            if (doubleValue == Double.POSITIVE_INFINITY || doubleValue == Double.NEGATIVE_INFINITY) {
                throw new ArithmeticException("Value out of range for Double: " + value);
            }
            return (T) Double.valueOf(doubleValue);
        } else if (type == BigInteger.class) {
            return (T) new BigInteger((String) value);
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) value;
        } else if (type == String.class) {
            return (T) value.toString();
        } else if (type == byte[].class) {
            if (value instanceof String) {
                return (T) Base64.getDecoder().decode((String) value);
            } else {
                throw new RuntimeException("Unsupported value type for byte[]: " + value.getClass());
            }
        } else if (type.isArray()) {
            return (T) convertArray(type.getComponentType(), (List<?>) value);
        }

        throw new RuntimeException("Unsupported field type: " + type);
    }

    @SuppressWarnings("unchecked")
    private <T> T handleWholeNumber(Object value, Long min, Long max) {
        BigInteger bigIntValue = new BigInteger((String) value);
        if (bigIntValue.compareTo(BigInteger.valueOf(min)) < 0 ||
            bigIntValue.compareTo(BigInteger.valueOf(max)) > 0) {
            throw new ArithmeticException("Value out of range number type: " + value);
        }
        return (T) Integer.valueOf(bigIntValue.intValue());
    }

    private Object convertArray(Class<?> componentType, List<?> jsonArray) {
        Object array = Array.newInstance(componentType, jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            Array.set(array, i, convertValue(componentType, jsonArray.get(i)));
        }
        return array;
    }

    // Utility method to check if a class is a primitive type or its wrapper
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
            clazz == Integer.class || clazz == Long.class ||
            clazz == Double.class || clazz == Float.class ||
            clazz == Boolean.class || clazz == Byte.class ||
            clazz == Short.class || clazz == Character.class ||
            clazz == BigInteger.class;
    }
}