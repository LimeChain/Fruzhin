package com.limechain.consensus.grandpa;

import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.exception.grandpa.GrandpaJustificationException;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.protocol.grandpa.GrandpaMessageHandler;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Justification;
import com.limechain.rpc.server.AppBean;
import com.limechain.state.AbstractState;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.sync.state.SyncState;
import com.limechain.utils.async.AsyncExecutor;
import io.emeraldpay.polkaj.types.Hash256;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrandpaServiceTest {

    private final Hash256 HASH_0 = new Hash256(generateHash(0));
    private final Hash256 HASH_1 = new Hash256(generateHash(1));
    private final Hash256 HASH_2 = new Hash256(generateHash(2));
    private final BigInteger BLOCK_1_NUM = BigInteger.ONE;
    private final BigInteger BLOCK_2_NUM = BigInteger.TWO;
    private final Authority authority1 = new Authority(generateHash(1), BigInteger.ONE);
    private final Authority authority2 = new Authority(generateHash(2), BigInteger.TEN);
    private final GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet(
            BigInteger.ZERO,
            List.of(authority1, authority2)
    );

    @Mock
    private StateManager stateManager;

    @Mock
    private BlockState blockState;
    @Mock
    private SyncState syncState;

    @Mock
    private GrandpaSetState grandpaSetState;

    @Mock
    private GrandpaRound currentRound;

    @Mock
    private BlockHeader blockHeader;

    private GrandpaService grandpaService;

    @BeforeEach
    void setUp() {
        grandpaService = spy(new GrandpaService(stateManager));
        when(stateManager.getGrandpaSetState()).thenReturn(grandpaSetState);
    }

    @Test
    void testStartWithNoJustification() {
        try (MockedStatic<AsyncExecutor> asyncMock = mockStatic(AsyncExecutor.class);
             MockedStatic<AbstractState> abstractStateMock = mockStatic(AbstractState.class)) {

            AsyncExecutor mockExecutor = mock(AsyncExecutor.class);
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(mockExecutor).executeAndForget(any(Runnable.class));
            asyncMock.when(AsyncExecutor::withSingleThread).thenReturn(mockExecutor);

            LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
            when(blockHeader.getBlockNumber()).thenReturn(BLOCK_2_NUM);
            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);
            when(stateManager.getSyncState()).thenReturn(syncState);
            when(syncState.getGenesisBlockHash()).thenReturn(HASH_0);
            when(grandpaSetState.getCurrentGrandpaRound()).thenReturn(currentRound);
            when(currentRound.getRoundNumber()).thenReturn(BigInteger.ONE);
            abstractStateMock.when(AbstractState::isActiveAuthority).thenReturn(true);

            grandpaService.start();

            verify(blockState).getHighestFinalizedHeader();
            verify(grandpaSetState, times(2)).getCurrentGrandpaRound();
            verify(blockState).getJustification(any());
        }
    }

    @Test
    void testStartWithNoAuthoritySetFound() {
        try (MockedStatic<AsyncExecutor> asyncMock = mockStatic(AsyncExecutor.class)) {
            AsyncExecutor mockExecutor = mock(AsyncExecutor.class);
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(mockExecutor).executeAndForget(any(Runnable.class));

            asyncMock.when(AsyncExecutor::withSingleThread).thenReturn(mockExecutor);

            GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
            authoritySet.setSetId(BigInteger.ONE);
            authoritySet.setAuthorities(List.of(authority1, authority2));
            LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = new LinkedHashMap<>();

            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
            when(blockHeader.getBlockNumber()).thenReturn(BLOCK_2_NUM);
            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);

            grandpaService.start();

            verify(blockState, never()).getJustification(any());
            verify(grandpaSetState, never()).getCurrentGrandpaRound();
        }
    }

    @Test
    void testStartWithJustification() {
        try (MockedStatic<AsyncExecutor> asyncMock = mockStatic(AsyncExecutor.class);
             MockedStatic<AbstractState> abstractStateMock = mockStatic(AbstractState.class);
             MockedStatic<AppBean> mockedAppBean = mockStatic(AppBean.class)) {

            AsyncExecutor mockExecutor = mock(AsyncExecutor.class);
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(mockExecutor).executeAndForget(any(Runnable.class));
            asyncMock.when(AsyncExecutor::withSingleThread).thenReturn(mockExecutor);

            Justification mockJustification = mock(Justification.class);

            GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
            authoritySet.setSetId(BigInteger.ZERO);
            authoritySet.setAuthorities(List.of(authority1, authority2));
            LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
            when(blockHeader.getBlockNumber()).thenReturn(BigInteger.TEN);
            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);
            when(stateManager.getSyncState()).thenReturn(syncState);
            when(syncState.getGenesisBlockHash()).thenReturn(HASH_0);
            when(grandpaSetState.getCurrentGrandpaRound()).thenReturn(currentRound);
            when(currentRound.getRoundNumber()).thenReturn(BigInteger.ONE);
            when(blockHeader.getHash()).thenReturn(HASH_0);
            when(blockState.getJustification(any(Hash256.class))).thenReturn(Optional.of(mockJustification));
            abstractStateMock.when(AbstractState::isActiveAuthority).thenReturn(true);
            when(mockJustification.getRoundNumber()).thenReturn(BigInteger.TEN);
            when(grandpaSetState.getThreshold(anyList())).thenReturn(BigInteger.TEN);

            mockGrandpaRoundRequiredObjects(mockedAppBean);

            grandpaService.start();

            verify(blockState).getHighestFinalizedHeader();
            verify(grandpaSetState, times(3)).getCurrentGrandpaRound();
            verify(blockState).getJustification(any());

            ArgumentCaptor<GrandpaRound> roundCaptor = ArgumentCaptor.forClass(GrandpaRound.class);
            verify(grandpaSetState).addNewGrandpaRound(roundCaptor.capture());

            GrandpaRound capturedRound = roundCaptor.getValue();
            assertNotNull(capturedRound);
            assertEquals(BigInteger.valueOf(11), capturedRound.getRoundNumber());
            assertEquals(authoritySet.getSetId(), capturedRound.getAuthoritySet().getSetId());
            assertEquals(authoritySet.getAuthorities(), capturedRound.getAuthoritySet().getAuthorities());
        }
    }

    @Test
    void testGetAuthoritiesForBlock() {
        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.ZERO);
        authoritySet.setAuthorities(List.of(authority1, authority2));
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

        when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);
        when(stateManager.getSyncState()).thenReturn(syncState);
        when(syncState.getGenesisBlockHash()).thenReturn(HASH_0);

        Optional<GrandpaAuthoritySet> result = grandpaService.getAuthoritiesForBlock(BLOCK_1_NUM);

        assertTrue(result.isPresent());
        assertThat(authoritySet).usingRecursiveComparison().isEqualTo(result.get());
    }

    @Test
    void testTryStartFromPreviousRound() {
        try (MockedStatic<AbstractState> mockedState = mockStatic(AbstractState.class);
             MockedStatic<AppBean> mockedAppBean = mockStatic(AppBean.class)) {

            GrandpaRound prevRound = mock(GrandpaRound.class);

            GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
            authoritySet.setSetId(BigInteger.ZERO);
            authoritySet.setAuthorities(List.of(authority1, authority2));
            LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

            when(prevRound.getRoundNumber()).thenReturn(BigInteger.ONE);
            when(grandpaSetState.getCurrentGrandpaRound()).thenReturn(prevRound);
            when(prevRound.getFinalizedBlock()).thenReturn(blockHeader);
            when(prevRound.getFinalizedBlock().getBlockNumber()).thenReturn(BLOCK_1_NUM);
            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);
            when(stateManager.getSyncState()).thenReturn(syncState);
            when(syncState.getGenesisBlockHash()).thenReturn(HASH_0);
            when(prevRound.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getThreshold(anyList())).thenReturn(BigInteger.TWO);
            mockedState.when(AbstractState::getGrandpaKeyPair)
                    .thenReturn(new Pair<>(generateHash(2), generateHash(1)));
            when(grandpaSetState.derivePrimary(any(BigInteger.class))).thenReturn(BigInteger.ONE);

            mockGrandpaRoundRequiredObjects(mockedAppBean);

            grandpaService.tryStartFromPreviousRound(prevRound);

            ArgumentCaptor<GrandpaRound> roundCaptor = ArgumentCaptor.forClass(GrandpaRound.class);
            verify(grandpaSetState).addNewGrandpaRound(roundCaptor.capture());

            GrandpaRound nextRound = roundCaptor.getValue();
            assertNotNull(nextRound);
            assertEquals(prevRound.getRoundNumber().add(BigInteger.ONE), nextRound.getRoundNumber());
            assertThat(prevRound.getAuthoritySet()).usingRecursiveComparison().isEqualTo(nextRound.getAuthoritySet());
        }
    }

    @Test
    void testTryStartFromPreviousRoundWithNoAuthoritySet() {
        GrandpaRound prevRound = mock(GrandpaRound.class);

        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.ONE);
        authoritySet.setAuthorities(List.of(authority1, authority2));
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = new LinkedHashMap<>();

        when(grandpaSetState.getCurrentGrandpaRound()).thenReturn(prevRound);
        when(prevRound.getFinalizedBlock()).thenReturn(blockHeader);
        when(prevRound.getFinalizedBlock().getBlockNumber()).thenReturn(BLOCK_1_NUM);
        when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);

        grandpaService.tryStartFromPreviousRound(prevRound);

        verify(grandpaSetState, never()).addNewGrandpaRound(any());
    }

    @Test
    void testFinalizeJustificationWithNoAuthoritySet() {
        Justification justification = new Justification();
        justification.setTargetBlock(BigInteger.ONE);
        justification.setRoundNumber(BigInteger.ONE);

        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.ONE);
        authoritySet.setAuthorities(List.of(authority1, authority2));
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = new LinkedHashMap<>();

        when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);

        assertThrows(GrandpaJustificationException.class, () -> grandpaService.finalizeJustification(justification));
    }

    @Test
    void testFinalizeJustificationWithJustificationForPastBlock() {
        Justification justification = new Justification();
        justification.setTargetBlock(BigInteger.ONE);
        justification.setRoundNumber(BigInteger.ONE);

        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.ZERO);
        authoritySet.setAuthorities(List.of(authority1, authority2));

        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

        when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);
        when(stateManager.getSyncState()).thenReturn(syncState);
        when(syncState.getGenesisBlockHash()).thenReturn(HASH_0);
        when(grandpaSetState.getGrandpaRound(any(BigInteger.class))).thenReturn(null);
        when(stateManager.getBlockState()).thenReturn(blockState);
        when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
        when(blockHeader.getBlockNumber()).thenReturn(BLOCK_2_NUM);

        assertThrows(GrandpaJustificationException.class, () -> grandpaService.finalizeJustification(justification));
    }

    @Test
    void testFinalizeJustificationWithNonExistingPreviousRoundAndPastSetJustification() {
        Justification justification = new Justification();
        justification.setTargetBlock(BigInteger.TEN);
        justification.setRoundNumber(BigInteger.ONE);

        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.TWO);
        authoritySet.setAuthorities(List.of(authority1, authority2));
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

        GrandpaAuthoritySet currAuthSet = new GrandpaAuthoritySet(BigInteger.TEN, List.of());

        when(grandpaSetState.getAuthoritySet()).thenReturn(currAuthSet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);
        when(grandpaSetState.getGrandpaRound(any(BigInteger.class))).thenReturn(null);
        when(stateManager.getBlockState()).thenReturn(blockState);
        when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
        when(blockHeader.getBlockNumber()).thenReturn(BigInteger.TEN);
        when(blockState.getHeader(justification.getTargetHash())).thenReturn(blockHeader);

        assertThrows(GrandpaJustificationException.class, () -> grandpaService.finalizeJustification(justification));
    }

    @Test
    void testFinalizeJustificationWithNonExistingPreviousRoundAndPastRound() {
        Justification justification = new Justification();
        justification.setTargetBlock(BigInteger.TEN);
        justification.setRoundNumber(BigInteger.ONE);

        GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
        authoritySet.setSetId(BigInteger.TWO);
        authoritySet.setAuthorities(List.of(authority1, authority2));
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

        when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
        when(grandpaSetState.getSetChanges()).thenReturn(changes);
        when(grandpaSetState.getGrandpaRound(any(BigInteger.class))).thenReturn(null);
        when(stateManager.getBlockState()).thenReturn(blockState);
        when(blockState.getHighestFinalizedHeader()).thenReturn(blockHeader);
        when(blockHeader.getBlockNumber()).thenReturn(BigInteger.TEN);
        when(blockState.getHeader(justification.getTargetHash())).thenReturn(blockHeader);
        when(blockState.getHighestRoundAndSetID()).thenReturn(Pair.with(BigInteger.TWO, BigInteger.TWO));

        assertThrows(GrandpaJustificationException.class, () -> grandpaService.finalizeJustification(justification));
    }

    @Test
    void testFinalizeJustificationWithNoJustificationRoundAndHavingPreviousRound() throws Exception {
        try (MockedStatic<AppBean> mockedAppBean = mockStatic(AppBean.class)) {
            Justification justification = new Justification();
            justification.setTargetBlock(BigInteger.TEN);
            justification.setRoundNumber(BigInteger.TEN);

            BlockHeader highestFinalizedHeader = new BlockHeader();
            highestFinalizedHeader.setBlockNumber(BigInteger.TEN);

            GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
            authoritySet.setSetId(BigInteger.ONE);
            authoritySet.setAuthorities(List.of(authority1, authority2));
            LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = getSetChanges(authoritySet);

            when(stateManager.getGrandpaSetState()).thenReturn(grandpaSetState);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHighestFinalizedHeader()).thenReturn(highestFinalizedHeader);

            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);
            when(grandpaSetState.getThreshold(anyList())).thenReturn(BigInteger.TEN);
            when(grandpaSetState.getCurrentGrandpaRound()).thenReturn(null);
            mockGrandpaRoundRequiredObjects(mockedAppBean);

            doReturn(currentRound).when(grandpaService).initRoundFromJustification(
                    eq(justification), eq(highestFinalizedHeader), eq(authoritySet)
            );
            when(currentRound.getRoundNumber()).thenReturn(BigInteger.TWO);
            when(currentRound.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getAuthoritySet()).thenReturn(authoritySet);
            when(grandpaSetState.getSetChanges()).thenReturn(changes);
            when(currentRound.getFinalizedBlock()).thenReturn(blockHeader);
            when(blockHeader.getBlockNumber()).thenReturn(BLOCK_2_NUM);

            ArgumentCaptor<GrandpaRound> roundCaptor = ArgumentCaptor.forClass(GrandpaRound.class);
            doNothing().when(grandpaSetState).addNewGrandpaRound(roundCaptor.capture());

            grandpaService.finalizeJustification(justification);

            verify(currentRound).finalizeJustification(justification);
            verify(grandpaSetState).addNewGrandpaRound(currentRound);

            GrandpaRound capturedRound = roundCaptor.getValue();

            assertEquals(BigInteger.valueOf(3), capturedRound.getRoundNumber());
            assertEquals(BigInteger.TEN, capturedRound.getThreshold());
            assertEquals(authoritySet.getSetId(), capturedRound.getAuthoritySet().getSetId());
            assertEquals(authoritySet.getAuthorities(), capturedRound.getAuthoritySet().getAuthorities());
        }
    }

    private static void mockGrandpaRoundRequiredObjects(MockedStatic<AppBean> mockedAppBean) {
        mockedAppBean.when(() -> AppBean.getBean(StateManager.class)).thenReturn(mock(StateManager.class));
        mockedAppBean.when(() -> AppBean.getBean(BeefyService.class)).thenReturn(mock(BeefyService.class));
        mockedAppBean.when(() -> AppBean.getBean(PeerMessageCoordinator.class))
                .thenReturn(mock(PeerMessageCoordinator.class));
        mockedAppBean.when(() -> AppBean.getBean(GrandpaMessageHandler.class))
                .thenReturn(mock(GrandpaMessageHandler.class));
    }

    private byte[] generateHash(int seed) {
        byte[] arr = new byte[32];
        Arrays.fill(arr, (byte) seed);
        return arr;
    }

    @NotNull
    private LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> getSetChanges(GrandpaAuthoritySet authoritySet) {
        LinkedHashMap<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> changes = new LinkedHashMap<>();
        changes.put(Pair.with(HASH_1, BLOCK_1_NUM), authoritySet);
        changes.put(Pair.with(HASH_2, BLOCK_2_NUM), authoritySet);
        return changes;
    }
}