package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Secp256k1Kt;
import kotlin.Pair;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Arrays;

public class EcdsaUtils {

    public static Pair<PrivKey, PubKey> generateKeyPair() {
        Pair<PrivKey, PubKey> keyPair = Secp256k1Kt.generateSecp256k1KeyPair();
        return new Pair<>(keyPair.getFirst(), keyPair.getSecond());
    }

    public static Pair<PrivKey, PubKey> generateKeyPair(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        Bip32ECKeyPair keyPair = Bip32ECKeyPair.generateKeyPair(seed);

        PrivKey privKey = Secp256k1Kt.unmarshalSecp256k1PrivateKey(keyPair.getPrivateKey().toByteArray());
        PubKey pubKey = privKey.publicKey();

        return new Pair<>(privKey, pubKey);
    }

    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        ECKeyPair keyPair = Bip32ECKeyPair.create(privateKey);

        Sign.SignatureData signature = Sign.signMessage(message, keyPair, false);

        byte[] signatureBytes = new byte[65];
        System.arraycopy(signature.getR(), 0, signatureBytes, 0, 32);
        System.arraycopy(signature.getS(), 0, signatureBytes, 32, 32);
        System.arraycopy(signature.getV(), 0, signatureBytes, 64, 1);

        return signatureBytes;
    }

    public static boolean verifySignature(final VerifySignature sig) {
        if (sig.getPublicKeyData() == null) return false;
        byte[] publicKey = recoverPublicKeyFromSignature(sig.getSignatureData(), sig.getMessageData(), true);

        return Arrays.equals(publicKey, sig.getPublicKeyData());
    }

    public static byte[] recoverPublicKeyFromSignature(byte[] signatureData, byte[] messageData, boolean compressed) {
        if (signatureData[64] >= 27) {
            signatureData[64] -= 27;
        }

        byte[] r = Arrays.copyOfRange(signatureData, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureData, 32, 64);
        byte recId = signatureData[64];

        ECDSASignature sig =
                new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));

        byte[] fullPubKey = recoverFullPubKey(recId, sig, messageData);

        if (compressed) {
            return compressPublicKey(fullPubKey);
        } else {
            return fullPubKey;
        }
    }

    private static byte[] recoverFullPubKey(int recId, ECDSASignature sig, byte[] messageData) {
        byte[] trimmedKey = Sign.recoverFromSignature(recId, sig, messageData).toByteArray();
        if (trimmedKey.length == 64) {
            byte[] key = new byte[65];
            key[0] = 4;
            System.arraycopy(trimmedKey, 0, key, 1, 64);

            return key;
        } else {
            trimmedKey[0] = 4;
            return trimmedKey;
        }
    }

    private static byte[] compressPublicKey(byte[] publicKey) {
        ECPoint point = Sign.CURVE_PARAMS.getCurve().decodePoint(publicKey);

        return point.getEncoded(true);
    }
}