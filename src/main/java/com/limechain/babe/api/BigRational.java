package com.limechain.babe.api;

import lombok.Getter;

import java.math.BigInteger;

@Getter
public class BigRational {

    private BigInteger numerator;
    private BigInteger denominator;

    public BigRational(double f) {
        fromDouble(f);
    }

    private void fromDouble(double f) {
        final int expMask = 0x7FF;
        long bits = Double.doubleToLongBits(f);
        long mantissa = bits & ((1L << 52) - 1);
        int exp = (int) ((bits >> 52) & expMask);

        switch (exp) {
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

        // Calculate the shift based on the exponent
        int shift = 52 - exp;

        // Normalize the mantissa
        while ((mantissa & 1) == 0 && shift > 0) {
            mantissa >>= 1;
            shift--;
        }

        // Set the numerator and denominator
        this.numerator = f < 0 ? BigInteger.valueOf((-1) * mantissa) : BigInteger.valueOf(mantissa);
        this.denominator = BigInteger.ONE;

        // Adjust the denominator based on the shift
        if (shift > 0) {
            this.denominator = this.denominator.shiftLeft(shift);
        } else {
            this.numerator = this.numerator.shiftLeft((-1) * shift);
        }

    }
}
