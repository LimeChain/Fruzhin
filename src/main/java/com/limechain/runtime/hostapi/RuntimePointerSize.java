package com.limechain.runtime.hostapi;

public record RuntimePointerSize(long pointerSize) {
    public RuntimePointerSize(int pointer, int size) {
        this((long)size << 32 | (pointer & 0xffffffffL));
    }

    public RuntimePointerSize(Number pointerSize) {
        this(pointerSize.longValue());
    }
    public int pointer() {
        return (int) this.pointerSize;
    }

    public int size() {
        return (int) (pointerSize >> 32);
    }
}
