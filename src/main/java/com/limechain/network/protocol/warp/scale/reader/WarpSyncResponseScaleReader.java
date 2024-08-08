package com.limechain.network.protocol.warp.scale.reader;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

import java.util.ArrayList;
import java.util.List;

public class WarpSyncResponseScaleReader implements ScaleReader<WarpSyncResponse> {

    @Override
    public WarpSyncResponse read(ScaleCodecReader reader) {
        WarpSyncResponse response = new WarpSyncResponse();
        List<WarpSyncFragment> fragments = new ArrayList<>();
        WarpSyncFragmentReader fragmentReader = new WarpSyncFragmentReader();
        int fragmentCount = reader.readCompactInt();
        for (int i = 0; i < fragmentCount; i++) {
            fragments.add(fragmentReader.read(reader));
        }
        response.setFragments(fragments.toArray(WarpSyncFragment[]::new));
        response.setFinished(reader.readBoolean());
        return response;
    }
}
