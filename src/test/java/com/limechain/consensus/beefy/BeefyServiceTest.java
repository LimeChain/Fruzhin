package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.RoundAction;
import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeefyServiceTest {

    static final BigInteger MANDATORY_BLOCK_NUM = BigInteger.TWO;
    static final BigInteger GRANDPA_FINALIZED = BigInteger.TEN;
    public static final BigInteger BEEFY_FINALIZED = BigInteger.ONE;

    private BeefyService beefyService;

    @Mock
    private StateManager stateManager;

    @Mock
    private KeyStore keyStore;

    @BeforeEach
    void setUp() {
        beefyService = Mockito.spy(new BeefyService(stateManager, keyStore));
    }

    @Test
    @Disabled
    void testTriageIncomingJustificationWithEmptySessions() {
        SignedCommitment signedCommitment = mock(SignedCommitment.class);
        Commitment commitment = mock(Commitment.class);
        BeefyState mockBeefyState = mock(BeefyState.class);
        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);

        when(signedCommitment.getCommitment()).thenReturn(commitment);
        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TWO);

        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
        when(mockBeefyState.getSessions().peekFirst()).thenReturn(null);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> triageIncomingJustification(signedCommitment));

        assertInstanceOf(BeefyGenericException.class, thrown.getTargetException());
    }

    @Test
    @Disabled
    void testTriageIncomingJustificationWithNoBeefyFinalized() {
        SignedCommitment signedCommitment = mock(SignedCommitment.class);
        Commitment commitment = mock(Commitment.class);
        BeefyState mockBeefyState = mock(BeefyState.class);
        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);
        BeefySession mockedBeefySession1 = mock(BeefySession.class);
        mockedBeefySessions.add(mockedBeefySession1);

        when(signedCommitment.getCommitment()).thenReturn(commitment);
        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TWO);

        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
        when(mockBeefyState.getSessions().peekFirst()).thenReturn(mockedBeefySession1);
        when(mockBeefyState.getBeefyFinalized()).thenReturn(null);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> triageIncomingJustification(signedCommitment));

        assertInstanceOf(BeefyGenericException.class, thrown.getTargetException());
    }

    @Test
    void testTriageIncomingJustificationWithMandatoryBlockNotFinalizedAndRoundBetweenStartAndEndRound()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        SignedCommitment signedCommitment = mock(SignedCommitment.class);
        Commitment commitment = mock(Commitment.class);
        BeefyState mockBeefyState = mock(BeefyState.class);
        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);
        BeefySession mockedBeefySession1 = mock(BeefySession.class);
        mockedBeefySessions.add(mockedBeefySession1);

        when(signedCommitment.getCommitment()).thenReturn(commitment);
        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TWO);

        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
        when(mockBeefyState.getSessions().peekFirst()).thenReturn(mockedBeefySession1);
        when(mockBeefyState.getBeefyFinalized()).thenReturn(BEEFY_FINALIZED);
        when(mockedBeefySession1.isMandatoryBlockFinalized()).thenReturn(false);
        when(mockedBeefySession1.getMandatoryBlock()).thenReturn(MANDATORY_BLOCK_NUM);

        triageIncomingJustification(signedCommitment);
    }

    @Test
    void testTriageIncomingJustificationWithMandatoryBlockNotFinalizedWithRoundInFuture()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        SignedCommitment signedCommitment = mock(SignedCommitment.class);
        Commitment commitment = mock(Commitment.class);
        BeefyState mockBeefyState = mock(BeefyState.class);
        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);
        BeefySession mockedBeefySession1 = mock(BeefySession.class);
        mockedBeefySessions.add(mockedBeefySession1);

        when(signedCommitment.getCommitment()).thenReturn(commitment);
        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TEN);

        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
        when(mockBeefyState.getSessions().peekFirst()).thenReturn(mockedBeefySession1);
        when(mockBeefyState.getBeefyFinalized()).thenReturn(BEEFY_FINALIZED);
        when(mockedBeefySession1.isMandatoryBlockFinalized()).thenReturn(false);
        when(mockedBeefySession1.getMandatoryBlock()).thenReturn(MANDATORY_BLOCK_NUM);

        triageIncomingJustification(signedCommitment);
    }

    @Test
    void testCreateVoteMessageIfAuthorized()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        BigInteger authoritySetId = BigInteger.ONE;
        BigInteger targetVoteBlockNumber = BigInteger.TWO;

        Pair<PrivKey, PubKey> keyPair = EcdsaUtils.generateKeyPair();
        BeefyAuthoritySet authoritySet = new BeefyAuthoritySet(List.of(keyPair.component2().bytes()), authoritySetId);
        org.javatuples.Pair<byte[], byte[]> resultKeyPair = new org.javatuples.Pair<>(
                keyPair.component2().raw(),
                keyPair.component1().raw()
        );

        BlockState blockState = mock(BlockState.class);

        HeaderDigest beefyDigest = new HeaderDigest();
        beefyDigest.setType(DigestType.CONSENSUS_MESSAGE);
        beefyDigest.setId(ConsensusEngine.BEEFY);
        // Adding MMR message
        beefyDigest.setMessage(new byte[]{
                3, 94, -64, -78, -126, -46, 119, 76, 75, 107, 114, 113, -71, -79, 14, 95,
                -84, 6, 45, 115, 47, -57, 32, -66, -17, -86, -7, 41, -54, -127, 0, 32, 6
        });

        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setDigest(new HeaderDigest[]{beefyDigest});

        when(keyStore.findKeyPair(authoritySet.getPublicKeys(), KeyType.BEEFY))
                .thenReturn(Optional.of(resultKeyPair));
        when(stateManager.getBlockState()).thenReturn(blockState);
        when(blockState.getHeaderByNumber(targetVoteBlockNumber))
                .thenReturn(blockHeader);

        VoteMessage voteMessage = callCreateVoteMessageIfAuthorized(authoritySet, targetVoteBlockNumber);
        Commitment commitment = voteMessage.getCommitment();

        assertEquals(targetVoteBlockNumber, commitment.getBlockNumber());
        assertEquals(authoritySetId, commitment.getAuthoritySetId());
        assertEquals(BeefyPayloadId.MMR, commitment.getPayload().get(0).getPayloadId());

        byte[] encodedCommitment = ScaleUtils.Encode.encode(CommitmentScaleWriter.getInstance(), commitment);
        byte[] hashedCommitment = HashUtils.hashWithKeccak256(encodedCommitment);

        VerifySignature signature = new VerifySignature(
                voteMessage.getSignature(),
                hashedCommitment,
                keyPair.component2().raw(),
                Key.ECDSA
        );

        assertTrue(EcdsaUtils.verifySignature(signature));
    }

    private VoteMessage callCreateVoteMessageIfAuthorized(BeefyAuthoritySet authoritySet,
                                                          BigInteger targetVoteBlockNumber)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = BeefyService.class.getDeclaredMethod(
                "createVoteMessageIfAuthorized",
                BeefyAuthoritySet.class,
                BigInteger.class
        );
        method.setAccessible(true);
        return (VoteMessage) method.invoke(beefyService, authoritySet, targetVoteBlockNumber);
    }

    private RoundAction triageIncomingJustification(SignedCommitment signedCommitment)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method triageIncomingJustification = BeefyService.class.getDeclaredMethod(
                "triageIncomingJustification",
                SignedCommitment.class
        );
        triageIncomingJustification.setAccessible(true);
        return (RoundAction) triageIncomingJustification.invoke(beefyService, signedCommitment);
    }
}
