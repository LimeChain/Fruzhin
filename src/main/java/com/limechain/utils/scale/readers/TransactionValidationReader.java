package com.limechain.utils.scale.readers;

import com.limechain.transaction.dto.InvalidTransactionType;
import com.limechain.transaction.dto.TransactionValidationResponse;
import com.limechain.transaction.dto.TransactionValidity;
import com.limechain.transaction.dto.TransactionValidityError;
import com.limechain.transaction.dto.UnknownTransactionType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

import java.util.Objects;

public class TransactionValidationReader implements ScaleReader<TransactionValidationResponse> {

    @Override
    public TransactionValidationResponse read(ScaleCodecReader reader) {
        TransactionValidationResponse response = new TransactionValidationResponse();

        try {
            TransactionValidity validity = new TransactionValidity();

            validity.setPriority(new UInt64Reader().read(reader));

            int requiresCount = reader.readCompactInt();
            byte[][] requires = new byte[requiresCount][];
            for (int i = 0; i < requiresCount; i++) {
                requires[i] = reader.readByteArray();
            }
            validity.setRequires(requires);

            int providesCount = reader.readCompactInt();
            byte[][] provides = new byte[providesCount][];
            for (int i = 0; i < providesCount; i++) {
                provides[i] = reader.readByteArray();
            }
            validity.setProvides(provides);

            validity.setLongevity(new UInt64Reader().read(reader));
            validity.setPropagate(reader.readCompactInt() != 0);

            response.setValidTx(validity);
        } catch (Exception e) {
            response.setValidTx(null);
        }

        if (Objects.isNull(response.getValidTx())) {
            int errorType = reader.readCompactInt();
            int errorInt = reader.readCompactInt();
            TransactionValidityError error = errorType == 0
                    ? InvalidTransactionType.getFromInt(errorInt)
                    : UnknownTransactionType.getFromInt(errorInt);

            response.setTransactionValidityError(error);
        }

        return response;
    }
}
