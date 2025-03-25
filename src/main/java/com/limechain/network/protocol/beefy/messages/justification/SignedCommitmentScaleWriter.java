package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Optional;

/**
 * Signed commitments are communicated in a "compact" manner for encoding efficiency.
 * See
 * <a href="https://github.com/paritytech/substrate/blob/55bb6298e74d86be12732fd0f120185ee8fbfe97/primitives/consensus/beefy/src/commitment.rs#L114">rust.</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignedCommitmentScaleWriter implements ScaleWriter<SignedCommitment> {

    private static final SignedCommitmentScaleWriter INSTANCE = new SignedCommitmentScaleWriter();

    public static SignedCommitmentScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, SignedCommitment signedCommitment) throws IOException {

        writer.writeByte(BeefyMessageType.JUSTIFICATION.getType());
        writer.writeByte(BeefyJustificationVersion.V1.getVersion());
        // Write "commitment" field.
        writer.write(CommitmentScaleWriter.getInstance(), signedCommitment.getCommitment());


        int authoritiesLength = signedCommitment.getSignatures().size();
        byte[] bitsFromSignature = new byte[authoritiesLength / 8 + 1];

        int presentSignaturesLength = 0;

        for (int i = 0; i < authoritiesLength; i++) {
            Optional<byte[]> current = signedCommitment.getSignatures().get(i);
            if (current.isPresent()) {
                presentSignaturesLength++;
                // Same process as described in the reader, but with an "OR" operator to get the 1 from the bitmask.
                bitsFromSignature[i / 8] |= (byte) (1 << (7 - i % 8));
            }
        }

        // Write "signatures_from" as a Vec<u8>.
        writer.writeCompact(bitsFromSignature.length);
        writer.writeByteArray(bitsFromSignature);
        // Write "validator_set_len" field.
        writer.writeUint32(authoritiesLength);
        // Write "signatures_compact" field.
        writer.writeCompact(presentSignaturesLength);
        for (Optional<byte[]> current : signedCommitment.getSignatures()) {
            if (current.isPresent()) {
                writer.writeByteArray(current.get());
            }
        }
    }
}
