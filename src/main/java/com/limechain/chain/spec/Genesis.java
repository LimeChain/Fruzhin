package com.limechain.chain.spec;

import com.google.protobuf.ByteString;
import com.limechain.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Genesis {
    private final Map<ByteString, ByteString> top;
    // TODO: Add child storage support
    // private Map<ByteString, ...?> childrenDefault;

    Genesis(RawChainSpec rawChainSpec) {
        Map<ByteString, ByteString> parsedTopStorage = new HashMap<>();

        Function<String, ByteString> parser =
            rawPrefixedHex ->
                ByteString.fromHex(StringUtils.remove0xPrefix(rawPrefixedHex));

        Map<String, String> top = rawChainSpec.getGenesis().getRaw().get("top");
        for (Map.Entry<String, String> e : top.entrySet()) {
            parsedTopStorage.put(
                parser.apply(e.getKey()),
                parser.apply(e.getValue())
            );
        }

        this.top = parsedTopStorage;
    }

    public Map<ByteString, ByteString> getTop() {
        return Collections.unmodifiableMap(this.top);
    }

    public ByteString getTopValue(ByteString key) {
        return this.top.get(key);
    }

    public ByteString getTopValue(byte[] key) {
        return getTopValue(ByteString.copyFrom(key));
    }
}
