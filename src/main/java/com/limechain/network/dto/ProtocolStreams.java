package com.limechain.network.dto;

import io.libp2p.core.Stream;
import lombok.Data;

@Data
public class ProtocolStreams {
    private Stream initiator;
    private Stream responder;
}
