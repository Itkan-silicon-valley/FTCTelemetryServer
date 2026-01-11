# telelib Developer README

This folder contains the on-robot telemetry stack used by the FTC dashboard. It is designed
for schema-driven telemetry with a tiny text protocol that a laptop dashboard can consume.

---

## Architecture overview

Flow (robot -> laptop):

1) `TelemetrySchema` loads a JSON schema from assets or file.
2) `SchemaTelemetryService` builds a fixed field catalog from the schema.
3) Each loop: `begin()` -> `put()` fields -> `publish()` a snapshot.
4) `TelemetryServer` broadcasts snapshots to connected clients.
5) The dashboard subscribes to the fields it needs via `SUB`.

Live config flow (laptop -> robot):

1) Robot registers tunables in `ConfigRegistry`.
2) Dashboard requests them with `LISTCFG`.
3) Dashboard updates values with `SET key=value`.

---

## Protocol (TCP text)

Client -> Server:

- `HELLO` or `FIELDS`
  Returns the full field catalog.

- `SUB field1,field2 rate=20`
  Subscribe to specific fields at the given rate (Hz).

- `SUB ALL rate=20` (or `SUB *`)
  Subscribe to all fields.

- `LISTCFG`
  Request live-config entries.

- `SET key=value`
  Update a live-config value.

Server -> Client:

- `FIELDS name,type,unit;name,type,unit;...`
  Schema field catalog.

- `CFG name,type,min,max;name,type,min,max;...`
  Live-config list (empty when no config registry exists).

- `OK`
  Acknowledgement (after `SUB`, and for valid `SET`).

- `ERR ...`
  Error (invalid command, bad format, or invalid config value).

- `DATA v1,v2,v3,...`
  One CSV line containing only the subscribed fields.

Notes:
- The server always streams in the order of the schema field list.
- The dashboard should use `FIELDS` to map names to indexes.

---

## Schema JSON

The schema lives in `TeamCode/src/main/assets/configs/telemetry_schema.json`.

Top-level keys:

- `port` (number): TCP port for telemetry server.
- `strict` (boolean): reject unknown field names when true.
- `graphs` (array): dashboard graph definitions.
- `subscribe` (array, optional): field names the dashboard should request.
  If omitted, the dashboard subscribes to all fields.
- `fields` (array): authoritative field list.

`graphs[]` object:

- `id` (string): unique graph id.
- `title` (string): graph title for the UI.
- `series` (array of strings): field names to plot together.

`fields[]` object:

- `name` (string): field name used by `SchemaTelemetryService.put(...)`.
- `type` (string): `double`, `long`, or `string`.
- `unit` (string): optional unit string.

---

## File responsibilities

- `SchemaTelemetryService.java`
  Main API for OpModes. Owns schema, catalog, and server. Creates snapshots per loop.

- `TelemetrySchema.java`
  Loads JSON from assets or app files dir. Parses port, strict mode, and field list.

- `TelemetryServer.java`
  TCP server that accepts clients, handles commands, and broadcasts snapshots.

- `FieldCatalog.java`
  Fixed ordered list of schema fields used to align CSV indexes.

- `TelemetrySnapshotBuilder.java`
  Mutable builder for one loop of data.

- `TelemetrySnapshot.java`
  Immutable snapshot that can render to CSV for a given field list.

- `ConfigRegistry.java`
  Registry of live-tunable values exposed via LISTCFG / SET.

- `LimelightTunnel.java`
  Simple TCP relay for forwarding Limelight ports.

- `LimelightTunnelManager.java`
  Starts/stops the standard Limelight tunnels with one call.

- `RobotVitals.java`
  Helper for common robot power telemetry (battery, hub current, hub input volts).

- `SchemaField.java`
  Small data holder for a single schema field entry.

---

## Typical OpMode usage

```
SchemaTelemetryService telemetryService =
        new SchemaTelemetryService(hardwareMap, "configs/telemetry_schema.json");
telemetryService.start();

waitForStart();
long runId = System.currentTimeMillis() / 1000L;

while (opModeIsActive()) {
    telemetryService.begin();
    telemetryService.put("run_id", runId);
    telemetryService.put("robot_ts_ms", System.currentTimeMillis());
    telemetryService.put("front_left_power", frontLeft.getPower(), "%.3f");
    telemetryService.publish();
}

telemetryService.close();
```

---

## Extending the system

- Add new fields to `telemetry_schema.json` first.
- Use the same field name in `telemetryService.put(...)`.
- If `strict` is true, unknown field names will throw.
- For new live config values, register them in `ConfigRegistry` and add them
  to the schema `fields` and `subscribe` lists.
