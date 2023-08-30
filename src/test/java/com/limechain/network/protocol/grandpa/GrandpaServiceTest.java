package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.dto.PeerInfo;
import com.limechain.network.dto.ProtocolStreams;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrandpaServiceTest {
    private GrandpaService grandpaService;
    private final PeerInfo peerInfo = mock(PeerInfo.class);
    private final ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
    private final Host host = mock(Host.class);
    private final PeerId peerId = mock(PeerId.class);
    private static final ConnectionManager CONNECTION_MANAGER = mock(ConnectionManager.class);

    @BeforeAll
    static void init() {
        mockStatic(ConnectionManager.class).when(ConnectionManager::getInstance).thenReturn(CONNECTION_MANAGER);
    }

    @BeforeEach
    void setup() {
        grandpaService = new GrandpaService("pid");
    }

    @Test
    void sendNeighbourMessageWhenNotConnectionShouldSendHandshake() {
        AddressBook addressBook = mock(AddressBook.class);
        Grandpa grandpa = mock(Grandpa.class);
        GrandpaController grandpaController = mock(GrandpaController.class);
        ReflectionTestUtils.setField(grandpaService, "protocol", grandpa);
        when(CONNECTION_MANAGER.getPeerInfo(peerId)).thenReturn(peerInfo);
        when(peerInfo.getGrandpaStreams()).thenReturn(protocolStreams);
        when(protocolStreams.getInitiator()).thenReturn(null);
        when(host.getAddressBook()).thenReturn(addressBook);
        when(grandpa.dialPeer(host, peerId, addressBook)).thenReturn(grandpaController);

        grandpaService.sendNeighbourMessage(host, peerId);

        verify(grandpaController).sendHandshake();
    }

    @Test
    void sendNeighbourMessageWhenExistingConnection() {
        PeerInfo peerInfo = mock(PeerInfo.class);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        Stream stream = mock(Stream.class);
        when(CONNECTION_MANAGER.getPeerInfo(peerId)).thenReturn(peerInfo);
        when(peerInfo.getGrandpaStreams()).thenReturn(protocolStreams);
        when(protocolStreams.getInitiator()).thenReturn(stream);

        try (MockedConstruction<GrandpaController> mock = mockConstruction(GrandpaController.class)) {
            grandpaService.sendNeighbourMessage(host, peerId);

            assertEquals(1, mock.constructed().size());
            GrandpaController controller = mock.constructed().get(0);
            verify(controller).sendNeighbourMessage();
        }
    }
}