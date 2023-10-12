package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.dto.PeerInfo;
import com.limechain.network.dto.ProtocolStreams;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrandpaServiceTest {
    @InjectMocks
    private GrandpaService grandpaService = new GrandpaService("pid");
    @Mock
    private PeerInfo peerInfo;
    @Mock
    private ProtocolStreams protocolStreams;
    @Mock
    private Host host;
    @Mock
    private PeerId peerId;
    @Mock
    private ConnectionManager connectionManager;
    @Mock
    private Grandpa protocol;

    @Test
    void sendNeighbourMessageWhenNotConnectionShouldSendHandshake() {
        AddressBook addressBook = mock(AddressBook.class);
        GrandpaController grandpaController = mock(GrandpaController.class);
        when(connectionManager.getPeerInfo(peerId)).thenReturn(peerInfo);
        when(peerInfo.getGrandpaStreams()).thenReturn(protocolStreams);
        when(protocolStreams.getInitiator()).thenReturn(null);
        when(host.getAddressBook()).thenReturn(addressBook);
        when(protocol.dialPeer(host, peerId, addressBook)).thenReturn(grandpaController);

        grandpaService.sendNeighbourMessage(host, peerId);

        verify(grandpaController).sendHandshake();
    }

    @Test
    void sendNeighbourMessageWhenExistingConnection() {
        PeerInfo peerInfo = mock(PeerInfo.class);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        Stream stream = mock(Stream.class);
        when(connectionManager.getPeerInfo(peerId)).thenReturn(peerInfo);
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