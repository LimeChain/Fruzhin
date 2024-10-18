package com.limechain.babe;

import com.limechain.utils.math.BigRational;
import com.limechain.chain.lightsyncstate.Authority;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Authorship {

    // threshold = 2^128 * (1 - (1 - c) ^ (authority_weight / sum(authorities_weights)))
    public static BigInteger calculatePrimaryThreshold(
            @NotNull final Pair<BigInteger, BigInteger> constant,
            @NotNull final List<Authority> authorities,
            final int authorityIndex) {

        if (BigInteger.ZERO.equals(constant.getValue1()) ||
                authorityIndex >= authorities.size() ||
                authorityIndex < 0) {
            throw new IllegalArgumentException("Invalid denominator or authority index provided");
        }

        double numerator = constant.getValue0().doubleValue();
        double denominator = constant.getValue1().doubleValue();
        double c = numerator / denominator;

        if (c > 1 || c < 0) {
            throw new IllegalStateException("BABE constant must be within the range (0, 1)");
        }

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
        BigInteger result = scaledNumer.divide(pRational.getDenominator());

        return result;
    }
}