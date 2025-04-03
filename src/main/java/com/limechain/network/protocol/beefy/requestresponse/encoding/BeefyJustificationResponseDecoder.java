package com.limechain.network.protocol.beefy.requestresponse.encoding;

import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitmentScaleReader;
import com.limechain.utils.scale.ScaleUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class BeefyJustificationResponseDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) {
        byte[] messageBytes = new byte[in.readableBytes()];
        in.readBytes(messageBytes);
        SignedCommitment response = ScaleUtils.Decode.decode(messageBytes,
                SignedCommitmentScaleReader.getInstance()::readNonGossiped);
        out.add(response);
    }
}
