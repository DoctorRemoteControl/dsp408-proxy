package com.drremote.dsp408.proxy.control;

public class ProxyControlCommand {
    private String type;
    private String frameHex;
    private String payloadHex;
    private Long timeoutMs;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrameHex() {
        return frameHex;
    }

    public void setFrameHex(String frameHex) {
        this.frameHex = frameHex;
    }

    public String getPayloadHex() {
        return payloadHex;
    }

    public void setPayloadHex(String payloadHex) {
        this.payloadHex = payloadHex;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
