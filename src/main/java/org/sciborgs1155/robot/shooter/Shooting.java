package org.sciborgs1155.robot.shooter;

import static edu.wpi.first.units.Units.Volts;
import static org.sciborgs1155.robot.shooter.ShooterConstants.*;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.Hashtable;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import monologue.Annotations.Log;
import monologue.Logged;
import org.sciborgs1155.robot.shooter.feeder.Feeder;
import org.sciborgs1155.robot.shooter.flywheel.Flywheel;
import org.sciborgs1155.robot.shooter.pivot.Pivot;

public class Shooting implements Logged {

  @Log.NT private final Feeder feeder;
  @Log.NT private final Pivot pivot;
  @Log.NT private final Flywheel flywheel;

  private final Hashtable<Translation2d, ShooterState> shootingData;

  public static record ShooterState(Rotation2d angle, double speed) {
    public static ShooterState create(double angle, double speed) {
      return new ShooterState(Rotation2d.fromRadians(angle), speed);
    }
  }

  public Shooting(Flywheel flywheel, Pivot pivot, Feeder feeder) {
    this(flywheel, pivot, feeder, new Hashtable<Translation2d, ShooterState>());
  }

  public Shooting(
      Flywheel flywheel,
      Pivot pivot,
      Feeder feeder,
      Hashtable<Translation2d, ShooterState> shootingData) {
    this.shootingData = shootingData;
    this.flywheel = flywheel;
    this.pivot = pivot;
    this.feeder = feeder;
  }

  // shooting commands
  public Command shootStoredNote(DoubleSupplier desiredVelocity) {
    return Commands.parallel(
        flywheel.runFlywheel(() -> desiredVelocity.getAsDouble()),
        Commands.waitUntil(flywheel::atSetpoint).andThen(feeder.runFeeder(Volts.of(1))));
  }

  public Command pivotThenShoot(Supplier<Rotation2d> goalAngle, DoubleSupplier desiredVelocity) {
    return pivot
        .runPivot(goalAngle)
        .alongWith(Commands.waitUntil(pivot::atSetpoint).andThen(shootStoredNote(desiredVelocity)));
  }

  public ShooterState getDesiredState2(Translation2d position) {
    double x0 = Math.floor(position.getX() / DATA_INTERVAL) * DATA_INTERVAL;
    double x1 = Math.ceil(position.getX() / DATA_INTERVAL) * DATA_INTERVAL;
    double y0 = Math.floor(position.getY() / DATA_INTERVAL) * DATA_INTERVAL;
    double y1 = Math.ceil(position.getY() / DATA_INTERVAL) * DATA_INTERVAL;

    List<Translation2d> tList =
        List.of(
            new Translation2d(x0, y0),
            new Translation2d(x0, y1),
            new Translation2d(x1, y0),
            new Translation2d(x1, y1));

    double angle = 0;
    double speed = 0;

    for (Translation2d pos : tList) {
      angle += getAnglePart(pos);
      speed += getSpeedPart(pos);
    }

    return new ShooterState(Rotation2d.fromRadians(angle), speed);
  }

  public double getAnglePart(Translation2d pos) {
    return 0;
  }

  public double getSpeedPart(Translation2d pos) {
    return 0;
  }

  private static double interpolate(double a, double b, double dist) {
    assert 0 <= dist && dist <= 1;
    return a * dist + b * (1 - dist);
  }

  public static ShooterState interpolateStates(ShooterState a, ShooterState b, double dist) {
    assert 0 <= dist && dist <= 1;
    return new ShooterState(
        Rotation2d.fromRadians(interpolate(a.angle().getRadians(), b.angle().getRadians(), dist)),
        interpolate(a.speed(), b.speed(), dist));
  }

  /** bilinear interpolation ({@link https://en.wikipedia.org/wiki/Bilinear_interpolation}) */
  public ShooterState desiredState(Translation2d position) throws Exception {
    double x0 = Math.floor(position.getX() / DATA_INTERVAL) * DATA_INTERVAL;
    double x1 = Math.ceil(position.getX() / DATA_INTERVAL) * DATA_INTERVAL;
    double y0 = Math.floor(position.getY() / DATA_INTERVAL) * DATA_INTERVAL;
    double y1 = Math.ceil(position.getY() / DATA_INTERVAL) * DATA_INTERVAL;

    double x_dist = (position.getX() - x0) / DATA_INTERVAL;
    double y_dist = (position.getY() - y0) / DATA_INTERVAL;

    try {
      return interpolateStates(
          interpolateStates(
              shootingData.get(new Translation2d(x0, y0)),
              shootingData.get(new Translation2d(x1, y0)),
              x_dist),
          interpolateStates(
              shootingData.get(new Translation2d(x0, y1)),
              shootingData.get(new Translation2d(x1, y1)),
              x_dist),
          y_dist);
    } catch (Exception e) {
      throw (new Exception("cannot shoot from this position!"));
    }
  }
}
