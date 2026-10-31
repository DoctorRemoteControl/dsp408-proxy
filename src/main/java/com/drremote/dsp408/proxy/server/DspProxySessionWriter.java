package com.drremote.dsp408.proxy.server;

import com.drremote.dsp408.proxy.model.ProxySessionLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DspProxySessionWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private DspProxySessionWriter() {
    }

    public static void write(Path file, ProxySessionLog sessionLog) throws IOException {
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), sessionLog);
    }
}
