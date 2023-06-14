package com.limechain.sync.warpsync;

import com.limechain.chain.ChainService;
import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
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
    @Setter
    private WarpSyncState state;

    @Getter
    @Setter
    private Queue<WarpSyncFragment> fragmentsQueue;

    @Getter
    @Setter
    private PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges =
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0));

    @Getter
    @Setter
    private boolean isFinished;

    @Getter
    @Setter
    private Hash256 lastFinalizedBlockHash;

    @Getter
    @Setter
    private BigInteger lastFinalizedBlockNumber;

    @Getter
    @Setter
    private Authority[] authoritySet;

    @Getter
    @Setter
    private BigInteger setId;

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
        state.next(this);
    }

    public void handleState() {
        state.handle(this);
    }

    public boolean isSyncing() {
        return this.state.getClass() != FinishedState.class;
    }

    public void start() {
        LightSyncState initState = LightSyncState.decode(this.chainService.getGenesis().getLightSyncState());
        // Always start with requesting fragments
        this.state = new RequestFragmentsState(initState.getFinalizedBlockHeader().getParentHash());
        this.setAuthoritySet(initState.getGrandpaAuthoritySet().getCurrentAuthorities());
        this.setSetId(initState.getGrandpaAuthoritySet().getSetId());

        // Process should be non-blocking...
        while (this.state.getClass() != FinishedState.class) {
            this.handleState();
            this.nextState();
        }
    }
}
