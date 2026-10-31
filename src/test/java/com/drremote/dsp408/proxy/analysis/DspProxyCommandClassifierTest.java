package com.drremote.dsp408.proxy.analysis;

import com.drremote.dsp408.proxy.core.DspHex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DspProxyCommandClassifierTest {
    @Test
    void classifiesReadParameterBlock() {
        byte[] payload = DspHex.hexToBytes("0001022700");
        DspProxyCommand command = DspProxyCommandClassifier.classify(payload);

        assertEquals(0x27, command.commandCode());
        assertEquals("Read-Parameterblock", command.commandName());
        assertEquals(DspProxyCommandType.READ, command.type());
    }

    @Test
    void classifiesSetGain() {
        byte[] payload = DspHex.hexToBytes("00010434000100");
        DspProxyCommand command = DspProxyCommandClassifier.classify(payload);

        assertEquals(0x34, command.commandCode());
        assertEquals(DspProxyCommandType.WRITE, command.type());
    }
}
