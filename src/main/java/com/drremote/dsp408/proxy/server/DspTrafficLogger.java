package com.drremote.dsp408.proxy.server;

import com.drremote.dsp408.proxy.core.DspFrameCodec;
import com.drremote.dsp408.proxy.core.DspHex;
import com.drremote.dsp408.proxy.core.DspResponseAnalyzer;
import com.drremote.dsp408.proxy.model.LoggedFrame;
import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.drremote.dsp408.proxy.model.TrafficDirection;
import com.drremote.dsp408.proxy.stream.TrafficSink;
import de.drremote.dsp408.protocol.DspFrameDecoder;

public final class DspTrafficLogger {
    private final DspFrameDecoder pcToDsp = new DspFrameDecoder();
    private final DspFrameDecoder dspToPc = new DspFrameDecoder();
    private int frameCounter = 1;
    private final ProxySessionLog sessionLog;
    private final TrafficSink sink;

    public DspTrafficLogger(ProxySessionLog sessionLog, TrafficSink sink) {
        this.sessionLog = sessionLog;
        this.sink = sink;
    }

    public synchronized void acceptChunk(TrafficDirection direction, byte[] chunk) {
        if (chunk == null || chunk.length == 0) return;
        synchronized (sessionLog) {
            DspFrameDecoder decoder;
            if (direction == TrafficDirection.PC_TO_DSP) {
                sessionLog.setBytesPcToDsp(sessionLog.getBytesPcToDsp() + chunk.length);
                decoder = pcToDsp;
            } else {
                sessionLog.setBytesDspToPc(sessionLog.getBytesDspToPc() + chunk.length);
                decoder = dspToPc;
            }
            long discarded = decoder.discardedBytes();
            for (var frame : decoder.accept(chunk)) logFrame(direction, frame.raw());
            long delta = decoder.discardedBytes() - discarded;
            if (delta > 0) addNote(direction.name() + ": " + delta + " Byte Vorspann verworfen.");
        }
    }

    public synchronized void addNote(String note) {
        synchronized (sessionLog) {
            sessionLog.addNote(note);
        }
        try {
            sink.onNote(sessionLog.getSessionId(), note);
        } catch (Exception ignored) {
        }
    }

    private void logFrame(TrafficDirection direction, byte[] frame) {
        LoggedFrame entry = new LoggedFrame();
        entry.setIndex(frameCounter++);
        entry.setDirection(direction.name());
        entry.setTimestampUtc(TimeUtil.nowUtc());
        entry.setFrameHex(DspHex.bytesToHex(frame));
        entry.setValidFrame(DspFrameCodec.isValidFrame(frame));

        if (entry.isValidFrame()) {
            byte[] payload = DspFrameCodec.extractPayload(frame);
            entry.setPayloadHex(DspHex.bytesToHex(payload));
            entry.setPayloadPrettyHex(DspHex.prettyHex(payload));
            entry.setPayloadAscii(DspResponseAnalyzer.printableAscii(payload));
            entry.setPayloadLength(payload.length);
            entry.setNote("");
        } else {
            entry.setPayloadHex("");
            entry.setPayloadPrettyHex("");
            entry.setPayloadAscii("");
            entry.setPayloadLength(0);
            entry.setNote("Kein gueltiger DSP-Frame.");
        }

        sessionLog.addFrame(entry);
        try {
            sink.onFrame(sessionLog.getSessionId(), entry);
        } catch (Exception ignored) {
        }
    }

}
