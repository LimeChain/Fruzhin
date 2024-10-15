package com.limechain.babe.state;

import com.limechain.babe.api.BabeApiConfiguration;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

/**
 * Represents the state information for an epoch in the system.
 * This class encapsulates all the necessary configuration and parameters related to a specific epoch.
 */
@Getter
@Component
public class EpochState {
    private BigInteger slotDuration;
    private BigInteger epochLength;
    private EpochData currentEpochData;
    private EpochDescriptor currentEpochDescriptor;


    public void initialize(BabeApiConfiguration babeApiConfiguration) {
        this.slotDuration = babeApiConfiguration.getSlotDuration();
        this.epochLength = babeApiConfiguration.getEpochLength();
        this.currentEpochData = new EpochData(babeApiConfiguration.getAuthorities(), babeApiConfiguration.getRandomness());
        this.currentEpochDescriptor = new EpochDescriptor(babeApiConfiguration.getConstant(), babeApiConfiguration.getAllowedSlots());
    }
}
