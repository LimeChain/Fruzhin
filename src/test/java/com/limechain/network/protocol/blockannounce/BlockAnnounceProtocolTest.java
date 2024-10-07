package com.limechain.network.protocol.blockannounce;

import com.limechain.network.ConnectionManager;
import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import com.limechain.rpc.server.AppBean;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockAnnounceProtocolTest {
    @InjectMocks
    private BlockAnnounceProtocol blockAnnounceProtocol;
    @InjectMocks
    private BlockAnnounceProtocol.NotificationHandler notificationHandler;
    @Mock
    private BlockAnnounceEngine blockAnnounceEngine;
    @Mock
    private Stream stream;
    @Mock
    private ConnectionManager connectionManager;

    @Test
    void onStartInitiator() {
        try (MockedStatic<AppBean> appBean = Mockito.mockStatic(AppBean.class)) {
            appBean.when(() -> AppBean.getBean(BlockAnnounceEngine.class)).thenReturn(blockAnnounceEngine);
            BlockAnnounceController result = blockAnnounceProtocol.onStartInitiator(stream).join();

            verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
            verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
            verify(stream).pushHandler(any(ByteArrayEncoder.class));
            verify(stream).pushHandler(any(BlockAnnounceProtocol.NotificationHandler.class));

            assertEquals(stream, result.stream);
        }
    }

    @Test
    void onStartResponder() {
        try (MockedStatic<AppBean> appBean = Mockito.mockStatic(AppBean.class)) {
            appBean.when(() -> AppBean.getBean(BlockAnnounceEngine.class)).thenReturn(blockAnnounceEngine);
            BlockAnnounceController result = blockAnnounceProtocol.onStartResponder(stream).join();

            verify(stream).pushHandler(any(Leb128LengthFrameEncoder.class));
            verify(stream).pushHandler(any(Leb128LengthFrameDecoder.class));
            verify(stream).pushHandler(any(ByteArrayEncoder.class));
            verify(stream).pushHandler(any(BlockAnnounceProtocol.NotificationHandler.class));

            assertEquals(stream, result.stream);
        }
    }

    @Test
    void onMessage() {
        byte[] message = new byte[]{1, 2, 3};
        ByteBuf byteBuf = Unpooled.copiedBuffer(message);
        notificationHandler.engine = blockAnnounceEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onMessage(stream, byteBuf);

        verify(blockAnnounceEngine).receiveRequest(message, stream);
    }

    @Test
    void onClosed() {
        notificationHandler.engine = blockAnnounceEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onClosed(stream);

        verify(connectionManager).closeBlockAnnounceStream(stream);
    }

    @Test
    void onException() {
        notificationHandler.engine = blockAnnounceEngine;
        notificationHandler.connectionManager = connectionManager;

        notificationHandler.onException(mock(Throwable.class));

        verify(connectionManager).closeBlockAnnounceStream(stream);
    }
}