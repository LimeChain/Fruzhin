package com.limechain.sync.warpsync;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class ChainInformation {
    public static final BigInteger GRANDPA_VERSION_SUPPORTING_CURRENT_SET_ID = BigInteger.valueOf(3);
    private BigInteger runtimeAuraVersion;
    private BigInteger runtimeBabeVersion;
    private BigInteger runtimeGrandpaVersion;

    //TODO Fill missing calls when runtime calls are working
    //finalized_block_header
    //block_number_bytes
    //aura_autorities_call_output
    //aura_slot_duration_call_output
    //babe_current_epoch_call_output
    //babe_next_epoch_call_output
    //babe_configuration_call_output
    //grandpa_autorities_call_output
    //grandpa_current_set_id_call_output

    public boolean runtimeBabeApiIsV1() {
        return runtimeBabeVersion.equals(BigInteger.ONE);
    }

    public boolean runtimeGrandpaSupportsCurrentSetId() {
        return runtimeGrandpaVersion.equals(GRANDPA_VERSION_SUPPORTING_CURRENT_SET_ID);
    }

    public boolean runtimeHasAura() {
        return !runtimeAuraVersion.equals(BigInteger.valueOf(-1));
    }
}
