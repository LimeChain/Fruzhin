package com.limechain.network.encoding;

public class Leb128LengthFrameEncoder /*extends MessageToByteEncoder<ByteBuf>*/ {

    /**
     * Gets the number of bytes in the unsigned LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    public static int unsignedLeb128Size(int value) {
        int remaining = value >> 7;
        int count = 0;
        while (remaining != 0) {
            remaining >>= 7;
            count++;
        }
        return count + 1;
    }

/*
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int bodyLen = msg.readableBytes();
        int headerLen = unsignedLeb128Size(bodyLen);
        out.ensureWritable(headerLen + bodyLen);
        out.writeByte(bodyLen);
        out.writeBytes(msg, msg.readerIndex(), bodyLen);

    }
*/
}
