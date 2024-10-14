package com.limechain.transaction;

import com.limechain.exception.misc.RuntimeApiVersionException;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeEndpoint;
import com.limechain.runtime.version.ApiVersionName;
import com.limechain.storage.block.BlockState;
import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.TransactionSource;
import com.limechain.transaction.dto.TransactionValidationRequest;
import com.limechain.transaction.dto.TransactionValidationResponse;
import com.limechain.transaction.dto.ValidTransaction;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.TransactionValidationReader;
import com.limechain.utils.scale.writers.TransactionValidationWriter;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
@Log
public class TransactionValidator {

    private final BlockState blockState;

    public TransactionValidator() {
        this.blockState = BlockState.getInstance();
    }

    public TransactionValidationResponse validateTransaction(Runtime runtime, Extrinsic transaction) {
        byte[] scaleRequest = createValidationRequest(runtime, transaction);
        byte[] validationResult = runtime.call(
                RuntimeEndpoint.TRANSACTION_QUEUE_VALIDATE_TRANSACTION, scaleRequest);
        return ScaleUtils.Decode.decode(validationResult, new TransactionValidationReader());
    }

    private byte[] createValidationRequest(Runtime runtime, Extrinsic transaction) {
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
                request.setParentBlockHash(blockState.bestBlockHash());
            }
            default -> throw new RuntimeApiVersionException(
                    String.format("Invalid transaction queue version: %d", txQueueVersion));
        }

        return ScaleUtils.Encode.encode(new TransactionValidationWriter(), request);
    }
}
