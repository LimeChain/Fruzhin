package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.warp.dto.WarpSyncRequest;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import com.limechain.network.protocol.warp.scale.WarpSyncRequestWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.ConnectionClosedException;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class WarpSyncProtocol extends ProtocolHandler<WarpSyncController> {
    // Sizes taken from smoldot
    public static final int MAX_REQUEST_SIZE = 16 * 1024 * 1024;
    public static final int MAX_RESPONSE_SIZE = 16 * 1024 * 1024;

    public WarpSyncProtocol() {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    @Override
    protected CompletableFuture<WarpSyncController> onStartInitiator(Stream stream) {
        WarpSyncProtocol.Sender handler = new WarpSyncProtocol.Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class Sender implements ProtocolMessageHandler<WarpSyncResponse>, WarpSyncController {
        private final CompletableFuture<WarpSyncResponse> resp = new CompletableFuture<>();
        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, WarpSyncResponse msg) {
            System.out.println("Encoded warp sync response: " + msg);
//            System.out.println("Encoded warp sync response: " + Hex.encodeHexString(msg));

//            ScaleCodecReader reader = new ScaleCodecReader(msg);
//            WarpSyncResponse warpSyncResponse = reader.read(new WarpSyncRequestReader());
//
            resp.complete(msg);
            stream.closeWrite();
        }

        @Override
        public CompletableFuture<WarpSyncResponse> send(WarpSyncRequest req) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
                writer.write(new WarpSyncRequestWriter(), req);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Encoded warp sync request: " + Hex.encodeHexString(buf.toByteArray()));

//            stream.writeAndFlush(buf.toByteArray());
            stream.writeAndFlush(Hex.encodeHexString(buf.toByteArray()));

            return resp;
        }

        @Override
        public void onClosed(Stream stream) {
            resp.completeExceptionally(new ConnectionClosedException());
        }

        @Override
        public void onException(Throwable cause) {
            resp.completeExceptionally(cause);
        }

    }
}
