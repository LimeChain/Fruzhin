package com.limechain.chain.lightsyncstate;

import com.limechain.chain.lightsyncstate.scale.AuthoritySetReader;
import com.limechain.chain.lightsyncstate.scale.EpochChangesReader;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.utils.StringUtils;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Map;

@Getter
public class LightSyncState {
    private BlockHeader finalizedBlockHeader;
    private EpochChanges epochChanges;
    private AuthoritySet grandpaAuthoritySet;

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
        byte[] bytes = StringUtils.hexToBytes(header);
        state.finalizedBlockHeader = new BlockHeaderReader()
                .read(new ScaleCodecReader(bytes));

        byte[] bytes1 = StringUtils.hexToBytes(epochChanges);
        state.epochChanges = new EpochChangesReader()
                .read(new ScaleCodecReader(bytes1));

        state.grandpaAuthoritySet = new AuthoritySetReader()
                .read(new ScaleCodecReader(StringUtils.hexToBytes(grandpaAuthoritySet)));

        return state;
    }
}
