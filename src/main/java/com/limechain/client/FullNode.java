package com.limechain.client;

import com.limechain.chain.ChainService;
import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockStateHelper;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.structure.BranchNodeHandle;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.StorageNodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.BranchNodeHandle;
import com.limechain.trie.structure.StorageNodeHandle;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.Vacant;
import com.limechain.trie.structure.decoded.node.DecodedNode;
import com.limechain.trie.structure.decoded.node.StorageValue;
import com.limechain.trie.structure.nibble.BytesToNibbles;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertTrieNode;
import com.limechain.trie.structure.node.NodeChildData;
import com.limechain.trie.structure.node.TrieNodeData;
import com.limechain.utils.HashUtils;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.utils.HashUtils;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log
public class FullNode implements HostNode {
    /**
     * Starts the light client by instantiating all dependencies and services
     *
     * @implNote the RpcApp is assumed to have been started before starting the client,
     * as it relies on the application context
     */
    @SneakyThrows
    public void start() {
        //Initialize state

        // TODO: Is there a better way to do this?
        KVRepository<String, Object> db = AppBean.getBean(KVRepository.class); //presume this works
        // if: database exists and has some persisted storage
        if (db != null && db.find(new BlockStateHelper().headerHashKey(BigInteger.ZERO)).isPresent()) {
            // do nothing?
        } else {
            // do the initial one-time population of the database with the genesis storage
            TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure = initializeDatabaseStorageTrie();
            List<InsertTrieNode> insertTrieNodes = buildTrieNodesList(trieStructure);
            insertStorage(db, insertTrieNodes, (byte) 0); //Todo: calculate state versoin
        }

        // Start network
        final Network network = AppBean.getBean(Network.class);
        network.start();

        // Wait for peers
        while (true) {
            if (!network.kademliaService.getBootNodePeerIds().isEmpty()) {
                if (network.kademliaService.getSuccessfulBootNodes() > 0) {
                    break;
                }
                network.updateCurrentSelectedPeer();
            }

            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000); // TODO: Maybe extract this number into an application.property as it's duplicated
        }

