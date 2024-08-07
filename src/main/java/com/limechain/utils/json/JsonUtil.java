package com.limechain.utils.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class JsonUtil {

    static Map<String, Object> parseJson(String jsonPath) {
        try {
            return new JsonParser(readJsonFromFile(jsonPath)).parse();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse json at path " + jsonPath);
        }
    }

    private static String readJsonFromFile(String filePath) throws IOException {
        StringBuilder jsonStringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
        }
        return jsonStringBuilder.toString();
    }
}
