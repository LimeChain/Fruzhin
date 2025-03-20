package com.limechain.consensus.beefy.event;

import java.util.EventListener;

public interface FinalizedBlockChangeListener extends EventListener {
    void finalizedBlockChanged(FinalizedBlockChangeEvent event);
}
