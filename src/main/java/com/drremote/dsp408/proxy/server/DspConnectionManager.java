package com.drremote.dsp408.proxy.server;

import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.drremote.dsp408.proxy.model.TrafficDirection;
import com.drremote.dsp408.proxy.stream.TrafficSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class DspConnectionManager implements AutoCloseable {
    private final ProxyConfig config;
    private final TrafficSink sink;
    private final Object connectLock = new Object();
    private final Object dspWriteLock = new Object();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private final ReentrantLock controlTxLock = new ReentrantLock();
    private final Condition controlReleased = controlTxLock.newCondition();
    private final AtomicBoolean controlTxActive = new AtomicBoolean(false);
    private volatile long controlLeaseUntilMs = 0L;

    private Socket dspSocket;
    private InputStream dspIn;
    private OutputStream dspOut;
// [PROBLEM2CODE] WARNING: The value of the field DspConnectionManager.dspReaderThread is not used
    private Thread dspReaderThread;

    private ProxySessionLog sessionLog;
    private DspTrafficLogger trafficLogger;
    private Path sessionFile;
    private AtomicBoolean writerRunning;
    private Thread writerThread;

    public DspConnectionManager(ProxyConfig config, TrafficSink sink) {
        this.config = config;
        this.sink = sink;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isReady() {
        return connected.get() && dspOut != null;
    }

    public boolean isControlTransactionActive() {
        return controlTxActive.get();
    }

    public boolean ensureConnected() {
        if (isReady()) {
            return true;
        }
        synchronized (connectLock) {
            if (isReady() || closing.get()) {
                return isReady();
            }
            try {
                startConnection();
                return true;
            } catch (IOException e) {
                System.out.println("[PROXY] DSP-Verbindung fehlgeschlagen: " + e.getMessage());
                closeQuietly(dspSocket);
                dspSocket = null;
                dspIn = null;
                dspOut = null;
                endSession("DSP-Verbindung fehlgeschlagen: " + e.getMessage());
                return false;
            }
        }
    }

    public void registerClient(Socket clientSocket) {
        if (clientSocket == null) {
            return;
        }
        if (!ensureConnected()) {
            closeQuietly(clientSocket);
            return;
        }
        try {
            ClientConnection client = new ClientConnection(clientSocket);
            clients.add(client);
            if (trafficLogger != null) {
                trafficLogger.addNote("Client verbunden: " + client.address());
            }
            client.start();
        } catch (IllegalStateException e) {
            if (trafficLogger != null) {
                trafficLogger.addNote("Client-Initialisierung fehlgeschlagen: " + e.getMessage());
            }
            closeQuietly(clientSocket);
        }
    }

    public void injectPcToDsp(byte[] chunk) throws IOException {
        if (chunk == null || chunk.length == 0) {
            return;
        }
        if (!isReady()) {
            throw new IOException("DSP-Stream nicht bereit.");
        }
        synchronized (dspWriteLock) {
            dspOut.write(chunk);
            dspOut.flush();
        }
        if (trafficLogger != null) {
            trafficLogger.acceptChunk(TrafficDirection.PC_TO_DSP, chunk);
        }
    }

    public boolean beginControlTransaction(long timeoutMs) {
        long leaseMs = timeoutMs > 0 ? timeoutMs : 7000L;
        long now = System.currentTimeMillis();
        controlTxLock.lock();
        try {
            if (controlTxActive.get()) {
                if (controlLeaseUntilMs > 0 && now >= controlLeaseUntilMs) {
                    controlTxActive.set(false);
                } else {
                    return false;
                }
            }
            controlTxActive.set(true);
            controlLeaseUntilMs = now + leaseMs;
            return true;
        } finally {
            controlTxLock.unlock();
        }
    }

    public void endControlTransaction() {
        controlTxLock.lock();
        try {
            controlTxActive.set(false);
            controlLeaseUntilMs = 0L;
            controlReleased.signalAll();
        } finally {
            controlTxLock.unlock();
        }
    }

    public void resetSession() {
        closing.set(true);
        endSession("Session reset requested");
        closing.set(false);
    }

    private void waitForControlRelease() throws InterruptedException {
        if (!controlTxActive.get()) {
            return;
        }
        controlTxLock.lock();
        try {
            while (controlTxActive.get()) {
                long now = System.currentTimeMillis();
                long remaining = controlLeaseUntilMs - now;
                if (remaining <= 0) {
                    controlTxActive.set(false);
                    controlLeaseUntilMs = 0L;
                    controlReleased.signalAll();
                    return;
                }
                controlReleased.awaitNanos(remaining * 1_000_000L);
            }
        } finally {
            controlTxLock.unlock();
        }
    }

    private void startConnection() throws IOException {
        dspSocket = new Socket();
        dspSocket.connect(new InetSocketAddress(config.targetHost(), config.targetPort()), 3000);
        dspIn = dspSocket.getInputStream();
        dspOut = dspSocket.getOutputStream();

        sessionLog = new ProxySessionLog();
        sessionLog.setSessionId(TimeUtil.fileTimestamp());
        sessionLog.setStartedAt(TimeUtil.nowUtc());
        sessionLog.setListenHost(config.listenHost());
        sessionLog.setListenPort(config.listenPort());
        sessionLog.setTargetHost(config.targetHost());
        sessionLog.setTargetPort(config.targetPort());
        sessionLog.setClientAddress("MULTI");

        sessionFile = config.logDir().resolve("proxy_session_" + sessionLog.getSessionId() + ".json");

        trafficLogger = new DspTrafficLogger(sessionLog, sink);
        try {
            sink.onSessionStarted(sessionLog);
        } catch (IOException e) {
            System.out.println("[PROXY] Session-Start konnte nicht an Sink gesendet werden: " + e.getMessage());
        }

        writerRunning = new AtomicBoolean(true);
        writerThread = Thread.ofVirtual().name("dsp-proxy-writer").start(this::liveWriterLoop);

        connected.set(true);
        dspReaderThread = Thread.ofVirtual().name("dsp-proxy-dsp-reader").start(this::dspReaderLoop);
    }

    private void dspReaderLoop() {
        byte[] buffer = new byte[2048];
        try {
            while (!closing.get()) {
                int read = dspIn.read(buffer);
                if (read < 0) {
                    return;
                }
                if (read == 0) {
                    continue;
                }

                byte[] chunk = Arrays.copyOf(buffer, read);
                broadcastToClients(chunk);
                if (trafficLogger != null) {
                    trafficLogger.acceptChunk(TrafficDirection.DSP_TO_PC, chunk);
                }
            }
        } catch (IOException e) {
            if (!closing.get() && trafficLogger != null) {
                trafficLogger.addNote("DSP_TO_PC Fehler: " + e.getMessage());
            }
        } finally {
            endSession("DSP-Verbindung beendet");
        }
    }

    private void broadcastToClients(byte[] chunk) {
        for (ClientConnection client : clients) {
            if (!client.send(chunk)) {
                clients.remove(client);
                if (trafficLogger != null) {
                    trafficLogger.addNote("Client getrennt: " + client.address());
                }
            }
        }
    }

    private void liveWriterLoop() {
        while (writerRunning.get()) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                synchronized (sessionLog) {
                    DspProxySessionWriter.write(sessionFile, sessionLog);
                }
            } catch (IOException e) {
                System.out.println("[PROXY] Live-Write fehlgeschlagen: " + e.getMessage());
            }
        }
    }

    private void endSession(String note) {
        synchronized (connectLock) {
            if (!connected.get()) {
                return;
            }
            connected.set(false);
        }

        endControlTransaction();

        if (trafficLogger != null && note != null) {
            trafficLogger.addNote(note);
        }

        for (ClientConnection client : clients) {
            client.close();
        }
        clients.clear();

        closeQuietly(dspSocket);
        dspSocket = null;
        dspIn = null;
        dspOut = null;

        if (writerRunning != null) {
            writerRunning.set(false);
        }
        if (writerThread != null) {
            try {
                writerThread.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (sessionLog != null) {
            sessionLog.setEndedAt(TimeUtil.nowUtc());
            try {
                synchronized (sessionLog) {
                    DspProxySessionWriter.write(sessionFile, sessionLog);
                }
            } catch (IOException e) {
                System.out.println("[PROXY] Konnte Sitzung nicht speichern: " + e.getMessage());
            }

            try {
                sink.onSessionEnded(sessionLog);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void close() {
        closing.set(true);

        for (ClientConnection client : clients) {
            client.close();
        }
        clients.clear();

        endSession("Proxy beendet");
    }

    private final class ClientConnection {
        private final Socket socket;
        private final String address;
        private final InputStream in;
        private final OutputStream out;
        private final Object outLock = new Object();
        private final AtomicBoolean active = new AtomicBoolean(true);

        ClientConnection(Socket socket) {
            this.socket = socket;
            this.address = String.valueOf(socket.getRemoteSocketAddress());
            try {
                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Client-Streams nicht verfuegbar: " + e.getMessage(), e);
            }
        }

        String address() {
            return address;
        }

        void start() {
            Thread.ofVirtual().name("dsp-proxy-client-reader").start(this::readerLoop);
        }

        void readerLoop() {
            byte[] buffer = new byte[2048];
            try {
                while (active.get() && !closing.get()) {
                    int read = in.read(buffer);
                    if (read < 0) {
                        return;
                    }
                    if (read == 0) {
                        continue;
                    }
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    waitForControlRelease();
                    injectPcToDsp(chunk);
                }
            } catch (IOException e) {
                if (active.get() && trafficLogger != null) {
                    trafficLogger.addNote("Client-Fehler (" + address + "): " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                close();
            }
        }

        boolean send(byte[] chunk) {
            if (!active.get()) {
                return false;
            }
            try {
                synchronized (outLock) {
                    out.write(chunk);
                    out.flush();
                }
                return true;
            } catch (IOException e) {
                close();
                return false;
            }
        }

        void close() {
            if (active.compareAndSet(true, false)) {
                closeQuietly(socket);
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
