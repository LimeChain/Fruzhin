package com.limechain.network.protocol.warp.dto;

import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

@Setter
@Getter
public class BlockHeader {
    private Hash256 parentHash;
    private BigInteger blockNumber;
    private Hash256 stateRoot;
    private Hash256 extrinsicsRoot;
    private HeaderDigest[] digest;

    @Override
    public String toString() {
        return "BlockHeader{" +
                "parentHash=" + parentHash +
                ", blockNumber=" + blockNumber +
                ", stateRoot=" + stateRoot +
                ", extrinsicsRoot=" + extrinsicsRoot +
                ", digest=" + Arrays.toString(digest) +
                '}';
    }

    public byte[] getHash() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockHeaderScaleWriter(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return HashUtils.hashWithBlake2b(buf.toByteArray());
    }
}
