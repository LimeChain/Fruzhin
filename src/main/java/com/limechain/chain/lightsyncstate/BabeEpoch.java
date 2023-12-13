package com.limechain.chain.lightsyncstate;

import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
public class BabeEpoch {
    private BigInteger epochIndex;
    private BigInteger slotNumber;
    private BigInteger duration;
    private List<Authority> authorities;
    private byte[] randomness;
    private NextBabeConfig nextConfig;

    public void validate() {
        if (nextConfig.c.getValue0().compareTo(nextConfig.c.getValue1()) > 0) {
            throw new IllegalStateException("Invalid epoch configuration: c0 > c1");
        }
    }

    public enum BabeAllowedSlots {
        /// Only allow primary slot claims.
        PRIMARY_SLOTS,
        /// Allow primary and secondary plain slot claims.
        PRIMARY_AND_SECONDARY_PLAIN_SLOTS,
        /// Allow primary and secondary VRF slot claims.
        PRIMARY_AND_SECONDARY_VRF_SLOTS;

        public static BabeAllowedSlots fromId(int i) {
            return switch (i) {
                case 0 -> PRIMARY_SLOTS;
                case 1 -> PRIMARY_AND_SECONDARY_PLAIN_SLOTS;
                case 2 -> PRIMARY_AND_SECONDARY_VRF_SLOTS;
                default -> throw new IllegalArgumentException("Unknown BabeAllowedSlots id " + i);
            };

        }
    }

    @Getter
    @Setter
    public static class NextBabeConfig {
        // Value of `c` in `BabeEpochConfiguration`.
        private Pair<BigInteger, BigInteger> c;

        // Value of `allowed_slots` in `BabeEpochConfiguration`.
        private BabeAllowedSlots allowedSlots;
    }
}
