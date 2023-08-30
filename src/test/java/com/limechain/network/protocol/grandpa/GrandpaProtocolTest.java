package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class GrandpaProtocolTest {
    private GrandpaProtocol grandpaProtocol;

    private GrandpaProtocol.NotificationHandler notificationHandler;
    private GrandpaEngine grandpaEngine;
    private Stream stream;
    private static final ConnectionManager CONNECTION_MANAGER = mock(ConnectionManager.class);

    @BeforeAll
    static void init() {
        mockStatic(ConnectionManager.class).when(ConnectionManager::getInstance).thenReturn(CONNECTION_MANAGER);
    }
    @BeforeEach
    void setUp() {
        stream = mock(Stream.class);
        grandpaEngine = mock(GrandpaEngine.class);
        grandpaProtocol = new GrandpaProtocol();
        notificationHandler = new GrandpaProtocol.NotificationHandler(stream);
        notificationHandler.engine = grandpaEngine;
    }

    @Test
    void onStartInitiator() {
        GrandpaController result = grandpaProtocol.onStartInitiator(stream).join();

        verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
        verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
        verify(stream).pushHandler(any(ByteArrayEncoder.class));
        verify(stream).pushHandler(any(GrandpaProtocol.NotificationHandler.class));

        assertEquals(stream, result.stream);
    }

    @Test
    void onStartResponder() {
        GrandpaController result = grandpaProtocol.onStartResponder(stream).join();

        verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
        verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
        verify(stream).pushHandler(any(ByteArrayEncoder.class));
        verify(stream).pushHandler(any(GrandpaProtocol.NotificationHandler.class));

        assertEquals(stream, result.stream);
    }

    @Test
    void onMessage() {
        byte[] message = new byte[] { 1, 2, 3 };
        ByteBuf byteBuf = Unpooled.copiedBuffer(message);

        notificationHandler.onMessage(stream, byteBuf);

        verify(grandpaEngine).receiveRequest(message, stream);
    }

    @Test
    void onClosed() {
        notificationHandler.onClosed(stream);

        verify(CONNECTION_MANAGER).closeGrandpaStream(stream);
    }

    @Test
    void onException() {
        notificationHandler.onException(mock(Throwable.class));

        verify(CONNECTION_MANAGER).closeGrandpaStream(stream);
    }
}