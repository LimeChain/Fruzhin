package com.limechain.transaction;

import com.limechain.exception.misc.RuntimeApiVersionException;
import com.limechain.exception.transaction.TransactionValidationException;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeEndpoint;
import com.limechain.runtime.version.ApiVersionName;
import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.TransactionSource;
import com.limechain.transaction.dto.TransactionValidationRequest;
import com.limechain.transaction.dto.TransactionValidationResponse;
import com.limechain.transaction.dto.ValidTransaction;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.TransactionValidationReader;
import com.limechain.utils.scale.writers.TransactionValidationWriter;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Objects;

@Log
public class TransactionValidator {

    public static ValidTransaction validateTransaction(Runtime runtime, Hash256 hash, Extrinsic transaction) {
        byte[] scaleRequest = createValidationRequest(runtime, hash, transaction);
        byte[] validationResult = runtime.call(
                RuntimeEndpoint.TRANSACTION_QUEUE_VALIDATE_TRANSACTION, scaleRequest);
        TransactionValidationResponse response =
                ScaleUtils.Decode.decode(validationResult, new TransactionValidationReader());

        if (!Objects.isNull(response.getTransactionValidityError())) {
            throw new TransactionValidationException(response.getTransactionValidityError().toString());
        }

        return new ValidTransaction(transaction, response.getValidTx());
    }

    private static byte[] createValidationRequest(Runtime runtime, Hash256 hash256, Extrinsic transaction) {
        BigInteger txQueueVersion = runtime.getVersion().getApis()
                .getApiVersion(ApiVersionName.TRANSACTION_QUEUE_API.getHashedName());

        TransactionValidationRequest request = new TransactionValidationRequest();

        switch (txQueueVersion.intValue()) {
            case 1 -> request.setTransaction(transaction.getData());
            case 2 -> {
                request.setSource(TransactionSource.EXTERNAL);
                request.setTransaction(transaction.getData());
            }
            case 3 -> {
                request.setSource(TransactionSource.EXTERNAL);
                request.setTransaction(transaction.getData());
                request.setParentBlockHash(hash256);
            }
            default -> throw new RuntimeApiVersionException(
                    String.format("Invalid transaction queue version: %d", txQueueVersion));
        }

        return ScaleUtils.Encode.encode(new TransactionValidationWriter(), request);
    }
}
