package com.drremote.dsp408.proxy.analysis;

import com.drremote.dsp408.proxy.core.DspHex;
import com.drremote.dsp408.proxy.model.LoggedFrame;
import com.drremote.dsp408.proxy.model.ProxySessionLog;

import java.util.List;
import java.util.Locale;

public final class DspProxySessionAnalyzer {
    private DspProxySessionAnalyzer() {
    }

    public static DspProxySessionReport analyze(ProxySessionLog session) {
        DspProxySessionReport report = new DspProxySessionReport();
        if (session == null || session.getFrames() == null) {
            return report;
        }

        List<LoggedFrame> frames = session.getFrames();
        report.setTotalFrames(frames.size());

        int validFrames = 0;
        int commandFrames = 0;
        int readCommands = 0;
        int writeCommands = 0;
        int otherCommands = 0;

        for (LoggedFrame frame : frames) {
            if (frame == null) {
                continue;
            }
            if (!"PC_TO_DSP".equalsIgnoreCase(frame.getDirection())) {
                continue;
            }
            if (!frame.isValidFrame()) {
                continue;
            }

            validFrames++;
            String payloadHex = frame.getPayloadHex();
            if (payloadHex == null || payloadHex.isBlank()) {
                continue;
            }

            byte[] payload;
            try {
                payload = DspHex.hexToBytes(payloadHex);
            } catch (Exception e) {
                continue;
            }

            DspProxyCommand command = DspProxyCommandClassifier.classify(payload);
            if (command.commandCode() >= 0) {
                commandFrames++;
            }

            switch (command.type()) {
                case READ -> readCommands++;
                case WRITE -> writeCommands++;
                case OTHER -> otherCommands++;
            }

            if (command.commandCode() == 0x34 || command.commandCode() == 0x33) {
                DspProxyWriteCandidate candidate = buildWriteCandidate(frame, command);
                if (candidate != null) {
                    report.addWriteCandidate(candidate);
                }
            }
        }

        report.setValidFrames(validFrames);
        report.setCommandFrames(commandFrames);
        report.setReadCommands(readCommands);
        report.setWriteCommands(writeCommands);
        report.setOtherCommands(otherCommands);
        return report;
    }

    private static DspProxyWriteCandidate buildWriteCandidate(LoggedFrame frame, DspProxyCommand command) {
        Integer channelIndex = null;
        Integer rawValue = null;
        Double approxDb = null;
        String note = "";

        if (command.commandCode() == 0x34) {
            byte[] data = command.data();
            if (data.length >= 3) {
                int packed = Byte.toUnsignedInt(data[0])
                        | (Byte.toUnsignedInt(data[1]) << 8)
                        | (Byte.toUnsignedInt(data[2]) << 16);
                channelIndex = packed & 0xFF;
                rawValue = (packed >>> 8) & 0xFFFF;
                try {
                    approxDb = DspGainMapping.deviceValueToApproxDb(rawValue);
                } catch (Exception e) {
                    note = "Gain ausserhalb des Mappings: " + e.getMessage();
                }
            } else {
                note = "Set-Gain Payload zu kurz.";
            }
        } else if (command.commandCode() == 0x33) {
            byte[] data = command.data();
            if (data.length >= 9) {
                int group = (Byte.toUnsignedInt(data[1]) << 8) | Byte.toUnsignedInt(data[0]);
                int param = (Byte.toUnsignedInt(data[3]) << 8) | Byte.toUnsignedInt(data[2]);
                int value = (Byte.toUnsignedInt(data[5]) << 8) | Byte.toUnsignedInt(data[4]);
                int tail = (Byte.toUnsignedInt(data[7]) << 8) | Byte.toUnsignedInt(data[6]);
                int flags = Byte.toUnsignedInt(data[8]);

                rawValue = value;
                note = String.format(Locale.ROOT,
                        "Cmd33 group=0x%04X param=0x%04X tail=0x%04X flags=0x%02X",
                        group, param, tail, flags);
            } else {
                note = "Cmd33 Payload zu kurz.";
            }
        }

        return new DspProxyWriteCandidate(
                frame.getIndex(),
                frame.getTimestampUtc(),
                frame.getDirection(),
                command.commandCode(),
                command.commandName(),
                channelIndex,
                rawValue,
                approxDb,
                frame.getPayloadPrettyHex(),
                note
        );
    }
}
