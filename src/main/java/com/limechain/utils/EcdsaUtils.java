package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Secp256k1Kt;
import kotlin.Pair;
import org.web3j.crypto.MnemonicUtils;

import java.math.BigInteger;
import java.util.Arrays;

import static org.web3j.crypto.Hash.hmacSha512;

public class EcdsaUtils {

    public static Pair<PrivKey, PubKey> generateKeyPair() {
        Pair<PrivKey, PubKey> keyPair = Secp256k1Kt.generateSecp256k1KeyPair();
        return keyPair;
    }

    public static Pair<PrivKey, PubKey> generateKeyPair(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        byte[] i = hmacSha512("Bitcoin seed".getBytes(), seed);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        Arrays.fill(i, (byte) 0);

        PrivKey privKey = Secp256k1Kt.unmarshalSecp256k1PrivateKey(il);
        Arrays.fill(il, (byte) 0);

        return new Pair<>(privKey, privKey.publicKey());
    }

    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        PrivKey privKey = Secp256k1Kt.unmarshalSecp256k1PrivateKey(privateKey);
        return privKey.sign(message);
    }

    public static boolean verifySignature(final VerifySignature signature) {
        if (signature.getPublicKeyData() == null) return false;
        PubKey pubKey = Secp256k1Kt.unmarshalSecp256k1PublicKey(signature.getPublicKeyData());
        return pubKey.verify(signature.getMessageData(), signature.getSignatureData());
    }
    
    public static byte[] decompressSecp256k1(PubKey pubKey) {
        byte[] compressedKey = pubKey.raw();
        // Ensure the compressed key is 33 bytes
        if (compressedKey.length != 33) {
            throw new IllegalArgumentException("Invalid compressed key length");
        }

        // Retrieve the x-coordinate
        byte[] xBytes = new byte[32];
        System.arraycopy(compressedKey, 1, xBytes, 0, 32);
        BigInteger x = new BigInteger(1, xBytes);

        // Define the secp256k1 curve parameters
        BigInteger q = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
        // finite field order

        // Calculate the y-coordinate
        BigInteger ySquared = x.pow(3).add(BigInteger.valueOf(7)).mod(q);
        BigInteger y = ySquared.modPow(q.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), q);

        // Choose the correct y-coordinate
        if (y.testBit(0) != (compressedKey[0] == 0x03)) {
            y = q.subtract(y);
        }

        // Concatenate to form the uncompressed key
        byte[] uncompressedKey = new byte[64];
        System.arraycopy(xBytes, 0, uncompressedKey, 0, 31);
        System.arraycopy(y.toByteArray(), 0, uncompressedKey, 32, 31);

        return uncompressedKey;
    }
}