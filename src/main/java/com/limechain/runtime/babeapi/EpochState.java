package com.limechain.runtime.babeapi;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Represents the state information for an epoch in the system.
 * This class encapsulates all the necessary configuration and parameters related to a specific epoch.
 */
@Data
@Component
public class EpochState {
    private BabeApiConfiguration babeApiConfiguration;
}
