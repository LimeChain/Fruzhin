package com.limechain.network.protocol.warp.dto;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scaletypes.Extrinsic;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;
import io.libp2p.crypto.keys.Ed25519PublicKey;
import lombok.Setter;
import lombok.extern.java.Log;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Setter
@Log
public class WarpSyncJustification {
    public BigInteger round;
    public Hash256 targetHash;
    public BigInteger targetBlock;
    public Precommit[] precommits;
    public BlockHeader[] ancestryVotes;

    @Override
    public String toString() {
        return "WarpSyncJustification{" +
                "round=" + round +
                ", targetHash=" + targetHash +
                ", targetBlock=" + targetBlock +
                ", precommits=" + Arrays.toString(precommits) +
                ", ancestryVotes=" + Arrays.toString(ancestryVotes) +
                '}';
    }
}
