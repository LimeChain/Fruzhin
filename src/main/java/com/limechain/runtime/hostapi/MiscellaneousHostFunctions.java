package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.utils.scale.exceptions.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.lang.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Memory;
import org.wasmer.Module;
import org.wasmer.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Implementations of the Miscellaneous and Logging HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-misc-api">Miscellaneous API</a>}
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-logging-api">Logging API</a>}
 */
@Log
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MiscellaneousHostFunctions {

    private final HostApi hostApi;

    public static List<ImportObject> getFunctions(final HostApi hostApi) {
        return new MiscellaneousHostFunctions(hostApi).buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_misc_print_num_version_1", argv ->
                        printNumV1(argv.get(0)), List.of(Type.I64)),
                HostApi.getImportObject("ext_misc_print_utf8_version_1", argv ->
                        printUtf8V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64)),
                HostApi.getImportObject("ext_misc_print_hex_version_1", argv ->
                        printHexV1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64)),
                HostApi.getImportObject("ext_misc_runtime_version_version_1", argv ->
                                runtimeVersionV1(new RuntimePointerSize(argv.get(0))).pointerSize(),
                        List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_logging_log_version_1", argv ->
                        logV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                new RuntimePointerSize(argv.get(1))), Arrays.asList(Type.I32, Type.I64, Type.I64)),
                HostApi.getImportObject("ext_logging_max_level_version_1", argv ->
                        maxLevelV1(), List.of(), Type.I32)
        );
    }

    /**
     * Print a number.
     *
     * @param number the number to be printed
     */
    public void printNumV1(Number number) {
        log.info("Printing number from runtime: " + number);
    }

    /**
     * Print a valid UTF8 encoded buffer.
     *
     * @param strPointer a pointer-size to the valid buffer to be printed.
     */
    public void printUtf8V1(RuntimePointerSize strPointer) {
        byte[] data = hostApi.getDataFromMemory(strPointer);

        final String strToPrint = new String(data, StandardCharsets.UTF_8);
        log.info("Printing utf8 from runtime: " + strToPrint);
    }

    /**
     * Print any buffer in hexadecimal representation.
     *
     * @param pointer a pointer-size to the buffer to be printed.
     */
    public void printHexV1(RuntimePointerSize pointer) {
        byte[] data = hostApi.getDataFromMemory(pointer);

        final String hexString = HexUtils.toHexString(data);
        log.info("Printing hex from runtime: " + hexString);
    }

    /**
     * Extract the Runtime version of the given Wasm blob by calling Core_version. Returns the SCALE encoded runtime
     * version or None if the call fails. This function gets primarily used when upgrading Runtimes.
     * <p>
     * <b>Caution</b>
     * Calling this function is very expensive and should only be done very occasionally. For getting the runtime
     * version, it requires instantiating the Wasm blob and calling the Core_version function in this blob.
     *
     * @param data a pointer-size to the Wasm blob
     * @return a pointer-size to the SCALE encoded Option value containing the Runtime version of the given Wasm blob
     * which is encoded as a byte array.
     */
    public RuntimePointerSize runtimeVersionV1(RuntimePointerSize data) {
        byte[] wasmBlob = hostApi.getDataFromMemory(data);

        byte[] versionOption;

        try {
            Module module = new Module(wasmBlob);
            Runtime runtime = new Runtime(module, RuntimeBuilder.DEFAULT_HEAP_PAGES);
            Memory memory = runtime.getInstance().exports.getMemory("memory");
            Object[] response = runtime.call("Core_version");

            byte[] runtimeVersionData = null;
            if (response != null && response[0] != null) {
                final RuntimePointerSize responsePointer = new RuntimePointerSize((long) response[0]);
                runtimeVersionData = new byte[responsePointer.size()];
                memory.buffer().get(responsePointer.pointer(), runtimeVersionData, 0, responsePointer.size());
            }
            versionOption = scaleEncodedOption(runtimeVersionData);
        } catch (UnsatisfiedLinkError e) {
            log.log(Level.SEVERE, "Error loading wasm module: " + e.getMessage());
            versionOption = scaleEncodedOption(null);
        }

        return hostApi.writeDataToMemory(versionOption);
    }

    /**
     * Request to print a log message on the host. Note that this will be only displayed if the host is enabled to
     * display log messages with given level and target.
     *
     * @param level      the log level
     * @param targetPtr  a pointer-size to the string which contains the path, module or location from where the log
     *                   was executed.
     * @param messagePtr a pointer-size to the UTF-8 encoded log message.
     */
    public void logV1(int level, RuntimePointerSize targetPtr, RuntimePointerSize messagePtr) {
        byte[] target = hostApi.getDataFromMemory(targetPtr);
        byte[] message = hostApi.getDataFromMemory(messagePtr);

        final String messageToPrint = new String(message, StandardCharsets.UTF_8);
        final String targetToPrint = new String(target);

        log.log(internalGetLogLevel(level), String.format("Log message from runtime: target=%s, message=%s",
                targetToPrint,
                messageToPrint));
    }

    private Level internalGetLogLevel(int i) {
        return switch (i) {
            case 0 -> Level.SEVERE;
            case 1 -> Level.WARNING;
            case 2 -> Level.INFO;
            case 3 -> Level.FINE;
            default -> Level.FINEST;
        };
    }

    /**
     * Returns the max logging level used by the host.
     * <p>
     * Levels are as follows:
     * Error = 1
     * Warn = 2
     * Info = 3
     * Debug = 4
     * Trace = 5
     * </p>
     * We have Severe, Warning, Info, Fine and Finest
     * </p>
     * If we map
     * Severe -> Error
     * Warning -> Warn
     * Info -> Info
     * Fine -> Debug
     * Finest -> Trace
     * </p>
     * our max level is 5. (The returned value is the index of the level - 4)
     *
     * @return the max log level used by the host.
     */
    public int maxLevelV1() {
        return 4;
    }

    private byte[] scaleEncodedOption(@Nullable byte[] data) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeOptional(ScaleCodecWriter::writeByteArray, data);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
        return buf.toByteArray();
    }
}
