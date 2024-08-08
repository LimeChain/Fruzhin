package com.limechain.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public class HttpRequest {
    @JSBody(params = {"method", "url", "body"}, script = "return httpRequestSync(method, url, body);")
    public static native String httpRequestSync(String method, String url, JSObject body);
}
