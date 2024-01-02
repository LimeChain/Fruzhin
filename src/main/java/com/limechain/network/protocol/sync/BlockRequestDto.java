package com.limechain.network.protocol.sync;

import com.limechain.network.protocol.sync.pb.SyncMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlockRequestDto {
    private Integer fields;
    private String hash;
    private Integer number;
    private SyncMessage.Direction direction;
    private int maxBlocks;
}
