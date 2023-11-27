package com.limechain.network.protocol.warp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Block {
    private BlockHeader header;
    private BlockBody body;
}
