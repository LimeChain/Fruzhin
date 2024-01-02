package com.limechain.network.protocol.warp.dto;

import com.limechain.sync.JustificationVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpSyncJustificationTest {

    @Test
    void verifySignature() {
        byte[] dataToVerify = new byte[]{1, -13, 36, -82, -83, -110, 86, 43, -28, -50, 90, -22, -96, 49, -67, 120,
                7, 18, 63, 20, 82, 56, 100, -100, -30, 64, 67, -126, 60, 78, 32, -74, 26, -86, 117, -17, 0, -11, 46,
                4, 0, 0, 0, 0, 0, 14, 25, 0, 0, 0, 0, 0, 0};
        String publicKey = "0x19b6ba718c28b1078474dc63e9d8a6f4d794da79f661da070fcca13b3bba50e7";
        String signatureHex = "0xad0a9bd0680665848cf227e9d0ff547ed075421c2a24875a99e26736f0499daf5e582dd5a2de42980663210d8d289e0cb0803b2ba5cfb917d56fdc1cdadd7209";
        boolean isValid = JustificationVerifier.verifySignature(publicKey, signatureHex, dataToVerify);
        assertTrue(isValid);
    }
}
