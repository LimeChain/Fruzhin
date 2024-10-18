package com.limechain.utils.math;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * Used to represent quotient a/b of arbitrary precision.
 * The implementation is derived from the standard Go math/big package.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BigRational {

    private BigInteger numerator;
    private BigInteger denominator;

    public BigRational(double f) {
        fromDouble(f);
    }

    private void fromDouble(double f) {
        //0x7FF = 2047
        final int expMask = 0x7FF;
        long bits = Double.doubleToLongBits(f);
        long mantissa = bits & ((1L << 52) - 1);
        int exp = (int) ((bits >> 52) & expMask);

        switch (exp) {
            //non-finite cases result in numerator and denominator equal null
            case expMask:
                return;
            case 0:
                exp -= 1023;
                break;
            default:
                mantissa |= 1L << 52;
                exp -= 1023;
                break;
        }

        int shift = 52 - exp;

        while ((mantissa & 1) == 0 && shift > 0) {
            mantissa >>= 1;
            shift--;
        }

        this.numerator = f < 0 ? BigInteger.valueOf((-1) * mantissa) : BigInteger.valueOf(mantissa);
        this.denominator = BigInteger.ONE;

        if (shift > 0) {
            this.denominator = this.denominator.shiftLeft(shift);
        } else {
            this.numerator = this.numerator.shiftLeft((-1) * shift);
        }
    }
}
