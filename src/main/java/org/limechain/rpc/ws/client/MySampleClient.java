package org.limechain.rpc.ws.client;

import io.emeraldpay.polkaj.api.PolkadotApi;
import io.emeraldpay.polkaj.api.Subscription;
import io.emeraldpay.polkaj.apiws.JavaHttpSubscriptionAdapter;
import org.limechain.rpc.chain.events.FollowEvent;
import org.limechain.rpc.chain.subscriptions.SubscriptionHandlers;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.limechain.rpc.chain.subscriptions.SubscriptionCalls.unstableFollow;

public class MySampleClient {
    public static void main (String[] args) throws ExecutionException, InterruptedException, TimeoutException, URISyntaxException {
        JavaHttpSubscriptionAdapter wsAdapter = JavaHttpSubscriptionAdapter
                .newBuilder()
                // Requires smoldot node to be running
                .connectTo("ws://127.0.0.1:9944/westend2")
                .build();

        PolkadotApi api = PolkadotApi
                .newBuilder()
                .subscriptionAdapter(wsAdapter)
                .build();

        // IMPORTANT! connect to the node as the first step before making calls or subscriptions.
        wsAdapter.connect().get(5, TimeUnit.SECONDS);

        Future<Subscription<FollowEvent>> hashFuture = api.subscribe(unstableFollow(true));
        Subscription<FollowEvent> subscription = hashFuture.get(30, TimeUnit.SECONDS);

        // TODO: I'm sure there's a better way to set handler to specific implementation but I'm not sure of the syntax
        subscription.handler((Subscription.Event<FollowEvent> event) -> {
            SubscriptionHandlers.unstableFollowHandler(event);
        });
    }


//    public static void main (String[] args) throws Exception {
//        PolkadotApi api = PolkadotApi.newBuilder()
//                .rpcCallAdapter(JavaHttpAdapter
//                        .newBuilder()
//                        .connectTo("https://westend-rpc.polkadot.io")
//                        .build())
//                .build();
//
//        Future<Hash256> hashFuture = api.execute(
//                // use RpcCall.create to define the request
//                // the first parameter is Class / JavaType of the expected result
//                // second is the method name
//                // and optionally a list of parameters for the call
//                RpcCall.create(Hash256.class, PolkadotMethod.CHAIN_GET_FINALIZED_HEAD)
//        );
//
//        Hash256 hash = hashFuture.get();
//        Hash256 blockHash = api.execute(PolkadotApi.commands().getBlockHash()).get();
//
//        Future<BlockResponseJson> blockFuture = api.execute(
//                // Another way to prepare a call, instead of manually constructing RpcCall instances
//                // is to use standard commands provided by PolkadotApi.commands()
//                // the following line is same as calling with
//                // RpcCall.create(BlockResponseJson.class, "chain_getBlock", hash)
//                PolkadotApi.commands().getBlock(hash)
//        );
//        BlockResponseJson block = blockFuture.get();
//
//        String version = api.execute(PolkadotApi.commands().systemVersion())
//                .get(5, TimeUnit.SECONDS);
//
////        RuntimeVersionJson runtimeVersion = api.execute(PolkadotApi.commands().getRuntimeVersion())
////                .get(5, TimeUnit.SECONDS);
//
//        SystemHealthJson health = api.execute(PolkadotApi.commands().systemHealth())
//                .get(5, TimeUnit.SECONDS);
//
//        System.out.println("Software: " + version);
////        System.out.println("Spec: " + runtimeVersion.getSpecName() + "/" + runtimeVersion.getSpecVersion());
////        System.out.println("Impl: " + runtimeVersion.getImplName() + "/" + runtimeVersion.getImplVersion());
//        System.out.println("Peers count: " + health.getPeers());
//        System.out.println("Is syncing: " + health.getSyncing());
//        System.out.println("Current head: " + hash);
//        System.out.println("Current block hash: " + blockHash);
//        System.out.println("Current height: " + block.getBlock().getHeader().getNumber());
//        System.out.println("State hash: " + block.getBlock().getHeader().getStateRoot());
//        api.close();
//    }
}


