package com.limechain.network.protocol;

import io.libp2p.core.multistream.ProtocolBinding;

public interface NetworkService {
    ProtocolBinding getProtocol();
    
}
