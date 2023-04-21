package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.util.ArrayList;
import java.util.List;

public class WarpSyncResponseScaleReader implements ScaleReader<WarpSyncResponse> {

    @Override
    public WarpSyncResponse read(ScaleCodecReader reader) {
        WarpSyncResponse response = new WarpSyncResponse();
        List<WarpSyncFragment> fragments = new ArrayList<>();
        var fragmentCount = reader.readCompactInt();
        for (int i = 0; i < fragmentCount; i++) {
            fragments.add(new WarpSyncFragmentReader().read(reader));
        }
        response.setFragments(fragments.toArray(WarpSyncFragment[]::new));
        response.setFinished(reader.readBoolean());
        return response;
    }
}
