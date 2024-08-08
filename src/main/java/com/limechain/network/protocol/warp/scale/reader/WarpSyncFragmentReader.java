package com.limechain.network.protocol.warp.scale.reader;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

public class WarpSyncFragmentReader implements ScaleReader<WarpSyncFragment> {
    @Override
    public WarpSyncFragment read(ScaleCodecReader reader) {
        WarpSyncFragment fragment = new WarpSyncFragment();
        fragment.setHeader(new BlockHeaderReader().read(reader));
        fragment.setJustification(new JustificationReader().read(reader));
        return fragment;
    }
}
