package com.limechain.babe.api;

import com.limechain.chain.lightsyncstate.Authority;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Authorship {

    public static BigInteger calculatePrimaryThreshold(
            @NotNull final Pair<BigInteger, BigInteger> constant,
            @NotNull final List<Authority> authorities,
            final int authorityIndex) {

        if (BigInteger.ZERO.equals(constant.getValue0()) || authorityIndex >= authorities.size()) {
            return BigInteger.ZERO;
        }

        double numerator = constant.getValue0().doubleValue();
        double denominator = constant.getValue1().doubleValue();
        double c = numerator / denominator;

        double totalWeight = authorities.stream()
                .map(Authority::getWeight)
                .reduce(BigInteger.ZERO, BigInteger::add)
                .doubleValue();

        double weight = authorities.get(authorityIndex)
                .getWeight()
                .doubleValue();

        double theta = weight / totalWeight;

        //  p = 1 - (1 - c) ^ theta
        double p = 1.0 - Math.pow((1.0 - c), theta);

        BigRational pRational = new BigRational(p);
        BigInteger twoToThe128 = BigInteger.ONE.shiftLeft(128);
        BigInteger scaledNumer = twoToThe128.multiply(pRational.getNumerator());

        return scaledNumer.divide(pRational.getDenominator());
    }
}