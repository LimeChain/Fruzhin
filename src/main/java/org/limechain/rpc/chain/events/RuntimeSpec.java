package org.limechain.rpc.chain.events;

import java.util.Map;

public class RuntimeSpec {
    String specName;
    String implName;
    Integer authoringVersion;
    Integer specVersion;
    Integer transactionVersion;
    Map<String, Integer> apis;

}
