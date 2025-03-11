package com.limechain.network.protocol.warp;

import com.limechain.consensus.babe.dto.message.BabeConsensusMessage;
import com.limechain.consensus.babe.scale.message.BabeConsensusMessageReader;
import com.limechain.consensus.babe.dto.predigest.BabePreDigest;
import com.limechain.consensus.babe.scale.predigest.PreDigestReader;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.consensus.beefy.scale.message.BeefyConsensusMessageReader;
import com.limechain.consensus.grandpa.dto.message.GrandpaConsensusMessage;
import com.limechain.consensus.grandpa.scale.message.GrandpaConsensusMessageReader;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.utils.Sr25519Utils;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper class for processing different types of header digests
 */
@UtilityClass
public class DigestHelper {

    public List<BabeConsensusMessage> getBabeConsensusMessages(HeaderDigest[] headerDigests) {
        return Arrays.stream(headerDigests)
                .filter(headerDigest -> DigestType.CONSENSUS_MESSAGE.equals(headerDigest.getType()) &&
                        ConsensusEngine.BABE.equals(headerDigest.getId()))
                .map(HeaderDigest::getMessage)
                .map(message -> ScaleUtils.Decode.decode(message, BabeConsensusMessageReader.getInstance()))
                .collect(Collectors.toList());
    }

    public List<GrandpaConsensusMessage> getGrandpaConsensusMessages(HeaderDigest[] headerDigests) {
        return Arrays.stream(headerDigests)
                .filter(headerDigest -> DigestType.CONSENSUS_MESSAGE.equals(headerDigest.getType()) &&
                        ConsensusEngine.GRANDPA.equals(headerDigest.getId()))
                .map(HeaderDigest::getMessage)
                .map(message -> ScaleUtils.Decode.decode(message, GrandpaConsensusMessageReader.getInstance()))
                .collect(Collectors.toList());
    }

    public List<BeefyConsensusMessage> getBeefyConsensusMessages(HeaderDigest[] headerDigests) {
        return Arrays.stream(headerDigests)
                .filter(headerDigest -> DigestType.CONSENSUS_MESSAGE.equals(headerDigest.getType()) &&
                        ConsensusEngine.BEEFY.equals(headerDigest.getId()))
                .map(HeaderDigest::getMessage)
                .map(message -> ScaleUtils.Decode.decode(message, BeefyConsensusMessageReader.getInstance()))
                .collect(Collectors.toList());
    }

    public Optional<BabePreDigest> getBabePreRuntimeDigest(HeaderDigest[] headerDigests) {
        return Arrays.stream(headerDigests)
                .filter(headerDigest -> DigestType.PRE_RUNTIME.equals(headerDigest.getType()) &&
                        ConsensusEngine.BABE.equals(headerDigest.getId()))
                .findFirst()
                .map(HeaderDigest::getMessage)
                .map(message -> ScaleUtils.Decode.decode(message, PreDigestReader.getInstance()));
    }

    public HeaderDigest buildSealHeaderDigest(BlockHeader blockHeader, Schnorrkel.KeyPair keyPair) {
        byte[] signedMessage = Sr25519Utils.signMessage(
                keyPair.getPublicKey(), keyPair.getSecretKey(), blockHeader.getBlake2bHash(true));
        HeaderDigest sealHeaderDigest = new HeaderDigest();
        sealHeaderDigest.setType(DigestType.SEAL);
        sealHeaderDigest.setId(ConsensusEngine.BABE);
        sealHeaderDigest.setMessage(signedMessage);
        return sealHeaderDigest;
    }
}
