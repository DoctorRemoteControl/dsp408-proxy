package com.drremote.dsp408.proxy.cli;

import com.drremote.dsp408.proxy.control.ProxyControlServer;
import com.drremote.dsp408.proxy.server.DspProxyServer;
import com.drremote.dsp408.proxy.server.ProxyConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ProxyMain {
    private ProxyMain() {
    }

    public static void main(String[] args) throws Exception {
        if (contains(args, "--help") || contains(args, "-h")) {
            printUsage();
            return;
        }

        ProxyConfig config = args.length == 0 ? promptInteractive() : fromArgs(args);
        ProxyControlServer controlServer = null;

        System.out.println();
        System.out.println("Starte DSP408 Proxy ...");
        System.out.println("Track DSP verbinden mit : " + config.listenHost() + ":" + config.listenPort());
        System.out.println("Weiterleitung an        : " + config.targetHost() + ":" + config.targetPort());
        System.out.println("Log-Verzeichnis         : " + config.logDir().toAbsolutePath());
        if (config.streamEnabled()) {
            System.out.println("Live-Stream (Server)    : " + config.streamHost() + ":" + config.streamPort());
        }
        if (config.controlEnabled()) {
            System.out.println("Control-Port            : " + config.controlHost() + ":" + config.controlPort());
        }
        System.out.println();

        DspProxyServer server = new DspProxyServer(config);
        try {
            server.start();

            if (config.controlEnabled()) {
                controlServer = new ProxyControlServer(config.controlHost(), config.controlPort(), server);
                controlServer.start();
            }

            System.out.println("Proxy laeuft.");
            System.out.println("Jetzt in Track DSP als Ziel-IP den Proxy eintragen.");
            System.out.println("Wenn Track DSP auf demselben PC laeuft:");
            System.out.println("  IP   : " + config.listenHost());
            System.out.println("  Port : " + config.listenPort());
            System.out.println();
            System.out.println("Kommandos: status | quit");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("proxy> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim().toLowerCase();
                if (line.isEmpty()) {
                    continue;
                }
                if ("quit".equals(line) || "exit".equals(line) || "q".equals(line)) {
                    break;
                }
                if ("status".equals(line)) {
                    System.out.println("Session: " + (server.isSessionActive() ? "connected" : "idle"));
                    continue;
                }
                if ("help".equals(line)) {
                    System.out.println("Kommandos: status | quit");
                    continue;
                }
                System.out.println("Unbekanntes Kommando. 'help' zeigt verfuegbare Kommandos.");
            }
        } finally {
            if (controlServer != null) {
                controlServer.close();
            }
            server.close();
            System.out.println("Proxy beendet.");
        }
    }

    private static ProxyConfig fromArgs(String[] args) {
        Map<String, String> values = parseNamedArgs(args);

        String listenHost = values.getOrDefault("listen-host", "127.0.0.1");
        int listenPort = Integer.parseInt(values.getOrDefault("listen-port", "9761"));
        String targetHost = values.getOrDefault("target-host", values.getOrDefault("target-ip", "192.168.0.166"));
        int targetPort = Integer.parseInt(values.getOrDefault("target-port", "9761"));
        Path logDir = Path.of(values.getOrDefault("log-dir", "proxy_logs"));
        String streamHost = values.get("stream-host");
        Integer streamPort = values.containsKey("stream-port")
                ? Integer.parseInt(values.get("stream-port"))
                : null;
        String controlHost = values.get("control-host");
        Integer controlPort = values.containsKey("control-port")
                ? Integer.parseInt(values.get("control-port"))
                : null;

        return new ProxyConfig(
                listenHost,
                listenPort,
                targetHost,
                targetPort,
                logDir,
                streamHost,
                streamPort,
                controlHost,
                controlPort
        );
    }

    private static ProxyConfig promptInteractive() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("DSP408 Proxy / Mitschnitt");
        System.out.println("Einfach ENTER druecken, um den Standardwert zu uebernehmen.");
        System.out.println();

        String listenHost = ask(reader, "Proxy-Listen-IP", "127.0.0.1");
        int listenPort = Integer.parseInt(ask(reader, "Proxy-Listen-Port", "9761"));
        String targetHost = ask(reader, "DSP-IP", "192.168.0.166");
        int targetPort = Integer.parseInt(ask(reader, "DSP-Port", "9761"));
        Path logDir = Path.of(ask(reader, "Log-Verzeichnis", "proxy_logs"));
        String streamHost = ask(reader, "Stream-Bind-Host (leer = aus)", "");
        String streamPortText = ask(reader, "Stream-Bind-Port (leer = aus)", "");
        Integer streamPort = streamPortText.isBlank() ? null : Integer.parseInt(streamPortText);

        if (streamHost.isBlank()) {
            streamHost = null;
            streamPort = null;
        }

        String controlHost = ask(reader, "Control-Host (leer = aus)", "");
        String controlPortText = ask(reader, "Control-Port (leer = aus)", "");
        Integer controlPort = controlPortText.isBlank() ? null : Integer.parseInt(controlPortText);

        if (controlHost.isBlank()) {
            controlHost = null;
            controlPort = null;
        }

        return new ProxyConfig(
                listenHost,
                listenPort,
                targetHost,
                targetPort,
                logDir,
                streamHost,
                streamPort,
                controlHost,
                controlPort
        );
    }

    private static String ask(BufferedReader reader, String label, String defaultValue) throws Exception {
        System.out.print(label + " [" + defaultValue + "]: ");
        String line = reader.readLine();
        if (line == null) {
            return defaultValue;
        }
        line = line.trim();
        return line.isEmpty() ? defaultValue : line;
    }

    private static boolean contains(String[] args, String value) {
        for (String arg : args) {
            if (value.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> parseNamedArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            if (key.contains("=")) {
                String[] parts = key.split("=", 2);
                values.put(parts[0], parts[1]);
                continue;
            }
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                values.put(key, args[++i]);
            } else {
                values.put(key, "true");
            }
        }
        return values;
    }

    private static void printUsage() {
        System.out.println("DSP408 Proxy");
        System.out.println();
        System.out.println("Interaktiv:");
        System.out.println("  java -jar dsp408-proxy-0.1.0-SNAPSHOT-all.jar");
        System.out.println();
        System.out.println("Mit Parametern:");
        System.out.println("  java -jar dsp408-proxy-0.1.0-SNAPSHOT-all.jar \\");
        System.out.println("    --listen-host 127.0.0.1 --listen-port 9761 \\");
        System.out.println("    --target-host 192.168.0.166 --target-port 9761 \\");
        System.out.println("    --log-dir proxy_logs \\");
        System.out.println("    --stream-host 127.0.0.1 --stream-port 19081 \\");
        System.out.println("    --control-host 127.0.0.1 --control-port 19082");
    }
}
