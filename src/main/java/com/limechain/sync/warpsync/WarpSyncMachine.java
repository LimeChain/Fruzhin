package com.limechain.sync.warpsync;

import com.limechain.chain.ChainService;
import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
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
import java.util.logging.Level;

@Log
public class WarpSyncMachine {
    private static WarpSyncMachine warpSync;
    private final ChainService chainService;
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
    @Setter
    @Getter
    private boolean isFinished;

    public WarpSyncMachine(Network network, ChainService chainService) {
        this.networkService = network;
        this.chainService = chainService;
    }

    /**
     * Initializes singleton Network instance
     * This is used two times on startup
     *
     * @return Network instance saved in class or if not found returns new Network instance
     */
    public static WarpSyncMachine initialize(Network network, ChainService chainService) {
        if (warpSync != null) {
            log.log(Level.WARNING, "Warp Sync State Machine already initialized.");
            return warpSync;
        }
        warpSync = new WarpSyncMachine(network, chainService);
        log.log(Level.INFO, "Initialized Warp Sync State Machine module!");
        return warpSync;
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
        // networkService.sendNeighbourMessages();
    }
}
