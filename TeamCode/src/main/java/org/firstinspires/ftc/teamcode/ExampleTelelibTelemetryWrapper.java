package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.telelib.SchemaTelemetryBus;
import org.firstinspires.ftc.teamcode.telelib.TelelibTelemetry;
import org.firstinspires.ftc.teamcode.telelib.TelemetryBus;

/**
 * Example OpMode using the TelelibTelemetry wrapper + SchemaTelemetryBus.
 *
 * Key idea: keep using the normal SDK telemetry API (addData/update),
 * while the wrapper mirrors those fields into the schema-driven telemetry server.
 *
 * Rename this file if needed so the public class name matches the file name.
 */
@TeleOp(name = "ExampleTelelibTelemetryWrapper", group = "Examples")
public class ExampleTelelibTelemetryWrapper extends LinearOpMode {
    private static final String TELEMETRY_SCHEMA_PATH = "configs/telemetry_schema.json";
    // Live-tunable values exposed to the dashboard.
    private volatile double shooterKp = 0.015;
    private volatile double turretLockGain = 3.0;
    private TelelibTelemetry telelibTelemetry;

    @Override
    public void runOpMode() {
        DcMotorEx frontLeft = hardwareMap.get(DcMotorEx.class, "FrontLeft");
        IMU imu = hardwareMap.get(IMU.class, "imu");

        // 1) Build the schema-driven telemetry bus.
        TelemetryBus telemetryBus = new SchemaTelemetryBus(hardwareMap, TELEMETRY_SCHEMA_PATH);
        // 2) Register tunables that the dashboard can update live via SET.
        telemetryBus
                .config()
                .registerDouble("shooter_kp", () -> shooterKp, v -> shooterKp = v, 0.0, 1.0);
        telemetryBus
                .config()
                .registerDouble("turret_lock_gain", () -> turretLockGain, v -> turretLockGain = v, 0.0, 10.0);
        // 3) Wrap the SDK telemetry so addData/update also publishes to the bus.
        telelibTelemetry = new TelelibTelemetry(telemetry, telemetryBus);
        telemetry = telelibTelemetry;

        telemetry.addLine("Ready for start");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // Standard SDK telemetry calls (also mirrored to the schema bus).
            telemetry.addData("robot_ts_ms", System.currentTimeMillis());
            telemetry.addData("front_left_power", "%.3f", frontLeft.getPower());
            telemetry.addData("imu_yaw_deg", "%.2f",
                    imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
            // Live-config values still show up as normal fields.
            telemetry.addData("shooter_kp", "%.4f", shooterKp);
            telemetry.addData("turret_lock_gain", "%.2f", turretLockGain);
            telemetry.update();
        }

        telelibTelemetry.close();
    }
}
