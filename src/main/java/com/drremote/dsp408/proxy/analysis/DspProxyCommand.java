package com.drremote.dsp408.proxy.analysis;

public record DspProxyCommand(
        int commandCode,
        String commandName,
        DspProxyCommandType type,
        int payloadLength,
        byte[] data,
        String note
) {
}
