package com.limechain.network.protocol;

import com.limechain.network.StrictProtocolBinding;
import lombok.Getter;

public class NetworkService<P extends StrictProtocolBinding> {
    @Getter
    protected P protocol;
}
