package com.drremote.dsp408.proxy.analysis;

public class DspProxyWriteCandidate {
    private final int frameIndex;
    private final String timestampUtc;
    private final String direction;
    private final int commandCode;
    private final String commandName;
    private final Integer channelIndex;
    private final Integer rawValue;
    private final Double approxDb;
    private final String payloadHex;
    private final String note;

    public DspProxyWriteCandidate(int frameIndex,
                                  String timestampUtc,
                                  String direction,
                                  int commandCode,
                                  String commandName,
                                  Integer channelIndex,
                                  Integer rawValue,
                                  Double approxDb,
                                  String payloadHex,
                                  String note) {
        this.frameIndex = frameIndex;
        this.timestampUtc = timestampUtc;
        this.direction = direction;
        this.commandCode = commandCode;
        this.commandName = commandName;
        this.channelIndex = channelIndex;
        this.rawValue = rawValue;
        this.approxDb = approxDb;
        this.payloadHex = payloadHex;
        this.note = note;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public String getTimestampUtc() {
        return timestampUtc;
    }

    public String getDirection() {
        return direction;
    }

    public int getCommandCode() {
        return commandCode;
    }

    public String getCommandName() {
        return commandName;
    }

    public Integer getChannelIndex() {
        return channelIndex;
    }

    public Integer getRawValue() {
        return rawValue;
    }

    public Double getApproxDb() {
        return approxDb;
    }

    public String getPayloadHex() {
        return payloadHex;
    }

    public String getNote() {
        return note;
    }
}
