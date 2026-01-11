package org.firstinspires.ftc.teamcode.telelib;

/**
 * One field definition from the JSON schema.
 *
 * This is a tiny data holder: name, type, and unit.
 * The JSON file is the rulebook; this class is one line in that rulebook.
 */
public class SchemaField {
    public final String name;
    public final String type;
    public final String unit;

    /**
     * Create a schema field from the JSON name/type/unit triplet.
     */
    public SchemaField(String name, String type, String unit) {
        // Store the field info exactly as defined in the JSON schema.
        this.name = name;
        this.type = type;
        this.unit = unit;
    }
}
