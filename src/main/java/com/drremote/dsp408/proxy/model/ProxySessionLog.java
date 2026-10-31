package com.drremote.dsp408.proxy.model;

import java.util.ArrayList;
import java.util.List;

public class ProxySessionLog {
    private String sessionId;
    private String startedAt;
    private String endedAt;
    private String listenHost;
    private int listenPort;
    private String targetHost;
    private int targetPort;
    private String clientAddress;
    private long bytesPcToDsp;
    private long bytesDspToPc;
    private List<String> notes = new ArrayList<>();
    private List<LoggedFrame> frames = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(String endedAt) {
        this.endedAt = endedAt;
    }

    public String getListenHost() {
        return listenHost;
    }

    public void setListenHost(String listenHost) {
        this.listenHost = listenHost;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public long getBytesPcToDsp() {
        return bytesPcToDsp;
    }

    public void setBytesPcToDsp(long bytesPcToDsp) {
        this.bytesPcToDsp = bytesPcToDsp;
    }

    public long getBytesDspToPc() {
        return bytesDspToPc;
    }

    public void setBytesDspToPc(long bytesDspToPc) {
        this.bytesDspToPc = bytesDspToPc;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public List<LoggedFrame> getFrames() {
        return frames;
    }

    public void setFrames(List<LoggedFrame> frames) {
        this.frames = frames;
    }

    public void addNote(String note) {
        notes.add(note);
    }

    public void addFrame(LoggedFrame frame) {
        frames.add(frame);
    }
}
