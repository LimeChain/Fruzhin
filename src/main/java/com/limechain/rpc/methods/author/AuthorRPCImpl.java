package com.limechain.rpc.methods.author;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeEndpoint;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthorRPCImpl {

    private static final Logger log = LoggerFactory.getLogger(AuthorRPCImpl.class);
    private final BlockState blockState = BlockState.getInstance();
    private final KeyStore keyStore = AppBean.getBean(KeyStore.class);

    public String authorRotateKeys() {
        var bestBlockHash = blockState.bestBlockHash();
        Runtime runtime = blockState.getRuntime(bestBlockHash);

        if (runtime != null) {
            try {
                //TODO: Temp solution without seed
                byte[] response = runtime.call(
                        RuntimeEndpoint.SESSION_KEYS_GENERATE_SESSION_KEYS,
                        ScaleUtils.Encode.encodeOptional(ScaleCodecWriter::writeByteArray, null)
                );

//                byte[] response = runtime.call(
//                        RuntimeEndpoint.SESSION_KEYS_GENERATE_SESSION_KEYS,
//                        ScaleUtils.Encode.encodeOptional(ScaleCodecWriter::writeAsList, bestBlockHash.getBytes())
//                );

                log.info(StringUtils.toHexWithPrefix(response));
                return StringUtils.toHexWithPrefix(response);
            } catch (Exception e) {
                log.info(e.getMessage(), e.getCause());
            }
        }

        return null;
    }

//    fn rotate_keys(&self, ext: &Extensions) -> Result<Bytes> {
//        check_if_safe(ext)?;
//
//        let best_block_hash = self.client.info().best_hash;
//        let mut runtime_api = self.client.runtime_api();
//
//        runtime_api.register_extension(KeystoreExt::from(self.keystore.clone()));
//
//        runtime_api
//                .generate_session_keys(best_block_hash, None)
//                .map(Into::into)
//                .map_err(|api_err| Error::Client(Box::new(api_err)).into())
//    }

    public String authorInsertKey(String keyType, String suri, String publicKey) {
        KeyType parsedKeyType = KeyType.getByBytes(keyType.getBytes());

        if (parsedKeyType == null) {
            return "";
        }

        keyStore.put(parsedKeyType, suri.getBytes(), publicKey.getBytes());

        return "";
    }

//    fn insert_key(
//		&self,
//        ext: &Extensions,
//        key_type: String,
//        suri: String,
//        public: Bytes,
//        ) -> Result<()> {
//        check_if_safe(ext)?;
//
//        let key_type = key_type.as_str().try_into().map_err(|_| Error::BadKeyType)?;
//        self.keystore
//                .insert(key_type, &suri, &public[..])
//			.map_err(|_| Error::KeystoreUnavailable)?;
//        Ok(())
//    }

    public Boolean authorHasKey(String publicKey, String keyType) {
        return false;
    }

//    fn has_key(&self, ext: &Extensions, public_key: Bytes, key_type: String) -> Result<bool> {
//        check_if_safe(ext)?;
//
//        let key_type = key_type.as_str().try_into().map_err(|_| Error::BadKeyType)?;
//        Ok(self.keystore.has_keys(&[(public_key.to_vec(), key_type)]))
//    }

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
}
