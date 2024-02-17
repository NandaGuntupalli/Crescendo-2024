package org.sciborgs1155.robot.feeder;

import static edu.wpi.first.units.Units.Seconds;
import static org.sciborgs1155.robot.feeder.FeederConstants.*;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import monologue.Annotations.Log;
import monologue.Logged;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.commands.NoteVisualizer;

public class Feeder extends SubsystemBase implements AutoCloseable, Logged {
  private final FeederIO feeder;

  public Feeder(FeederIO feeder) {
    this.feeder = feeder;
  }

  /** Creates a real or non-existent feeder based on {@link Robot#isReal()}. */
  public static Feeder create() {
    return Robot.isReal() ? new Feeder(new RealFeeder()) : new Feeder(new NoFeeder());
  }

  /** Creates a non-existent feeder. */
  public static Feeder none() {
    return new Feeder(new NoFeeder());
  }

  public Command runFeeder(double power) {
    return run(() -> feeder.set(power)).alongWith(NoteVisualizer.shoot());
  }

  public Command eject(double velocity) {
    return runFeeder(velocity).withTimeout(TIMEOUT.in(Seconds)).finallyDo(() -> feeder.set(0));
  }

  public Trigger atShooter() {
    return new Trigger(feeder::beambreak);
  }

  @Log.NT
  public double getVelocity() {
    return feeder.getVelocity();
  }

  @Override
  public void close() throws Exception {
    feeder.close();
  }
}
