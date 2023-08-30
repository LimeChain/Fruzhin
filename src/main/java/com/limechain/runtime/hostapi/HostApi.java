package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.SyncedState;
import lombok.extern.java.Log;
import net.openhft.hashing.LongHashFunction;
import org.apache.tomcat.util.buf.HexUtils;
import org.wasmer.Memory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;

@Log
public class HostApi {

    private static final String keyToIgnore = ":child_storage:default:";
    private static Runtime runtime;
    private static KVRepository<String, Object> repository = SyncedState.getInstance().getRepository();

    public static void extStorageSetVersion1(long keyPtr, long valuePtr) {
        byte[] key = getDataFromMemory(keyPtr);
        byte[] value = getDataFromMemory(valuePtr);

        repository.save(new String(key), value);
    }

    public static int extStorageGetVersion1(long keyPtr) {
        byte[] key = getDataFromMemory(keyPtr);
        Object data = repository.find(new String(key)).orElse(null);
        if (data instanceof byte[] dataArray) {
            return putDataToMemory(dataArray);
        }
        return 0;
    }

    public static byte[] getDataFromMemory(long pointer) {
        int ptr = (int) pointer;
        int ptrLength = (int) (pointer >> 32);
        byte[] data = new byte[ptrLength];

        Memory memory = getMemory();
        memory.buffer().get(ptr, data, 0, ptrLength);
        return data;
    }

    public static int putDataToMemory(byte[] data) {
        Memory memory = getMemory();
        ByteBuffer buffer = getByteBuffer(memory);
        int position = buffer.position();
        buffer.put(data, 0, data.length);
        buffer.position(position + data.length);
        return position;
    }

    private static Memory getMemory() {
        return runtime.getInstance().exports.getMemory("memory");
    }

    public static void extMiscPrintHex(long pointer) {
        byte[] data = getDataFromMemory(pointer);
        System.out.println(HexUtils.toHexString(data));
    }

    public static void extLoggingLog(int level, long targetPtr, long messagePtr) {
        byte[] target = getDataFromMemory(targetPtr);
        byte[] message = getDataFromMemory(messagePtr);

        log.log(getLogLevel(level), new String(target) + ": " + new String(message));
    }

    public static void setRuntime(Runtime runtime) {
        HostApi.runtime = runtime;
    }

    private static Level getLogLevel(int i) {
        return switch (i) {
            case 0 -> Level.SEVERE;
            case 1 -> Level.WARNING;
            case 2 -> Level.INFO;
            case 3 -> Level.FINE;
            default -> Level.FINEST;
        };
    }

    public static void setRepository(KVRepository<String, Object> repository) {
        HostApi.repository = repository;
    }

    public static int extAllocatorMallocVersion1(int size) {
        Memory memory = getMemory();
        ByteBuffer buffer = getByteBuffer(memory);
        int position = buffer.position();
        if (size > buffer.limit() - position) {
            memory.grow(buffer.limit() - position);
        }
        buffer.position(position + size);

        return position;
    }

    private static ByteBuffer getByteBuffer(Memory memory) {
        ByteBuffer buffer;
        try {
            Field bufferField = memory.getClass().getDeclaredField("buffer");
            bufferField.setAccessible(true);
            buffer = (ByteBuffer) bufferField.get(memory);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.log(Level.WARNING, "Unable to get buffer", e);
            buffer = null;
        }
        return buffer != null ? buffer : memory.buffer();
    }

    public static int extHashingTwox64Version1(long addr) {
        byte[] dataToHash = getDataFromMemory(addr);

        byte[] hash0 = hash64(0, dataToHash);

        return putDataToMemory(hash0);
    }

    public static int extHashingTwox128Version1(long addr) {
        byte[] dataToHash = getDataFromMemory(addr);

        byte[] hash0 = hash64(0, dataToHash);
        byte[] hash1 = hash64(1, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(hash0);
        buffer.put(hash1);

        byte[] byteArray = buffer.array();
        return putDataToMemory(byteArray);
    }

    public static int extHashingTwox256Version1(long addr) {
        byte[] dataToHash = getDataFromMemory(addr);

        byte[] hash0 = hash64(0, dataToHash);
        byte[] hash1 = hash64(1, dataToHash);
        byte[] hash2 = hash64(2, dataToHash);
        byte[] hash3 = hash64(3, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(hash0);
        buffer.put(hash1);
        buffer.put(hash2);
        buffer.put(hash3);

        byte[] byteArray = buffer.array();
        return putDataToMemory(byteArray);
    }

    private static byte[] hash64(int seed, byte[] dataToHash) {
        final long res3 = LongHashFunction
                .xx(seed)
                .hashBytes(dataToHash.clone());

        final ByteBuffer buffer = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(res3);

        return buffer.array();
    }

    public static void extAllocatorFreeVersion1() {
    }
}
