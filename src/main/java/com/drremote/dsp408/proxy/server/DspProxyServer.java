package com.drremote.dsp408.proxy.server;

import com.drremote.dsp408.proxy.stream.NdjsonStreamServer;
import com.drremote.dsp408.proxy.stream.NoopTrafficSink;
import com.drremote.dsp408.proxy.stream.TrafficSink;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DspProxyServer implements Closeable {
    private final ProxyConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptThread;

    private TrafficSink streamSink = NoopTrafficSink.INSTANCE;
    private DspConnectionManager connectionManager;

    public DspProxyServer(ProxyConfig config) {
        this.config = config;
    }

    public boolean isSessionActive() {
        return connectionManager != null && connectionManager.isConnected();
    }

    public boolean isInjectReady() {
        return connectionManager != null && connectionManager.isReady();
    }

    public boolean isControlTransactionActive() {
        return connectionManager != null && connectionManager.isControlTransactionActive();
    }

    public ProxyConfig getConfig() {
        return config;
    }

    public boolean injectPcToDsp(byte[] frame) {
        if (connectionManager == null) {
            return false;
        }
        if (!connectionManager.ensureConnected()) {
            return false;
        }
        try {
            connectionManager.injectPcToDsp(frame);
            return true;
        } catch (IOException e) {
            System.out.println("[PROXY] Inject fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    public boolean ensureStandaloneSession() {
        return connectionManager != null && connectionManager.ensureConnected();
    }

    public boolean beginControlTransaction(long timeoutMs) {
        return connectionManager != null && connectionManager.beginControlTransaction(timeoutMs);
    }

    public void endControlTransaction() {
        if (connectionManager != null) {
            connectionManager.endControlTransaction();
        }
    }

    public void resetSession() {
        if (connectionManager != null) {
            connectionManager.resetSession();
        }
    }

    public void start() throws IOException {
        Path logDir = config.logDir();
        Files.createDirectories(logDir);

        if (config.streamEnabled()) {
            streamSink = new NdjsonStreamServer(config.streamHost(), config.streamPort());
        }

        connectionManager = new DspConnectionManager(config, streamSink);

        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(config.listenHost(), config.listenPort()));
        running.set(true);
        acceptThread = Thread.ofVirtual().name("dsp-proxy-accept").start(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().name("dsp-proxy-client").start(() -> handleClient(clientSocket));
            } catch (SocketException e) {
                if (running.get()) {
                    System.out.println("[PROXY] Socket-Fehler im Accept-Loop: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.out.println("[PROXY] Fehler im Accept-Loop: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        System.out.println("[PROXY] Client verbunden: " + clientSocket.getRemoteSocketAddress());
        if (connectionManager != null) {
            connectionManager.registerClient(clientSocket);
        } else {
            closeQuietly(clientSocket);
        }
    }

    @Override
    public void close() {
        running.set(false);
        closeQuietly(serverSocket);

        if (connectionManager != null) {
            connectionManager.close();
        }

        try {
            streamSink.close();
        } catch (IOException ignored) {
        }

        if (acceptThread != null) {
            try {
                acceptThread.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
