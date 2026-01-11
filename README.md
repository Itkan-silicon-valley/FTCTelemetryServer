# FTC Telemetry Server

Schema-driven telemetry server for FTC robots (Java + JSON).

---

## Features
- Monitor variable running on Control Hub and plot on [Telemetry Dashboard](https://github.com/Itkan-silicon-valley/FTCTelemetryDashboard)
- Live updates and turning to any variable running on [Telemetry Dashboard](https://github.com/Itkan-silicon-valley/FTCTelemetryDashboard)
- Logging and data storage for offline data analysis
- Real-time streaming of Limelight camera and metrics
- Access to Limelight webserver configuration via tunneling
- Access to Basic robot vitals like control hub voltage and current (total power)
- Developer details, see [Dev README](./TeamCode/src/main/java/org/firstinspires/ftc/teamcode/telelib/README.md)

---

## Quick Start

### 0) Clone repo to favorite folder

### 1) Copy the Java files into your FTC project

From this repo:
- Copy everything inside `TeamCode/src/main/java/` into your FTC project:
  `.../TeamCode/src/main/java/`
- Copy `TeamCode/src/main/assets/configs/telemetry_schema.json` into your FTC project at
  `TeamCode/src/main/assets/configs/telemetry_schema.json`.

### 2) Add telemetry to your OpMode

```java
import org.firstinspires.ftc.teamcode.telelib.SchemaTelemetryService;

SchemaTelemetryService telemetryService = new SchemaTelemetryService(hardwareMap, "configs/telemetry_schema.json");
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

### Live config (optional)

```java
import org.firstinspires.ftc.teamcode.telelib.ConfigRegistry;
import org.firstinspires.ftc.teamcode.telelib.SchemaTelemetryService;

ConfigRegistry configRegistry = new ConfigRegistry();
configRegistry.registerDouble("shooter_kp", () -> shooterKp, v -> shooterKp = v, 0.0, 1.0);
configRegistry.registerDouble("turret_lock_gain", () -> turretLockGain, v -> turretLockGain = v, 0.0, 10.0);

SchemaTelemetryService telemetryService = new SchemaTelemetryService(hardwareMap, "configs/telemetry_schema.json", configRegistry);
```

### Limelight tunnel (optional)

```java
import org.firstinspires.ftc.teamcode.telelib.LimelightTunnelManager;

LimelightTunnelManager limelightTunnelManager = LimelightTunnelManager.createDefault();
limelightTunnelManager.start();
// ...
limelightTunnelManager.close();
```

### 3) Run The Dashboard
Launch the FTC [Telemetry Dashboard](https://github.com/Itkan-silicon-valley/FTCTelemetryDashboard). 

---

## Example OpMode

See `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/example.java` for a minimal example.

Note: when you copy it into your FTC project, rename the file to match the
public class name (e.g. ExampleSchemaTelemetry.java).

---

## Folder Layout

```
ftctelemetryserver/
  TeamCode/
    src/main/java/org/firstinspires/ftc/teamcode/
      example.java
      telelib/
    src/main/assets/configs/
      telemetry_schema.json
```

---

## Telemetry schema JSON

The schema defines what fields exist, how the dashboard subscribes, and how graphs are built.

Top-level keys:

- `port` (number): TCP port the robot listens on.
- `strict` (boolean): if true, publishing a field not in `fields` throws an error.
- `graphs` (array): dashboard graph definitions.
- `subscribe` (array, optional): list of field names the dashboard should subscribe to.
  If omitted, the dashboard subscribes to all fields.
- `fields` (array): the canonical list of data fields and their types.

`graphs[]` object:

- `id` (string): unique graph id (used as the component id).
- `title` (string): graph title shown in the UI.
- `series` (array of strings): field names to plot on this graph.

`fields[]` object:

- `name` (string): field name used by `SchemaTelemetryService.put(...)`.
- `type` (string): data type (`double`, `long`, or `string`).
- `unit` (string): optional unit label (shown in the field list).
