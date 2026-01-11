package org.firstinspires.ftc.teamcode.telelib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the list of allowed telemetry fields and their order.
 *
 * Think of this like a numbered list of "labels" for each data value.
 */
public class FieldCatalog {
    private final List<SchemaField> fields = new ArrayList<>();
    private final Map<String, Integer> indexByName = new HashMap<>();

    /**
     * Add a field definition and return its index in the catalog.
     */
    public int add(String name, String type, String unit) {
        // Add the field to the end of the list and remember its index.
        int index = fields.size();
        fields.add(new SchemaField(name, type, unit));
        indexByName.put(name, index);
        return index;
    }

    public int size() {
        // Total number of fields.
        return fields.size();
    }

    /**
     * Retrieve a field definition by index (index -> name/type/unit).
     */
    public SchemaField get(int index) {
        // Get a field by its numeric index.
        return fields.get(index);
    }

    /**
     * Look up the index for a field name.
     */
    public Integer indexOf(String name) {
        // Find the index for a field name (fast lookup).
        return indexByName.get(name);
    }

    /**
     * Return all field definitions in the order they were added.
     */
    public List<SchemaField> getFields() {
        // All fields, in order.
        return fields;
    }
}
