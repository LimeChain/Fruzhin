package com.limechain.network.protocol.blockannounce.encoding;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class BlockAnnounceHandshakeDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        byte[] messageBytes = new byte[in.readableBytes()];
        in.readBytes(messageBytes);
        BlockAnnounceHandShake response = new BlockAnnounceHandshakeScaleReader().read(
                new ScaleCodecReader(messageBytes));
        out.add(response);
    }
}
