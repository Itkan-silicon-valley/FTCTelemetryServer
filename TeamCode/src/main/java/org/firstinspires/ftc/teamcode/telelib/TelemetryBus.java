package org.firstinspires.ftc.teamcode.telelib;

public interface TelemetryBus extends AutoCloseable {
    void start();

    void begin();

    void put(String name, String value);

    void put(String name, double value, String format);

    void put(String name, long value);

    void publish();

    LiveConfigRegistry config();

    boolean isEnabled();

    @Override
    void close();
}
