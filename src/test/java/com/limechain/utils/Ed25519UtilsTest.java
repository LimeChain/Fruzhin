package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.PeerId;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ed25519UtilsTest {

    private static final String PEER_ID = "12D3KooWKsULPNyZ4zDy1dcBhEsticfvYQgQUWiFNXbcPLefJ9Zd";
    private static final byte[] PRIVATE_KEY_BYTE = new byte[] {109, -89, 75, 125, 88, 37, -93, 89, 11, -93, -76, -39,
            111, -33, -101, -51, -19, 63, 124, -90, 40, -33, 119, -108, 91, 127, 123, -54, -110, -89, -11, 10};

    private final byte[] hashedMessage = HashUtils.hashWithBlake2b("This is test message".getBytes());

    @Test
    void generatesPrivateKey() {
        Ed25519PrivateKey ed25519PrivateKey = Ed25519Utils.generateKeyPair();
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

    @Test
    void generateSignVerify() {
        Ed25519PrivateKey privKey = Ed25519Utils.generateKeyPair();

        byte[] signed = Ed25519Utils.signMessage(privKey.raw(), hashedMessage);

        VerifySignature signature = new VerifySignature(signed, hashedMessage, privKey.publicKey().raw(), Key.ED25519);
        boolean verified = Ed25519Utils.verifySignature(signature);

        assertTrue(verified);
    }

    @Test
    void generateKeyFromSeedNoException(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = secureRandom.generateSeed(16);
        String mnemonicString = MnemonicUtils.generateMnemonic(entropy);
        assertNotNull(mnemonicString);

        Ed25519PrivateKey privKey = Ed25519Utils.generateKeyPair(mnemonicString);

        assertNotNull(privKey);
    }

}