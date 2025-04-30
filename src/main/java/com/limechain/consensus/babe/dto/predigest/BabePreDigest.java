package com.limechain.consensus.babe.dto.predigest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BabePreDigest {
     private PreDigestType type;
     private long authorityIndex;
     private BigInteger slotNumber;
     private byte[] vrfOutput;
     private byte[] vrfProof;

     public int getAuthorityIndexAsInt() {
          if (authorityIndex < 0 || authorityIndex > Integer.MAX_VALUE) {
               throw new IllegalArgumentException("Authority index out of valid int range: " + authorityIndex);
          }

          return (int) authorityIndex;
     }
}
