package com.limechain.rpc.methods.chain.subscriptions;

import com.limechain.rpc.methods.chain.events.FollowEvent;
import io.emeraldpay.polkaj.api.Subscription;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SubscriptionHandlers {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionHandlers.class.getName());

    public static void unstableFollowHandler (Subscription.Event<FollowEvent> event) {
        // TODO: Change handler logic to have meaningful logic
        FollowEvent header = event.getResult();
        if (header.event.equals("initialized")) {
            LOGGER.log(Level.INFO, "Initialized Event:");
            LOGGER.log(Level.INFO, String.format("Finalized block hash: %s%n", header.finalizedBlockHash));
            LOGGER.log(Level.INFO, String.format("Finalized block runtime: %s%n%n", header.finalizedBlockRuntime));
        } else if (header.event.equals("newBlock")) {
            LOGGER.log(Level.INFO, "New block Event:");
            LOGGER.log(Level.INFO, String.format("New block hash: %s%n", header.blockHash));
            LOGGER.log(Level.INFO, String.format("Parent block hash: %s%n", header.parentBlockHash));
            LOGGER.log(Level.INFO, String.format("New runtime: %s%n%n", header.newRuntime));
        } else if (header.event.equals("bestBlockChanged")) {
            LOGGER.log(Level.INFO, "Best block changed Event:");
            LOGGER.log(Level.INFO, String.format("Best block hash: %s%n%n", header.bestBlockHash));
        } else if (header.event.equals("finalized")) {
            LOGGER.log(Level.INFO, "Finalized block Event:");
            LOGGER.log(Level.INFO, String.format("Finalized block hashes: %s%n", header.finalizedBlockHashes.toString()));
            LOGGER.log(Level.INFO, String.format("Pruned block hashes: %s%n%n", header.prunedBlockHashes.toString()));
        } else if (header.event.equals("stop")) {
            LOGGER.log(Level.INFO, "Stop event received!%n");
        } else {
            LOGGER.log(Level.INFO, String.format("Unknown event received (%s)%n", header.event));
        }
    }
}
