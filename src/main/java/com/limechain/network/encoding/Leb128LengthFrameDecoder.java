package com.limechain.network.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.security.InvalidParameterException;
import java.util.List;

/**
 * Decodes a LEB128-encoded integer into a {@link ByteBuf}. Used in decoding SCALE encoded data.
 * <p>
 * As per Polkadot Spec - requests, responses, handshakes and messages are prefixed with their LEB128 encoded length
 */
public class Leb128LengthFrameDecoder extends ByteToMessageDecoder {

    /**
     * Reads an unsigned integer from {@code in}.
     * <p>
     * Note: Reading u32 into an int is safe here because we have a
     * limit on the size of the message applied into every protocol's controller.
     */
    private static int readUnsignedLeb128(ByteBuf in) {
        int result = 0;
        int cur;
        int count = 0;
        do {
            cur = in.readByte() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);
        if ((cur & 0x80) == 0x80) {
            throw new InvalidParameterException("invalid LEB128 sequence");
        }
        return result;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        int preIndex = in.readerIndex();
        int length = readUnsignedLeb128(in);
        if (preIndex != in.readerIndex()) {
            if (length < 0) {
                throw new CorruptedFrameException("negative length: " + length);
            } else {
                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                } else {
                    out.add(in.readRetainedSlice(length));
                }
            }
        }
    }
}
