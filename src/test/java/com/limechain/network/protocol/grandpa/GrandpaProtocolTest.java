package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GrandpaProtocolTest {
    @InjectMocks
    private GrandpaProtocol grandpaProtocol;
    @InjectMocks
    private GrandpaProtocol.NotificationHandler notificationHandler;
    @Mock
    private GrandpaEngine grandpaEngine;
    @Mock
    private Stream stream;
    @Mock
    private ConnectionManager connectionManager;

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
        notificationHandler.engine = grandpaEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onMessage(stream, byteBuf);

        verify(grandpaEngine).receiveRequest(message, stream);
    }

    @Test
    void onClosed() {
        notificationHandler.engine = grandpaEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onClosed(stream);

        verify(connectionManager).closeGrandpaStream(stream);
    }

    @Test
    void onException() {
        notificationHandler.engine = grandpaEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onException(mock(Throwable.class));

        verify(connectionManager).closeGrandpaStream(stream);
    }
}