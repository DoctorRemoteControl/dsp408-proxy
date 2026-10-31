package com.drremote.dsp408.proxy.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DspFrameCodecTest {
    @Test
    void validatesBinaryPayloadContainingEndMarkers() {
        byte[] payload = {1,0,5,0x24,0x10,0x03,0x55,0x66};
        byte[] frame = DspFrameCodec.buildFrame(payload);
        assertTrue(DspFrameCodec.isValidFrame(frame));
        assertArrayEquals(payload, DspFrameCodec.extractPayload(frame));
    }

    @Test
    void rejectsCorruptChecksum() {
        byte[] frame = DspFrameCodec.buildFrame(new byte[]{0,1,1,0x2c});
        frame[frame.length-1] ^= 1;
        assertFalse(DspFrameCodec.isValidFrame(frame));
    }
    @Test
    void buildsAndValidatesFrame() {
        byte[] payload = DspHex.hexToBytes("0001012C");
        byte[] frame = DspFrameCodec.buildFrame(payload);

        assertTrue(DspFrameCodec.isValidFrame(frame));
        assertArrayEquals(payload, DspFrameCodec.extractPayload(frame));
    }
}
