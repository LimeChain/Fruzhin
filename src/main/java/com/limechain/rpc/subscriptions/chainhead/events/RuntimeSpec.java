package com.limechain.rpc.subscriptions.chainhead.events;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RuntimeSpec {
    private String specName;
    private String implName;
    private Integer authoringVersion;
    private Integer specVersion;
    private Integer transactionVersion;
    private Map<String, Integer> apis;

}
