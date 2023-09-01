package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.SyncedState;
import lombok.extern.java.Log;
import org.wasmer.Memory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.logging.Level;

@Log
public class HostApi {

    protected static final String KEY_TO_IGNORE = ":child_storage:default:";
    protected static Runtime runtime;
    protected static final KVRepository<String, Object> repository = SyncedState.getInstance().getRepository();

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

    public static void extStorageClearVersion1(long keyPtr) {
        byte[] key = getDataFromMemory(keyPtr);
        repository.delete(new String(key));
    }

    public static long extStorageClearPrefixVersion2(long prefixPtr, long limitPtr) {
        String prefix = new String(getDataFromMemory(prefixPtr));
        int limit = new BigInteger(getDataFromMemory(limitPtr)).intValue();

        int deleted = repository.deletePrefix(prefix, limit);
        //TODO: Count how many are left?

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
        buffer.put(position, data, 0, data.length);
        buffer.position(position + data.length);

        return position;
    }

    protected static Memory getMemory() {
        return runtime.getInstance().exports.getMemory("memory");
    }



    public static void setRuntime(Runtime runtime) {
        HostApi.runtime = runtime;
    }




    protected static ByteBuffer getByteBuffer(Memory memory) {
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


}
