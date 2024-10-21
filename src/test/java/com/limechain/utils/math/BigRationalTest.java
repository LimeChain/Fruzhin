package com.limechain.utils.math;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BigRationalTest {

    @Test
    void testBigRational() {
        verifyBigRationalComputations(0.25, 1L, 4L);
        verifyBigRationalComputations(-0.25, -1L, 4L);
        verifyBigRationalComputations(0.75, 3L, 4L);
        verifyBigRationalComputations(0.5, 1L, 2L);
        verifyBigRationalComputations(2.0, 2L, 1L);
        verifyBigRationalComputations(1.0, 1L, 1L);
    }

    @Test
    void testBigRationalWithDoubleMaxValue() {
        BigRational bigRational = new BigRational(Double.MAX_VALUE);

        BigInteger expectedNumerator = new BigInteger("1797693134862315708145274237317043567980705675258449965989174" +
                "7680315726078002853876058955863276687817154045895351438246423432132688946418276846754670353751698604991" +
                "0576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723" +
                "168738177180919299881250404026184124858368");

        assertEquals(expectedNumerator, bigRational.getNumerator());
        assertEquals(BigInteger.ONE, bigRational.getDenominator());
    }

    @Test
    void testBigRationalValueOfNaN() {
        BigRational bigRational = new BigRational(Double.NaN);
        assertNull(bigRational.getNumerator());
        assertNull(bigRational.getDenominator());
    }

    @Test
    void testBigRationalValueOfPositiveInfinity() {
        BigRational bigRational = new BigRational(Double.POSITIVE_INFINITY);
        assertNull(bigRational.getNumerator());
        assertNull(bigRational.getDenominator());
    }

    @Test
    void testBigRationalValueOfNegativeInfinity() {
        BigRational bigRational = new BigRational(Double.NEGATIVE_INFINITY);
        assertNull(bigRational.getNumerator());
        assertNull(bigRational.getDenominator());
    }

    private void verifyBigRationalComputations(double initialValue, Long expectedNumerator, Long expectedDenominator) {
        BigRational bigRational = new BigRational(initialValue);
        assertEquals(BigInteger.valueOf(expectedNumerator), bigRational.getNumerator());
        assertEquals(BigInteger.valueOf(expectedDenominator), bigRational.getDenominator());
    }
}
