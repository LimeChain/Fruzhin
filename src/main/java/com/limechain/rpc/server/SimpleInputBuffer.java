package com.limechain.rpc.server;

import lombok.extern.java.Log;
import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.net.ApplicationBufferHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

@Log
class SimpleInputBuffer implements InputBuffer {
    private final InputStream messageStream;

    public SimpleInputBuffer(InputStream messageStream) {
        this.messageStream = messageStream;
    }

    @Override
    public int doRead(ApplicationBufferHandler handler) throws IOException {
        var size = messageStream.readAllBytes();
        handler.setByteBuffer(ByteBuffer.wrap(size));
        return size.length;
    }

    @Override
    public int available() {
        try {
            return messageStream.available();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error while checking available bytes", e);
        }
        return 0;
    }
}