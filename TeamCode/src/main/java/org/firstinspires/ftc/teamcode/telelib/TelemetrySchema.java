package org.firstinspires.ftc.teamcode.telelib;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Loads a telemetry field schema from JSON.
 *
 * The schema defines which fields are allowed and their order.
 * Think of it like a class roster: only names in the roster are allowed.
 */
public class TelemetrySchema {
    private final int port;
    private final boolean strict;
    private final List<SchemaField> fields;
    private final Map<String, Integer> indexByName;

    private TelemetrySchema(int port, boolean strict, List<SchemaField> fields) {
        this.port = port;
        this.strict = strict;
        this.fields = fields;
        this.indexByName = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            indexByName.put(fields.get(i).name, i);
        }
    }

    /**
     * Load a schema from assets, a local file, or raw JSON text.
     */
    public static TelemetrySchema fromConfig(HardwareMap hardwareMap, String pathOrJson) {
        // Load the JSON text, then parse it into fields we can use in code.
        JSONObject json = loadConfig(hardwareMap, pathOrJson);
        int port = json.optInt("port", 5599);
        boolean strict = json.optBoolean("strict", false);
        JSONArray fieldArray = json.optJSONArray("fields");
        if (fieldArray == null) {
            throw new IllegalArgumentException("Telemetry schema must include a fields array.");
        }
        List<SchemaField> fields = new ArrayList<>();
        for (int i = 0; i < fieldArray.length(); i++) {
            JSONObject field = fieldArray.optJSONObject(i);
            if (field == null) {
                continue;
            }
            // Each field is a name + type + unit.
            fields.add(
                    new SchemaField(
                            field.optString("name", ""),
                            field.optString("type", "double"),
                            field.optString("unit", "")));
        }
        return new TelemetrySchema(port, strict, fields);
    }

    /**
     * TCP port the telemetry server should listen on.
     */
    public int getPort() {
        // TCP port where the robot will listen for the laptop connection.
        return port;
    }

    /**
     * Whether to throw on unknown fields (strict mode).
     */
    public boolean isStrict() {
        // Strict mode means: crash if you try to publish a field not in the schema.
        return strict;
    }

    /**
     * Ordered list of schema fields defined in the JSON file.
     */
    public List<SchemaField> getFields() {
        // Full list of fields in the exact order from the JSON file.
        return fields;
    }

    /**
     * Resolve a field name to its index in the schema, or null if missing.
     */
    public Integer indexOf(String name) {
        // Fast lookup so we can fill values in a fixed array.
        return indexByName.get(name);
    }

    /**
     * Clean a string so it is safe to put in one CSV column.
     */
    public static String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", ";").replace("\n", " ").replace("\r", " ");
    }

    private static JSONObject loadConfig(HardwareMap hardwareMap, String pathOrJson) {
        // If the string starts with "{", treat it as raw JSON.
        String trimmed = pathOrJson == null ? "" : pathOrJson.trim();
        if (trimmed.startsWith("{")) {
            try {
                return new JSONObject(trimmed);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid schema JSON string.", e);
            }
        }
        // Otherwise, treat it like a file path (assets or disk).
        String jsonText = readConfigText(hardwareMap, trimmed);
        try {
            return new JSONObject(jsonText);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema JSON file.", e);
        }
    }

    private static String readConfigText(HardwareMap hardwareMap, String path) {
        /*
         * Load order:
         * 1) Android assets (TeamCode/src/main/assets).
         * 2) App files dir (/data/user/0/.../files) when a file is pushed manually.
         */
        IOException lastException = null;
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Config path is empty.");
        }
        try (InputStream in = hardwareMap.appContext.getAssets().open(path)) {
            // First try assets (good for on-bot files).
            return readAll(in);
        } catch (IOException e) {
            lastException = e;
        }

        File file = new File(path);
        if (!file.isAbsolute()) {
            // If it's a relative path, search inside the app's files folder.
            file = new File(hardwareMap.appContext.getFilesDir(), path);
        }
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load telemetry schema: " + path, lastException);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        // Read all text from the input stream into one big String.
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}
