package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class WarpSyncFragmentReader implements ScaleReader<WarpSyncFragment> {
    @Override
    public WarpSyncFragment read(ScaleCodecReader scaleCodecReader) {
        WarpSyncFragment fragment = new WarpSyncFragment();
        return fragment;
    }
}
