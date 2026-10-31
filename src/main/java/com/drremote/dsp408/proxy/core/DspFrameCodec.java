package com.drremote.dsp408.proxy.core;

/** Compatibility facade over the shared DSP408 wire protocol. */
public final class DspFrameCodec {
    public static final byte DLE = 0x10;
    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;

    private DspFrameCodec() { }

    public static byte[] buildFrame(byte[] payload) {
        return de.drremote.dsp408.protocol.DspFrameCodec.encode(payload);
    }

    public static byte[] buildFrameFromPayloadHex(String payloadHex) {
        return buildFrame(DspHex.hexToBytes(payloadHex));
    }

    public static byte xor(byte[] data) {
        byte value = 0;
        for (byte b : data) value ^= b;
        return value;
    }

    public static boolean isValidFrame(byte[] frame) {
        return de.drremote.dsp408.protocol.DspFrameCodec.isValidFrame(frame);
    }

    public static byte[] extractPayload(byte[] frame) {
        var decoded = de.drremote.dsp408.protocol.DspFrameCodec.decode(frame);
        if (!decoded.checksumValid()) throw new IllegalArgumentException("Ungueltiger DSP-Frame.");
        return decoded.payload();
    }

    public static int findClosingDleEtx(byte[] frame) {
        if (frame == null || frame.length < 5) return -1;
        int end = frame.length - 3;
        return frame[end] == DLE && frame[end + 1] == ETX ? end : -1;
    }
}
