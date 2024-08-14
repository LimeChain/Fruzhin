package com.limechain.teavm;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSError;

import java.io.IOException;

public class HttpRequest {
    @JSFunctor
    interface HttpRequestCallback extends JSObject {
        void apply(JSError error, String response);
    }

    @Async
    public static native String asyncHttpRequest(String method, String url, JSObject body);
    private static void asyncHttpRequest(String method, String url, JSObject body, AsyncCallback<String> callback) {
        createAsyncHttpRequest(method, url, body, (error, response) -> {
            if (error != null) {
                callback.error(new IOException(error.getMessage()));
            } else {
                callback.complete(response);
            }
        });
    }

    @JSBody(params = {"method", "url", "body", "callback"}, script = "return asyncHttpRequest(method, url, body, callback);")
    public static native void createAsyncHttpRequest(String method, String url, JSObject body, HttpRequestCallback callback);
}
