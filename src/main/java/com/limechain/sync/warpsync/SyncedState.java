package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.rpc.http.server.AppBean;
import com.limechain.sync.warpsync.runtime.Runtime;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

public class SyncedState {
    private static final SyncedState INSTANCE = new SyncedState();
    @Getter
    @Setter
    private Hash256 lastFinalizedBlockHash;
    @Getter
    @Setter
    private boolean isFinished;
    @Getter
    @Setter
    private Hash256 stateRoot;
    @Getter
    @Setter
    private BigInteger lastFinalizedBlockNumber = BigInteger.ZERO;
    @Getter
    @Setter
    private Authority[] authoritySet;
    @Getter
    @Setter
    private BigInteger setId;
    @Getter
    @Setter
    private byte[] runtimeCode;
    @Getter
    @Setter
    private byte[] heapPages;
    @Getter
    @Setter
    private Runtime runtime;
    
    public static SyncedState getInstance() {
        return INSTANCE;
    }

    public BlockAnnounceHandshake getHandshake() {
        Hash256 genesisBlockHash;
        Network network = AppBean.getBean(Network.class);
        switch (network.getChain()) {
            case POLKADOT -> genesisBlockHash = GenesisBlockHash.POLKADOT;
            case KUSAMA -> genesisBlockHash = GenesisBlockHash.KUSAMA;
            case WESTEND -> genesisBlockHash = GenesisBlockHash.WESTEND;
            case LOCAL -> genesisBlockHash = GenesisBlockHash.LOCAL;
            default -> throw new IllegalStateException("Unexpected value: " + network.chain);
        }

        Hash256 lastFinalizedBlockHash = this.getLastFinalizedBlockHash() == null
                ? genesisBlockHash
                : this.getLastFinalizedBlockHash();
        return new BlockAnnounceHandshake(
                NodeRole.LIGHT.getValue(),
                this.getLastFinalizedBlockNumber().toString(),
                lastFinalizedBlockHash,
                genesisBlockHash
        );
    }

}