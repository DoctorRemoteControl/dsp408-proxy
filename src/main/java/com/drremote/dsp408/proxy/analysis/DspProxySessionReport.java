package com.drremote.dsp408.proxy.analysis;

import java.util.ArrayList;
import java.util.List;

public class DspProxySessionReport {
    private int totalFrames;
    private int validFrames;
    private int commandFrames;
    private int readCommands;
    private int writeCommands;
    private int otherCommands;
    private List<DspProxyWriteCandidate> writeCandidates = new ArrayList<>();

    public int getTotalFrames() {
        return totalFrames;
    }

    public void setTotalFrames(int totalFrames) {
        this.totalFrames = totalFrames;
    }

    public int getValidFrames() {
        return validFrames;
    }

    public void setValidFrames(int validFrames) {
        this.validFrames = validFrames;
    }

    public int getCommandFrames() {
        return commandFrames;
    }

    public void setCommandFrames(int commandFrames) {
        this.commandFrames = commandFrames;
    }

    public int getReadCommands() {
        return readCommands;
    }

    public void setReadCommands(int readCommands) {
        this.readCommands = readCommands;
    }

    public int getWriteCommands() {
        return writeCommands;
    }

    public void setWriteCommands(int writeCommands) {
        this.writeCommands = writeCommands;
    }

    public int getOtherCommands() {
        return otherCommands;
    }

    public void setOtherCommands(int otherCommands) {
        this.otherCommands = otherCommands;
    }

    public List<DspProxyWriteCandidate> getWriteCandidates() {
        return writeCandidates;
    }

    public void setWriteCandidates(List<DspProxyWriteCandidate> writeCandidates) {
        this.writeCandidates = writeCandidates;
    }

    public void addWriteCandidate(DspProxyWriteCandidate candidate) {
        this.writeCandidates.add(candidate);
    }
}
