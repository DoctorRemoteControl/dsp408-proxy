package com.drremote.dsp408.proxy.core;

public final class DspResponseAnalyzer {
    private DspResponseAnalyzer() {
    }

    public static String printableAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes == null ? 0 : bytes.length);
        if (bytes == null) {
            return "";
        }
        for (byte b : bytes) {
            int v = Byte.toUnsignedInt(b);
            sb.append(v >= 32 && v <= 126 ? (char) v : '.');
        }
        return sb.toString();
    }
}
