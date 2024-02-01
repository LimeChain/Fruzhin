package com.limechain.chain;

import com.limechain.chain.spec.ChainSpec;
import com.limechain.chain.spec.RawChainSpec;
import com.limechain.runtime.StateVersion;
import com.limechain.trie.TrieStructureFactory;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateRootHashesIntegrationTest {

    // TODO: Refactor these hardcoded paths after we refactor to extract a logical ChainSpec module
    private static final Map<String, String> chainSpecToRootHash = Map.of(
        "genesis/polkadot.json", "29d0d972cd27cbc511e9589fcb7a4506d5eb6a9e8df205f00472e5ab354a4e17",
        "genesis/ksmcc3.json", "b0006203c3a6e6bd2c6a17b1d4ae8ca49a31da0f4579da950b127774b44aef6b",
        "genesis/westend2.json", "7e92439a94f79671f9cade9dff96a094519b9001a7432244d46ab644bb6f746f"
    );

    @Test
    void testProperCalculationOfStateRootHashForMainChains() throws IOException {
        for (var entry : chainSpecToRootHash.entrySet()) {
            String chainSpecPath = entry.getKey();
            String expectedRootHash = entry.getValue();

            RawChainSpec rawChainSpec = RawChainSpec.newFromJSON(chainSpecPath);
            ChainSpec chainSpec = ChainSpec.fromRaw(rawChainSpec);
            var top = chainSpec.getGenesis().getTop();

            var trie = TrieStructureFactory.buildFromKVPs(top, StateVersion.V0);
            var root = trie.getRootNode().get();
            var rootHash = HexUtils.toHexString(Objects.requireNonNull(root.getUserData()).getMerkleValue());

            assertEquals(expectedRootHash, rootHash, String.format("Root hashes don't match for chain spec at: %s.", chainSpecPath));
        }
    }
}
