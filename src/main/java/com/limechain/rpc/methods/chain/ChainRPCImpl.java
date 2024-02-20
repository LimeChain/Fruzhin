package com.limechain.rpc.methods.chain;

import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.block.exception.BlockNotFoundException;
import com.limechain.storage.block.exception.HeaderNotFoundException;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.buf.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
@AllArgsConstructor
public class ChainRPCImpl {

    private final static String HEX_PREFIX = "0x";
    private final BlockState blockState = BlockState.getInstance();

    public Map<String, Object> chainGetHeader(String blockHash) {
        if (!blockState.isInitialized()) {
            return null;
        }
        if (blockHash == null) {
            blockHash = blockState.getHighestFinalizedHash().toString();
        }

        final BlockHeader header;
        try {
            header = blockState.getHeader(Hash256.from(blockHash));
        } catch (HeaderNotFoundException e) {
            return null;
        }

        return headerToMap(header);
    }

    public Map<String, Object> chainGetBlock(String blockHash) {
        if (!blockState.isInitialized()) {
            return null;
        }
        if (blockHash == null) {
            blockHash = blockState.getHighestFinalizedHash().toString();
        }

        Block block;
        try {
            block = blockState.getBlockByHash(Hash256.from(blockHash));
        } catch (HeaderNotFoundException | BlockNotFoundException e) {
            return null;
        }

        return Map.of(
                "block", Map.of("header", headerToMap(block.getHeader()),
                        "extrinsics", Arrays.stream(block.getBody().getExtrinsicsAsByteArray())
                                .map(HexUtils::toHexString)
                                .toArray())
        );
    }

    public String chainGetBlockHash() {
        return null;
    }

    public String chainGetFinalizedHead() {
        return null;
    }

    public String chainSubscribeAllHeads() {
        return null;
    }

    public String chainUnsubscribeAllHeads() {
        return null;
    }

    public String chainSubscribeNewHeads() {
        return null;
    }

    public String chainUnsubscribeNewHeads() {
        return null;
    }

    public String chainSubscribeFinalizedHeads() {
        return null;
    }

    public String chainUnsubscribeFinalizedHeads() {
        return null;
    }

    @NotNull
    private Map<String, Object> headerToMap(BlockHeader header) {
        return Map.of(
                "digest", Map.of(
                        "logs", Arrays
                                .stream(header.getDigest())
                                .map(HeaderDigest::getMessage)
                                .map(HexUtils::toHexString)
                                .toArray()
                ),
                "extrinsicsRoot", header.getExtrinsicsRoot().toString(),
                "number", HEX_PREFIX + header.getBlockNumber().toString(16),
                "parentHash", header.getParentHash().toString(),
                "stateRoot", header.getStateRoot().toString()
        );
    }
}
