package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.ConnectionManager;
import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import com.limechain.network.protocol.BaseUtils;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BeefyNotificationProtocolTest {

    @InjectMocks
    private BeefyNotificationProtocol beefyNotificationProtocol;
    @InjectMocks
    private BeefyNotificationProtocol.NotificationHandler notificationHandler;
    @Mock
    private BeefyNotificationEngine beefyNotificationEngine;
    @Mock
    private Stream stream;
    @Mock
    private ConnectionManager connectionManager;

    @Test
    void onStartInitiator()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {

        Object result = BaseUtils.callProtectedMethod(beefyNotificationProtocol, stream, "onStartInitiator");
        BeefyNotificationController actualResult = ((CompletableFuture<BeefyNotificationController>) result).join();

        verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
        verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
        verify(stream).pushHandler(any(ByteArrayEncoder.class));
        verify(stream).pushHandler(any(BeefyNotificationProtocol.NotificationHandler.class));

        assertEquals(stream, BaseUtils.getProtectedStreamField(actualResult));
    }

    @Test
    void onStartResponder()
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Object result = BaseUtils.callProtectedMethod(beefyNotificationProtocol, stream, "onStartResponder");
        BeefyNotificationController actualResult = ((CompletableFuture<BeefyNotificationController>) result).join();

        verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
        verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
        verify(stream).pushHandler(any(ByteArrayEncoder.class));
        verify(stream).pushHandler(any(BeefyNotificationProtocol.NotificationHandler.class));

        assertEquals(stream, BaseUtils.getProtectedStreamField(actualResult));
    }

    @Test
    void onMessage() throws NoSuchFieldException, IllegalAccessException {

        byte[] message = new byte[] { 1, 2, 3 };
        ByteBuf byteBuf = Unpooled.copiedBuffer(message);

        BaseUtils.setProtectedEngineField(notificationHandler, beefyNotificationEngine);
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onMessage(stream, byteBuf);

        verify(beefyNotificationEngine).receiveRequest(message, stream);
    }

    @Test
    void onClosed() throws NoSuchFieldException, IllegalAccessException {

        BaseUtils.setProtectedEngineField(notificationHandler, beefyNotificationEngine);
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onClosed(stream);

        verify(connectionManager).closeBeefyStream(stream);
    }

    @Test
    void onException() throws NoSuchFieldException, IllegalAccessException {

        BaseUtils.setProtectedEngineField(notificationHandler, beefyNotificationEngine);
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onException(mock(Throwable.class));

        verify(connectionManager).closeBeefyStream(stream);
    }
}
