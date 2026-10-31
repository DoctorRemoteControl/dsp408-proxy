package com.drremote.dsp408.proxy.stream;

import com.drremote.dsp408.proxy.model.LoggedFrame;
import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NdjsonTcpTrafficSink implements TrafficSink {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Socket socket;
    private final BufferedWriter writer;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicLong droppedEvents = new AtomicLong();
    private final Thread worker;

    public NdjsonTcpTrafficSink(String host, int port) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 1500);
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        this.worker = Thread.ofVirtual().name("traffic-sink-writer").start(this::writerLoop);
    }

    @Override
    public void onSessionStarted(ProxySessionLog session) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "session_started");
        event.put("sessionId", session.getSessionId());
        event.put("startedAt", session.getStartedAt());
        event.put("listenHost", session.getListenHost());
        event.put("listenPort", session.getListenPort());
        event.put("targetHost", session.getTargetHost());
        event.put("targetPort", session.getTargetPort());
        event.put("clientAddress", session.getClientAddress());
        enqueue(event);
    }

    @Override
    public void onFrame(String sessionId, LoggedFrame frame) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "frame");
        event.put("sessionId", sessionId);
        event.put("frame", frame);
        enqueue(event);
    }

    @Override
    public void onNote(String sessionId, String note) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "note");
        event.put("sessionId", sessionId);
        event.put("note", note);
        enqueue(event);
    }

    @Override
    public void onSessionEnded(ProxySessionLog session) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "session_ended");
        event.put("sessionId", session.getSessionId());
        event.put("endedAt", session.getEndedAt());
        event.put("bytesPcToDsp", session.getBytesPcToDsp());
        event.put("bytesDspToPc", session.getBytesDspToPc());
        event.put("frameCount", session.getFrames() == null ? 0 : session.getFrames().size());
        event.put("droppedEvents", droppedEvents.get());
        enqueue(event);
    }

    private void enqueue(Object event) {
        if (!active.get()) {
            return;
        }
        try {
            String line = MAPPER.writeValueAsString(event);
            if (!queue.offer(line)) {
                droppedEvents.incrementAndGet();
            }
        } catch (Exception ignored) {
        }
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

    @Override
    public void close() {
        active.set(false);
        long deadline = System.currentTimeMillis() + 3000L;
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        try {
            worker.join(1500L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
