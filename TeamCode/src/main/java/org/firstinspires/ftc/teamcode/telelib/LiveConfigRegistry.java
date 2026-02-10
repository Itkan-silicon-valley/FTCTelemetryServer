package org.firstinspires.ftc.teamcode.telelib;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public interface LiveConfigRegistry {
    void registerDouble(
            String name, DoubleSupplier getter, DoubleConsumer setter, double min, double max);
}
