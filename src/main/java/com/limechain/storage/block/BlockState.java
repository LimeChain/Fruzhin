package com.limechain.storage.block;

import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockBody;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.exception.LowerThanRootException;
import com.limechain.storage.block.tree.BlockTree;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * contains the historical block data of the blockchain, including block headers and bodies.
 * It wraps the blocktree (which contains unfinalized blocks) and the database (which contains finalized blocks).
 */
@Log
public class BlockState {
    private final BlockTree blockTree;
    private final Map<byte[], Block> unfinalizedBlocks;
    private final KVRepository<String, Object> db;
    private final BlockStateHelper helper = new BlockStateHelper();

    @Getter
    private final byte[] genesisHash;
    @Getter
    private byte[] lastfinalized;

    public BlockState(KVRepository<String, Object> repository, BlockHeader header) {
        this.blockTree = new BlockTree(header);
        this.db = repository;
        this.unfinalizedBlocks = new HashMap<>();
        this.genesisHash = header.getHash();
        this.lastfinalized = header.getHash();

        setArrivalTime(header.getHash(), Instant.now());
        setHeader(header);
        db.save(helper.headerHashKey(header.getBlockNumber()), header.getHash());
        setBlockBody(header.getHash(), new BlockBody(new ArrayList<>()));

        //set the latest finalized head to the genesis header
        setfinalizedHash(genesisHash, BigInteger.ZERO, BigInteger.ZERO);
    }

    /**
     * Check if the hash is part of the unfinalized blocks in-memory or persisted in the database.
     *
     * @param hash the hash of the block header in byte array representation
     * @return true if the block header is found, false otherwise
     */
    public boolean hasHeader(byte[] hash) {
        if (unfinalizedBlocks.containsKey(hash)) {
            return true;
        }

        return hasHeaderInDatabase(hash);
    }

    /**
     * Check if the hash is persisted in the database.
     *
     * @param hash the hash of the block header in byte array representation
     * @return true if the block header is found, false otherwise
     */
    public boolean hasHeaderInDatabase(byte[] hash) {
        Optional<Object> foundHeader = db.find(helper.headerKey(hash));
        return foundHeader.isPresent();
    }

    /**
     * Get the block header for a given hash
     *
     * @param hash the hash of the block header in byte array representation
     * @return the block header
     */
    public BlockHeader getHeader(byte[] hash) {
        Block block = unfinalizedBlocks.get(hash);
        if (block != null && block.getHeader() != null) {
            return block.getHeader();
        }

        return loadHeaderFromDatabase(hash);
    }

    /**
     * Get the block hash on our best chain with the given number
     *
     * @param blockNum the block number
     * @return the block hash in byte array representation
     */
    public byte[] getHashByNumber(BigInteger blockNum) {
        try {
            // Try to get the hash from the block tree
            return blockTree.getHashByNumber(blockNum.longValue());
        } catch (RuntimeException e) {
            // If the error is not LowerThanRootException, rethrow it
            if (!(e instanceof LowerThanRootException)) {
                throw new RuntimeException("Failed to get hash from blocktree: " + e.getMessage(), e);
            }

            // If error is LowerThanRootException, number has already been finalized, so check db
            byte[] hash = (byte[]) db.find(helper.headerHashKey(blockNum)).orElse(null);

            if (hash == null) {
                throw new RuntimeException("Block " + blockNum + " not found");
            }

            return hash;
        }
    }

    /**
     * Get the block hash on our best chain with the given number
     *
     * @param blockNumber the block number
     * @return List of block hashes in byte array representation
     */
    public List<byte[]> getHashesByNumber(BigInteger blockNumber) {
        Block block;
        try {
            block = getBlockByNumber(blockNumber);
        } catch (Exception e) {
            throw new RuntimeException("Getting block by number: " + e.getMessage(), e);
        }

        List<byte[]> blockHashes = blockTree.getAllBlocksAtNumber(block.getHeader().getParentHash().getBytes());

        byte[] hash = block.getHeader().getHash();
        if (!blockHashes.contains(hash)) {
            blockHashes.add(hash);
        }

        return blockHashes;
    }

