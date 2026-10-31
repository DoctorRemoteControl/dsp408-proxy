package com.drremote.dsp408.proxy.cli;

import com.drremote.dsp408.proxy.analysis.DspProxyReportPrinter;
import com.drremote.dsp408.proxy.analysis.DspProxySessionAnalyzer;
import com.drremote.dsp408.proxy.analysis.DspProxySessionReport;
import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public final class ProxyAnalyzeMain {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProxyAnalyzeMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        File input = new File(args[0]);
        if (!input.exists()) {
            throw new IllegalArgumentException("Datei nicht gefunden: " + input.getPath());
        }

        ProxySessionLog session = MAPPER.readValue(input, ProxySessionLog.class);
        DspProxySessionReport report = DspProxySessionAnalyzer.analyze(session);
        DspProxyReportPrinter.print(report);
    }

    private static void printUsage() {
        System.out.println("Proxy-Analyze");
        System.out.println("Verwendung:");
        System.out.println("  java -cp target/dsp408-proxy-0.1.0-SNAPSHOT-all.jar com.drremote.dsp408.proxy.cli.ProxyAnalyzeMain <proxy_session.json>");
    }
}
