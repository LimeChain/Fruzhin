package com.limechain.chain.lightsyncstate;

import com.limechain.chain.lightsyncstate.scale.EpochChangesReader;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.scale.GrandpaAuthoritySetReader;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import lombok.Getter;

import java.util.Map;

@Getter
public class LightSyncState {

    private BlockHeader finalizedBlockHeader;
    private EpochChanges epochChanges;
    private GrandpaAuthoritySet grandpaAuthoritySet;

    public static LightSyncState decode(Map<String, String> lightSyncState) {
        String header = lightSyncState.get("finalizedBlockHeader");
        String epochChanges = lightSyncState.get("babeEpochChanges");
        String grandpaAuthoritySet = lightSyncState.get("grandpaAuthoritySet");

        if (header == null) {
            throw new IllegalStateException("finalizedBlockHeader is null");
        }
        if (epochChanges == null) {
            throw new IllegalStateException("epochChanges is null");
        }
        if (grandpaAuthoritySet == null) {
            throw new IllegalStateException("grandpaAuthoritySet is null");
        }

        var state = new LightSyncState();

        state.finalizedBlockHeader = ScaleUtils.Decode.decode(
                StringUtils.hexToBytes(header),
                BlockHeaderReader.getInstance()
        );

        state.epochChanges = ScaleUtils.Decode.decode(
                StringUtils.hexToBytes(epochChanges),
                EpochChangesReader.getInstance()
        );

        state.grandpaAuthoritySet = ScaleUtils.Decode.decode(
                StringUtils.hexToBytes(grandpaAuthoritySet),
                GrandpaAuthoritySetReader.getInstance()
        );

        return state;
    }
}
