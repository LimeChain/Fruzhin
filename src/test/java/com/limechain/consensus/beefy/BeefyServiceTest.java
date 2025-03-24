package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.RoundAction;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayDeque;

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

    @BeforeEach
    void setUp() {
        beefyService = Mockito.spy(new BeefyService(stateManager));
    }

//    @Test
//    void testTriageIncomingJustificationWithEmptySessions() {
//        SignedCommitment signedCommitment = mock(SignedCommitment.class);
//        Commitment commitment = mock(Commitment.class);
//        BeefyState mockBeefyState = mock(BeefyState.class);
//        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);
//
//        when(signedCommitment.getCommitment()).thenReturn(commitment);
//        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TWO);
//
//        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
//        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
//        when(mockBeefyState.getSessions().peekFirst()).thenReturn(null);
//
//        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
//                () -> triageIncomingJustification(signedCommitment));
//
//        assertInstanceOf(BeefyGenericException.class, thrown.getTargetException());
//    }
//
//    @Test
//    void testTriageIncomingJustificationWithNoBeefyFinalized() {
//        SignedCommitment signedCommitment = mock(SignedCommitment.class);
//        Commitment commitment = mock(Commitment.class);
//        BeefyState mockBeefyState = mock(BeefyState.class);
//        ArrayDeque<BeefySession> mockedBeefySessions = mock(ArrayDeque.class);
//        BeefySession mockedBeefySession1 = mock(BeefySession.class);
//        mockedBeefySessions.add(mockedBeefySession1);
//
//        when(signedCommitment.getCommitment()).thenReturn(commitment);
//        when(signedCommitment.getCommitment().getBlockNumber()).thenReturn(BigInteger.TWO);
//
//        when(stateManager.getBeefyState()).thenReturn(mockBeefyState);
//        when(mockBeefyState.getSessions()).thenReturn(mockedBeefySessions);
//        when(mockBeefyState.getSessions().peekFirst()).thenReturn(mockedBeefySession1);
//        when(mockBeefyState.getBeefyFinalized()).thenReturn(null);
//
//        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
//                () -> triageIncomingJustification(signedCommitment));
//
//        assertInstanceOf(BeefyGenericException.class, thrown.getTargetException());
//    }

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
