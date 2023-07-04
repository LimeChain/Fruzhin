package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.SyncedState;
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

    private final SyncedState syncedState = SyncedState.getINSTANCE();

    @Override
    public void next(WarpSyncMachine sync) {
        // After runtime is downloaded, we have to build the chain info
        sync.setWarpSyncState(new ChainInformationDownloadState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: Implement runtime download which is remoteReadRequest with keys :code and :heappages
        log.log(Level.INFO, "Downloading runtime...");
        LightClientMessage.Response response = sync.getNetworkService().makeRemoteReadRequest(
                syncedState.getLastFinalizedBlockHash().toString(),
                new String[]{StringUtils.toHex(":code"), StringUtils.toHex(":heappages")});

        byte[] proof = response.getRemoteReadResponse().getProof().toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(proof);
        int size = reader.readCompactInt();
        byte[][] decodedProofs = new byte[size][];

        for (int i = 0; i < size; ++i) {
            decodedProofs[i] = reader.readByteArray();
        }

        Trie trie;
        try {
            trie = TrieVerifier.buildTrie(decodedProofs, syncedState.getStateRoot().getBytes());
        } catch (TrieDecoderException e) {
            throw new RuntimeException("Couldn't build trie from proofs list");
        }
        var code = trie.get(
                LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code"))));
        if (code == null) {
            throw new RuntimeException("Couldn't retrieve runtime code from trie");
        }
        var heapPages = trie.get(
                LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":heappages"))));
        if (heapPages == null) {
            throw new RuntimeException("Couldn't retrieve runtime heap pages from trie");
        }
        syncedState.setRuntime(code);
        syncedState.setHeapPages(heapPages);
        log.log(Level.INFO, "Runtime & heap pages downloaded");
    }
}
