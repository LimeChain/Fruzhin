package com.limechain.storage;

import com.limechain.utils.json.JsonUtil;
import com.limechain.utils.json.ObjectMapper;
import org.teavm.jso.JSBody;

import java.util.Optional;

/**
 * Local storage interface
 */
public abstract class LocalStorage {

    private static final ObjectMapper MAPPER = new ObjectMapper(false);

    public static void save(String key, Object object) {
        save(key, JsonUtil.stringify(object));
    }

    /**
     * Persists/Updates a key-value pair in the local storage.
     *
     * @param key   key of the pair
     * @param value value of the pair
     */
    @JSBody(params = {"key", "value"}, script = "localStorage.setItem(key, value);")
    private static native void save(String key, String value);

    public static <T> Optional<T> find(String key, Class<T> clazz) {
        String found = find(key);
        return found != null
            ? Optional.ofNullable(MAPPER.mapToClass(find(key), clazz))
            : Optional.empty();
    }

    /**
     * Tries to find a value for a given key in the local storage.
     *
     * @param key the key to search for
     * @return Result under the provided key or null.
     */
    @JSBody(params = {"key"}, script = "return localStorage.getItem(key);")
    private static native String find(String key);

    /**
     * Deletes a key-value pair from the local storage.
     *
     * @param key the key of the pair
     */
    @JSBody(params = {"key"}, script = "localStorage.removeItem(key);")
    public static native void delete(String key);

    /**
     * Clears the local storage.
     */
    @JSBody(script = "localStorage.clear();")
    public static native void clear();

}
