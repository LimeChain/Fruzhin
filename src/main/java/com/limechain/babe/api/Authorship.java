package com.limechain.babe.api;

import com.limechain.chain.lightsyncstate.Authority;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Authorship {

    //  Calculates the primary selection threshold for a given authority, taking
    //  into account `c` (`1 - c` represents the probability of a slot being empty).
    public static BigInteger calculatePrimaryThreshold(
            @NotNull final Pair<BigInteger, BigInteger> constant,
            @NotNull final List<Authority> authorities,
            final int authorityIndex) {

        if (BigInteger.ZERO.equals(constant.getValue0()) || authorityIndex >= authorities.size()) {
            return BigInteger.ZERO;
        }

        //TODO: choose appropriate rounding
        var numinator = new BigDecimal(constant.getValue0());
        var denominator = new BigDecimal(constant.getValue1());
        var c = numinator.divide(denominator);

        var totalWeight = new BigDecimal(
                authorities.stream()
                        .map(Authority::getWeight)
                        .reduce(BigInteger.ZERO, BigInteger::add)
        );

        var weight = new BigDecimal(authorities.get(authorityIndex).getWeight());
        var theta = weight.divide(totalWeight);

        //  p = 1 - (1 - c) ^ theta
        var p = 1.0 - Math.pow((1.0 - Double.parseDouble(c.toString())), Double.parseDouble(theta.toString()));

        return BigInteger.TEN;
    }
}

//// Prevent div by zero and out of bounds access.
//// While Babe's pallet implementation that ships with FRAME performs a sanity check over
//// configuration parameters, this is not sufficient to guarantee that `c.1` is non-zero
//// (i.e. third party implementations are possible).
//	if c.1 == 0 || authority_index >= authorities.len() {
//    return 0
//}
//
//let c = c.0 as f64 / c.1 as f64;
//
//let theta = authorities[authority_index].1 as f64 /
//        authorities.iter().map(|(_, weight)| weight).sum::<u64>() as f64;
//
//	assert!(theta > 0.0, "authority with weight 0.");
//
//// NOTE: in the equation `p = 1 - (1 - c)^theta` the value of `p` is always
//// capped by `c`. For all practical purposes `c` should always be set to a
//// value < 0.5, as such in the computations below we should never be near
//// edge cases like `0.999999`.
//
//let p = BigRational::from_float(1f64 - (1f64 - c).powf(theta)).expect(
//		"returns None when the given value is not finite; \
//		 c is a configuration parameter defined in (0, 1]; \
//		 theta must be > 0 if the given authority's weight is > 0; \
//		 theta represents the validator's relative weight defined in (0, 1]; \
//		 powf will always return values in (0, 1] given both the \
//		 base and exponent are in that domain; \
//		 qed.",
//);
//
//let numer = p.numer().to_biguint().expect(
//        "returns None when the given value is negative; \
//		 p is defined as `1 - n` where n is defined in (0, 1]; \
//		 p must be a value in [0, 1); \
//		 qed.",
//        );
//
//let denom = p.denom().to_biguint().expect(
//        "returns None when the given value is negative; \
//		 p is defined as `1 - n` where n is defined in (0, 1]; \
//		 p must be a value in [0, 1); \
//		 qed.",
//        );
//
//	((BigUint::one() << 128usize) * numer / denom).to_u128().expect(
//		"returns None if the underlying value cannot be represented with 128 bits; \
//		 we start with 2^128 which is one more than can be represented with 128 bits; \
//		 we multiple by p which is defined in [0, 1); \
//		 the result must be lower than 2^128 by at least one and thus representable with 128 bits; \
//		 qed.",
//)
