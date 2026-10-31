package com.drremote.dsp408.proxy.stream;

public final class NoopTrafficSink implements TrafficSink {
    public static final NoopTrafficSink INSTANCE = new NoopTrafficSink();

    private NoopTrafficSink() {
    }
}