    /**
     * Get all the descendants for a given block hash (including itself), by first checking in memory
     * and, if not found, reading from the block state database
     *
     * @param hash the hash of the block header in byte array representation
     * @return List of block hashes in byte array representation
     */
    public List<byte[]> getAllDescendants(byte[] hash) {
        List<byte[]> allDescendants;
        try {
            return blockTree.getAllDescendants(hash);
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                throw e;
            }
            // If the node is not found in the block tree, start the manual process
            allDescendants = new ArrayList<>();
            allDescendants.add(hash);
        }

        BlockHeader header = getHeader(hash);

        List<byte[]> nextBlockHashes = getHashesByNumber(header.getBlockNumber().add(BigInteger.ONE));

        for (byte[] nextBlockHash : nextBlockHashes) {
            BlockHeader nextHeader = getHeader(nextBlockHash);

            if (!Arrays.equals(nextHeader.getParentHash().getBytes(), hash)) {
                continue;
            }

            List<byte[]> nextDescendants = getAllDescendants(nextBlockHash);
            allDescendants.addAll(nextDescendants);
        }

        return allDescendants;
    }

    /**
     * Get the block header on our best chain with the given number
     *
     * @param num the block number
     * @return the block header
     */
    public BlockHeader getHeaderByNumber(BigInteger num) {
        byte[] hash = getHashByNumber(num);
        return getHeader(hash);
    }

    /**
     * Get the block on our best chain with the given number
     *
     * @param num the block number
     * @return the block
     */
    public Block getBlockByNumber(BigInteger num) {
        byte[] hash = getHashByNumber(num);

        return getBlockByHash(hash);
    }

    /**
     * Get the block with the given hash
     *
     * @param hash the block hash
     * @return the block
     */
    public Block getBlockByHash(byte[] hash) {
        // Assuming unfinalizedBlocks is a map or similar structure with a getBlock method
        Block block = unfinalizedBlocks.get(hash);
        if (block != null) {
            return block;
        }

        BlockHeader header = getHeader(hash);

        BlockBody blockBody = getBlockBody(hash);
        return new Block(header, blockBody);
    }

    /**
     * Persist the block header in the database
     *
     * @param header the block header to be persisted
     * @return true if the block header was successfully persisted, false otherwise
     */
    public boolean setHeader(final BlockHeader header) {
        byte[] byteArray = helper.writeHeader(header);
        return db.save(helper.headerKey(header.getHash()), byteArray);
    }

    /**
     * Checks if the block body is in the database
     *
     * @param hash the block hash
     * @return true if the block body is in the database, false otherwise
     */
    public boolean hasBlockBody(byte[] hash) {
        if (unfinalizedBlocks.containsKey(hash)) {
            return true;
        }

        return db.find(helper.blockBodyKey(hash)).isPresent();
    }

    /**
     * Get the block body for a given hash
     *
     * @param hash the block hash
     * @return the block body
     */
    public BlockBody getBlockBody(byte[] hash) {
        // Assuming unfinalizedBlocks is a map or similar structure with a getBlockBody method
        Block block = unfinalizedBlocks.get(hash);
        if (block != null && block.getBody() != null) {
            return block.getBody();
        }

        byte[] data = (byte[]) db.find(helper.blockBodyKey(hash)).orElse(null);
        if (data == null) {
            throw new RuntimeException("Failed to get block body from database");
        }

        return BlockBody.fromEncoded(data);
    }

    /**
     * Persist the block body in the database
     *
     * @param hash      the block hash
     * @param blockBody the block body to be persisted
     */
    public void setBlockBody(byte[] hash, BlockBody blockBody) {
        db.save(helper.blockBodyKey(hash), blockBody.getEncoded());
    }

    /**
     * Adds block to the blocktree and stores it in the database with the current time as arrival time
     *
     * @param block the block to be added
     */
    public void addBlock(final Block block) {
        addBlockWithArrivalTime(block, Instant.now());
    }

    /**
     * Adds block to the blocktree and stores it in the database with the given arrival time
     *
     * @param block       the block to be added
     * @param arrivalTime the arrival time of the block
     */
    public void addBlockWithArrivalTime(Block block, Instant arrivalTime) {
        if (block.getBody() == null) {
            throw new RuntimeException("Block body cannot be null");
        }

        // Add block to blocktree
        blockTree.addBlock(block.getHeader(), arrivalTime);

        // Store block in unfinalized blocks
        unfinalizedBlocks.put(block.getHeader().getHash(), block);
    }

    /**
     * Gets all the hashes of unfinalized blocks with the given number
     *
     * @param blockNum the block number
     * @return List of hashes of blocks
     */
    public List<byte[]> getAllBlocksAtNumber(final BigInteger blockNum) {
        BlockHeader header = getHeaderByNumber(blockNum);

        if (header == null) {
            throw new RuntimeException("Header not found for block number: " + blockNum);
        }

        return getAllBlocksAtDepth(header.getParentHash().getBytes());
    }

    /**
     * Gets all the hashes of unfinalized blocks with the depth of the given hash plus one
     *
     * @param hash the parent hash
     * @return List of hashes of blocks
     */
    public List<byte[]> getAllBlocksAtDepth(byte[] hash) {
        return blockTree.getAllBlocksAtNumber(hash);
    }

    /**
     * Verifies if a block is on our current chain by checking if it is a descendant of our best block
     *
     * @param header the block header
     * @return true if the block is on our current chain, false otherwise
     */
    public boolean isBlockOnCurrentChain(BlockHeader header) {
        BlockHeader bestBlock = bestBlockHeader();
        if (bestBlock == null) {
            throw new RuntimeException("Best block header cannot be retrieved");
        }

        // If the new block's number is greater than our best block's number, then it is on our current chain.
        if (header.getBlockNumber().compareTo(bestBlock.getBlockNumber()) > 0) {
            return true;
        }

        return isDescendantOf(header.getHash(), bestBlock.getHash());
    }

    /**
     * Gets the current beest block's hash
     *
     * @return the best block's hash
     */
    public byte[] bestBlockHash() {
        return blockTree.bestBlockHash();
    }

    /**
     * Gets the current best block's header
     *
     * @return the best block's header
     */
    public BlockHeader bestBlockHeader() {
        byte[] bestHash = bestBlockHash();
        return getHeader(bestHash);
    }

    /**
     * Gets the current best block's state root's hash
     *
     * @return the best block's state root's hash in byte array representation
     */
    public byte[] bestBlockStateRoot() {
        return bestBlockHeader().getStateRoot().getBytes();
    }

    /**
     * Gets the given block's state root's hash
     *
     * @param blockHash the block hash
     * @return the given block's state root's hash in byte array representation
     */
    public byte[] getBlockStateRoot(byte[] blockHash) {
        return getHeader(blockHash).getStateRoot().getBytes();
    }

    /**
     * Gets the current best block's number
     *
     * @return the best block's number
     */
    public BigInteger bestBlockNumber() {
        BlockHeader header = bestBlockHeader();
        if (header == null) {
            throw new RuntimeException("Failed to get best block header");
        }
        return header.getBlockNumber();
    }

    /**
     * Gets the current best block
     *
     * @return the best block
     */
    public Block bestBlock() {
        return getBlockByHash(bestBlockHash());
    }

    /**
     * Gets the header of the block with the given hash from the database
     *
     * @param hash the block hash
     * @return the block header
     */
    public BlockHeader loadHeaderFromDatabase(byte[] hash) {
        Optional<Object> foundHeader = db.find(helper.headerKey(hash));

        if (foundHeader.isEmpty()) {
            throw new RuntimeException("Header not found in database");
        }

        return helper.readHeader((byte[]) foundHeader.get());
    }

    /**
     * Gets the sub-blockchain between the starting hash and the ending hash using both block tree and database
     *
     * @param startHash the hash of the block to start with
     * @param endHash   the hash of the block to end with
     * @return the list of block hashes
     */
    public List<byte[]> range(byte[] startHash, byte[] endHash) {
        List<byte[]> hashes = new ArrayList<>();

        if (Arrays.equals(startHash, endHash)) {
            hashes.add(startHash);
            return hashes;
        }

        BlockHeader endHeader;
        try {
            endHeader = loadHeaderFromDatabase(endHash);
        } catch (RuntimeException e) {
            // end hash is not in the database so we should lookup the
            // block that could be in memory and in the database as well
            return retrieveRange(startHash, endHash);
        }

        // end hash was found in the database, that means all the blocks
        // between start and end can be found in the database
        return retrieveRangeFromDatabase(startHash, endHeader);
    }

    /**
     * Gets the sub-blockchain between the starting hash and the ending hash using both block tree and database
     *
     * @param startHash the hash of the block to start with
     * @param endHash   the hash of the block to end with
     * @return the list of block hashes
     */
    public List<byte[]> retrieveRange(byte[] startHash, byte[] endHash) {
        List<byte[]> inMemoryHashes = blockTree.range(startHash, endHash);
        byte[] firstItem = inMemoryHashes.get(0);

        // if the first item is equal to the startHash that means we got the range
        // from the in-memory blocktree
        if (Arrays.equals(firstItem, startHash)) {
            return inMemoryHashes;
        }

        // since we got as many blocks as we could from the block tree but still missing blocks to
        // fulfil the range we should lookup in the database for the remaining ones, the first item in the hashes array
        // must be the block tree root that is also placed in the database
        // so we will start from its parent since it is already in the array
        BlockHeader blockTreeRootHeader = loadHeaderFromDatabase(firstItem);
        BlockHeader startingAtParentHeader = loadHeaderFromDatabase(blockTreeRootHeader.getParentHash().getBytes());

        List<byte[]> inDatabaseHashes = retrieveRangeFromDatabase(startHash, startingAtParentHeader);

        List<byte[]> hashes = new ArrayList<>(inDatabaseHashes);
        hashes.addAll(inMemoryHashes);
        return hashes;
    }

    /**
     * Gets the sub-blockchain between the starting hash and the ending hash using only the database
     *
     * @param startHash the hash of the block to start with
     * @param endHeader the header of the block to end with
     * @return the list of block hashes
     */
    public List<byte[]> retrieveRangeFromDatabase(byte[] startHash, BlockHeader endHeader) {
        BlockHeader startHeader = loadHeaderFromDatabase(startHash);
        if (startHeader.getBlockNumber().compareTo(endHeader.getBlockNumber()) > 0) {
            throw new RuntimeException("Start block number is greater than end block number");
        }

        BigInteger blocksInRange = endHeader.getBlockNumber()
                .subtract(startHeader.getBlockNumber()).add(BigInteger.ONE);
        List<byte[]> hashes = new ArrayList<>(blocksInRange.intValue());

        int lastPosition = blocksInRange.intValue() - 1;

        hashes.add(0, startHash);
        hashes.add(lastPosition, endHeader.getHash());

        byte[] inLoopHash = endHeader.getParentHash().getBytes();
        for (int currentPosition = lastPosition - 1; currentPosition > 0; currentPosition--) {
            hashes.add(currentPosition, inLoopHash);

            BlockHeader inLoopHeader = loadHeaderFromDatabase(inLoopHash);
            inLoopHash = inLoopHeader.getParentHash().getBytes();
        }

        // Verify that we ended up with the start hash
        if (!Arrays.equals(inLoopHash, startHash)) {
            throw new RuntimeException("Start hash mismatch: expected " + startHash + ", found: " + inLoopHash);
        }

        return hashes;
    }

    /**
     * Gets the sub-blockchain between the starting hash and the ending hash using only the block tree
     *
     * @param startHash the hash of the block to start with
     * @param endHash   the hash of the block to end with
     * @return the list of block hashes
     */
    public List<byte[]> rangeInMemory(byte[] startHash, byte[] endHash) {
        if (blockTree == null) {
            throw new RuntimeException("Block tree is not initialized");
        }

        return blockTree.rangeInMemory(startHash, endHash);
    }

    /**
     * Checks if a given child block is a descendant of a given parent block
     *
     * @param ancestor   parent block to verify for
     * @param descendant child block to verify for
     * @return true if the child block is a descendant of the parent block, false otherwise
     */
    public boolean isDescendantOf(byte[] ancestor, byte[] descendant) {
        if (blockTree == null) {
            throw new RuntimeException("Block tree is not initialized");
        }

        try {
            return blockTree.isDescendantOf(ancestor, descendant);
        } catch (Exception e) {
            BlockHeader descendantHeader = getHeader(descendant);
            BlockHeader ancestorHeader = getHeader(ancestor);

            BlockHeader current = descendantHeader;
            while (current.getBlockNumber().compareTo(ancestorHeader.getBlockNumber()) > 0) {
                if (Arrays.equals(current.getParentHash().getBytes(), ancestor)) {
                    return true;
                }
                current = getHeader(current.getParentHash().getBytes());
            }

            return false;
        }
    }

    /**
     * Gets the lowest common ancestor between two blocks in the tree
     *
     * @param startHash the hash of the first block
     * @param endHash   the hash of the second block
     * @return the lowest common ancestor
     */
    public byte[] lowestCommonAncestor(byte[] startHash, byte[] endHash) {
        return blockTree.lowestCommonAncestor(startHash, endHash);
    }

    /**
     * Gets the leaves of the blocktree as an array of byte arrays
     *
     * @return the leaves of the blocktree as an array of byte arrays
     */
    public List<byte[]> leaves() {
        return blockTree.leaves();
    }

    /**
     * Sets the arrival time of a block
     *
     * @param hash the hash of the block
     * @param now  the arrival time of the block
     */
    public void setArrivalTime(byte[] hash, Instant now) {
        byte[] serialize = SerializationUtils.serialize(now);
        db.save(helper.arrivalTimeKey(hash), serialize);
    }

    /**
     * Get the arrival time of a block
     *
     * @param hash the hash of the block
     * @return the arrival time of the block
     */
    public Instant getArrivalTime(byte[] hash) {
        Optional<Object> object = db.find(helper.arrivalTimeKey(hash));

        if (object.isEmpty()) {
            throw new RuntimeException("Arrival time not found");
        }
        Object obj = object.get();

        if (obj instanceof Instant instant) {
            return instant;
        } else {
            return SerializationUtils.deserialize((byte[]) obj);
        }
    }

    /**
     * Get the runtime for a given block hash
     *
     * @param blockHash the block hash
     * @return runtime for the block
     */
    public Runtime getRuntime(byte[] blockHash) {
        try {
            return blockTree.getBlockRuntime(blockHash);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("While getting runtime: ", e);
        }
    }

    /**
     * Store the runtime for a given block hash
     *
     * @param blockHash the block hash
     * @param runtime   the runtime to be stored
     */
    public void storeRuntime(byte[] blockHash, Runtime runtime) {
        blockTree.storeRuntime(blockHash, runtime);
    }

    /**
     * Get all non-finalized blocks
     *
     * @return list of hashes of the non-finalized blocks
     */
    public List<byte[]> getNonfinalizedBlocks() {
        return blockTree.getAllBlocks();
    }

    /**
     * Get non-finalized block from hash
     *
     * @param hash the hash of the block
     * @return the block
     */
    public Block getUnfinalizedBlockFromHash(final byte[] hash) {
        return unfinalizedBlocks.get(hash);
    }

    /**
     * Add non-finalized block to the blocktree
     *
     * @param hash  the hash of the block
     * @param block the block
     */
    public void addUnfinalizedBlock(final byte[] hash, final Block block) {
        this.unfinalizedBlocks.put(hash, block);
    }

    /**
     * Delete non-finalized block from the blocktree
     *
     * @param blockHash the hash of the block
     * @return the block
     */
    public Block deleteUnfinalizedBlock(final byte[] blockHash) {
        return this.unfinalizedBlocks.remove(blockHash);
    }

    /* Block finalization */

    public void setfinalizedHash(byte[] hash, BigInteger round, BigInteger setId) {
        if (!hasHeader(hash)) {
            throw new RuntimeException("Cannot finalise unknown block " + new Hash256(hash));
        }

        handlefinalizedBlock(hash);
        db.save(helper.finalizedHashKey(round, setId), hash);
        setHighestRoundAndSetID(round, setId);

        if (round.compareTo(BigInteger.ZERO) > 0) {
            //Notify that we have finalized a block
        }

        List<byte[]> pruned = blockTree.prune(hash);

        for (byte[] prunedHash : pruned) {
            Block block = unfinalizedBlocks.remove(prunedHash);
            if (block == null) continue;
            //TODO: tries.delete(blockheader.StateRoot)
        }

        // if nothing was previously finalized, set the first slot of the network to the
        // slot number of block 1, which is now being set as final
        if (Arrays.equals(this.lastfinalized, this.genesisHash) && Arrays.equals(hash, this.genesisHash)) {
//            setFirstSlotOnFinalisation();
            //TODO: Implement when BABE is implemented
        }

        if (this.lastfinalized != hash) {
            //Delete from trie last finalized
            //TODO: implement when the Trie is ready
        }

        this.lastfinalized = hash;
    }

    public void setHighestRoundAndSetID(BigInteger round, BigInteger setId) {
        final Pair<BigInteger, BigInteger> highestRoundAndSetID = getHighestRoundAndSetID();
        final BigInteger highestSetID = highestRoundAndSetID.getValue1();

        if (setId.compareTo(highestSetID) < 0) {
            throw new RuntimeException("SetID " + setId + " should be greater or equal to " + highestSetID);
        }

        db.save(DBConstants.HIGHEST_ROUND_AND_SET_ID_KEY, helper.bigIntegersToByteArray(round, setId));
    }

    public Pair<BigInteger, BigInteger> getHighestRoundAndSetID() {
        Optional<Object> roundAndSetId = db.find(DBConstants.HIGHEST_ROUND_AND_SET_ID_KEY);
        byte[] data = (byte[]) roundAndSetId.orElse(null);

        if (data == null || data.length < 16) {
            throw new RuntimeException("Failed to get highest round and setID");
        }

        return helper.bytesToRoundAndSetId(data);
    }

    public void handlefinalizedBlock(byte[] currentFinalizedHash) {
        if (currentFinalizedHash == this.lastfinalized) {
            return;
        }

        List<byte[]> subchain = rangeInMemory(lastfinalized, currentFinalizedHash);

        List<byte[]> subchainExcludingLatestFinalized = subchain.subList(1, subchain.size());

        // root of subchain is previously finalized block, which has already been stored in the db
        for (byte[] subchainHash : subchainExcludingLatestFinalized) {
            if (Arrays.equals(subchainHash, genesisHash)) {
                continue;
            }

            Block block = unfinalizedBlocks.get(subchainHash);
            if (block == null) {
                throw new RuntimeException("Failed to find block in unfinalized block map, block=" + subchainHash);
            }

            setHeader(block.getHeader());
            setBlockBody(subchainHash, block.getBody());

            Instant arrivalTime = getArrivalTime(subchainHash);
            setArrivalTime(subchainHash, arrivalTime);

            db.save(helper.headerHashKey(block.getHeader().getBlockNumber()), subchainHash);

//          delete from the unfinalizedBlockMap and delete reference to in-memory trie
            Block tempBlock = unfinalizedBlocks.remove(subchainHash);
            if (tempBlock == null || tempBlock.getHeader() == null) {
                continue;
            }

            // prune all the subchain hashes state tries from memory
            // but keep the state trie from the current finalized block
            if (!currentFinalizedHash.equals(subchainHash)) {
                //TODO: tries.delete(tempBlock.getHeader().getStateRoot());
            }
        }
    }

}
