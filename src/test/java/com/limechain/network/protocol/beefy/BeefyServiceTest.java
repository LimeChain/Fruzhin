package com.limechain.network.protocol.beefy;

import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.beefy.BeefyState;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.DoubleVotingProof;
import com.limechain.consensus.beefy.dto.VoteImportResult;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessageFormat;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.utils.EcdsaUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BeefyServiceTest {

    private final BigInteger BLOCK_1_NUM = BigInteger.ONE;
    private final BigInteger BLOCK_2_NUM = BigInteger.TWO;
    private final BigInteger BLOCK_10_NUM = BigInteger.TEN;

    private final byte[] BYTE_ARRAY_2 = generateHash(2);
    private final byte[] BYTE_ARRAY_3 = generateHash(3);

    private final Hash256 HASH_1 = new Hash256(generateHash(1));

    private final Commitment commitment = new Commitment(new ArrayList<>(), BigInteger.ONE, BigInteger.valueOf(3));
    private final SignedCommitment signedCommitment = new SignedCommitment(
            commitment,
            List.of(Optional.of(BYTE_ARRAY_2))
    );

    private final BeefyVoteMessage beefyVoteMessage = new BeefyVoteMessage(commitment, BYTE_ARRAY_2, BYTE_ARRAY_3);
    private final BeefyAuthoritySet authoritySet = new BeefyAuthoritySet(new LinkedList<>(), BigInteger.ONE);

    private final Pair<byte[], byte[]> keyPair = Pair.with(BYTE_ARRAY_2, BYTE_ARRAY_3);

    private BeefyConsensusMessage beefyConsensusMessage;
    private LinkedList<BeefySession> sessions;

    @Mock
    private StateManager stateManager;

    @Mock
    private PeerMessageCoordinator peerMessageCoordinator;

    @Mock
    private BeefyState beefyState;

    @Mock
    private BlockState blockState;

    @Mock
    private BeefySession session;

    @Mock
    private BlockHeader blockHeader;

    private BeefyService beefyService;

    @BeforeEach
    void setUp() {
        beefyService = new BeefyService(stateManager, peerMessageCoordinator);
        sessions = new LinkedList<>();
        sessions.add(session);
        beefyConsensusMessage = getBeefyConsensusMessage();
        when(stateManager.getBeefyState()).thenReturn(beefyState);
    }

    @Test
    void testVoteWhenNoSessionExists() {
        when(beefyState.getSessions()).thenReturn(new LinkedList<>());

        beefyService.vote();
        verify(beefyState, never()).getTargetVoteBlockNumber();
    }

    @Test
    void testVoteWhenNoKeyPairExists() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_10_NUM);
        when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_2_NUM);
        when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_1_NUM);

        beefyService.vote();

        verify(sessions.peekFirst(), never()).getAuthoritySet();
    }

    @Test
    void testVoteWhenShouldSkipVoteAndMandatoryBlockWithNoJustification() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_10_NUM);
        when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_2_NUM);
        when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_1_NUM);
        when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.ONE);
        when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);

        beefyService.vote();

        ArgumentCaptor<BigInteger> captor = ArgumentCaptor.forClass(BigInteger.class);
        verify(beefyState).setTargetVoteBlockNumber(captor.capture());

        BigInteger capturedValue = captor.getValue();
        assertNotNull(capturedValue);
        assertEquals(BigInteger.TEN, capturedValue);
        verify(sessions.peekFirst(), never()).getBeefyKeyPair();
    }

    @Test
    void testVoteWhenShouldSkipVoteAndMandatoryBlockWithJustification() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_2_NUM);
        when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
        when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.ONE);
        when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);

        beefyService.vote();

        ArgumentCaptor<BigInteger> captor = ArgumentCaptor.forClass(BigInteger.class);
        verify(beefyState).setTargetVoteBlockNumber(captor.capture());

        BigInteger capturedValue = captor.getValue();
        assertNotNull(capturedValue);
        assertEquals(BigInteger.valueOf(18), capturedValue);
        verify(sessions.peekFirst(), never()).getBeefyKeyPair();
    }

    @Test
    void testVoteFailedToExtractMmrRootHash() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of());

            assertThrows(BeefyGenericException.class, () -> beefyService.vote());

            verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
            verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());
        }
    }

    @Test
    void testVoteFailedToGenerateSignature() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(null);

            assertThrows(BeefyGenericException.class, () -> beefyService.vote());
            verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
            verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());
        }
    }

    @Test
    void testVoteWithNonExistingBeefySession() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            when(beefyState.getSessions()).thenReturn(sessions).thenReturn(sessions).thenReturn(new LinkedList<>());
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(BYTE_ARRAY_2);

            assertThrows(BeefyGenericException.class, () -> beefyService.vote());
        }
    }

    @Test
    void testVoteWhenDoubleVoting() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            VoteImportResult.DoubleVoting mockedVoteImportResult = mock(VoteImportResult.DoubleVoting.class);
            com.limechain.runtime.Runtime mockedRuntime = mock(Runtime.class);

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(BYTE_ARRAY_2);
            when(session.addVote(any(BeefyVoteMessage.class))).thenReturn(mockedVoteImportResult);
            when(mockedVoteImportResult.doubleVotingProof())
                    .thenReturn(new DoubleVotingProof(beefyVoteMessage, beefyVoteMessage));
            when(blockState.getRuntime(any(Hash256.class))).thenReturn(mockedRuntime);
            when(blockState.getHighestFinalizedHash()).thenReturn(HASH_1);

            beefyService.vote();

            ArgumentCaptor<BeefyVoteMessage> voteCaptor = ArgumentCaptor.forClass(BeefyVoteMessage.class);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(voteCaptor.capture());

            BeefyVoteMessage captured = voteCaptor.getValue();
            assertNotNull(captured);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(captured);
        }
    }

    @Test
    void testVoteWhenRoundConcluded() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            VoteImportResult.RoundConcluded mockedVoteImportResult = mock(VoteImportResult.RoundConcluded.class);

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(BYTE_ARRAY_2);
            when(session.addVote(any(BeefyVoteMessage.class))).thenReturn(mockedVoteImportResult);
            when(mockedVoteImportResult.signedCommitment()).thenReturn(signedCommitment);

            beefyService.vote();

            ArgumentCaptor<SignedCommitment> commitmentCaptor = ArgumentCaptor.forClass(SignedCommitment.class);
            verify(peerMessageCoordinator).sendSignedCommitmentToPeers(commitmentCaptor.capture());

            SignedCommitment capturedCommitment = commitmentCaptor.getValue();
            assertNotNull(capturedCommitment);
            verify(peerMessageCoordinator).sendSignedCommitmentToPeers(capturedCommitment);
        }
    }

    @Test
    void testVoteWhenOk() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            VoteImportResult.Ok mockedVoteImportResult = mock(VoteImportResult.Ok.class);

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(BYTE_ARRAY_2);
            when(session.addVote(any(BeefyVoteMessage.class))).thenReturn(mockedVoteImportResult);

            beefyService.vote();

            ArgumentCaptor<BeefyVoteMessage> voteCaptor = ArgumentCaptor.forClass(BeefyVoteMessage.class);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(voteCaptor.capture());

            BeefyVoteMessage captured = voteCaptor.getValue();
            assertNotNull(captured);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(captured);
        }
    }

    @Test
    void testVoteWhenInvalid() {
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class);
             MockedStatic<EcdsaUtils> ecdsaUtilsMock = mockStatic(EcdsaUtils.class)) {

            VoteImportResult.Invalid mockedVoteImportResult = mock(VoteImportResult.Invalid.class);

            when(beefyState.getSessions()).thenReturn(sessions);
            when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
            when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_10_NUM);
            when(beefyState.getTargetVoteBlockNumber()).thenReturn(BigInteger.valueOf(3));
            when(beefyState.getLastVoted()).thenReturn(BigInteger.TWO);
            when(session.getBeefyKeyPair()).thenReturn(keyPair);
            when(session.getAuthoritySet()).thenReturn(authoritySet);
            when(stateManager.getBlockState()).thenReturn(blockState);
            when(blockState.getHeaderByNumber(any(BigInteger.class))).thenReturn(blockHeader);
            digestHelperMock.when(() -> DigestHelper.getBeefyConsensusMessages(any()))
                    .thenReturn(List.of(beefyConsensusMessage));
            ecdsaUtilsMock.when(() -> EcdsaUtils.signMessage(any(), any())).thenReturn(BYTE_ARRAY_2);
            when(session.addVote(any(BeefyVoteMessage.class))).thenReturn(mockedVoteImportResult);

            beefyService.vote();

            ArgumentCaptor<BeefyVoteMessage> voteCaptor = ArgumentCaptor.forClass(BeefyVoteMessage.class);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(voteCaptor.capture());

            BeefyVoteMessage captured = voteCaptor.getValue();
            assertNotNull(captured);
            verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(captured);
        }
    }

    @Test
    void testTriageIncomingVoteWithInvalidRoundAction() {
        LinkedList<BeefySession> sessions = new LinkedList<>();

        when(beefyState.getSessions()).thenReturn(sessions);
        beefyService.triageIncomingVote(beefyVoteMessage);

        verify(stateManager, times(1)).getBeefyState();
    }

    @Test
    void testTriageIncomingVoteWithDropRoundAction() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_2_NUM);

        beefyService.triageIncomingVote(beefyVoteMessage);

        verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
        verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());
    }

    @Test
    void testTriageIncomingVoteWithEnqueueRoundAction() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BigInteger.ZERO);

        beefyService.triageIncomingVote(beefyVoteMessage);

        verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
        verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());
    }

    @Test
    void testTriageIncomingVoteWithProcessRoundAction() {
        VoteImportResult.Ok mockedVoteImportResult = mock(VoteImportResult.Ok.class);

        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(session.addVote(any(BeefyVoteMessage.class))).thenReturn(mockedVoteImportResult);

        beefyService.triageIncomingVote(beefyVoteMessage);

        ArgumentCaptor<BeefyVoteMessage> voteCaptor = ArgumentCaptor.forClass(BeefyVoteMessage.class);
        verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(voteCaptor.capture());

        BeefyVoteMessage captured = voteCaptor.getValue();
        assertNotNull(captured);
        verify(peerMessageCoordinator).sendBeefyVoteMessageToPeers(captured);
    }

    @Test
    void testTriageIncomingJustificationWithInvalidRoundAction() {
        when(beefyState.getSessions()).thenReturn(new LinkedList<>());
        beefyService.triageIncomingJustification(signedCommitment);

        verify(stateManager, times(1)).getBeefyState();
    }

    @Test
    void testTriageIncomingJustificationWithDropRoundAction() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_2_NUM);

        beefyService.triageIncomingJustification(signedCommitment);

        verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
        verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());
    }

    @Test
    void testTriageIncomingJustificationWithEnqueueRoundAction() {
        SignedCommitment signedCommitment = new SignedCommitment(
                new Commitment(new ArrayList<>(), BigInteger.TEN, BigInteger.valueOf(3)),
                List.of(Optional.of(new byte[]{4}))
        );
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BigInteger.valueOf(5));
        when(session.isMandatoryBlockFinalized()).thenReturn(true);
        when(beefyState.getBeefyFinalized()).thenReturn(BLOCK_1_NUM);
        when(beefyState.getGrandpaFinalized()).thenReturn(BLOCK_2_NUM);

        beefyService.triageIncomingJustification(signedCommitment);

        verify(peerMessageCoordinator, never()).sendSignedCommitmentToPeers(any());
        verify(peerMessageCoordinator, never()).sendBeefyVoteMessageToPeers(any());

        verify(beefyState, times(1)).getPendingJustifications();
    }

    @Test
    void testTriageIncomingJustificationWithProcessRoundActionAndNoBeefySession() {
        when(beefyState.getSessions()).thenReturn(sessions)
                .thenReturn(sessions).thenReturn(sessions).thenReturn(new LinkedList<>());
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(beefyState.getBeefyFinalized()).thenReturn(BigInteger.ZERO);

        beefyService.triageIncomingJustification(signedCommitment);

        verify(beefyState, never()).setBeefyFinalized(any(BigInteger.class));
    }

    @Test
    void testTriageIncomingJustificationWithProcessRoundAction() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(beefyState.getBeefyFinalized()).thenReturn(BigInteger.ZERO);

        beefyService.triageIncomingJustification(signedCommitment);

        ArgumentCaptor<BigInteger> blockNumberCaptor = ArgumentCaptor.forClass(BigInteger.class);
        verify(beefyState).setBeefyFinalized(blockNumberCaptor.capture());

        BigInteger captured = blockNumberCaptor.getValue();
        assertNotNull(captured);
        assertEquals(BLOCK_1_NUM, captured);
    }

    @Test
    void testIsBeefyMessageAcceptableWhenEarlierBlockCandidate() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_2_NUM);

        assertFalse(beefyService.isBeefyMessageAcceptable(commitment));
    }

    @Test
    void testIsBeefyMessageAcceptableWhenSetIdMismatch() {
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(session.getAuthoritySet()).thenReturn(authoritySet);

        assertFalse(beefyService.isBeefyMessageAcceptable(commitment));
    }

    @Test
    void testIsBeefyMessageAcceptableWhenBlockIsOutsideAcceptedRange() {
        Commitment commitment = new Commitment(new ArrayList<>(), BigInteger.TWO, BigInteger.valueOf(1));
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(session.getAuthoritySet()).thenReturn(authoritySet);

        assertFalse(beefyService.isBeefyMessageAcceptable(commitment));
    }

    @Test
    void testIsBeefyMessageAcceptableWhenOK() {
        Commitment commitment = new Commitment(new ArrayList<>(), BigInteger.ONE, BigInteger.valueOf(1));
        when(beefyState.getSessions()).thenReturn(sessions);
        when(session.getMandatoryBlock()).thenReturn(BLOCK_1_NUM);
        when(session.getAuthoritySet()).thenReturn(authoritySet);

        assertTrue(beefyService.isBeefyMessageAcceptable(commitment));
    }

    private byte[] generateHash(int seed) {
        byte[] arr = new byte[32];
        Arrays.fill(arr, (byte) seed);
        return arr;
    }

    @NotNull
    private BeefyConsensusMessage getBeefyConsensusMessage() {
        BeefyConsensusMessage mockedConsensusMessage = new BeefyConsensusMessage();
        mockedConsensusMessage.setFormat(BeefyConsensusMessageFormat.BEEFY_MMR_ROOT);
        mockedConsensusMessage.setMmrRootHash(BYTE_ARRAY_3);
        return mockedConsensusMessage;
    }
}
