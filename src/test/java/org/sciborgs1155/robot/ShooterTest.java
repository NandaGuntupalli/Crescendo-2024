package org.sciborgs1155.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sciborgs1155.lib.TestingUtil.closeSubsystem;
import static org.sciborgs1155.lib.TestingUtil.fastForward;
import static org.sciborgs1155.lib.TestingUtil.run;
import static org.sciborgs1155.lib.TestingUtil.setupHAL;
import static org.sciborgs1155.robot.pivot.PivotConstants.MAX_ANGLE;
import static org.sciborgs1155.robot.pivot.PivotConstants.MIN_ANGLE;
import static org.sciborgs1155.robot.pivot.PivotConstants.STARTING_ANGLE;

import edu.wpi.first.math.MathUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sciborgs1155.robot.commands.Shooting;
import org.sciborgs1155.robot.drive.Drive;
import org.sciborgs1155.robot.feeder.Feeder;
import org.sciborgs1155.robot.pivot.Pivot;
import org.sciborgs1155.robot.shooter.Shooter;

public class ShooterTest {
  Shooting shooting;
  Pivot pivot;
  Shooter shooter;
  Feeder feeder;
  Drive drive;

  final double DELTA = 1e-1;

  @BeforeEach
  public void setup() {
    setupHAL();
    pivot = Pivot.create();
    shooter = Shooter.create();
    feeder = Feeder.create();
    drive = Drive.create();
    shooting = new Shooting(shooter, pivot, feeder, drive);
  }

  @AfterEach
  public void destroy() throws Exception {
    closeSubsystem(pivot);
    closeSubsystem(shooter);
    closeSubsystem(feeder);
    closeSubsystem(drive);
  }

  @Test
  public void testShooter() {
    run(shooter.runShooter(() -> 3));
    fastForward(400);

    assertEquals(3, shooter.getVelocity(), DELTA);
  }

  @Disabled
  @ParameterizedTest
  @ValueSource(doubles = {1.104793, 3 * Math.PI / 8})
  public void testPivot(double theta) {
    run((pivot.runPivot(() -> theta)));
    fastForward(200);
    assertEquals(theta, pivot.goal().getY(), DELTA);
    assertEquals(theta, pivot.setpoint().getY(), DELTA);
    assertEquals(
        MathUtil.clamp(theta, MIN_ANGLE.getRadians(), MAX_ANGLE.getRadians()),
        pivot.rotation().getY(),
        0.15);
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.104793, 3 * Math.PI / 8})
  public void testClimb(double theta) {
    run(pivot.climb(theta));
    fastForward(1000);

    assertEquals(STARTING_ANGLE.getRadians(), pivot.rotation().getY(), DELTA);
  }

  @Test
  public void testShootStoredNote() {
    run(shooting.shoot(() -> 4));
    fastForward();

    assertEquals(4, shooter.getVelocity(), DELTA);
  }

  @Disabled
  @Test
  public void testPivotThenShoot() {
    run(shooting.pivotThenShoot(Math.PI / 4, 4));
    fastForward();

    assertEquals(
        MathUtil.clamp(Math.PI / 4, MIN_ANGLE.getRadians(), MAX_ANGLE.getRadians()),
        pivot.rotation().getY(),
        DELTA);
    assertEquals(4, shooter.getVelocity(), DELTA);
  }
}
