package com.limechain.network.protocol.warp.encodings;

import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import com.limechain.network.protocol.warp.scale.WarpSyncResponseScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class WarpSyncResponseDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) {
        if (in.hasArray()) {
            byte[] messageBytes = in.array();
            int offset = in.arrayOffset() + in.readerIndex();
            int length = in.readableBytes();
            WarpSyncResponse response = new WarpSyncResponseScaleReader().read(new ScaleCodecReader(messageBytes));
            out.add(response);
        } else {
            byte[] messageBytes = new byte[in.readableBytes()];
            in.readBytes(messageBytes);
            System.out.println(messageBytes.length);
            WarpSyncResponse response = new WarpSyncResponseScaleReader().read(new ScaleCodecReader(messageBytes));
            out.add(response);
        }
    }
}
