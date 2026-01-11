package org.firstinspires.ftc.teamcode.telelib;

import java.util.Arrays;

/**
 * One immutable snapshot of telemetry values for a single loop.
 *
 * Imagine taking a photo of all your data at one moment.
 */
public class TelemetrySnapshot {
    private final String[] values;

    /**
     * Create a snapshot with the given ordered value list.
     */
    public TelemetrySnapshot(String[] values) {
        // Store the values exactly as they are.
        this.values = values;
    }

    /**
     * Get a single value by its field index.
     */
    public String get(int index) {
        // Read one value by index.
        return values[index];
    }

    /**
     * Number of fields in this snapshot.
     */
    public int size() {
        // Number of values in this snapshot.
        return values.length;
    }

    /**
     * Render a CSV line for the requested field indexes, in order.
     */
    public String toCsv(int[] fieldIndexes) {
        // Build a CSV line in the order requested by the client.
        StringBuilder line = new StringBuilder(fieldIndexes.length * 8);
        for (int i = 0; i < fieldIndexes.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            int idx = fieldIndexes[i];
            String value = values[idx];
            line.append(value == null ? "" : value);
        }
        return line.toString();
    }

    /**
     * Create an empty snapshot with blank values.
     */
    public static TelemetrySnapshot empty(int fieldCount) {
        // Create an "empty" snapshot (all values blank).
        String[] vals = new String[fieldCount];
        Arrays.fill(vals, "");
        return new TelemetrySnapshot(vals);
    }
}
