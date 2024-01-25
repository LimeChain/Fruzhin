package com.limechain.constants;

import com.limechain.chain.ChainService;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.InsertTrieBuilder;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;

@Component
@Getter
public class GenesisBlockHash {
    private final Hash256 genesisHash;
    private final TrieStructure<NodeData> genesisTrie;
    private final BlockHeader genesisBlockHeader;
    private final ChainService chainService;

    public GenesisBlockHash(ChainService chainService) {
        this.chainService = chainService;
        this.genesisTrie = buildGenesisTrie();
        this.genesisBlockHeader = buildGenesisBlock(getMerkleValue(this.genesisTrie));
        this.genesisHash = this.genesisBlockHeader.getHash();
    }

    public BlockHeader buildGenesisBlock(byte[] merkleValue) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(Hash256.empty());
        blockHeader.setBlockNumber(BigInteger.ZERO);
        blockHeader.setStateRoot(new Hash256(merkleValue));
        blockHeader.setExtrinsicsRoot(Hash256.from(HexUtils.toHexString(HashUtils.hashWithBlake2b(new byte[1]))));
        blockHeader.setDigest(new HeaderDigest[0]);

        return blockHeader;
    }

    byte[] getMerkleValue(TrieStructure<NodeData> trieStructure) {
        //TODO: GENESIS STATE ROOT MERKLE VALUE MUST BE
        // '0x29d0d972cd27cbc511e9589fcb7a4506d5eb6a9e8df205f00472e5ab354a4e17' for the polkadot chain
        return trieStructure.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow(() -> new RuntimeException("Root node merkle value not found"));
    }

    private TrieStructure<NodeData> buildGenesisTrie() {
        var genesisStorageRaw = chainService.getGenesis().getGenesis().getRaw();
        Map<String, String> top = genesisStorageRaw.get("top");
        return new InsertTrieBuilder()
                .initializeTrieStructure(top)
                .getTrieStructure();
    }

}
