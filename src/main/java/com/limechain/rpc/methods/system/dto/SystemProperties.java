package com.limechain.rpc.methods.system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class SystemProperties {
    private Integer ss58Format;
    private Integer tokenDecimals;
    private String tokenSymbol;
}
