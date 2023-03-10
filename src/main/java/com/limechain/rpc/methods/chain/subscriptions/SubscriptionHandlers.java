package com.limechain.rpc.methods.chain.subscriptions;

import com.limechain.rpc.methods.chain.events.FollowEvent;
import io.emeraldpay.polkaj.api.Subscription;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class SubscriptionHandlers {
    public static void unstableFollowHandler(Subscription.Event<FollowEvent> event) {
        // TODO: Change handler logic to have meaningful logic
        FollowEvent header = event.getResult();
        if (header.getEvent().equals("initialized")) {
            log.log(Level.INFO, "Initialized Event:");
            log.log(Level.INFO, String.format("Finalized block hash: %s%n", header.getFinalizedBlockHash()));
            log.log(Level.INFO, String.format("Finalized block runtime: %s%n%n", header.getFinalizedBlockRuntime()));
        } else if (header.getEvent().equals("newBlock")) {
            log.log(Level.INFO, "New block Event:");
            log.log(Level.INFO, String.format("New block hash: %s%n", header.getBlockHash()));
            log.log(Level.INFO, String.format("Parent block hash: %s%n", header.getParentBlockHash()));
            log.log(Level.INFO, String.format("New runtime: %s%n%n", header.getNewRuntime()));
        } else if (header.getEvent().equals("bestBlockChanged")) {
            log.log(Level.INFO, "Best block changed Event:");
            log.log(Level.INFO, String.format("Best block hash: %s%n%n", header.getBestBlockHash()));
        } else if (header.getEvent().equals("finalized")) {
            log.log(Level.INFO, "Finalized block Event:");
            log.log(Level.INFO, String.format("Finalized block hashes: %s%n", header.getFinalizedBlockHash()));
            log.log(Level.INFO, String.format("Pruned block hashes: %s%n%n", header.getPrunedBlockHashes().toString()));
        } else if (header.getEvent().equals("stop")) {
            log.log(Level.INFO, "Stop event received!%n");
        } else {
            log.log(Level.INFO, String.format("Unknown event received (%s)%n", header.getEvent()));
        }
    }
}
