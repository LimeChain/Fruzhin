package com.limechain.constants;

import com.limechain.chain.ChainService;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.runtime.StateVersion;
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

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

        byte[] stateRootHash = getMerkleValue(this.genesisTrie);
        this.genesisBlockHeader = buildGenesisBlock(stateRootHash);
        this.genesisHash = this.genesisBlockHeader.getHash();
    }

    public BlockHeader buildGenesisBlock(byte[] merkleValue) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(Hash256.empty());
        blockHeader.setBlockNumber(BigInteger.ZERO);
        blockHeader.setStateRoot(new Hash256(merkleValue));
        blockHeader.setExtrinsicsRoot(new Hash256(HashUtils.hashWithBlake2b(new byte[1])));
        blockHeader.setDigest(new HeaderDigest[0]);

        return blockHeader;
    }

    byte[] getMerkleValue(TrieStructure<NodeData> trieStructure) {
        return trieStructure.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow(() -> new RuntimeException("Root node merkle value not found"));
    }

    private TrieStructure<NodeData> buildGenesisTrie() {
        var genesisStorage = chainService.getChainSpec().getGenesis().getTop();
        return TrieStructureFactory.buildFromKVPs(genesisStorage, StateVersion.V0);
    }
}
