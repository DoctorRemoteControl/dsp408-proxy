package com.drremote.dsp408.proxy.server;

import java.nio.file.Path;

public record ProxyConfig(
        String listenHost,
        int listenPort,
        String targetHost,
        int targetPort,
        Path logDir,
        String streamHost,
        Integer streamPort,
        String controlHost,
        Integer controlPort
) {
    public boolean streamEnabled() {
        return streamHost != null
                && !streamHost.isBlank()
                && streamPort != null
                && streamPort > 0;
    }

    public boolean controlEnabled() {
        return controlHost != null
                && !controlHost.isBlank()
                && controlPort != null
                && controlPort > 0;
    }
}