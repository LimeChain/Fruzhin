package com.limechain.rpc.methods.chain.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuntimeInfo {
    private String type;
    private RuntimeSpec spec;
    private String error;
}

