package com.limechain.babe;

import io.emeraldpay.polkaj.schnorrkel.VrfOutputAndProof;
import lombok.AllArgsConstructor;
import lombok.Getter;

//TODO: Refactor this class because this isn't the optimal structure
@Getter
@AllArgsConstructor
public class PreDigest {

    private PreDigestType preDigestType;
    private Long slotNumber;
    private Integer authorityIndex;
    private VrfOutputAndProof vrfOutputAndProof;

    public enum PreDigestType {
        PRIMARY,
        SECONDARY_PLAIN,
        SECONDARY_VRF
    }
}
