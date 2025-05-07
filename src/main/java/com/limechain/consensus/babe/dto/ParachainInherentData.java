package com.limechain.consensus.babe.dto;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ParachainInherentData {

    private BlockHeader parentHeader;
    // currently fields below aren't used, so they are omitted
//    private List<> bitfields;
//    private List<> backedCandidates;
//    private List<> disputes;
}