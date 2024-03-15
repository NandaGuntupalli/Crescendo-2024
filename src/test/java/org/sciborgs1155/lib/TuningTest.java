package org.sciborgs1155.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sciborgs1155.lib.TestingUtil.setupHAL;

import edu.wpi.first.networktables.BooleanEntry;
import edu.wpi.first.networktables.DoubleEntry;
import edu.wpi.first.networktables.IntegerEntry;
import edu.wpi.first.networktables.StringEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sciborgs1155.lib.Tuning.*;

public class TuningTest {
  private DoubleEntry dbleEnt;
  private IntegerEntry intEnt;
  private StringEntry strEnt;
  private BooleanEntry boolEnt;

  private double dbleVal = 2.0;
  private long intVal = 7823; // IntgerTopic.getEntry() accepts longs for default values
  private String strVal = "Hello, World! <3";
  private boolean boolVal = true;

  @BeforeEach
  public void setup() {
    setupHAL();
  }

  @Test
  public void fullEntryTest() {
    dbleEnt = Tuning.entry("/Robot/a", dbleVal);
    intEnt = Tuning.entry("/Robot/b", intVal);
    strEnt = Tuning.entry("/Robot/c", strVal);
    boolEnt = Tuning.entry("/Robot/d", boolVal);

    assertEquals(dbleVal, dbleEnt.get());
    assertEquals(intVal, intEnt.get());
    assertEquals(strVal, strEnt.get());
    assertEquals(boolVal, boolEnt.get());
  }
}
