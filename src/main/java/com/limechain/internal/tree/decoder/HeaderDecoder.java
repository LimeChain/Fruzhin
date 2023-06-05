package com.limechain.internal.tree.decoder;

import com.limechain.internal.NodeVariant;
import com.limechain.utils.LittleEndianUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

import static com.limechain.internal.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

@Getter
@Setter
public class HeaderDecoder {
    public byte variantBits;
    public int partialKeyLengthHeader;
    public byte partialKeyLengthHeaderMask;

    public HeaderDecoder(byte variantBits, int partialKeyLengthHeader, byte partialKeyLengthHeaderMask) {
        this.variantBits = variantBits;
        this.partialKeyLengthHeader = partialKeyLengthHeader;
        this.partialKeyLengthHeaderMask = partialKeyLengthHeaderMask;
    }

    public static HeaderDecoder decodeHeaderByte(byte header) throws TrieDecoderException {
        List<NodeVariant> nodeVariantList = Arrays.asList(NodeVariant.values());
        for (int i = nodeVariantList.size() - 1; i >= 0; i--) {
            NodeVariant nodeVariant = nodeVariantList.get(i);
            int variantBits = header & nodeVariant.bits;
            if (variantBits != nodeVariant.bits) {
                continue;
            }

            byte partialKeyLengthHeader = (byte) (header & nodeVariant.mask);
            return new HeaderDecoder((byte) variantBits, partialKeyLengthHeader, (byte) nodeVariant.mask);
        }
        throw new TrieDecoderException("Node variant is unknown for header byte " +
                String.format("%08d", Integer.parseInt(Integer.toBinaryString(header & 0xFF))));
    }

    public static HeaderDecoder decodeHeader(ScaleCodecReader reader) throws TrieDecoderException {
        try {
            byte currentByte = reader.readByte();
            HeaderDecoder header = HeaderDecoder.decodeHeaderByte(currentByte);
            int partialKeyLengthHeader = header.getPartialKeyLengthHeader();
            int partialKeyLengthHeaderMask = header.getPartialKeyLengthHeaderMask();

            if (partialKeyLengthHeader < partialKeyLengthHeaderMask) {
                return new HeaderDecoder(header.getVariantBits(), header.getPartialKeyLengthHeader(), (byte) 0);
            }

            while (true) {
                int nextByte = reader.readUByte();
                partialKeyLengthHeader += nextByte;
                if (partialKeyLengthHeader > MAX_PARTIAL_KEY_LENGTH) {
                    throw new IllegalStateException("Partial key overflow");
                }

                //check if current byte is max byte value
                if (nextByte < 255) {
                    return new HeaderDecoder(header.getVariantBits(), partialKeyLengthHeader, (byte) 0);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TrieDecoderException("Could not decode header: " + e.getMessage());
        }
    }

    public static byte[] decodeKey(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
        try {
            if (partialKeyLength == 0) {
                return new byte[]{};
            }

            int keySize = partialKeyLength / 2 + partialKeyLength % 2;
            byte[] key = reader.readByteArray(keySize);
            if (keySize != key.length) {
                throw new TrieDecoderException("Read bytes is not equal to key size. Read " +
                        key + " bytes, expected " + key.length);
            }
            // Maybe we will have to return only [partialKeyLength%2:]
            return LittleEndianUtils.convertBytes(key);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode partial key: " + error.getMessage());
        }
    }
}