package com.drremote.dsp408.proxy.analysis;

public final class DspProxyReportPrinter {
    private DspProxyReportPrinter() {
    }

    public static void print(DspProxySessionReport report) {
        System.out.println("Proxy-Analyse");
        System.out.println("Frames        : " + report.getTotalFrames());
        System.out.println("Valid Frames  : " + report.getValidFrames());
        System.out.println("Commands      : " + report.getCommandFrames());
        System.out.println("Reads         : " + report.getReadCommands());
        System.out.println("Writes        : " + report.getWriteCommands());
        System.out.println("Other         : " + report.getOtherCommands());
        System.out.println();

        System.out.println("Write-Kandidaten: " + report.getWriteCandidates().size());
        System.out.println();

        int i = 1;
        for (DspProxyWriteCandidate candidate : report.getWriteCandidates()) {
            System.out.println("Write " + i++);
            System.out.println("  Frame     : " + candidate.getFrameIndex() + " (" + candidate.getDirection() + ")");
            System.out.println("  Time      : " + candidate.getTimestampUtc());
            System.out.println("  Command   : 0x" + String.format("%02X", candidate.getCommandCode()) + " " + candidate.getCommandName());
            if (candidate.getChannelIndex() != null) {
                System.out.println("  Kanal     : " + candidate.getChannelIndex());
            }
            if (candidate.getRawValue() != null) {
                System.out.println("  Raw       : " + candidate.getRawValue());
            }
            if (candidate.getApproxDb() != null) {
                System.out.println("  Wert      : " + String.format("%.1f dB", candidate.getApproxDb()));
            }
            if (candidate.getPayloadHex() != null && !candidate.getPayloadHex().isBlank()) {
                System.out.println("  Payload   : " + candidate.getPayloadHex());
            }
            if (candidate.getNote() != null && !candidate.getNote().isBlank()) {
                System.out.println("  Hinweis   : " + candidate.getNote());
            }
            System.out.println();
        }
    }
}
