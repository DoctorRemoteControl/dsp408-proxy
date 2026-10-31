package com.drremote.dsp408.proxy.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter ISO_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC);

    private TimeUtil() {
    }

    public static String nowUtc() {
        return ISO_MILLIS.format(Instant.now());
    }

    public static String fileTimestamp() {
        return FILE_STAMP.format(Instant.now());
    }
}
