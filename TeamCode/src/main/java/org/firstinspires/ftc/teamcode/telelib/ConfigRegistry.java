package org.firstinspires.ftc.teamcode.telelib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Registry of live-tunable variables.
 *
 * The dashboard can ask for the list of tunable values and then send
 * updates like: SET shooter_kp=0.02
 */
public final class ConfigRegistry {
    private final Map<String, DoubleEntry> doubles = new HashMap<>();

    /**
     * Register a live-tunable double value.
     *
     * The dashboard can LISTCFG to discover these entries and SET them by name.
     */
    public void registerDouble(
            String name, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        doubles.put(name, new DoubleEntry(name, getter, setter, min, max));
    }

    /**
     * Update a registered value using the raw string from a SET command.
     *
     * Returns true when the value exists, parses correctly, and is in range.
     */
    public boolean set(String name, String raw) {
        DoubleEntry entry = doubles.get(name);
        if (entry == null) {
            return false;
        }
        try {
            double value = Double.parseDouble(raw);
            if (!entry.isValid(value)) {
                return false;
            }
            entry.setter.accept(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public List<ConfigEntry> list() {
        return new ArrayList<>(doubles.values());
    }

    /**
     * Minimal interface used by the telemetry server when it lists config options.
     */
    public interface ConfigEntry {
        String getName();

        String getType();

        double getMin();

        double getMax();
    }

    private static final class DoubleEntry implements ConfigEntry {
        final String name;
        final DoubleSupplier getter;
        final DoubleConsumer setter;
        final double min;
        final double max;

        DoubleEntry(
                String name, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
        }

        boolean isValid(double value) {
            return value >= min && value <= max && !Double.isNaN(value);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return "double";
        }

        @Override
        public double getMin() {
            return min;
        }

        @Override
        public double getMax() {
            return max;
        }
    }
}
