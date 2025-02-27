package com.limechain.network.protocol.base;

import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Base protocol class to reduce code duplication in protocol implementations
 * @param <T> The controller type
 * @param <E> The notification handler type that must extend T and implement ProtocolMessageHandler
 */
public abstract class BaseProtocol<T, E extends ProtocolMessageHandler<ByteBuf>> extends ProtocolHandler<T> {

    protected BaseProtocol(long initiatorTrafficLimit, long responderTrafficLimit) {
        super(initiatorTrafficLimit, responderTrafficLimit);
    }

    protected abstract E createNotificationHandler(Stream stream);

    @NotNull
    @Override
    public CompletableFuture<T> onStartInitiator(Stream stream) {
        return onStartStream(stream);
    }

    @NotNull
    @Override
    public CompletableFuture<T> onStartResponder(Stream stream) {
        return onStartStream(stream);
    }

    protected CompletableFuture<T> onStartStream(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());
        stream.pushHandler(new ByteArrayEncoder());

        E handler = createNotificationHandler(stream);
        stream.pushHandler(handler);

        return CompletableFuture.completedFuture((T) handler);
    }
}
