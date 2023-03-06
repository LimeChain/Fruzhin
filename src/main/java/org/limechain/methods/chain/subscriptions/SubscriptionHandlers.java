package org.limechain.methods.chain.subscriptions;

import io.emeraldpay.polkaj.api.Subscription;
import org.limechain.methods.chain.events.FollowEvent;

public class SubscriptionHandlers {
    public static void unstableFollowHandler (Subscription.Event<FollowEvent> event) {
        FollowEvent header = event.getResult();
        if (header.event.equals("initialized")) {
            System.out.println("Initialized Event:");
            System.out.printf("Finalized block hash: %s%n", header.finalizedBlockHash);
            System.out.printf("Finalized block runtime: %s%n%n", header.finalizedBlockRuntime);
        } else if (header.event.equals("newBlock")) {
            System.out.println("New block Event:");
            System.out.printf("New block hash: %s%n", header.blockHash);
            System.out.printf("Parent block hash: %s%n", header.parentBlockHash);
            System.out.printf("New runtime: %s%n%n", header.newRuntime);
        } else if (header.event.equals("bestBlockChanged")) {
            System.out.println("Best block changed Event:");
            System.out.printf("Best block hash: %s%n%n", header.bestBlockHash);
        } else if (header.event.equals("finalized")) {
            System.out.println("Finalized block Event:");
            System.out.printf("Finalized block hashes: %s%n", header.finalizedBlockHashes.toString());
            System.out.printf("Pruned block hashes: %s%n%n", header.prunedBlockHashes.toString());
        } else if (header.event.equals("stop")) {
            System.out.printf("Stop event received!%n");
        } else {
            System.out.printf("Unknown event received (%s)%n", header.event);
        }
    }
}
