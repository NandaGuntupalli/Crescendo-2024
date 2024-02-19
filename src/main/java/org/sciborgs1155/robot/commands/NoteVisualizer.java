package org.sciborgs1155.robot.commands;

import static edu.wpi.first.units.Units.*;
import static org.sciborgs1155.robot.Constants.*;
import static org.sciborgs1155.robot.Constants.Field.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructArrayPublisher;
import edu.wpi.first.networktables.StructArrayTopic;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ScheduleCommand;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import monologue.Logged;
import org.sciborgs1155.robot.shooter.ShooterConstants;

public class NoteVisualizer implements Logged {
  // notes
  private static LinkedList<Pose3d> notes = new LinkedList<>();
  private static ArrayList<Pose3d> pathPosition = new ArrayList<>();
  private static Pose3d[] firedNotes = new Pose3d[1];

  // suppliers
  private static Supplier<Pose2d> pose = Pose2d::new;
  private static Supplier<Rotation3d> angle = Rotation3d::new;
  private static DoubleSupplier velocity = () -> 1;

  private static double zVelocity;
  private static int i = 0;

  private static Pose3d currentNotePose = new Pose3d();
  private static Pose3d lastNotePose = new Pose3d();

  // publishers
  private static StructArrayPublisher<Pose3d> posePub;
  private static StructArrayPublisher<Pose3d> pathPub;

  public static void setSuppliers(
      Supplier<Pose2d> robotPose, Supplier<Rotation3d> pivotAngle, DoubleSupplier shotVelocity) {
    pose = robotPose;
    angle = pivotAngle;
    velocity = shotVelocity;
  }

  /** Set up NT publisher. Call only once before beginning to log notes. */
  public static void startPublishing() {
    NetworkTableInstance inst = NetworkTableInstance.getDefault();
    StructArrayTopic<Pose3d> path = inst.getStructArrayTopic("note path", Pose3d.struct);
    pathPub = path.publish();

    StructArrayTopic<Pose3d> poseTopic = inst.getStructArrayTopic("poses", Pose3d.struct);
    posePub = poseTopic.publish();
  }

  public static void log() {
    posePub.set(firedNotes);
  }

  public static Command shoot() {
    return new ScheduleCommand(
            Commands.defer(
                () -> {
                  generatePath();
                  return Commands.run(
                          () -> {
                            currentNotePose = pathPosition.get(i);
                            firedNotes = new Pose3d[] {currentNotePose};
                            i++;
                          })
                      .until(() -> i == pathPosition.size() - 1)
                      .finallyDo(
                          () -> {
                            firedNotes = new Pose3d[] {};
                            i = 0;
                          });
                },
                Set.of()))
        .ignoringDisable(true);
  }

  private static void generatePath() {
    double g = 9.81;
    double linearVelocity = velocity.getAsDouble() * ShooterConstants.CIRCUMFERENCE.in(Meters);
    Rotation2d shootingAngle = new Rotation2d(angle.get().getY());

    // flipped over origin
    Rotation2d armPosition = shootingAngle.plus(Rotation2d.fromDegrees(180));

    lastNotePose =
        new Pose3d(pose.get())
            .plus(
                new Transform3d(
                    new Translation3d(Inches.of(-10.465), Inches.of(0), Inches.of(25)),
                    new Rotation3d(0, armPosition.getRadians(), 0)));
    Rotation2d robot = pose.get().getRotation();

    final double xVelocity = -linearVelocity * robot.getCos() * shootingAngle.getCos();
    final double yVelocity = -linearVelocity * robot.getSin() * shootingAngle.getCos();
    zVelocity = linearVelocity * shootingAngle.getSin();

    pathPosition = new ArrayList<>();
    pathPosition.add(lastNotePose);

    while (lastNotePose.getZ() > 0) {
      currentNotePose =
          new Pose3d(
              new Translation3d(
                  lastNotePose.getX() + xVelocity * PERIOD.in(Seconds),
                  lastNotePose.getY() + yVelocity * PERIOD.in(Seconds),
                  lastNotePose.getZ() + zVelocity * PERIOD.in(Seconds)),
              lastNotePose.getRotation());

      pathPosition.add(currentNotePose);
      zVelocity = zVelocity - g * PERIOD.in(Seconds);
      lastNotePose = currentNotePose;
    }
    pathPub.set(pathPosition.toArray(new Pose3d[0]));
  }
}
