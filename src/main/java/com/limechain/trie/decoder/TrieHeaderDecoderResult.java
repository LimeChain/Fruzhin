package com.limechain.trie.decoder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrieHeaderDecoderResult {
    public byte variantBits;
    public int partialKeyLengthHeader;
    public byte partialKeyLengthHeaderMask;

    public TrieHeaderDecoderResult(byte variantBits, int partialKeyLengthHeader, byte partialKeyLengthHeaderMask) {
        this.variantBits = variantBits;
        this.partialKeyLengthHeader = partialKeyLengthHeader;
        this.partialKeyLengthHeaderMask = partialKeyLengthHeaderMask;
    }

}
