package com.drremote.dsp408.proxy.core;

public final class DspHex {
    private DspHex() {
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("HEX darf nicht null sein.");
        }

        String normalized = hex.replaceAll("\\s+", "").toUpperCase();
        if ((normalized.length() & 1) != 0) {
            throw new IllegalArgumentException("Ungerade HEX-Laenge: " + normalized.length());
        }

        byte[] out = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            int hi = Character.digit(normalized.charAt(i), 16);
            int lo = Character.digit(normalized.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Ungueltiger HEX-Wert bei Position " + i);
            }
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static String prettyHex(byte[] bytes) {
        String hex = bytesToHex(bytes);
        return hex.replaceAll("(..)", "$1 ").trim();
    }
}
