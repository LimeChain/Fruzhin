package com.limechain.consensus.beefy.event;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import lombok.Getter;

import java.util.EventObject;
import java.util.List;

@Getter
public class FinalizedBlockChangeEvent extends EventObject {

    // List of block headers from the last GRANDPA finalized block (exclusive)
    // to the new GRANDPA finalized block (inclusive)
    private final List<BlockHeader> blockHeaders;
    private final BlockHeader grandpaFinalized;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public FinalizedBlockChangeEvent(Object source, List<BlockHeader> blockHeaders, BlockHeader grandpaFinalized) {
        super(source);
        this.blockHeaders = blockHeaders;
        this.grandpaFinalized = grandpaFinalized;
    }
}
