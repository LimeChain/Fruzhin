package com.limechain.state;

import com.limechain.consensus.babe.EpochState;
import com.limechain.consensus.beefy.BeefyState;
import com.limechain.consensus.grandpa.GrandpaSetState;
import com.limechain.storage.block.state.BlockState;
import com.limechain.sync.state.SyncState;
import com.limechain.transaction.TransactionState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
@AllArgsConstructor
public class StateManager {

    private final SyncState syncState;
    private final GrandpaSetState grandpaSetState;
    private final EpochState epochState;
    private final TransactionState transactionState;
    private final BlockState blockState;
    private final BeefyState beefyState;
}
