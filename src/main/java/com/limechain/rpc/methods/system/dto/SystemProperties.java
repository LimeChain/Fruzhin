package com.limechain.rpc.methods.system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemProperties {
    private Integer ss58Format;
    private Integer tokenDecimals;
    private String tokenSymbol;
}
