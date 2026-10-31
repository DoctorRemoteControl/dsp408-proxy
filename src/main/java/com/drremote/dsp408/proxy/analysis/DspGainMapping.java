package com.drremote.dsp408.proxy.analysis;

public final class DspGainMapping {
    private DspGainMapping() {
    }

    public static double deviceValueToApproxDb(int value) {
        if (value < 0 || value > 400) {
            throw new IllegalArgumentException("Device-Value ausserhalb des erwarteten Bereichs: " + value);
        }
        if (value < 80) {
            return -60.0 + (value * 0.5);
        }
        return -20.0 + ((value - 80) * 0.1);
    }
}
