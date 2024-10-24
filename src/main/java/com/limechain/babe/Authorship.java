package com.limechain.babe;

import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.math.BigRational;
import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.merlin.TranscriptData;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.schnorrkel.VrfOutputAndProof;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Authorship {

    //TODO: Probably that aren't the most optimal input arguments for that method
    // we can get randomness from the epochData, but epoch number isn't in that epoch data which is strange
    // after implementing claim secondary slot and claim slot function we can determine which are the best input params
    public static PreDigest claimPrimarySlot(byte[] randomness,
                                             long slotNumber,
                                             long epochNumber,
                                             BigInteger threshold,
                                             Schnorrkel.KeyPair keyPair,
                                             int authorityIndex) {

        var transcript = makeTranscript(randomness, slotNumber, epochNumber);

        Schnorrkel schnorrkel = Schnorrkel.getInstance();
        VrfOutputAndProof vrfOutputAndProof = schnorrkel.vrfSign(keyPair, transcript);
        byte[] makeBytes = schnorrkel.makeBytes(keyPair, transcript, vrfOutputAndProof);

        var isBelowThreshold = LittleEndianUtils.fromLittleEndianByteArray(makeBytes).compareTo(threshold) < 0;

        if (isBelowThreshold) {
            return new PreDigest(
                    PreDigest.PreDigestType.PRIMARY,
                    slotNumber,
                    authorityIndex,
                    vrfOutputAndProof
            );
        }

        return null;
    }

    // threshold = 2^128 * (1 - (1 - c) ^ (authority_weight / sum(authorities_weights)))
    public static BigInteger calculatePrimaryThreshold(
            @NotNull final Pair<BigInteger, BigInteger> constant,
            @NotNull final List<Authority> authorities,
            final int authorityIndex) {

        double c = getBabeConstant(constant, authorities, authorityIndex);

        double totalWeight = authorities.stream()
                .map(Authority::getWeight)
                .reduce(BigInteger.ZERO, BigInteger::add)
                .doubleValue();

        double weight = authorities.get(authorityIndex)
                .getWeight()
                .doubleValue();

        double theta = weight / totalWeight;

        // p = 1 - (1 - c) ^ theta
        double p = 1.0 - Math.pow((1.0 - c), theta);

        BigRational pRational = new BigRational(p);

        // 1<<128 == 2^128
        BigInteger twoToThe128 = BigInteger.ONE.shiftLeft(128);
        BigInteger scaledNumer = twoToThe128.multiply(pRational.getNumerator());

        return scaledNumer.divide(pRational.getDenominator());
    }

    private static double getBabeConstant(@NotNull Pair<BigInteger, BigInteger> constant,
                                          @NotNull List<Authority> authorities,
                                          int authorityIndex) {

        if (BigInteger.ZERO.equals(constant.getValue1())) {
            throw new IllegalArgumentException("Invalid authority index provided");
        }

        if (authorityIndex >= authorities.size() || authorityIndex < 0) {
            throw new IllegalArgumentException("Invalid denominator provided");
        }

        double numerator = constant.getValue0().doubleValue();
        double denominator = constant.getValue1().doubleValue();
        double c = numerator / denominator;

        if (c > 1 || c < 0) {
            throw new IllegalStateException("BABE constant must be within the range (0, 1)");
        }

        return c;
    }

    private static TranscriptData makeTranscript(byte[] randomness, Long slotNumber, Long epochNumber) {
        var transcript = new TranscriptData("BABE".getBytes());
        transcript.appendMessage("slot number".getBytes(), LittleEndianUtils.longToLittleEndianBytes(slotNumber));
        transcript.appendMessage("current epoch".getBytes(), LittleEndianUtils.longToLittleEndianBytes(epochNumber));
        transcript.appendMessage("chain randomness".getBytes(), randomness);
        return transcript;
    }
}