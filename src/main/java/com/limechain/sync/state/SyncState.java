package com.limechain.sync.state;

import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.prometheus.PrometheusServer;
import com.limechain.state.AbstractState;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import io.emeraldpay.polkaj.types.Hash256;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Log
@Getter
@Component
@RequiredArgsConstructor
public class SyncState extends AbstractState {

    private final GenesisBlockHash genesisBlockHashCalculator;
    private final KVRepository<String, Object> repository;
    private final PeerMessageCoordinator peerMessageCoordinator;
    private final PrometheusServer prometheusServer;

    private Hash256 lastFinalizedBlockHash;
    private Hash256 stateRoot;
    private BigInteger lastFinalizedBlockNumber;
    private BigInteger startingBlock;

    private Hash256 genesisBlockHash;

    @Override
    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("SyncState already initialized");
        }
        initialized = true;

        setLastFinalizedBlockNumber(BigInteger.ZERO);
        genesisBlockHash = genesisBlockHashCalculator.getGenesisHash();
        lastFinalizedBlockHash = new Hash256(genesisBlockHash.getBytes());
        startingBlock = this.lastFinalizedBlockNumber;
        stateRoot = genesisBlockHashCalculator.getGenesisBlockHeader().getStateRoot();
    }

    @Override
    public void initializeFromDatabase() {
        if (initialized) {
            throw new IllegalStateException("SyncState already initialized");
        }
        initialized = true;

        loadFromDatabase();
    }

    private void loadFromDatabase() {
        setLastFinalizedBlockNumber(
                repository.find(DBConstants.LAST_FINALIZED_BLOCK_NUMBER, BigInteger.ZERO)
        );

        this.genesisBlockHash = genesisBlockHashCalculator.getGenesisHash();
        this.lastFinalizedBlockHash = repository.find(DBConstants.LAST_FINALIZED_BLOCK_HASH,
                genesisBlockHashCalculator.getGenesisHash());
        Hash256 stateRootBytes = repository.find(DBConstants.STATE_ROOT, null);
        this.stateRoot = stateRootBytes != null ? stateRootBytes : genesisBlockHashCalculator
                .getGenesisBlockHeader().getStateRoot();
    }

    @Override
    @PreDestroy
    public void persistState() {
        repository.save(DBConstants.LAST_FINALIZED_BLOCK_NUMBER, lastFinalizedBlockNumber);
        repository.save(DBConstants.LAST_FINALIZED_BLOCK_HASH, lastFinalizedBlockHash);
        repository.save(DBConstants.STATE_ROOT, stateRoot);
    }

    public void finalizeBlock(BlockHeader header) {
        setLastFinalizedBlockNumber(header.getBlockNumber());
        this.lastFinalizedBlockHash = header.getHash();
        this.stateRoot = header.getStateRoot();
        persistState();
    }

    public void setLightSyncState(LightSyncState initState) {
        finalizeBlock(initState.getFinalizedBlockHeader());
    }

    // setter method for updating prometheus metrics
    private void setLastFinalizedBlockNumber(BigInteger finalizedBlockNumber) {
        this.lastFinalizedBlockNumber = finalizedBlockNumber;
        prometheusServer.emitFinalizedBlock(lastFinalizedBlockNumber);
    }
}
