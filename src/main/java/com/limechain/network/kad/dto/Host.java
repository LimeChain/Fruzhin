package com.limechain.network.kad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Host {
    private PeerId peerId;
}
