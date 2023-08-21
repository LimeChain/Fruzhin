package com.limechain.sync.warpsync;

import com.limechain.chain.ChainService;
import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.state.FinishedState;
import com.limechain.sync.warpsync.state.RequestFragmentsState;
import com.limechain.sync.warpsync.state.WarpSyncState;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

@Log
public class WarpSyncMachine {
    private final ChainService chainService;
    private final KVRepository<String, Object> repository;
    @Getter
    private final Network networkService;
    @Getter
    private final SyncedState syncedState = SyncedState.getInstance();
    @Setter
    private WarpSyncState warpSyncState;
    @Getter
    @Setter
    private Queue<WarpSyncFragment> fragmentsQueue;
    @Getter
    @Setter
    private PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges =
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0));

    @Getter
    private ChainInformation chainInformation = new ChainInformation();

    public WarpSyncMachine(Network network, ChainService chainService, KVRepository<String, Object> repository) {
        this.networkService = network;
        this.chainService = chainService;
        this.repository = repository;
    }

    public void nextState() {
        warpSyncState.next(this);
    }

    public void handleState() {
        warpSyncState.handle(this);
    }

    public boolean isSyncing() {
        return this.warpSyncState.getClass() != FinishedState.class;
    }

    public void start() {
        Hash256 initStateHash;
        if (this.chainService.getGenesis().getLightSyncState() != null) {
            LightSyncState initState = LightSyncState.decode(this.chainService.getGenesis().getLightSyncState());
            initStateHash = initState.getFinalizedBlockHeader().getParentHash();
            this.syncedState.setAuthoritySet(initState.getGrandpaAuthoritySet().getCurrentAuthorities());
            this.syncedState.setSetId(initState.getGrandpaAuthoritySet().getSetId());
            this.syncedState.setRepository(repository);
            this.syncedState.loadState();
        } else {
            initStateHash = GenesisBlockHash.LOCAL;
        }

        // Always start with requesting fragments
        this.warpSyncState = new RequestFragmentsState(initStateHash);

        // Process should be non-blocking...
        while (this.warpSyncState.getClass() != FinishedState.class) {
            this.handleState();
            this.nextState();
        }

        startFullSync();
    }

    private void startFullSync() {
        this.syncedState.setWarpSyncFinished(true);
        networkService.sendNeighbourMessages();
    }
}
