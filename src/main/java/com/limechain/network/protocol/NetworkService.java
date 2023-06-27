package com.limechain.network.protocol;

import io.libp2p.core.multistream.ProtocolBinding;
import lombok.Getter;

public class NetworkService<P extends ProtocolBinding> {
    @Getter
    protected P protocol;
}
