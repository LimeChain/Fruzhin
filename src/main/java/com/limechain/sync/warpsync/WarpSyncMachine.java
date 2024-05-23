package com.limechain.sync.warpsync;

import com.limechain.chain.ChainService;
import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.storage.block.SyncState;
import com.limechain.sync.warpsync.action.FinishedAction;
import com.limechain.sync.warpsync.action.RequestFragmentsAction;
import com.limechain.sync.warpsync.action.WarpSyncAction;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@Log
public class WarpSyncMachine {
    private final ChainService chainService;
    @Getter
    private final Network networkService;
    @Getter
    private final WarpSyncState warpState = WarpSyncState.getInstance();
    private final SyncState syncState;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Setter
    private WarpSyncAction warpSyncAction;
    @Getter
    @Setter
    private Queue<WarpSyncFragment> fragmentsQueue;
    @Getter
    @Setter
    private PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges =
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0));
    @Getter
    private final ChainInformation chainInformation = new ChainInformation();

    public WarpSyncMachine(Network network, ChainService chainService, SyncState syncState) {
        this.networkService = network;
        this.chainService = chainService;
        this.syncState = syncState;
        warpState.setNetwork(network);
    }

    public void nextState() {
        warpSyncAction.next(this);
    }

    public void handleState() {
        warpSyncAction.handle(this);
    }

    public boolean isSyncing() {
        return this.warpSyncAction.getClass() != FinishedAction.class;
    }

    public void start() {
        if (this.chainService.getChainSpec().getLightSyncState() != null) {
            LightSyncState initState = LightSyncState.decode(this.chainService.getChainSpec().getLightSyncState());
            if (this.syncState.getLastFinalizedBlockNumber()
                        .compareTo(initState.getFinalizedBlockHeader().getBlockNumber()) < 0) {
                this.syncState.setLightSyncState(initState);
            }
        }
        final Hash256 initStateHash = this.syncState.getLastFinalizedBlockHash();

        // Always start with requesting fragments
        log.log(Level.INFO, "Requesting fragments...");
        this.networkService.updateCurrentSelectedPeerWithNextBootnode();
        this.warpSyncAction = new RequestFragmentsAction(initStateHash);

        executor.submit(() -> {
            while (this.warpSyncAction.getClass() != FinishedAction.class) {
                this.handleState();
                this.nextState();
            }

            startFullSync();
        });
    }

    public void stop() {
        log.info("Stopping warp sync machine");
        executor.shutdown();
        this.warpSyncAction = null;
        log.info("Warp sync machine stopped.");
    }

    private void startFullSync() {
        this.warpState.setWarpSyncFinished(true);
        this.networkService.handshakeBootNodes();
    }
}
