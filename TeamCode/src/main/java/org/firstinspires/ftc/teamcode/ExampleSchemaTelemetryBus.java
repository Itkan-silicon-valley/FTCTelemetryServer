package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.telelib.LimelightTunnelManager;
import org.firstinspires.ftc.teamcode.telelib.SchemaTelemetryBus;
import org.firstinspires.ftc.teamcode.telelib.TelemetryBus;

/**
 * Example of the new TelemetryBus usage with live-config.
 *
 * Rename this file if needed so the public class name matches the file name.
 */
@TeleOp(name = "ExampleSchemaTelemetryBus", group = "Examples")
public class ExampleSchemaTelemetryBus extends LinearOpMode {
    private static final String TELEMETRY_SCHEMA_PATH = "configs/telemetry_schema.json";

    // Live-tunable variables (example).
    private double shooterKp = 0.015;
    private double turretLockGain = 3.0;

    @Override
    public void runOpMode() {
        DcMotorEx frontLeft = hardwareMap.get(DcMotorEx.class, "FrontLeft");
        IMU imu = hardwareMap.get(IMU.class, "imu");

        // (Optional) Start the Limelight tunnel so the laptop can view the camera.
        LimelightTunnelManager limelightTunnelManager = LimelightTunnelManager.createDefault();
        limelightTunnelManager.start();

        TelemetryBus telemetryBus = new SchemaTelemetryBus(hardwareMap, TELEMETRY_SCHEMA_PATH);

        // Live config registry: the dashboard can SET these while the robot runs.
        telemetryBus.config().registerDouble("shooter_kp", () -> shooterKp, v -> shooterKp = v, 0.0, 1.0);
        telemetryBus.config()
                .registerDouble("turret_lock_gain", () -> turretLockGain, v -> turretLockGain = v, 0.0, 10.0);

        telemetryBus.start();

        waitForStart();
        long runId = System.currentTimeMillis() / 1000L;

        while (opModeIsActive()) {
            telemetryBus.begin();
            telemetryBus.put("run_id", runId);
            telemetryBus.put("robot_ts_ms", System.currentTimeMillis());
            telemetryBus.put("front_left_power", frontLeft.getPower(), "%.3f");
            telemetryBus.put(
                    "imu_yaw_deg",
                    imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES),
                    "%.2f");
            telemetryBus.put("shooter_kp", shooterKp, "%.4f");
            telemetryBus.put("turret_lock_gain", turretLockGain, "%.2f");
            telemetryBus.publish();
        }

        telemetryBus.close();
        limelightTunnelManager.close();
    }
}
