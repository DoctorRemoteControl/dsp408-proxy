package com.drremote.dsp408.proxy.server;

import com.drremote.dsp408.proxy.core.DspFrameCodec;
import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.drremote.dsp408.proxy.model.TrafficDirection;
import com.drremote.dsp408.proxy.stream.TrafficSink;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DspTrafficLoggerTest {
    @Test void logsFragmentedBinaryFramesInBothDirections() {
        var log = new ProxySessionLog();
        var logger = new DspTrafficLogger(log, new TrafficSink() {});
        byte[] wire = DspFrameCodec.buildFrame(new byte[]{1,0,5,0x24,0x10,0x03,0x55,0x66});
        for (byte b : wire) {
            logger.acceptChunk(TrafficDirection.PC_TO_DSP, new byte[]{b});
            logger.acceptChunk(TrafficDirection.DSP_TO_PC, new byte[]{b});
        }
        assertEquals(2, log.getFrames().size());
        assertTrue(log.getFrames().stream().allMatch(f -> f.isValidFrame()));
        assertEquals(wire.length, log.getBytesPcToDsp());
        assertEquals(wire.length, log.getBytesDspToPc());
    }

    @Test void recordsCorruptFrameAndContinuesWithNextFrame() {
        var log = new ProxySessionLog();
        var logger = new DspTrafficLogger(log, new TrafficSink() {});
        byte[] wire = DspFrameCodec.buildFrame(new byte[]{0,1,1,0x2c});
        byte[] corrupt = wire.clone(); corrupt[corrupt.length-1] ^= 1;
        logger.acceptChunk(TrafficDirection.DSP_TO_PC, corrupt);
        logger.acceptChunk(TrafficDirection.DSP_TO_PC, wire);
        assertEquals(2, log.getFrames().size());
        assertFalse(log.getFrames().getFirst().isValidFrame());
        assertTrue(log.getFrames().getLast().isValidFrame());
    }
}
