package com.limechain.network.protocol.blockannounce;

import lombok.extern.java.Log;

@Log
public class BlockAnnounceProtocol /*extends ProtocolHandler<BlockAnnounceController>*/ {
    public static final int MAX_HANDSHAKE_SIZE = 1024 * 1024;
    public static final int MAX_NOTIFICATION_SIZE = 1024 * 1024;

    public BlockAnnounceProtocol() {
//        super(MAX_HANDSHAKE_SIZE, MAX_NOTIFICATION_SIZE);
    }

//    @Override
//    protected CompletableFuture<BlockAnnounceController> onStartInitiator(Stream stream) {
//        stream.pushHandler(new Leb128LengthFrameDecoder());
//        stream.pushHandler(new Leb128LengthFrameEncoder());

//        stream.pushHandler(new ByteArrayEncoder());
//        NotificationHandler handler = new NotificationHandler(stream);
//        stream.pushHandler(handler);
//        return CompletableFuture.completedFuture(handler);
//    }

//    @Override
//    protected CompletableFuture<BlockAnnounceController> onStartResponder(Stream stream) {
//        stream.pushHandler(new Leb128LengthFrameDecoder());
//        stream.pushHandler(new Leb128LengthFrameEncoder());

//        stream.pushHandler(new ByteArrayEncoder());
//        NotificationHandler handler = new NotificationHandler(stream);
//        stream.pushHandler(handler);
//        return CompletableFuture.completedFuture(handler);
//    }

//    static class NotificationHandler extends BlockAnnounceController implements ProtocolMessageHandler<ByteBuf> {
//        ConnectionManager connectionManager = ConnectionManager.getInstance();
//        public NotificationHandler(Stream stream) {
//            super(stream);
//        }
//
//        @Override
//        public void onMessage(Stream stream, ByteBuf msg) {
//            byte[] messageBytes = new byte[msg.readableBytes()];
//            msg.readBytes(messageBytes);
//            engine.receiveRequest(messageBytes, stream);
//        }
//
//        @Override
//        public void onClosed(Stream stream) {
////            connectionManager.closeBlockAnnounceStream(stream);
//            log.log(Level.INFO, "Block announce stream closed for peer " + stream.remotePeerId());
//            ProtocolMessageHandler.super.onClosed(stream);
//        }
//
//        @Override
//        public void onException(Throwable cause) {
////            connectionManager.closeBlockAnnounceStream(stream);
//            if (cause != null) {
//                log.log(Level.WARNING, "Block Announce Exception: " + cause.getMessage());
//                cause.printStackTrace();
//            } else {
//                log.log(Level.WARNING, "Block Announce Exception with unknown cause");
//            }
//            ProtocolMessageHandler.super.onException(cause);
//        }
//    }
}
