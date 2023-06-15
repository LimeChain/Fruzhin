package com.limechain.network.kad;

import io.libp2p.core.AddressBook;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RamAddressBook implements AddressBook {
    Map<PeerId, Set<Multiaddr>> addresses = new ConcurrentHashMap<>();

    @NotNull
    public CompletableFuture<Void> addAddrs(@NotNull PeerId peerId, long ttl, @NotNull Multiaddr... multiaddrs) {
        this.addresses.putIfAbsent(peerId, new HashSet<>());
        Set<Multiaddr> val = this.addresses.get(peerId);
        val.addAll(Arrays.asList(multiaddrs));

        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    public CompletableFuture<Collection<Multiaddr>> getAddrs(@NotNull PeerId peerId) {
        return CompletableFuture.completedFuture(this.addresses.getOrDefault(peerId, Collections.emptySet()));
    }

    @NotNull
    public CompletableFuture<Void> setAddrs(@NotNull PeerId peerId, long ttl, @NotNull Multiaddr... multiaddrs) {
        Set<Multiaddr> val = new HashSet<>(Arrays.asList(multiaddrs));
        this.addresses.put(peerId, val);
        return CompletableFuture.completedFuture(null);
    }

    public Map<PeerId, Set<Multiaddr>> getAddresses() {
        return addresses;
    }
}
