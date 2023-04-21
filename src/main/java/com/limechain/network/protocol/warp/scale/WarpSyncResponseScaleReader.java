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
        while (true) {
            // Read while an exception is thrown. This means that all
            // fragments have been read(assuming the payload is valid).
            try {
                WarpSyncFragment fragment = new WarpSyncFragmentReader().read(reader);
                fragments.add(fragment);
            } catch (Exception e) {
                break;
            }
        }
        response.setFragments(fragments.toArray(WarpSyncFragment[]::new));
        response.setFinished(reader.readBoolean());
        return response;
    }
}
