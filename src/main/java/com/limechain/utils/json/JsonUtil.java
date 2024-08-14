package com.limechain.utils.json;

import com.limechain.teavm.HttpRequest;

public class JsonUtil {

    public static Object parseJson(String jsonString) {
        return new JsonParser(jsonString).parse();
    }

    public static String stringify(Object object) {
        return JsonSerializer.serializeToJson(object);
    }

    public static String readJsonFromFile(String filePath) {
        return HttpRequest.asyncHttpRequest("GET", filePath, null);
    }
}
