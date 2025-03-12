package com.limechain.consensus.babe.coordinator;

import java.util.EventListener;

public interface SlotChangeListener extends EventListener {
    void slotChanged(SlotChangeEvent event);
}