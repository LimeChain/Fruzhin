package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Secp256k1Kt;
import kotlin.Pair;
import lombok.experimental.UtilityClass;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Arrays;

@UtilityClass
public class EcdsaUtils {

    public static final int SIGNATURE_LEN = 65;
    public static final int PUBLIC_KEY_PURE_LEN = 65;
    public static final int PUBLIC_KEY_TRIM_LEN = 64;
    public static final int PUBLIC_KEY_COMPRESSED_LEN = 33;
    public static final int HASHED_MESSAGE_LEN = 32;

    /**
     * Generates Secp256k1 key pair using the Secp256k1 library from libp2p
     * @return Secp256k1 Private key (32 bytes) and Public key (33 or 65 bytes)
     */
    public static Pair<PrivKey, PubKey> generateKeyPair() {
        return Secp256k1Kt.generateSecp256k1KeyPair();
    }

    /**
     * Generates Secp256k1 key pair from mnemonic phrase using the Bip32ECKeyPair library from web3j and then
     * converting it to Secp256k1 key pair using the Secp256k1 library from libp2p to get the KeyPair
     * @param mnemonic BIP-39 12 or 24 word mnemonic phrase
     * @return Secp256k1 Private key (32 bytes) and Public key (33 or 65 bytes)
     */
    public static Pair<PrivKey, PubKey> generateKeyPair(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        Bip32ECKeyPair keyPair = Bip32ECKeyPair.generateKeyPair(seed);

        PrivKey privKey = Secp256k1Kt.unmarshalSecp256k1PrivateKey(keyPair.getPrivateKey().toByteArray());
        PubKey pubKey = privKey.publicKey();

        return new Pair<>(privKey, pubKey);
    }

    /**
     * Signs message with Secp256k1 private key using the Bip32ECKeyPair library from web3j and splitting the signature
     * data to R S and V
     * @param privateKey 32 bytes Secp256k1 private key
     * @param message message to be signed
     * @return 65 bytes Secp256k1 signature {R (32 bytes), S (32 bytes), V (1 byte)}
     */
    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        ECKeyPair keyPair = Bip32ECKeyPair.create(privateKey);

        Sign.SignatureData signature = Sign.signMessage(message, keyPair, false);

        byte[] signatureBytes = new byte[SIGNATURE_LEN];
        System.arraycopy(signature.getR(), 0, signatureBytes, 0, 32);
        System.arraycopy(signature.getS(), 0, signatureBytes, 32, 32);
        System.arraycopy(signature.getV(), 0, signatureBytes, 64, 1);

        return signatureBytes;
    }

    /**
     * Verifies Secp256k1 signature using the Bip32ECKeyPair library from web3j to recover public key from signature
     * and then comparing it to the public key from the verify signature object
     * @param sig signature to be verified
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(final VerifySignature sig) {
        if (sig.getPublicKeyData() == null) return false;
        byte[] publicKey = recoverPublicKeyFromSignature(sig.getSignatureData(), sig.getMessageData(), true);

        return Arrays.equals(publicKey, sig.getPublicKeyData());
    }

    /**
     * Recovers public key from signature
     * @param signatureData 65 bytes Secp256k1 signature {R (32 bytes), S (32 bytes), V (1 byte)}
     * @param messageData signed message
     * @param compressed true if the public key should be compressed, false otherwise
     * @return 33 or 65 bytes Secp256k1 public key
     */
    public static byte[] recoverPublicKeyFromSignature(byte[] signatureData, byte[] messageData, boolean compressed) {
        if (signatureData[64] >= 27) {
            signatureData[64] -= 27;
        }

        byte[] r = Arrays.copyOfRange(signatureData, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureData, 32, 64);
        byte recId = signatureData[64];

        ECDSASignature sig =
                new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));

        byte[] fullPubKey = Sign.recoverFromSignature(recId, sig, messageData).toByteArray();

        if (compressed) {
            return compressPublicKey(fullPubKey);
        } else {
            if(fullPubKey.length == PUBLIC_KEY_PURE_LEN){
                return Arrays.copyOfRange(fullPubKey, 1, fullPubKey.length);
            }
            return fullPubKey;
        }
    }

    /**
     * Compresses public key
     * @param publicKey 64 or 65 bytes Secp256k1 public key (web3j generates 64 public key because it is always 4 and
     *                  not needed, but Secp256k1 requires 65 bytes public key to be able to compress it)
     * @return 33 bytes Secp256k1 public key
     */
    private static byte[] compressPublicKey(byte[] publicKey) {
        byte[] key = new byte[PUBLIC_KEY_PURE_LEN];
        if (publicKey.length == PUBLIC_KEY_TRIM_LEN) {
            System.arraycopy(publicKey, 0, key, 1, 64);
        } else {
            key = publicKey;
        }
        key[0] = 4;
        ECPoint point = Sign.CURVE_PARAMS.getCurve().decodePoint(key);

        return point.getEncoded(true);
    }
}