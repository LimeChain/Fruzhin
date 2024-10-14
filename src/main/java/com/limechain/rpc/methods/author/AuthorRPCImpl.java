package com.limechain.rpc.methods.author;

import com.limechain.runtime.Runtime;
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

@Service
public class AuthorRPCImpl {

    private final BlockState blockState = BlockState.getInstance();
    private final KeyStore keyStore;

    public AuthorRPCImpl(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    //TODO: Add documentation
    //TODO: Maybe some logs should be added

    // generate_session_keys method injects the generated keys into the keystore and that is done by the runtime
    public String authorRotateKeys() {
        var bestBlockHash = blockState.bestBlockHash();
        Runtime runtime = blockState.getRuntime(bestBlockHash);

        if (runtime == null) return null;

        try {
            byte[] response = runtime.call(
                    RuntimeEndpoint.SESSION_KEYS_GENERATE_SESSION_KEYS,
                    ScaleUtils.Encode.encodeOptional(ScaleCodecWriter::writeByteArray, null)
            );

            return StringUtils.toHexWithPrefix(response);
        } catch (Exception e) {
            return null;
        }
    }

    public String authorInsertKey(String keyType, String suri, String publicKey) {
        //TODO: apply validation here
        KeyType parsedKeyType = KeyType.getByBytes(keyType.getBytes());
        if (parsedKeyType == null) return "";

        try {
            byte[] privateKey = decodePrivateKey(
                    StringUtils.hexToBytes(suri),
                    parsedKeyType,
                    StringUtils.hexToBytes(publicKey)
            );

            keyStore.put(parsedKeyType, StringUtils.hexToBytes(publicKey), privateKey);
            return publicKey;
        } catch (Exception e) {
            return "";
        }
    }

    public Boolean authorHasKey(String publicKey, String keyType) {
        KeyType parsedKeyType = KeyType.getByBytes(keyType.getBytes());
        if (parsedKeyType == null) return false;
        return keyStore.contains(parsedKeyType, StringUtils.hexToBytes(publicKey));
    }

    public Boolean authorHasSessionKey(String sessionKey) {
        return false;
    }

//    fn has_session_keys(&self, ext: &Extensions, session_keys: Bytes) -> Result<bool> {
//        check_if_safe(ext)?;
//
//        let best_block_hash = self.client.info().best_hash;
//        let keys = self
//                .client
//                .runtime_api()
//                .decode_session_keys(best_block_hash, session_keys.to_vec())
//                .map_err(|e| Error::Client(Box::new(e)))?
//			.ok_or(Error::InvalidSessionKeys)?;
//
//        Ok(self.keystore.has_keys(&keys))
//    }

    public String authorSubmitExtrinsic(String extrinsics) {
        return "";
    }

//    async fn submit_extrinsic(&self, ext: Bytes) -> Result<TxHash<P>> {
//        let xt = match Decode::decode(&mut &ext[..]) {
//            Ok(xt) => xt,
//                    Err(err) => return Err(Error::Client(Box::new(err)).into()),
//        };
//        let best_block_hash = self.client.info().best_hash;
//        self.pool.submit_one(best_block_hash, TX_SOURCE, xt).await.map_err(|e| {
//                e.into_pool_error()
//                        .map(|e| Error::Pool(e))
//				.unwrap_or_else(|e| Error::Verification(Box::new(e)))
//				.into()
//		})
//    }

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
}
