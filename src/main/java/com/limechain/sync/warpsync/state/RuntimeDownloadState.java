package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.Trie;
import com.limechain.trie.TrieVerifier;
import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class RuntimeDownloadState implements WarpSyncState {
    private Exception error;
    private static byte[] codeKey =
            LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));
    private static byte[] heapPagesKey =
            LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":heappages")));

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            sync.setState(new RequestFragmentsState(sync.getLastFinalizedBlockHash()));
            return;
        }
        // After runtime is downloaded, we have to build the runtime and then build chain information
        sync.setState(new RuntimeBuildState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        log.log(Level.INFO, "Downloading runtime...");
        LightClientMessage.Response response = sync.getNetworkService().makeRemoteReadRequest(
                sync.getLastFinalizedBlockHash().toString(),
                new String[]{StringUtils.toHex(":code"), StringUtils.toHex(":heappages")});

        byte[] proof = response.getRemoteReadResponse().getProof().toByteArray();

        byte[][] decodedProofs = decodeProof(proof);

        setCodeAndHeapPages(sync, decodedProofs);
    }

    private byte[][] decodeProof(byte[] proof) {
        ScaleCodecReader reader = new ScaleCodecReader(proof);
        int size = reader.readCompactInt();
        byte[][] decodedProofs = new byte[size][];

        for (int i = 0; i < size; ++i) {
            decodedProofs[i] = reader.readByteArray();
        }
        return decodedProofs;
    }

    private void setCodeAndHeapPages(WarpSyncMachine sync, byte[][] decodedProofs) {
        Trie trie;
        try {
            trie = TrieVerifier.buildTrie(decodedProofs, sync.getStateRoot().getBytes());
            var code = trie.get(codeKey);
            if (code == null) {
                this.error = new RuntimeException("Couldn't retrieve runtime code from trie");
            }
            var heapPages = trie.get(heapPagesKey);
            //TODO Set error if heapPages is null
            //Currently other nodes are not returning :heappage information, only :code
            if (code == null) return;
            sync.setRuntime(code);
            sync.setHeapPages(heapPages);
            log.log(Level.INFO, "Runtime and heap pages downloaded");

        } catch (TrieDecoderException e) {
            this.error = new RuntimeException("Couldn't build trie from proofs list: " + e.getMessage());
        }
    }
}
