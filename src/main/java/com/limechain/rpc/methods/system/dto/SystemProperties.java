package com.limechain.rpc.methods.system.dto;

import com.limechain.rpc.methods.system.SystemRPCImpl;
import lombok.Getter;
import lombok.Setter;

/**
 * Jsonrpc response DTO for {@link SystemRPCImpl#systemProperties()}
 */
@Getter
@Setter
public class SystemProperties {
    private Integer ss58Format;
    private Integer tokenDecimals;
    private String tokenSymbol;
}
