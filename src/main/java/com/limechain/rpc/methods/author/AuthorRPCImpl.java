package com.limechain.rpc.methods.author;

import com.limechain.rpc.methods.author.dto.DecodedKey;
import com.limechain.rpc.methods.author.dto.DecodedKeyReader;
import com.limechain.runtime.RuntimeEndpoint;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.schnorrkel.SchnorrkelException;
import io.emeraldpay.polkaj.schnorrkel.SchnorrkelNative;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AuthorRPCImpl {

    private final BlockState blockState = BlockState.getInstance();
    private final KeyStore keyStore;

    public AuthorRPCImpl(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public String authorRotateKeys() {
        // The runtime injects the generated keys into the keystore.
        byte[] response = callRuntime(
                RuntimeEndpoint.SESSION_KEYS_GENERATE_SESSION_KEYS,
                ScaleUtils.Encode.encodeOptional(ScaleCodecWriter::writeByteArray, null)
        );

        return StringUtils.toHexWithPrefix(response);
    }

    public String authorInsertKey(String keyType, String suri, String publicKey) {
        KeyType parsedKeyType = parseKeyType(keyType);

        try {
            byte[] privateKey = decodePrivateKey(
                    StringUtils.hexToBytes(suri),
                    parsedKeyType,
                    StringUtils.hexToBytes(publicKey)
            );

            keyStore.put(parsedKeyType, StringUtils.hexToBytes(publicKey), privateKey);
            return publicKey;
        } catch (Exception e) {
            //TODO: Throw an exception
            return "";
        }
    }

    public Boolean authorHasKey(String publicKey, String keyType) {
        KeyType parsedKeyType = parseKeyType(keyType);
        return keyStore.contains(parsedKeyType, StringUtils.hexToBytes(publicKey));
    }

    public Boolean authorHasSessionKeys(String sessionKey) {
        byte[] response = callRuntime(
                RuntimeEndpoint.SESSION_KEYS_DECODE_SESSION_KEYS,
                ScaleUtils.Encode.encode(ScaleCodecWriter::writeByteArray, StringUtils.hexToBytes(sessionKey))
        );

        List<DecodedKey> decodedKeys = ScaleUtils.Decode.decode(response, new DecodedKeyReader());

        for (DecodedKey decodedKey : decodedKeys) {
            var key = StringUtils.toHexWithPrefix(decodedKey.getData());
            var type = new String(decodedKey.getKeyType().getBytes());

            if (Boolean.FALSE.equals(authorHasKey(key, type))) return false;
        }

        return true;
    }

    public String authorSubmitExtrinsic(String extrinsics) {
        return "";
    }

    public String authorSubmitAndWatchExtrinsic(String extrinsics) {
        return "";
    }

    private byte[] decodePrivateKey(byte[] suri, KeyType keyType, byte[] publicKey) throws SchnorrkelException, IllegalArgumentException {
        byte[] privateKey;
        byte[] generatedPublicKey;

        //TODO: By ED25519 private key is equal to secret seed
        if (keyType.getKey().equals(Key.ED25519)) {

            Ed25519PrivateKeyParameters param = new Ed25519PrivateKeyParameters(suri, 0);
            Ed25519PublicKeyParameters pubKey = param.generatePublicKey();

            Ed25519PrivateKey pk = new Ed25519PrivateKey(param);

            generatedPublicKey = pk.publicKey().raw();
            privateKey = param.getEncoded();

        } else if (keyType.getKey().equals(Key.SR25519)) {

            Schnorrkel schnorrkel = SchnorrkelNative.getInstance();
            Schnorrkel.KeyPair keyPair = schnorrkel.generateKeyPairFromSeed(suri);

            generatedPublicKey = keyPair.getPublicKey();
            privateKey = keyPair.getSecretKey();

        } else {
            throw new IllegalArgumentException("key type not supported");
        }

        if (!Arrays.equals(generatedPublicKey, publicKey)) {
            throw new IllegalArgumentException("provided public key or seed is invalid");
        }

        return privateKey;
    }

    private byte[] callRuntime(RuntimeEndpoint endpoint, byte[] parameter) {
        var bestBlockHash = blockState.bestBlockHash();
        var runtime = blockState.getRuntime(bestBlockHash);

        if (runtime == null) {
            throw new IllegalStateException("Runtime is null");
        }

        byte[] response;
        try {
            response = runtime.call(endpoint, parameter);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        return response;
    }

    private KeyType parseKeyType(String keyType) {
        KeyType parsedKeyType = KeyType.getByBytes(keyType.getBytes());
        if (parsedKeyType == null) throw new IllegalArgumentException("Invalid key type");
        return parsedKeyType;
    }
}
