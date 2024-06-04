package com.limechain.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@UtilityClass
public class TestUtils {

    private static final Gson GSON = new Gson();

    /**
     * Method used to compare the contents of a provided json to an object.
     * The comparison is done in a non-strict order fashion.
     *
     * @param jsonFileName name of the json file located in the resource folder.
     * @param object       the object that will be compared to the provided json.
     */
    public static void assertEquals(String jsonFileName, Object object) throws IOException {
        try (InputStream jsonFileStream = TestUtils.class.getClassLoader().getResourceAsStream(jsonFileName)) {
            assert jsonFileStream != null : "json file not found: " + jsonFileName;
            JsonElement jsonNode = GSON.fromJson(new InputStreamReader(jsonFileStream), JsonElement.class);
            JsonElement objectNode = GSON.toJsonTree(object);

            Assertions.assertEquals(jsonNode, objectNode);
        }
    }
}
