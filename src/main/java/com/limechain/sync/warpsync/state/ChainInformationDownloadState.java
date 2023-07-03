package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.Trie;
import com.limechain.trie.TrieVerifier;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 *  Downloads missing chain information from source
 */
@Log
public class ChainInformationDownloadState implements WarpSyncState {
    private String[] runtimeFunctionCalls = new String[]{
            "AuraApi_slot_duration",
            "AuraApi_authorities",
            "BabeApi_current_epoch",
            "BabeApi_next_epoch",
            "BabeApi_configuration",
            "GrandpaApi_grandpa_authorities",
            "GrandpaApi_current_set_id"
    };

    private String[][] functionInfoRetrievalKeys = new String[][]{
            {},
            {},
            {},
            {},
            {},//{" slotduration", "epochlength", "constant", "genesisauthorities", "randomness", "secondaryslot"},
            {":grandpa_authorities"},
            {}
    };

    @Override
    public void next(WarpSyncMachine sync) {
        // We're done with the warp sync process!
        sync.setState(new FinishedState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        // This information is retrieved using remoteCallRequests

        log.log(Level.INFO, "Downloading chain information...");
        LightClientMessage.Response[] responses = new LightClientMessage.Response[runtimeFunctionCalls.length];
        Trie[] tries = new Trie[runtimeFunctionCalls.length];
        byte[][][] data = new byte[runtimeFunctionCalls.length][][];

        //Make a call for every runtime function we need
        for (int i = 0; i < runtimeFunctionCalls.length; i++) {
            responses[i] = sync.getNetworkService()
                    .makeRemoteCallRequest(
                            sync.getLastFinalizedBlockHash().toString(),
                            runtimeFunctionCalls[i],
                            "");

            byte[] proof = responses[i].getRemoteCallResponse().getProof().toByteArray();
            if (proof != null) {
                try {
                    byte[][] decodedProofs = decodeProof(proof);
                    tries[i] = TrieVerifier.buildTrie(decodedProofs, sync.getStateRoot().getBytes());
                    log.log(Level.INFO, "Trie built successfully for " + runtimeFunctionCalls[i]);
                    data[i] = new byte[functionInfoRetrievalKeys[i].length][];

                    //Get storage from every key we know in the functionInfoRetrievalKeys array
                    for (int j = 0; j < functionInfoRetrievalKeys[i].length; j++) {
                        byte[] key = StringUtils.hexToBytes(StringUtils.toHex(functionInfoRetrievalKeys[i][j]));
                        data[i][j] = tries[i].get(key);
                    }
                } catch (RuntimeException e) {
                    log.log(Level.INFO, "No trie for " + runtimeFunctionCalls[i] + ". " + e.getMessage());
                }
            }
        }
        log.log(Level.INFO, "Downloaded calls");
    }

    private byte[][] decodeProof(byte[] proof) {
        ScaleCodecReader reader = new ScaleCodecReader(proof);
        long size = reader.readCompactInt();
        byte[][] decodedProofs = new byte[(int) size][];

        for (int i = 0; i < size; ++i) {
            decodedProofs[i] = reader.readByteArray();
        }
        return decodedProofs;
    }
}
