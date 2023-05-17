package com.limechain.sync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.PeerId;
import org.javatuples.Pair;
import org.peergos.PeerAddresses;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Sync {
    private Network network;
    public static final int MAX_WARP_SYNC_PROOF_SIZE = 100;

    public Sync(Network network) {
        this.network = network;
    }

    public boolean isSyncing() {
        // TODO: Should be changed when we're able to sync and really determine whether we're at the tip of the chain
        return true;
    }

    public boolean warpSync() {
        PeerAddresses peerForWarpSync = this.selectFullNode();
        WarpSyncResponse warpSyncResponse = this.syncWithNode(peerForWarpSync);
        return true;
    }

    public PeerAddresses selectFullNode() {
        //TODO Collect boot nodes in the connections property of network
        if (network.getPeers().size() == 0) {
            return null;
        }
        return network.getPeers().get(0);
    }

    public WarpSyncResponse syncWithNode(PeerAddresses peer) {
        final PeerId peerId = new PeerId(Multihash.fromBase58("12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5").toBytes());

        WarpSyncResponse warpSyncResponse = network.warpSyncService.getProtocol().warpSyncRequest(
                network.getHost(), network.getHost().getAddressBook(), peerId,
                "0x906558bc9ec91de6b110dee2b1f1cd69a4e3ae378677004bb46ef3dde1148ce0"
        );

        String message = new String(Arrays.stream(Arrays.stream(warpSyncResponse.getFragments()).toList().get(0).getHeader().getDigest()).toList().get(0).getMessage());

        return warpSyncResponse;
    }

    // Verification algorithm which checks the authenticity of the header only at the end of an era
    // where the authority set changes iteratively until reaching the latest era.
    public void verifyAuthoritySetChange() {

    }

    //Verifies the finalty of the latest block using the Grandpa Justifications messages.
    public void verify(WarpSyncFragment[] warpSyncFragments, BigInteger setId, Authority[] currentAuthorities /* hardfork */) {

        for(WarpSyncFragment fragment: warpSyncFragments){
            Hash256 hash = fragment.getHeader().getStateRoot();
            BigInteger number = fragment.getHeader().getBlockNumber();

            Map<Pair<Hash256, BigInteger>, List<Authority>> authorityMap = new HashMap<>();
            List<Authority> authorityList = authorityMap.get(new Pair<>(hash,number));
            if(authorityList== null){
                //Dont know
            } else {
                fragment.getJustification();
            }
        }
        //Iterate fragments
        //Compare fragments hash to hardfork

        //verify justification

        //check if justification target matches hash

        //

    }


    public void generateWarpSyncProof(WarpSyncResponse warpSyncResponse) {
        // Get starting number
        // Check if block with number is finalized
        // Check if begin number is lower than finalized number
        // Check if start block is in finalized chain

        var proofs = warpSyncResponse.getFragments();
        int proofs_encoded_length = 0;
        boolean proof_limit_reached = false;

        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setBlockNumber(BigInteger.ZERO);

        var fragments = warpSyncResponse.getFragments();
        boolean proofLimitReached = false;
        for (int i = 0; i < fragments.length; i++) {
            WarpSyncFragment fragment = fragments[i];
            BlockHeader currentHead = fragment.getHeader();
            if (blockHeader.getBlockNumber().compareTo(fragment.getHeader().getBlockNumber()) >= 0) {
                throw new IllegalArgumentException("header number comes from previously applied set changes; corresponding hash must exist in db; qed.");
            }
            if (/*Left side should be coming from our database*/
                    fragment.getHeader().getStateRoot() != fragment.getHeader().getStateRoot()) {
                throw new IllegalArgumentException("header hash obtained from header number exists in db; corresponding header must exist in db too; qed.");
            }

            //call findFirstDigestWithAuthoritySet to do check for authority set change that is expected

            //Get from our storage the header with the same hash as the current fragment
            var currentBlockHeader = fragment.getHeader();
            var justifications = fragment.getJustification();

            /*
            TODO this but need to get encoded length
            if proofs_encoded_len + proof_size >= MAX_WARP_SYNC_PROOF_SIZE - 50 {
				proof_limit_reached = true;
				break
			}

			proofs_encoded_len += proof_size;
			proofs.push(prof);
             */

            /*
            If proof limit not reached then the justification is valid
            and we can continue with the next logic


             the existing best justification must be for a block higher than the
			 last authority set change. if we didn't prove any authority set
			 change then we fallback to make sure it's higher or equal to the
			 initial warp sync block.

              Check if hash corresponds in db as well
                add latest justification to proofs
             */


            //AuthoritySetChanges authoritySetChanges = new AuthoritySetChanges(fragment.getJustification());
        }

        boolean is_finished;

    }

    public int findFirstDigestWithAuthoritySet(HeaderDigest[] digests) {
        for (int i = 0; i < digests.length; i++) {
            //Check for forced changes
        }
        return 0;
    }

//    public class AuthoritySetChanges {
//        private List<Pair<BigInteger, Hash256>> list;
//
//        public AuthoritySetChanges(Pair<BigInteger, Hash256>[] authoritySetChanges) {
//            list = Arrays.stream(authoritySetChanges).toList();
//        }
//
//        public Optional<Iterator<Pair<BigInteger, Hash256>>> iterFrom(Hash256 blockNumber) {
//            int idx = binarySearchByKey(blockNumber);
//
//            if (idx < 0) {
//                idx = -idx - 1;
//            } else {
//                // if there was a change at the given block number then we should start on the next index
//                // since we want to exclude the current block number
//                idx += 1;
//            }
//
//            if (idx < list.size()) {
//                Pair<BigInteger, Hash256> pair = list.get(idx);
//                BigInteger setId = pair.getValue0();
//
//                // if this is the first index but not the first set id then we are missing data.
//                if (idx == 0 && setId.compareTo(BigInteger.ZERO) != 0) {
//                    return Optional.empty();
//                }
//            }
//
//            return Optional.of(list.subList(idx, list.size()).iterator());
//        }
//
//        private int binarySearchByKey(Hash256 key) {
//            return Collections.binarySearch(list, new Pair<>(BigInteger.ZERO, key), Comparator.comparing(Pair::getValue0));
//        }
//    }
}
