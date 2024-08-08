package com.limechain.network.kad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.teavm.jso.JSObject;

@AllArgsConstructor
@Getter
public class PeerId implements JSObject {
    private byte[] privateKey;
    private byte[] publicKey;
    private String peerIdStr;
}
