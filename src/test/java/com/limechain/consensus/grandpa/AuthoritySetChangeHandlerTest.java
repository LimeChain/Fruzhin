package com.limechain.consensus.grandpa;

import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.AuthoritySetChangeHandler;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.storage.forktree.ForkTree;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AuthoritySetChangeHandlerTest {

    private static final BiPredicate<Hash256, Hash256> FALSE_BIPREDICATE = (a, b) -> false;
    private static final BiPredicate<Hash256, Hash256> TRUE_BIPREDICATE = (a, b) -> true;

    private static final BigInteger DELAY_OF_ZERO = BigInteger.ZERO;
    private static final BigInteger DELAY_OF_ONE = BigInteger.ONE;
    private static final BigInteger DELAY_OF_TWO = BigInteger.TWO;

    private static final BigInteger BLOCK_NUMBER_ONE = BigInteger.ONE;
    private static final BigInteger BLOCK_NUMBER_TWO = BigInteger.TWO;
    private static final BigInteger BLOCK_NUMBER_TEN = BigInteger.TEN;

    private final Hash256 HASH_1 = new Hash256(generateHash(1));
    private final Hash256 HASH_2 = new Hash256(generateHash(2));
    private final Hash256 HASH_3 = new Hash256(generateHash(3));

    private final List<Authority> firstAuthoritySet = List.of(new Authority(new byte[]{0}, BigInteger.ONE));
    private final List<Authority> secondAuthoritySet = List.of(new Authority(new byte[]{1}, BigInteger.ONE));

    private AuthoritySetChangeHandler handler;

    @BeforeEach
    public void setup() {
        handler = new AuthoritySetChangeHandler();
    }

    @Test
    public void testAddAndApplyForcedChangeWithZeroDelay() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        assertTrue(handler.getPendingForcedChanges().contains(forcedChange));

        Optional<PendingChange> appliedForced = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_ONE,
                TRUE_BIPREDICATE
        );

        assertFalse(handler.getPendingForcedChanges().contains(forcedChange));
        assertTrue(appliedForced.isPresent());
        assertEquals(appliedForced.get().getNextAuthorities(), forcedChange.getNextAuthorities());
    }

    @Test
    public void testAddAndApplyForcedChangeWithDelayEqualOne() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ONE,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        assertTrue(handler.getPendingForcedChanges().contains(forcedChange));

        Optional<PendingChange> appliedForced = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_TWO,
                TRUE_BIPREDICATE
        );

        assertFalse(handler.getPendingForcedChanges().contains(forcedChange));
        assertTrue(appliedForced.isPresent());
        assertEquals(appliedForced.get().getNextAuthorities(), forcedChange.getNextAuthorities());
    }

    @Test
    public void testAddDuplicateForcedChange() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        assertTrue(handler.getPendingForcedChanges().contains(forcedChange));

        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(forcedChange, TRUE_BIPREDICATE));
        assertEquals(1, handler.getPendingForcedChanges().size());
    }

    @Test
    public void testAddNonDescendantForcedChange() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        assertTrue(handler.getPendingForcedChanges().contains(forcedChange));

        PendingChange secondForcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_TWO,
                DELAY_OF_ZERO,
                secondAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        assertThrows(GrandpaGenericException.class, () ->
                handler.addPendingChange(secondForcedChange, FALSE_BIPREDICATE));

        assertEquals(1, handler.getPendingForcedChanges().size());
    }

    @Test
    public void testReorderingWhenAddingSecondForcedChange() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_2,
                BLOCK_NUMBER_TEN,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        PendingChange secondForcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_TWO,
                DELAY_OF_ZERO,
                secondAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(secondForcedChange, FALSE_BIPREDICATE);

        assertEquals(secondForcedChange, handler.getPendingForcedChanges().get(0));
        assertEquals(forcedChange, handler.getPendingForcedChanges().get(1));

        assertEquals(2, handler.getPendingForcedChanges().size());
    }

    @Test
    public void testKeepingTheOrderWhenAddingSecondForcedChange() throws Exception {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_TWO,
                DELAY_OF_ZERO,
                secondAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        PendingChange secondForcedChange = createPendingChange(
                HASH_2,
                BLOCK_NUMBER_TEN,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST
        );

        handler.addPendingChange(secondForcedChange, FALSE_BIPREDICATE);

        assertEquals(forcedChange, handler.getPendingForcedChanges().get(0));
        assertEquals(secondForcedChange, handler.getPendingForcedChanges().get(1));

        assertEquals(2, handler.getPendingForcedChanges().size());
    }

    @Test
    public void testAddForcedChangeWithDelayKindEnumNull() {

        PendingChange change = new PendingChange();

        change.setCanonHash(HASH_1);
        change.setCanonHeight(BLOCK_NUMBER_ONE);
        change.setNextAuthorities(firstAuthoritySet);
        change.setDelay(DELAY_OF_ONE);

        PendingChange.DelayKind delayKind = new PendingChange.DelayKind();
        delayKind.setKind(null);

        change.setDelayKind(delayKind);


        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(change, TRUE_BIPREDICATE));
    }

    @Test
    public void testAddForcedChangeWithDelayKindNull() {

        PendingChange change = new PendingChange();

        change.setCanonHash(HASH_1);
        change.setCanonHeight(BLOCK_NUMBER_ONE);
        change.setNextAuthorities(firstAuthoritySet);
        change.setDelay(DELAY_OF_ONE);

        change.setDelayKind(null);

        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(change, TRUE_BIPREDICATE));
    }

    @Test
    public void testAddForcedChangeWithInvalidAuthorityList() {

        List<Authority> authorities = List.of(new Authority(new byte[] {1}, BigInteger.ZERO));

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ONE,
                authorities,
                PendingChange.DelayKindEnum.BEST
        );

        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(forcedChange, TRUE_BIPREDICATE));
    }

    @Test
    public void testAddForcedChangeWithNullAuthorityList() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ONE,
                null,
                PendingChange.DelayKindEnum.BEST
        );

        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(forcedChange, TRUE_BIPREDICATE));
    }

    @Test
    public void testAddForcedChangeWithEmptyAuthorityList() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ONE,
                new ArrayList<>(),
                PendingChange.DelayKindEnum.BEST
        );

        assertThrows(GrandpaGenericException.class, () -> handler.addPendingChange(forcedChange, TRUE_BIPREDICATE));
    }

    @Test
    public void testAddAndApplyScheduledChangeSuccessfully() throws Exception {

        PendingChange scheduledChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_TWO,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.FINALIZED
        );

        handler.addPendingChange(scheduledChange, TRUE_BIPREDICATE);

        Class<?> clazz = handler.getClass();

        Field field = clazz.getDeclaredField("pendingScheduledChanges");
        field.setAccessible(true);
        ForkTree<PendingChange> forkTree = (ForkTree<PendingChange>) field.get(handler);
        forkTree.setBestFinalizedNumber(BLOCK_NUMBER_ONE);

        assertEquals(1, forkTree.getRoots().size());

        Optional<PendingChange> appliedScheduled = handler.applyScheduledChanges(
                HASH_1,
                BLOCK_NUMBER_TWO,
                TRUE_BIPREDICATE
        );

        assertTrue(appliedScheduled.isPresent());
    }

    @Test
    public void testApplyForcedChangesWhenNoForcedChanges() {
        Optional<PendingChange> pendingChange = handler.applyForcedChanges(HASH_1, BLOCK_NUMBER_ONE, TRUE_BIPREDICATE);
        assertTrue(pendingChange.isEmpty());
    }

    @Test
    public void testApplyForcedChangesWhenEffectiveNumberGreater() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                BLOCK_NUMBER_ONE,
                List.of(new Authority(new byte[]{1}, BigInteger.ONE)),
                PendingChange.DelayKindEnum.BEST);

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        Optional<PendingChange> result = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_ONE.subtract(BigInteger.ONE),
                TRUE_BIPREDICATE
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void testApplyForcedChangesMatchAndNoConflict() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                List.of(new Authority(new byte[]{1}, BigInteger.ONE)),
                PendingChange.DelayKindEnum.BEST);

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        Optional<PendingChange> result = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_ONE,
                TRUE_BIPREDICATE
        );

        assertTrue(result.isPresent());
        assertEquals(forcedChange.getNextAuthorities(), result.get().getNextAuthorities());

        Optional<PendingChange> secondResult = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_ONE,
                TRUE_BIPREDICATE
        );

        assertTrue(secondResult.isEmpty());
    }

    @Test
    public void testApplyForcedChangesWithScheduledChangeEffectiveNumberEqualMedianLastFinalized() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST,
                BLOCK_NUMBER_TWO
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        PendingChange scheduledChange = createPendingChange(
                HASH_2,
                BLOCK_NUMBER_TWO,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.FINALIZED);

        handler.addPendingChange(scheduledChange, TRUE_BIPREDICATE);

        assertThrows(GrandpaGenericException.class, () ->
                handler.applyForcedChanges(
                        HASH_1,
                        BLOCK_NUMBER_ONE,
                        TRUE_BIPREDICATE
                ));
    }

    @Test
    public void testApplyForcedChangesWithScheduledChangeAsAncestor() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST,
                BLOCK_NUMBER_TWO
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        PendingChange scheduledChange = createPendingChange(
                HASH_2,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.FINALIZED);

        handler.addPendingChange(scheduledChange, TRUE_BIPREDICATE);

        assertThrows(GrandpaGenericException.class, () ->
                handler.applyForcedChanges(
                        HASH_1,
                        BLOCK_NUMBER_ONE,
                        TRUE_BIPREDICATE
                ));
    }

    @Test
    public void testApplyForcedChangesWithoutConflictingScheduledChange() {

        PendingChange forcedChange = createPendingChange(
                HASH_1,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.BEST,
                BLOCK_NUMBER_TWO
        );

        handler.addPendingChange(forcedChange, TRUE_BIPREDICATE);

        PendingChange scheduledChange = createPendingChange(
                HASH_2,
                BLOCK_NUMBER_ONE,
                DELAY_OF_ZERO,
                firstAuthoritySet,
                PendingChange.DelayKindEnum.FINALIZED);

        handler.addPendingChange(scheduledChange, TRUE_BIPREDICATE);

        Optional<PendingChange> pendingChange = handler.applyForcedChanges(
                HASH_1,
                BLOCK_NUMBER_ONE,
                FALSE_BIPREDICATE
        );

        assertTrue(pendingChange.isPresent());
    }

    private PendingChange createPendingChange(Hash256 hash,
                                              BigInteger number,
                                              BigInteger delay,
                                              List<Authority> authorities,
                                              PendingChange.DelayKindEnum kind) {

        return createPendingChange(hash, number, delay, authorities, kind, null);
    }

    private PendingChange createPendingChange(Hash256 hash,
                                              BigInteger number,
                                              BigInteger delay,
                                              List<Authority> authorities,
                                              PendingChange.DelayKindEnum kind,
                                              BigInteger medianLastFinalized) {

        PendingChange change = new PendingChange();

        change.setCanonHash(hash);
        change.setCanonHeight(number);
        change.setNextAuthorities(authorities);
        change.setDelay(delay);

        PendingChange.DelayKind delayKind = new PendingChange.DelayKind();
        if (medianLastFinalized != null && kind.equals(PendingChange.DelayKindEnum.BEST)) {
            delayKind.setMedianLastFinalized(medianLastFinalized);
        }

        delayKind.setKind(kind);

        change.setDelayKind(delayKind);

        return change;
    }

    private byte[] generateHash(int seed) {
        byte[] arr = new byte[32];
        Arrays.fill(arr, (byte) seed);
        return arr;
    }
}
