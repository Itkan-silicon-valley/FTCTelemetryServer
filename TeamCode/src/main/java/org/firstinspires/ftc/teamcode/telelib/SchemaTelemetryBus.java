package org.firstinspires.ftc.teamcode.telelib;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

public class SchemaTelemetryBus implements TelemetryBus {
    private final SchemaTelemetryService service;
    private final LiveConfigRegistry configRegistry;
    private boolean enabled = true;
    private boolean errorLogged;

    public SchemaTelemetryBus(HardwareMap hardwareMap, String schemaPathOrJson) {
        ConfigRegistry config = new ConfigRegistry();
        this.service = new SchemaTelemetryService(hardwareMap, schemaPathOrJson, config);
        this.configRegistry = new TelelibConfigRegistry(config);
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        try {
            service.start();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void begin() {
        if (!enabled) {
            return;
        }
        try {
            service.begin();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, String value) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, double value, String format) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value, format);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, long value) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void publish() {
        if (!enabled) {
            return;
        }
        try {
            service.publish();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public LiveConfigRegistry config() {
        return configRegistry;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void close() {
        if (!enabled) {
            return;
        }
        try {
            service.close();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    private void disableWithError(RuntimeException ex) {
        if (!errorLogged) {
            RobotLog.ee("SchemaTelemetry", ex, "Telemetry service error; disabling.");
            errorLogged = true;
        }
        enabled = false;
    }

    private static final class TelelibConfigRegistry implements LiveConfigRegistry {
        private final ConfigRegistry registry;

        private TelelibConfigRegistry(ConfigRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void registerDouble(
                String name,
                java.util.function.DoubleSupplier getter,
                java.util.function.DoubleConsumer setter,
                double min,
                double max) {
            registry.registerDouble(name, getter, setter, min, max);
        }
    }
}
