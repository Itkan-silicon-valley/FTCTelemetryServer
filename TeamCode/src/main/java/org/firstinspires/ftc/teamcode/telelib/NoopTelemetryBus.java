package org.firstinspires.ftc.teamcode.telelib;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class NoopTelemetryBus implements TelemetryBus {
    private static final LiveConfigRegistry NOOP_CONFIG =
            new LiveConfigRegistry() {
                @Override
                public void registerDouble(
                        String name,
                        DoubleSupplier getter,
                        DoubleConsumer setter,
                        double min,
                        double max) {}
            };

    @Override
    public void start() {}

    @Override
    public void begin() {}

    @Override
    public void put(String name, String value) {}

    @Override
    public void put(String name, double value, String format) {}

    @Override
    public void put(String name, long value) {}

    @Override
    public void publish() {}

    @Override
    public LiveConfigRegistry config() {
        return NOOP_CONFIG;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void close() {}
}
