package com.drremote.dsp408.proxy.analysis;

import java.util.Arrays;

public final class DspProxyCommandClassifier {
    private DspProxyCommandClassifier() {
    }

    public static DspProxyCommand classify(byte[] payload) {
        if (payload == null || payload.length < 4) {
            return new DspProxyCommand(-1, "unbekannt", DspProxyCommandType.OTHER,
                    payload == null ? 0 : payload.length, new byte[0], "Payload zu kurz");
        }

        if (payload[0] != 0x00 || payload[1] != 0x01) {
            return new DspProxyCommand(-1, "unbekannt", DspProxyCommandType.OTHER,
                    payload.length, Arrays.copyOf(payload, payload.length), "Unbekanntes Header-Format");
        }

        int len = Byte.toUnsignedInt(payload[2]);
        int cmd = Byte.toUnsignedInt(payload[3]);
        int dataStart = 4;
        int dataLen = Math.max(0, Math.min(payload.length - dataStart, len - 1));
        byte[] data = Arrays.copyOfRange(payload, dataStart, dataStart + dataLen);

        return switch (cmd) {
            case 0x10 -> new DspProxyCommand(cmd, "Handshake-Init", DspProxyCommandType.OTHER, len, data, "");
            case 0x12 -> new DspProxyCommand(cmd, "KeepAlive", DspProxyCommandType.OTHER, len, data, "");
            case 0x13 -> new DspProxyCommand(cmd, "Handshake-Ack", DspProxyCommandType.OTHER, len, data, "");
            case 0x2C -> new DspProxyCommand(cmd, "SystemInfo", DspProxyCommandType.READ, len, data, "");
            case 0x2D -> new DspProxyCommand(cmd, "Login-PIN", DspProxyCommandType.WRITE, len, data, "");
            case 0x27 -> new DspProxyCommand(cmd, "Read-Parameterblock", DspProxyCommandType.READ, len, data, "");
            case 0x29 -> new DspProxyCommand(cmd, "Read-Presetname", DspProxyCommandType.READ, len, data, "");
            case 0x33 -> new DspProxyCommand(cmd, "Write-Parameter", DspProxyCommandType.WRITE, len, data, "");
            case 0x34 -> new DspProxyCommand(cmd, "Set-Gain", DspProxyCommandType.WRITE, len, data, "");
            default -> new DspProxyCommand(cmd, "Cmd-0x" + String.format("%02X", cmd), DspProxyCommandType.OTHER, len, data, "");
        };
    }
}
