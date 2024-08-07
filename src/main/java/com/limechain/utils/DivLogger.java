package com.limechain.utils;

import org.teavm.jso.dom.html.HTMLDocument;

import java.util.logging.Level;

public class DivLogger {
    public void log(String text){
        var document = HTMLDocument.current();
        var div = document.createElement("div");
        div.appendChild(document.createTextNode(text));
        document.getBody().appendChild(div);
    }

    public void log(Level info, String text) {
        log(info + " " + text);
    }
}
