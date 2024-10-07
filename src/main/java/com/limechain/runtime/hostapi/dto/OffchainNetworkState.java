package com.limechain.runtime.hostapi.dto;

import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;

import java.util.List;

/**
 * A DTO holding the data needed for <a href="https://spec.polkadot.network/chap-host-api#id-ext_offchain_network_state">ext_offchain_network_state_version_1</a>.
 * It is essentially <a href="https://spec.polkadot.network/chap-host-api#defn-opaque-network-state">Opaque Network State</a>, but not yet opaque as it's not SCALE encoded.
 * @param peerId the libp2p Peer ID of the local node
 * @param listenAddresses the list of libp2p Multiaddresses the node knows it can be reached at
 */
public record OffchainNetworkState(
    PeerId peerId,
    List<Multiaddr> listenAddresses
) {
}
