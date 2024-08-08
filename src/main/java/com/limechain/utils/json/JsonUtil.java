package com.limechain.utils.json;

import com.limechain.teavm.HttpRequest;

import java.util.Map;

public class JsonUtil {

    static Map<String, Object> parseJson(String jsonPath) {
        return new JsonParser(readJsonFromFile(jsonPath)).parse();
    }

    private static String readJsonFromFile(String filePath) {
        return HttpRequest.httpRequestSync("GET", filePath, null);
    }
}
