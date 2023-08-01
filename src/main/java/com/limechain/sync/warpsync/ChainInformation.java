package com.limechain.sync.warpsync;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class ChainInformation {
    BigInteger runtimeAuraVersion;
    BigInteger runtimeBabeVersion;
    BigInteger runtimeGrandpaVersion;

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
        return runtimeBabeVersion == BigInteger.ONE;
    }

    public boolean runtimeGrandpaSupportsCurrentSetId() {
        return runtimeGrandpaVersion.equals(BigInteger.valueOf(3));
    }

    public boolean runtimeHasAura() {
        return !runtimeAuraVersion.equals(BigInteger.valueOf(-1));
    }
}
