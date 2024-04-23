package com.limechain.sync.warpsync;

import com.google.protobuf.ByteString;
import com.limechain.exception.global.RuntimeCodeException;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class SyncedStateTest {
    @InjectMocks
    private SyncedState syncedState;

    @Mock
    private Set<BigInteger> scheduledRuntimeUpdateBlocks;
    @Mock
    private Hash256 lastFinalizedBlockHash;
    @Mock
    private Hash256 stateRoot;
    @Mock
    private RuntimeBuilder runtimeBuilder;
    @Mock
    private KVRepository<String, Object> repository;
    @Mock
    private Network network;

    @Test
    void updateRuntimeCode() throws RuntimeCodeException {
        LightClientMessage.Response response  = mock(LightClientMessage.Response.class);
        LightClientMessage.RemoteReadResponse remoteReadResponse = mock(LightClientMessage.RemoteReadResponse.class);
        ByteString wrappedProof = mock(ByteString.class);
        byte[] proof = new byte[] { 1, 2, 3 };
        byte[] decodedProof = new byte[] { 4, 5, 6 };
        byte[][] decodedProofs = new byte[][] { decodedProof };
        String blockHashString = "blhash";
        String[] codeKey = new String[]{ SyncedState.CODE_KEY };
        String stateRootString = "state root";
        byte[] runtimeCode = new byte[]{ 1, 2 };
        when(lastFinalizedBlockHash.toString()).thenReturn(blockHashString);
        when(stateRoot.toString()).thenReturn(stateRootString);
        when(network.makeRemoteReadRequest(blockHashString, codeKey)).thenReturn(response);
        when(response.getRemoteReadResponse()).thenReturn(remoteReadResponse);
        when(remoteReadResponse.getProof()).thenReturn(wrappedProof);
        when(wrappedProof.toByteArray()).thenReturn(proof);
        when(runtimeBuilder.buildRuntimeCode(decodedProofs, stateRoot)).thenReturn(runtimeCode);

        try (MockedConstruction<ScaleCodecReader> readerMock = mockConstruction((ScaleCodecReader.class),
                (mock, context) -> {
                    when(mock.readCompactInt()).thenReturn(1);
                    when(mock.readByteArray()).thenReturn(decodedProof);
                })
        ) {
            syncedState.updateRuntimeCode();

            verify(repository).save(DBConstants.STATE_TRIE_MERKLE_PROOF, decodedProofs);
            verify(repository).save(DBConstants.STATE_TRIE_ROOT_HASH, stateRootString);
            assertEquals(runtimeCode, syncedState.getRuntimeCode());
        }
    }

    @Test
    void syncBlockAnnounceWhenHasRunEnvUpdatedDigestShouldScheduleRuntimeUpdate() {
        BlockAnnounceMessage blockAnnounceMessage = mock(BlockAnnounceMessage.class);
        BlockHeader blockHeader = mock(BlockHeader.class);
        HeaderDigest headerDigest = mock(HeaderDigest.class);
        BigInteger blockNumber = mock(BigInteger.class);
        when(blockAnnounceMessage.getHeader()).thenReturn(blockHeader);
        when(blockHeader.getDigest()).thenReturn(new HeaderDigest[]{headerDigest});
        when(headerDigest.getType()).thenReturn(DigestType.RUN_ENV_UPDATED);
        when(blockHeader.getBlockNumber()).thenReturn(blockNumber);

        syncedState.syncBlockAnnounce(blockAnnounceMessage);

        verify(scheduledRuntimeUpdateBlocks).add(blockNumber);
    }

    @Test
    void syncBlockAnnounceWhenNoRunEnvUpdatedDigestShouldDoNothing() {
        BlockAnnounceMessage blockAnnounceMessage = mock(BlockAnnounceMessage.class);
        BlockHeader blockHeader = mock(BlockHeader.class);
        HeaderDigest headerDigest = mock(HeaderDigest.class);
        when(blockAnnounceMessage.getHeader()).thenReturn(blockHeader);
        when(blockHeader.getDigest()).thenReturn(new HeaderDigest[]{headerDigest});
        when(headerDigest.getType()).thenReturn(DigestType.OTHER);

        syncedState.syncBlockAnnounce(blockAnnounceMessage);

        verifyNoInteractions(scheduledRuntimeUpdateBlocks);
    }

    @Test
    void loadSavedRuntimeWhenValuesAvailableShouldBuildRuntimeCode() throws RuntimeCodeException {
        byte[][] merkleProof = new byte[][] {{1, 2}, {3, 4}};
        Object stateRootObject = mock(Object.class);
        String stateRoot = "root";
        Hash256 stateRootHash = mock(Hash256.class);
        byte[] runtimeCode = new byte[]{ 1, 2 };
        when(repository.find(DBConstants.STATE_TRIE_MERKLE_PROOF)).thenReturn(Optional.of(merkleProof));
        when(repository.find(DBConstants.STATE_TRIE_ROOT_HASH)).thenReturn(Optional.of(stateRootObject));
        when(stateRootObject.toString()).thenReturn(stateRoot);
        when(runtimeBuilder.buildRuntimeCode(merkleProof, stateRootHash)).thenReturn(runtimeCode);

        try (MockedStatic<Hash256> hashMock = mockStatic(Hash256.class)) {
            hashMock.when(() -> Hash256.from(stateRoot)).thenReturn(stateRootHash);

            syncedState.loadSavedRuntimeCode();

            assertEquals(runtimeCode, syncedState.getRuntimeCode());
        }
    }

    @Test
    void loadSavedRuntimeWhenMissingMerkleProofShouldThrow() {
        when(repository.find(DBConstants.STATE_TRIE_MERKLE_PROOF)).thenReturn(Optional.empty());

        assertThrows(RuntimeCodeException.class,
                () -> syncedState.loadSavedRuntimeCode(),
                "No available merkle proof");
    }

    @Test
    void loadSavedRuntimeWhenMissingStateHashProofShouldThrow() {
        byte[][] merkleProof = new byte[][]{{1, 2}, {3, 4}};
        when(repository.find(DBConstants.STATE_TRIE_MERKLE_PROOF)).thenReturn(Optional.of(merkleProof));
        when(repository.find(DBConstants.STATE_TRIE_ROOT_HASH)).thenReturn(Optional.empty());

        assertThrows(RuntimeCodeException.class,
                () -> syncedState.loadSavedRuntimeCode(),
                "No available state root");
    }

    @Test
    void buildRuntime() {
        byte[] runtimeCode = new byte[] { 1, 2, 3 };
        syncedState.setRuntimeCode(runtimeCode);
        Runtime runtime = mock(Runtime.class);
        when(runtimeBuilder.buildRuntime(runtimeCode)).thenReturn(runtime);

        syncedState.buildRuntime(mock(Hash256.class));

        assertEquals(runtime, syncedState.getRuntime());
    }
}