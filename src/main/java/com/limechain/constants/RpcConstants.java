package com.limechain.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RpcConstants {
    public static final String POLKADOT_WS_RPC = "wss://rpc.polkadot.io";
    public static final String KUSAMA_WS_RPC = "wss://kusama-rpc.polkadot.io";
    public static final String WESTEND_WS_RPC = "wss://westend-rpc.polkadot.io";
}