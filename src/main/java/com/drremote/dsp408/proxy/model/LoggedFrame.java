package com.drremote.dsp408.proxy.model;

public class LoggedFrame {
    private int index;
    private String direction;
    private String timestampUtc;
    private String frameHex;
    private boolean validFrame;
    private String payloadHex;
    private String payloadPrettyHex;
    private String payloadAscii;
    private int payloadLength;
    private String note;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTimestampUtc() {
        return timestampUtc;
    }

    public void setTimestampUtc(String timestampUtc) {
        this.timestampUtc = timestampUtc;
    }

    public String getFrameHex() {
        return frameHex;
    }

    public void setFrameHex(String frameHex) {
        this.frameHex = frameHex;
    }

    public boolean isValidFrame() {
        return validFrame;
    }

    public void setValidFrame(boolean validFrame) {
        this.validFrame = validFrame;
    }

    public String getPayloadHex() {
        return payloadHex;
    }

    public void setPayloadHex(String payloadHex) {
        this.payloadHex = payloadHex;
    }

    public String getPayloadPrettyHex() {
        return payloadPrettyHex;
    }

    public void setPayloadPrettyHex(String payloadPrettyHex) {
        this.payloadPrettyHex = payloadPrettyHex;
    }

    public String getPayloadAscii() {
        return payloadAscii;
    }

    public void setPayloadAscii(String payloadAscii) {
        this.payloadAscii = payloadAscii;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
