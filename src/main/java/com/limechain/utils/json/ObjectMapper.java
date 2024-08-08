package com.limechain.utils.json;

import com.limechain.chain.spec.ChainSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectMapper {

    boolean failOnUnknown;

    public ObjectMapper(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    public <T> T mapToClass(String jsonPath, Class<T> clazz) throws IOException {
        Map<String, Object> jsonMap = JsonUtil.parseJson(jsonPath);

        T instance = createInstance(clazz);
        populateFields(instance, jsonMap);

        return instance;
    }

    private <T> T createInstance(Class<T> clazz) {
        if (clazz == ChainSpec.class) {
            return (T) ObjectFactory.createChainSpec();
        }
        // Handle other types similarly
        throw new IllegalArgumentException("Unsupported class type: " + clazz.getName());
    }

    private void populateFields(Object instance, Map<String, Object> jsonMap) throws IOException {
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            setField(instance, key, value);
        }
    }

    private void setField(Object instance, String key, Object value) throws IOException {
        if (instance instanceof ChainSpec) {
            setChainSpecFields((ChainSpec) instance, key, value);
        } else {
            throw new IOException("Unsupported object type for field setting.");
        }
    }

    private void setChainSpecFields(ChainSpec instance, String key, Object value) throws IOException {
        switch (key) {
            case "id":
                instance.setId(convertValue(String.class, value));
                break;
            case "name":
                instance.setName(convertValue(String.class, value));
                break;
            case "protocolId":
                instance.setProtocolId(convertValue(String.class, value));
                break;
            case "bootNodes":
                instance.setBootNodes(convertValue(String[].class, value));
                break;
            case "lightSyncState":
                instance.setLightSyncState(convertValue(Map.class, value));
                break;
            default: {
                if (failOnUnknown) {
                    throw new IOException("Unsupported field key: " + key);
                }
            }
        }
    }

    private <T> T convertValue(Class<T> type, Object value) {
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return (T) value.toString();
        } else if (type == int.class || type == Integer.class) {
            return (T) Integer.valueOf(value.toString());
        } else if (type == long.class || type == Long.class) {
            return (T) Long.valueOf(value.toString());
        } else if (type == double.class || type == Double.class) {
            return (T) Double.valueOf(value.toString());
        } else if (type == float.class || type == Float.class) {
            return (T) Float.valueOf(value.toString());
        } else if (type == boolean.class || type == Boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        } else if (type == char.class || type == Character.class) {
            return (T) Character.valueOf(value.toString().charAt(0));
        } else if (type.isArray()) {
            // Handle arrays
            Class<?> componentType = type.getComponentType();
            List<?> list = (List<?>) value;
            Object array = java.lang.reflect.Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                java.lang.reflect.Array.set(array, i, convertValue(componentType, list.get(i)));
            }
            return (T) array;
        } else if (type == Map.class) {
            // Handle maps
            Map<String, String> map = (Map<String, String>) value;
            Map<String, String> resultMap = new HashMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                resultMap.put(entry.getKey(), convertValue(String.class, entry.getValue()));
            }
            return (T) resultMap;
        } else {
            throw new IllegalArgumentException("Unsupported conversion type: " + type.getName());
        }
    }
}