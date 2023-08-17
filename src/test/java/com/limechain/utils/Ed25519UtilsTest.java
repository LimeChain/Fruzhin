package com.limechain.utils;

import io.libp2p.core.PeerId;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ed25519UtilsTest {

    private static final String PEER_ID = "12D3KooWKsULPNyZ4zDy1dcBhEsticfvYQgQUWiFNXbcPLefJ9Zd";
    private static final byte[] PRIVATE_KEY_BYTE = new byte[] {109, -89, 75, 125, 88, 37, -93, 89, 11, -93, -76, -39,
            111, -33, -101, -51, -19, 63, 124, -90, 40, -33, 119, -108, 91, 127, 123, -54, -110, -89, -11, 10, };

    @Test
    void generatesPrivateKey() {
        Ed25519PrivateKey ed25519PrivateKey = Ed25519Utils.generatePrivateKey();
        PeerId peerId = PeerId.fromPubKey(ed25519PrivateKey.publicKey());

        assertNotNull(ed25519PrivateKey);
        assertNotNull(ed25519PrivateKey.publicKey());
        assertNotNull(peerId);
    }

    @Test
    void loadPrivateKey() {
        Ed25519PrivateKey ed25519PrivateKey = Ed25519Utils.loadPrivateKey(PRIVATE_KEY_BYTE);
        PeerId peerId = PeerId.fromPubKey(ed25519PrivateKey.publicKey());

        assertNotNull(ed25519PrivateKey);
        assertNotNull(ed25519PrivateKey.publicKey());
        assertNotNull(peerId);
        System.out.println(peerId.toBase58());
        assertEquals(PEER_ID, peerId.toBase58());
    }

    @Test
    void loadPrivateKeyFailsOddKeyLen() {
        byte[] tooShort = {109, -89, 75, 125, 88, 37, -93, 89};
        byte[] tooLong = {109, -89, 75, 125, 88, 37, -93, 89, 109, -89, 75, 125, 88, 37, -93, 89, 11, -93, -76, -39,
                111, -33, -101, -51, -19, 63, 124, -90, 40, -33, 119, -108, 91, 127, 123, -54, -110, -89, -11, 10,};

        assertThrows(IllegalArgumentException.class, () -> Ed25519Utils.loadPrivateKey(tooShort));
        assertThrows(IllegalArgumentException.class, () -> Ed25519Utils.loadPrivateKey(tooLong));
    }

}