package com.drremote.dsp408.proxy.stream;

import com.drremote.dsp408.proxy.model.LoggedFrame;
import com.drremote.dsp408.proxy.model.ProxySessionLog;

import java.io.Closeable;
import java.io.IOException;

public interface TrafficSink extends Closeable {
    default void onSessionStarted(ProxySessionLog session) throws IOException {
    }

    default void onFrame(String sessionId, LoggedFrame frame) throws IOException {
    }

    default void onNote(String sessionId, String note) throws IOException {
    }

    default void onSessionEnded(ProxySessionLog session) throws IOException {
    }

    @Override
    default void close() throws IOException {
    }
}
