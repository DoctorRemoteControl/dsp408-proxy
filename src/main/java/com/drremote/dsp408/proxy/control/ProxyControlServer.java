package com.drremote.dsp408.proxy.control;

import com.drremote.dsp408.proxy.core.DspFrameCodec;
import com.drremote.dsp408.proxy.core.DspHex;
import com.drremote.dsp408.proxy.server.DspProxyServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyControlServer implements Closeable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String host;
    private final int port;
    private final DspProxyServer proxyServer;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ProxyControlServer(String host, int port, DspProxyServer proxyServer) {
        this.host = host;
        this.port = port;
        this.proxyServer = proxyServer;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(host, port));
        running.set(true);
        acceptThread = Thread.ofVirtual().name("proxy-control-accept").start(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().name("proxy-control-client").start(() -> handleClient(socket));
            } catch (IOException e) {
                if (running.get()) {
                    System.out.println("[CONTROL] Accept-Fehler: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Map<String, Object> response = handleLine(line);
                writer.write(MAPPER.writeValueAsString(response));
                writer.write('\n');
                writer.flush();
            }
        } catch (Exception e) {
            System.out.println("[CONTROL] Client-Fehler: " + e.getMessage());
        }
    }

    private Map<String, Object> handleLine(String line) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            ProxyControlCommand cmd = MAPPER.readValue(line, ProxyControlCommand.class);
            if (cmd.getType() == null || cmd.getType().isBlank()) {
                return error("type fehlt");
            }

            String type = cmd.getType().trim().toLowerCase();

            if ("status".equals(type)) {
                out.put("ok", true);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("controlTxActive", proxyServer.isControlTransactionActive());
                return out;
            }

            if ("ensure_session".equals(type)) {
                boolean ok = proxyServer.ensureStandaloneSession();
                out.put("ok", ok);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("message", ok ? "Standalone-Session bereit" : "Standalone-Session konnte nicht gestartet werden");
                return out;
            }

            if ("begin_control_transaction".equals(type)) {
                long timeoutMs = cmd.getTimeoutMs() == null ? 7000L : cmd.getTimeoutMs();
                boolean ok = proxyServer.beginControlTransaction(timeoutMs);
                out.put("ok", ok);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("controlTxActive", proxyServer.isControlTransactionActive());
                out.put("message", ok ? "Control-Transaktion aktiv" : "Control-Transaktion bereits aktiv");
                return out;
            }

            if ("end_control_transaction".equals(type)) {
                proxyServer.endControlTransaction();
                out.put("ok", true);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("controlTxActive", proxyServer.isControlTransactionActive());
                out.put("message", "Control-Transaktion beendet");
                return out;
            }

            if ("reset_session".equals(type)) {
                proxyServer.resetSession();
                out.put("ok", true);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("controlTxActive", proxyServer.isControlTransactionActive());
                out.put("message", "Session reset");
                return out;
            }

            if ("send_frame".equals(type)) {
                if (cmd.getFrameHex() == null || cmd.getFrameHex().isBlank()) {
                    return error("frameHex fehlt");
                }
                if (!proxyServer.isInjectReady()) {
                    proxyServer.ensureStandaloneSession();
                }
                if (!proxyServer.isInjectReady()) {
                    out.put("ok", false);
                    out.put("sessionActive", proxyServer.isSessionActive());
                    out.put("injectReady", proxyServer.isInjectReady());
                    out.put("message", "Keine DSP-Session verfuegbar");
                    return out;
                }
                byte[] frame = DspHex.hexToBytes(cmd.getFrameHex());
                if (!DspFrameCodec.isValidFrame(frame)) {
                    return error("ungueltiger DSP-Frame");
                }
                boolean ok = proxyServer.injectPcToDsp(frame);

                out.put("ok", ok);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("message", ok ? "Frame injiziert" : "Injektion fehlgeschlagen");
                return out;
            }

            if ("send_payload".equals(type)) {
                if (cmd.getPayloadHex() == null || cmd.getPayloadHex().isBlank()) {
                    return error("payloadHex fehlt");
                }
                if (!proxyServer.isInjectReady()) {
                    proxyServer.ensureStandaloneSession();
                }
                if (!proxyServer.isInjectReady()) {
                    out.put("ok", false);
                    out.put("sessionActive", proxyServer.isSessionActive());
                    out.put("injectReady", proxyServer.isInjectReady());
                    out.put("message", "Keine DSP-Session verfuegbar");
                    return out;
                }
                byte[] frame = DspFrameCodec.buildFrameFromPayloadHex(cmd.getPayloadHex());
                boolean ok = proxyServer.injectPcToDsp(frame);

                out.put("ok", ok);
                out.put("sessionActive", proxyServer.isSessionActive());
                out.put("injectReady", proxyServer.isInjectReady());
                out.put("message", ok ? "Payload als Frame injiziert" : "Injektion fehlgeschlagen");
                return out;
            }

            return error("unbekannter type: " + cmd.getType());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("error", message);
        return out;
    }

    @Override
    public void close() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (acceptThread != null) {
            try {
                acceptThread.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
