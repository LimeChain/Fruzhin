package com.limechain.babe.state;

import com.limechain.babe.api.BabeApiConfiguration;
import com.limechain.babe.consesus.scale.BabeConsensusMessageReader;
import com.limechain.babe.consesus.BabeConsensusMessage;
import com.limechain.utils.scale.ScaleUtils;
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
    private EpochData nextEpochData;
    private long disabledAuthority;
    private EpochDescriptor nextEpochDescriptor;


    public void initialize(BabeApiConfiguration babeApiConfiguration) {
        this.slotDuration = babeApiConfiguration.getSlotDuration();
        this.epochLength = babeApiConfiguration.getEpochLength();
        this.currentEpochData = new EpochData(babeApiConfiguration.getAuthorities(), babeApiConfiguration.getRandomness());
        this.currentEpochDescriptor = new EpochDescriptor(babeApiConfiguration.getConstant(), babeApiConfiguration.getAllowedSlots());
    }

    public void updateNextEpochBlockConfig(byte[] message) {
        BabeConsensusMessage babeConsensusMessage = ScaleUtils.Decode.decode(message, new BabeConsensusMessageReader());
        switch (babeConsensusMessage.getFormat()) {
            case NEXT_EPOCH_DATA -> this.nextEpochData = babeConsensusMessage.getNextEpochData();
            case DISABLED_AUTHORITY -> this.disabledAuthority = babeConsensusMessage.getDisabledAuthority();
            case NEXT_EPOCH_DESCRIPTOR -> this.nextEpochDescriptor = babeConsensusMessage.getNextEpochDescriptor();
        }
    }
}
