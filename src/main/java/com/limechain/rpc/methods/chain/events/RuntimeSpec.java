package com.limechain.rpc.methods.chain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class RuntimeSpec {
    private String specName;
    private String implName;
    private Integer authoringVersion;
    private Integer specVersion;
    private Integer transactionVersion;
    private Map<String, Integer> apis;

}
