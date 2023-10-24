package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.web3j.crypto.Hash.hmacSha512;

@UtilityClass
public class Ed25519Utils {

    /**
     * Generates Ed25119 key pair
     * @return Ed25519 Private key (32 bytes) and Public key (32 bytes) which is attached to the private key
     */
    public static Ed25519PrivateKey generateKeyPair() {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    /**
     * Generates Ed25119 key pair from mnemonic phrase
     * @param mnemonic BIP-39 12 or 24 word mnemonic phrase
     * @return Ed25519 Private key (32 bytes) and Public key (32 bytes) which is attached to the private key
     */
    public static Ed25519PrivateKey generateKeyPair(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        byte[] i = hmacSha512("ed25519 seed".getBytes(), seed);
        byte[] il = Arrays.copyOfRange(i, 0, i.length / 2);
        Arrays.fill(i, (byte) 0);

        final Ed25519PrivateKeyParameters privateKeyParameters = new Ed25519PrivateKeyParameters(il, 0);
        Arrays.fill(il, (byte) 0);

        return new Ed25519PrivateKey(privateKeyParameters);
    }

    /**
     * Loads Ed25519 keypair from byte array
     * @param keyData 32 bytes Ed25519 private key
     * @return Ed25519 Private key (32 bytes) and Public key (32 bytes) which is attached to the private key
     */
    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData) {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }

    /**
     * Signs message with Ed25519 private key
     * @param privateKey 32 bytes Ed25519 private key
     * @param message message to be signed
     * @return 64 bytes signature
     */
    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        PrivKey privKey = Ed25519Kt.unmarshalEd25519PrivateKey(privateKey);
        return privKey.sign(message);
    }

    /**
     * Verifies signature
     * @param signature signature to be verified
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifySignature(final VerifySignature signature) {
        if (signature.getPublicKeyData() == null) return false;
        PubKey pubKey = Ed25519Kt.unmarshalEd25519PublicKey(signature.getPublicKeyData());
        return pubKey.verify(signature.getMessageData(), signature.getSignatureData());
    }
}
