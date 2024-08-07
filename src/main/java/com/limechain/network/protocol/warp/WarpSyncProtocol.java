package com.limechain.network.protocol.warp;

public class WarpSyncProtocol /*extends ProtocolHandler<WarpSyncController>*/ {
    // Sizes taken from smoldot
    public static final int MAX_REQUEST_SIZE = 32;
    public static final int MAX_RESPONSE_SIZE = 16 * 1024 * 1024;

    public WarpSyncProtocol() {
//        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    /*@Override
    protected CompletableFuture<WarpSyncController> onStartInitiator(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new WarpSyncResponseDecoder());

        stream.pushHandler(new Leb128LengthFrameEncoder());
        stream.pushHandler(new ByteArrayEncoder());
        WarpSyncProtocol.Sender handler = new WarpSyncProtocol.Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }*/

    /*static class Sender implements ProtocolMessageHandler<WarpSyncResponse>, WarpSyncController {
        public static final int MAX_QUEUE_SIZE = 50;
        private final LinkedBlockingDeque<CompletableFuture<WarpSyncResponse>> queue =
                new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, WarpSyncResponse msg) {
            Objects.requireNonNull(queue.poll()).complete(msg);
            stream.closeWrite();
        }

        @Override
        public CompletableFuture<WarpSyncResponse> send(WarpSyncRequest req) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
                writer.write(new WarpSyncRequestWriter(), req);
            } catch (IOException e) {
                throw new ScaleEncodingException(e);
            }
            CompletableFuture<WarpSyncResponse> res = new CompletableFuture<>();
            queue.add(res);
            stream.writeAndFlush(buf.toByteArray());
            return res;
        }

        @Override
        public void onException(Throwable cause) {
            Objects.requireNonNull(queue.poll()).completeExceptionally(cause);
        }
    }*/
}
