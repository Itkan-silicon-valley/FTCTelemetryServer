package org.firstinspires.ftc.teamcode.telelib;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

/**
 * Small helper for common "robot health" readings like battery and hub power.
 */
public final class RobotVitals {
    private RobotVitals() {}

    /**
     * Return the lowest non-zero battery voltage across sensors.
     */
    public static double getBatteryVoltage(Iterable<VoltageSensor> sensors) {
        double result = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : sensors) {
            double voltage = sensor.getVoltage();
            if (voltage > 0) {
                result = Math.min(result, voltage);
            }
        }
        return result == Double.POSITIVE_INFINITY ? 0.0 : result;
    }

    /**
     * Sum hub current in amps across all hubs (0.0 if unavailable).
     */
    public static double getHubCurrentAmps(List<LynxModule> hubs) {
        if (hubs == null || hubs.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (LynxModule hub : hubs) {
            try {
                total += hub.getCurrent(CurrentUnit.AMPS);
            } catch (Exception ignored) {
            }
        }
        return total;
    }

    /**
     * Return the minimum hub input voltage (0.0 if unavailable).
     */
    public static double getHubInputVolts(List<LynxModule> hubs) {
        if (hubs == null || hubs.isEmpty()) {
            return 0.0;
        }
        double minHubVoltage = Double.POSITIVE_INFINITY;
        for (LynxModule hub : hubs) {
            try {
                double voltage = hub.getInputVoltage(VoltageUnit.VOLTS);
                if (voltage > 0) {
                    minHubVoltage = Math.min(minHubVoltage, voltage);
                }
            } catch (Exception ignored) {
            }
        }
        return minHubVoltage == Double.POSITIVE_INFINITY ? 0.0 : minHubVoltage;
    }
}
