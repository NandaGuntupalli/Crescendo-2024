package org.sciborgs1155.robot.shooter;

import static edu.wpi.first.units.Units.*;
import static org.sciborgs1155.robot.Ports.Shooter.*;
import static org.sciborgs1155.robot.shooter.ShooterConstants.*;

import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkLowLevel.MotorType;

import monologue.Annotations.Log;

import com.revrobotics.RelativeEncoder;
import java.util.Set;
import org.sciborgs1155.lib.FaultLogger;
import org.sciborgs1155.lib.SparkUtils;
import org.sciborgs1155.lib.SparkUtils.Data;
import org.sciborgs1155.lib.SparkUtils.Sensor;

public class RealShooter implements ShooterIO {
  private final CANSparkFlex topMotor;
  private final CANSparkFlex bottomMotor;
  private final RelativeEncoder encoder;

  public RealShooter() {
    topMotor = new CANSparkFlex(TOP_MOTOR, MotorType.kBrushless);
    encoder = topMotor.getEncoder();

    SparkUtils.configure(
        topMotor,
        () ->
            SparkUtils.configureFrameStrategy(
                topMotor,
                Set.of(Data.POSITION, Data.VELOCITY, Data.APPLIED_OUTPUT),
                Set.of(Sensor.INTEGRATED),
                true),
        () -> topMotor.setIdleMode(IdleMode.kCoast),
        () -> topMotor.setSmartCurrentLimit((int) CURRENT_LIMIT.in(Amps)),
        () -> SparkUtils.setInverted(topMotor, true),
        () -> encoder.setPositionConversionFactor(POSITION_FACTOR.in(Radians)),
        () -> encoder.setVelocityConversionFactor(VELOCITY_FACTOR.in(RadiansPerSecond)),
        () -> encoder.setMeasurementPeriod(8),
        () -> encoder.setAverageDepth(2));

    bottomMotor = new CANSparkFlex(BOTTOM_MOTOR, MotorType.kBrushless);
    SparkUtils.configure(
        bottomMotor,
        () -> SparkUtils.configureNothingFrameStrategy(bottomMotor),
        () -> bottomMotor.setIdleMode(IdleMode.kCoast),
        () -> bottomMotor.setSmartCurrentLimit((int) CURRENT_LIMIT.in(Amps)),
        () -> bottomMotor.follow(topMotor, true));

    FaultLogger.register(topMotor);
    FaultLogger.register(bottomMotor);
  }

  @Override
  public void setVoltage(double voltage) {
    topMotor.setVoltage(voltage);
    FaultLogger.check(topMotor);
  }

  @Override
  @Log.NT
  public double getCurrent() {
      return topMotor.getOutputCurrent();
  }

  @Override
  public double getVelocity() {
    return encoder.getVelocity();
  }

  @Override
  public void close() throws Exception {
    topMotor.close();
    bottomMotor.close();
  }
}
