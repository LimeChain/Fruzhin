package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.scale.CommitmentScaleReader;
import com.limechain.exception.scale.ScaleDecodingException;
import com.limechain.exception.scale.WrongMessageTypeException;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import com.limechain.utils.EcdsaUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Signed commitments are communicated in a "compact" manner for encoding efficiency.
 * See
 * <a href="https://github.com/paritytech/substrate/blob/55bb6298e74d86be12732fd0f120185ee8fbfe97/primitives/consensus/beefy/src/commitment.rs#L114">rust.</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignedCommitmentScaleReader implements ScaleReader<SignedCommitment> {

    private static final SignedCommitmentScaleReader INSTANCE = new SignedCommitmentScaleReader();

    public static SignedCommitmentScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public SignedCommitment read(ScaleCodecReader reader) {
        return readInner(reader, true);
    }

    public SignedCommitment readNonGossiped(ScaleCodecReader reader) {
        return readInner(reader, false);
    }

    private SignedCommitment readInner(ScaleCodecReader reader, boolean isGossipMessage) {

        verifyTypes(reader, isGossipMessage);

        // Read "commitment" field.
        Commitment commitment = reader.read(CommitmentScaleReader.getInstance());

        // Read "signatures_from" field. Stored data is in Vec<u8> format.
        int bitsLength = reader.readCompactInt();
        byte[] signaturesFromBits = reader.readByteArray(bitsLength);

        // Check how many bits of value 1 are present.
        int expectedCount = 0;
        for (byte b : signaturesFromBits) {
            // Treat bytes as unsigned. Same as rust implementation.
            expectedCount += Integer.bitCount(Byte.toUnsignedInt(b));
        }

        // Read "validator_set_len".
        // Make sure "signatures_from" doesn't have fewer data than authority set length.
        long authSetLength = reader.readUint32();
        if (bitsLength * 8L < authSetLength) {
            throw new ScaleDecodingException("read: Not enough data in bitfield.");
        }

        List<Optional<byte[]>> signatureList = populateSignatures(reader,
                expectedCount,
                authSetLength,
                signaturesFromBits);

        return new SignedCommitment(commitment, signatureList);
    }

    private static List<Optional<byte[]>> populateSignatures(ScaleCodecReader reader,
                                                             int expectedCount,
                                                             long authSetLength,
                                                             byte[] signaturesFromBits) {

        // Read "signatures_compact".
        // Make sure that signature size matches the found bit count from "signatures_from".
        int signaturesLength = reader.readCompactInt();
        if (signaturesLength != expectedCount) {
            throw new ScaleDecodingException("read: Expected and actual size mismatch.");
        }

        Optional<byte[]>[] signatures = new Optional[(int) authSetLength];
        Arrays.fill(signatures, Optional.empty());

        // Map the indexes of bits from "signatures_from" that have a value 1 with indexes in the signature array.
        for (int i = 0; i < authSetLength; i++) {
            // "i / 8" selects the byte index in the array.
            // "i % 8" selects the bit index from the byte.
            // "7 - i % 8" decide shift amount to match the bit index.
            // "1 << (7 - i % 8)" shifts.
            // "[i / 8] & (1 << (7 - i % 8)))" logical AND operation to check if the bit at selected index is 1.
            if ((Byte.toUnsignedInt(signaturesFromBits[i / 8]) & (1 << (7 - i % 8))) != 0) {
                signatures[i] = Optional.of(reader.readByteArray(EcdsaUtils.SIGNATURE_LEN));
            }
        }

        return Arrays.stream(signatures).toList();
    }

    private static void verifyTypes(ScaleCodecReader reader, boolean isGossipMessage) {

        // Only messages from notification/gossip streams a prefixed with their message type.
        if (isGossipMessage) {
            int messageType = reader.readByte();
            if (messageType != BeefyMessageType.JUSTIFICATION.getType()) {
                throw new WrongMessageTypeException(
                        String.format("verifyTypes: Trying to read message of type %d as a beefy vote message.",
                                messageType));
            }
        }

        int version = reader.readByte();
        if (version != BeefyJustificationVersion.V1.getVersion()) {
            throw new WrongMessageTypeException(
                    String.format("verifyTypes: Trying to read justification of version %d as version 1.",
                            version));
        }
    }
}
