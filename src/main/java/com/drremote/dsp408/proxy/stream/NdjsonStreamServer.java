package com.drremote.dsp408.proxy.stream;

import com.drremote.dsp408.proxy.model.LoggedFrame;
import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class NdjsonStreamServer implements TrafficSink {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CopyOnWriteArrayList<ClientWriter> clients = new CopyOnWriteArrayList<>();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final AtomicReference<ProxySessionLog> currentSession = new AtomicReference<>();
    private final AtomicBoolean sessionOpen = new AtomicBoolean(false);

    public NdjsonStreamServer(String bindHost, int bindPort) throws IOException {
        this.serverSocket = new ServerSocket();
        this.serverSocket.bind(new InetSocketAddress(bindHost, bindPort));
        this.acceptThread = Thread.ofVirtual().name("ndjson-stream-accept").start(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                ClientWriter client = new ClientWriter(socket);
                clients.add(client);
                client.start();

                ProxySessionLog session = currentSession.get();
                if (sessionOpen.get() && session != null) {
                    client.enqueue(serialize(buildSessionStarted(session)));
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.out.println("[STREAM] Accept-Fehler: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onSessionStarted(ProxySessionLog session) {
        currentSession.set(session);
        sessionOpen.set(true);
        broadcast(buildSessionStarted(session));
    }

    @Override
    public void onFrame(String sessionId, LoggedFrame frame) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "frame");
        event.put("sessionId", sessionId);
        event.put("frame", frame);
        broadcast(event);
    }

    @Override
    public void onNote(String sessionId, String note) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "note");
        event.put("sessionId", sessionId);
        event.put("note", note);
        broadcast(event);
    }

    @Override
    public void onSessionEnded(ProxySessionLog session) {
        sessionOpen.set(false);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "session_ended");
        event.put("sessionId", session.getSessionId());
        event.put("endedAt", session.getEndedAt());
        event.put("bytesPcToDsp", session.getBytesPcToDsp());
        event.put("bytesDspToPc", session.getBytesDspToPc());
        event.put("frameCount", session.getFrames() == null ? 0 : session.getFrames().size());
        event.put("droppedEvents", droppedEvents.get());
        broadcast(event);
    }

    private Map<String, Object> buildSessionStarted(ProxySessionLog session) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "session_started");
        event.put("sessionId", session.getSessionId());
        event.put("startedAt", session.getStartedAt());
        event.put("listenHost", session.getListenHost());
        event.put("listenPort", session.getListenPort());
        event.put("targetHost", session.getTargetHost());
        event.put("targetPort", session.getTargetPort());
        event.put("clientAddress", session.getClientAddress());
        return event;
    }

    private void broadcast(Object event) {
        String line = serialize(event);
        if (line == null) {
            return;
        }
        for (ClientWriter client : clients) {
            if (!client.enqueue(line)) {
                droppedEvents.incrementAndGet();
            }
        }
    }

    private String serialize(Object event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void close() {
        running.set(false);

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }

        for (ClientWriter client : clients) {
            client.close();
        }
        clients.clear();

        try {
            acceptThread.join(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final class ClientWriter {
        private final Socket socket;
        private final BufferedWriter writer;
        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final Thread worker;

        ClientWriter(Socket socket) throws IOException {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.worker = Thread.ofVirtual().name("ndjson-stream-writer").start(this::writerLoop);
        }

        void start() {
            // already running
        }

        boolean enqueue(String line) {
            if (!active.get()) {
                return false;
            }
            return queue.offer(line);
        }

        private void writerLoop() {
            try {
                while (active.get() || !queue.isEmpty()) {
                    String line = queue.poll(250, TimeUnit.MILLISECONDS);
                    if (line == null) {
                        continue;
                    }
                    writer.write(line);
                    writer.write('\n');
                    writer.flush();
                }
            } catch (Exception e) {
                active.set(false);
            } finally {
                active.set(false);
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }

        void close() {
            active.set(false);
            long deadline = System.currentTimeMillis() + 1500L;
            while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                worker.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
