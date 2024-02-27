package org.sciborgs1155.robot.intake;

import static edu.wpi.first.units.Units.Seconds;
import static org.sciborgs1155.robot.Constants.PERIOD;

import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.Optional;
import monologue.Annotations.Log;
import monologue.Logged;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.commands.NoteVisualizer;

public class Intake extends SubsystemBase implements Logged, AutoCloseable {
  public static Intake create() {
    return Robot.isReal() ? new Intake(new RealIntake()) : new Intake(new NoIntake());
  }

  public static Intake none() {
    return new Intake(new NoIntake());
  }

  @Log.NT private final IntakeIO intake;

  public Intake(IntakeIO intake) {
    this.intake = intake;
    setDefaultCommand(runOnce(() -> intake.setPower(0)).andThen(Commands.idle()));
  }

  public Command intake() {
    return run(() -> intake.setPower(IntakeConstants.INTAKE_SPEED))
        .alongWith(NoteVisualizer.intake());
  }

  public Trigger inIntake() {
    return new Trigger(() -> !intake.beambreak())
        .debounce(PERIOD.times(3).in(Seconds), DebounceType.kBoth);
  }

  public Command outtake() {
    return run(() -> intake.setPower(-IntakeConstants.INTAKE_SPEED));
  }

  @Override
  public void periodic() {
    log("command", Optional.ofNullable(getCurrentCommand()).map(Command::getName).orElse("none"));
  }

  @Override
  public void close() throws Exception {
    intake.close();
  }
}
