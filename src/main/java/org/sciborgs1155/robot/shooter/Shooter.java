package org.sciborgs1155.robot.shooter;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Radians;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static org.sciborgs1155.robot.shooter.ShooterConstants.*;

import edu.wpi.first.math.controller.ArmFeedforward;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.units.Angle;
import edu.wpi.first.units.Measure;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.util.Color8Bit;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import monologue.Annotations.Log;
import monologue.Logged;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.shooter.ShooterConstants.FlywheelConstants;
import org.sciborgs1155.robot.shooter.ShooterConstants.PivotConstants;
import org.sciborgs1155.robot.shooter.ShooterConstants.PivotConstants.ClimbConstants;
import org.sciborgs1155.robot.shooter.feeder.FeederIO;
import org.sciborgs1155.robot.shooter.feeder.RealFeeder;
import org.sciborgs1155.robot.shooter.feeder.SimFeeder;
import org.sciborgs1155.robot.shooter.flywheel.FlywheelIO;
import org.sciborgs1155.robot.shooter.flywheel.RealFlywheel;
import org.sciborgs1155.robot.shooter.flywheel.SimFlywheel;
import org.sciborgs1155.robot.shooter.pivot.PivotIO;
import org.sciborgs1155.robot.shooter.pivot.PivotVisualizer;
import org.sciborgs1155.robot.shooter.pivot.RealPivot;
import org.sciborgs1155.robot.shooter.pivot.SimPivot;

public class Shooter extends SubsystemBase implements Logged {
  private final FlywheelIO flywheel;
  private final FeederIO feeder;
  private final PivotIO pivot;

  @Log.NT final Mechanism2d measurement = new Mechanism2d(3, 4);
  @Log.NT final Mechanism2d setpoint = new Mechanism2d(3, 4);
  private final PivotVisualizer positionVisualizer =
      new PivotVisualizer(measurement, new Color8Bit(255, 0, 0));
  private final PivotVisualizer setpointVisualizer =
      new PivotVisualizer(setpoint, new Color8Bit(0, 0, 255));

  private final PIDController flywheelPID =
      new PIDController(FlywheelConstants.kP, FlywheelConstants.kI, FlywheelConstants.kD);
  private final SimpleMotorFeedforward flywheelFeedforward =
      new SimpleMotorFeedforward(FlywheelConstants.kS, FlywheelConstants.kV, FlywheelConstants.kA);

  private final ProfiledPIDController pivotPID =
      new ProfiledPIDController(
          PivotConstants.kP,
          PivotConstants.kI,
          PivotConstants.kD,
          new TrapezoidProfile.Constraints(PivotConstants.MAX_VELOCITY, PivotConstants.MAX_ACCEL));
  private final ArmFeedforward pivotFeedforward =
      new ArmFeedforward(PivotConstants.kS, PivotConstants.kG, PivotConstants.kV);

  private final ProfiledPIDController climbPID =
      new ProfiledPIDController(
          ClimbConstants.kP,
          ClimbConstants.kI,
          ClimbConstants.kD,
          new TrapezoidProfile.Constraints(ClimbConstants.MAX_VELOCITY, ClimbConstants.MAX_ACCEL));
  private final ArmFeedforward climbFeedforward =
      new ArmFeedforward(ClimbConstants.kS, ClimbConstants.kG, ClimbConstants.kV);

  public static Shooter create() {
    return Robot.isReal()
        ? new Shooter(new RealFlywheel(), new RealPivot(), new RealFeeder())
        : new Shooter(new SimFlywheel(), new SimPivot(), new SimFeeder());
  }

  public Shooter(FlywheelIO flywheel, PivotIO pivot, FeederIO feeder) {
    this.flywheel = flywheel;
    this.feeder = feeder;
    this.pivot = pivot;
  }

  public Command runFeeder(double speed) {
    return run(() -> feeder.set(speed)).withName("running Feeder");
  }

  public void runFeederBase(double speed) {
    feeder.set(speed);
  }

  public Command runFeederInverse(double voltage) {
    return runFeeder((voltage * -1)).withName("running Feeder backwards");
  }