        // Start syncing
        log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");
        AppBean.getBean(WarpSyncMachine.class).start();
    }

    /**
     * Builds a list of {@link InsertTrieNode} objects representing the nodes in a trie structure.
     * Each trie node is constructed with its storage value, merkle value, children's merkle values,
     * and partial key nibbles.
     *
     * @param trieStructure The trie structure containing the nodes. This structure should be
     *                      a {@link TrieStructure} with user data of type {@link Pair}&lt;{@link Optional}&lt;byte[]&gt;, {@link Optional}&lt;byte[]&gt;&gt;,
     *                      where the first element of the pair is an optional storage value (byte array),
     *                      and the second element is the merkle value (byte array).
     * @return A list of {@link InsertTrieNode} objects representing the nodes in the given trie structure.
     * @throws IllegalStateException if the user data in the trie structure is empty or null, which
     *                               indicates an invalid state for the trie nodes.
     */
    public List<InsertTrieNode> buildTrieNodesList(
            TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure
    ) {
        List<InsertTrieNode> trieNodesIterator = new ArrayList<>();

        for (TrieNodeIndex nodeIndex : trieStructure.asIterableUnordered()) {
            Pair<Optional<byte[]>, Optional<byte[]>> userData = trieStructure.getUserDataAtIndex(nodeIndex);
            if (userData.getValue1().isEmpty()) {
                throw new IllegalStateException("Merkle value should not be empty!");
            }
            NodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>> nodeHandle =
                    trieStructure.nodeHandleAtIndex(nodeIndex);

            Optional<byte[]> storageValueOpt = userData.getValue0();
            byte[] merkleValue = userData.getValue1().get();
            final InsertTrieNode.InsertStorageValue storageValue = new InsertTrieNode.InsertStorageValue(
                    storageValueOpt.orElse(null),
                    storageValueOpt.isPresent(),
                    false);

            byte[] merkleValueCopy = merkleValue.clone();
            List<byte[]> childrenMerkleValues = new ArrayList<>();
            Nibbles partialKey = nodeHandle.getPartialKey();
            byte[] partialKeyNibbles = partialKey.asUnmodifiableList().stream().map(Nibble::asByte)
                    .collect(ByteArrayOutputStream::new, ByteArrayOutputStream::write, (baos1, baos2) -> {
                        try {
                            baos2.writeTo(baos1);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .toByteArray();

            for (Nibble n : Nibbles.ALL) {
                Optional<NodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>>> childHandle = nodeHandle.getChild(n);
                childHandle.ifPresent(handle -> {
                    Pair<Optional<byte[]>, Optional<byte[]>> child = handle.getUserData();
                    if (child != null && child.getValue1().isPresent()) {
                        childrenMerkleValues.add(child.getValue1().get().clone());
                    }
                });
            }

            trieNodesIterator.add(new InsertTrieNode(
                    storageValue,
                    merkleValueCopy,
                    childrenMerkleValues,
                    partialKeyNibbles
            ));
        }

        return trieNodesIterator;
    }

    @SuppressWarnings("unchecked")
    TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> initializeDatabaseStorageTrie() {
        final int STATE_VERSION = 1; // TODO: Figure out where we'll fetch this state version from

        // Get main storage's entries
        var genesisStorageRaw = AppBean.getBean(ChainService.class).getGenesis().getGenesis().getRaw();
        Map<String, String> mainStorage = genesisStorageRaw.get("top");

        // The chain specification only contains trie nodes that have a storage value attached
        // to them, while the database needs to know all trie nodes (including branch nodes).
        // The good news is that we can determine the latter from the former, which we do
        // here.
        TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure = new TrieStructure<>(mainStorage.size());
        // Initialize the trieStructure
        {

            // First, build the trie structure
            for (Map.Entry<String, String> entry : mainStorage.entrySet()) {
                Nibbles key = Nibbles.of(new BytesToNibbles(entry.getKey().getBytes()));
                byte[] value = entry.getValue().getBytes();

                switch (trieStructure.node(key)) {
                    case Vacant<Pair<Optional<byte[]>, Optional<byte[]>>> vacant -> {
                        vacant
                            .prepareInsert()
                            .insert(new Pair<>(Optional.of(value), Optional.empty()));
                    }
                    case BranchNodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>> handle -> {
                        handle.setUserData(new Pair<>(Optional.of(value), Optional.empty()));
                        handle.convertToStorageNode();
                    }
                    case StorageNodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>> __ -> {
                        // We have a duplicate entry:
                        // a second value corresponding to an already inserted key from the genesis storage.
                        // NOTE: don't throw?
                        throw new IllegalStateException("Duplicate key in genesis storage (raw.top).");
                    }
                }
            }

            // Then, calculate the Merkle values of the nodes
            List<TrieNodeIndex> reverseOrderIndices =
                trieStructure
                    .streamOrdered()
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.reverse(reverseOrderIndices);

            for (TrieNodeIndex nodeIndex : reverseOrderIndices) {
                var nodeHandle = Objects.requireNonNull(trieStructure.nodeHandleAtIndex(nodeIndex));

                List<Optional<byte[]>> children = IntStream.range(0, 16)
                    .mapToObj(n ->
                        nodeHandle
                            .getChild(Nibble.fromInt(n))
                            .map(child ->
                                child
                                    .getUserData()
                                    .getValue1()
                                    .get() // We're sure this is present, as it's already been traversed if it's a child of the current node
                                    .clone())
                    ).toList();

                boolean isRootNode = nodeHandle.isRootNode();
                Nibbles partialKey = nodeHandle.getPartialKey().copy();

                Optional<byte[]> storageValueHashed = Optional.empty();
                var maybeStorageValue = nodeHandle.getUserData().getValue0();
                storageValueHashedInit: {
                    if (maybeStorageValue.isPresent()) {
                        var storageValue = maybeStorageValue.get();

                        if (STATE_VERSION == 1 && storageValue.length >= 33) {
                            storageValueHashed = Optional.of(HashUtils.hashWithBlake2b(storageValue));
                        }
                    }
                }

                StorageValue storageValue = null;
                storageValueInit: {
                    if (storageValueHashed.isPresent()) {
                        storageValue = new StorageValue(storageValueHashed.get(), true);
                    } else if (maybeStorageValue.isPresent()) {
                        storageValue = new StorageValue(maybeStorageValue.get(), false);
                    }
                }

                DecodedNode<List<Byte>> decoded = new DecodedNode<>(
                        // TODO:
                        //  All of this ugly boilerplate for a simple List<Optional<byte[]>> to List<Byte>[] conversion...
                        //  figure out a better representation
                        List.of(children.stream().map(p -> p
                                .map(ba -> new ArrayList<>(List.of(ArrayUtils.toObject(ba))))
                                .orElse(null)).toArray(ArrayList[]::new)),
                        partialKey,
                        storageValue
                );

                byte[] merkleValue = decoded.calculateMerkleValue(
                    HashUtils::hashWithBlake2b,
                    isRootNode
                );

                // Ideally, we'd want to mutate the user data, but our tuples are immutable ;)
                nodeHandle.setUserData(nodeHandle.getUserData().setAt1(Optional.of(merkleValue)));
            }
        }

        return trieStructure;
    }

    /**
     * Inserts trie nodes into the key-value repository.
     *
     * @param db The key-value repository where trie nodes are to be stored.
     * @param insertTrieNodes The list of trie nodes to be inserted.
     * @param entriesVersion The version number of the trie entries.
     */
    private void insertStorage(KVRepository<String, Object> db, List<InsertTrieNode> insertTrieNodes, byte entriesVersion) {
        try {
            for (InsertTrieNode trieNode : insertTrieNodes) {
                //Insert tn(trie node) to db
                db.save("tn:" + new String(trieNode.getMerkleValue()), trieNode.getPartialKeyNibbles());



                // Handle storage value
                final InsertTrieNode.InsertStorageValue storageValue = trieNode.getStorageValue();
                final TrieNodeData tnsValue = new TrieNodeData(
                        storageValue.referencesMerkleValue() ? null : storageValue.value(),
                        storageValue.referencesMerkleValue() ? storageValue.value() : null,
                        entriesVersion);

                //Insert tns(trie node storage) to db
                db.save("tns:" + new String(trieNode.getMerkleValue()), tnsValue);

                // Insert children
                insertChildren(db, trieNode);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to insert trie structure to db storage", e);
        }
    }

    /**
     * Inserts the children of a given trie node into
     the key-value repository.

     @param db The key-value repository where trie node children are to be stored.
     @param trieNode The trie node whose children are to be inserted.
     */
    private void insertChildren(KVRepository<String, Object> db, InsertTrieNode trieNode)  {
        for (int childNum = 0; childNum < trieNode.getChildrenMerkleValues().size(); childNum++) {
            byte[] child = trieNode.getChildrenMerkleValues().get(childNum);
            NodeChildData nodeChildData = new NodeChildData(childNum, child);
            //Insert tnc(trie node child) to db
            db.save("tnc:" + new String(trieNode.getMerkleValue()), nodeChildData);
        }
    }


}
