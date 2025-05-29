package com.limechain.network.protocol.beefy.requestresponse;

import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.requestresponse.encoding.BeefyJustificationResponseDecoder;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class BeefyJustificationProtocol extends ProtocolHandler<BeefyJustificationController> {
    public static final int MAX_REQUEST_SIZE = 32;
    public static final int MAX_RESPONSE_SIZE = 1024 * 1024;

    public BeefyJustificationProtocol() {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    @Override
    protected CompletableFuture<BeefyJustificationController> onStartInitiator(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new BeefyJustificationResponseDecoder());

        stream.pushHandler(new Leb128LengthFrameEncoder());
        stream.pushHandler(new ByteArrayEncoder());
        BeefyJustificationProtocol.Sender handler = new BeefyJustificationProtocol.Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class Sender implements ProtocolMessageHandler<SignedCommitment>, BeefyJustificationController {
        public static final int MAX_QUEUE_SIZE = 1;
        private static final LinkedBlockingDeque<CompletableFuture<SignedCommitment>> queue =
                new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, SignedCommitment msg) {
            Objects.requireNonNull(queue.poll()).complete(msg);
            stream.closeWrite();
        }

        @Override
        public CompletableFuture<SignedCommitment> send(BigInteger req) {
            byte[] encodedReq = ScaleUtils.Encode.encode(ScaleCodecWriter.UINT32, req.intValueExact());
            CompletableFuture<SignedCommitment> res = new CompletableFuture<>();

            if (!queue.offer(res)) {
                throw new IllegalStateException("Queue is full. Skipping...");
            }

            stream.writeAndFlush(encodedReq);
            return res;
        }

        @Override
        public void onException(Throwable cause) {
            Objects.requireNonNull(queue.poll()).completeExceptionally(cause);
        }

    }
}