  public void runInverseFeederBase(double speed) {
    runFeederBase(speed * -1);
  }

  // Make sure this is correct !!!
  public Command runFlywheel(DoubleSupplier velocity) {
    return run(() ->
            flywheel.setVoltage(
                flywheelPID.calculate(flywheel.getVelocity(), velocity.getAsDouble())
                    + flywheelFeedforward.calculate(velocity.getAsDouble())))
        .withName("running Flywheel");
  }

  public void runFlywheelBase(DoubleSupplier velocity) {
    flywheel.setVoltage(
        flywheelPID.calculate(flywheel.getVelocity(), velocity.getAsDouble())
            + flywheelFeedforward.calculate(velocity.getAsDouble()));
  }

  public Command runPivot(Supplier<Measure<Angle>> goalAngle) {
    return runOnce(() -> pivotPID.setGoal(goalAngle.get().in(Radians)))
        .andThen(
            run(() ->
                    pivot.setVoltage(
                        pivotPID.calculate(pivot.getPosition())
                            + pivotFeedforward.calculate(
                                pivotPID.getSetpoint().position, pivotPID.getSetpoint().velocity)))
                .withName("running Pivot"));
  }

  public void runPivotBase(Supplier<Measure<Angle>> goalAngle) {
    pivotPID.setGoal(goalAngle.get().in(Radians));
    pivot.setVoltage(
        pivotPID.calculate(pivot.getPosition())
            + pivotFeedforward.calculate(
                pivotPID.getSetpoint().position, pivotPID.getSetpoint().velocity));
  }

  public Command climb(Supplier<Measure<Angle>> goalAngle) {
    return runOnce(() -> climbPID.setGoal(goalAngle.get().in(Radians)))
        .andThen(
            run(() ->
                    pivot.setVoltage(
                        climbPID.calculate(pivot.getPosition())
                            + climbFeedforward.calculate(
                                climbPID.getSetpoint().position, climbPID.getSetpoint().velocity)))
                .withName("running Climb"));
  }

  public void runClimbBase(Supplier<Measure<Angle>> goalAngle) {
    climbPID.setGoal(goalAngle.get().in(Radians));
    pivot.setVoltage(
        climbPID.calculate(pivot.getPosition())
            + climbFeedforward.calculate(
                climbPID.getSetpoint().position, climbPID.getSetpoint().velocity));
  }

  // shooting commands
  public Command shootStoredNote(DoubleSupplier desiredVelocity) {
    return Commands.parallel(
        Commands.run(() -> runFlywheelBase(() -> desiredVelocity.getAsDouble())),
        Commands.run(() -> runFeederBase(1))
            .onlyIf(
                () ->
                    flywheel.getVelocity()
                            <= desiredVelocity.getAsDouble()
                                + FlywheelConstants.VELOCITY_TOLERANCE.in(RadiansPerSecond)
                        && flywheel.getVelocity()
                            >= desiredVelocity.getAsDouble()
                                - FlywheelConstants.VELOCITY_TOLERANCE.in(RadiansPerSecond)));
  }

  public Command pivotThenShoot(
      Supplier<Measure<Angle>> goalAngle, DoubleSupplier desiredVelocity) {
    return runPivot(goalAngle)
        .alongWith(
            shootStoredNote(desiredVelocity)
                .onlyIf(
                    () ->
                        pivot.getPosition()
                                <= goalAngle.get().in(Radians)
                                    + PivotConstants.POSITION_TOLERANCE.in(Radians)
                            && pivot.getPosition()
                                >= goalAngle.get().in(Radians)
                                    - PivotConstants.POSITION_TOLERANCE.in(Radians)));
  }

  // ProfilePID doesn't log this stuff
  @Log.NT
  public double getPivotSetpointRadians() {
    return pivotPID.getSetpoint().position;
  }

  @Override
  public void periodic() {
    positionVisualizer.setState(Degrees.convertFrom(pivot.getPosition(), Radians));
    setpointVisualizer.setState(Degrees.convertFrom(pivotPID.getSetpoint().position, Radians));
  }
}
