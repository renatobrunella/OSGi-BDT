package uk.co.brunella.osgi.bdt.example.sensor.temperature.ice;

import static org.junit.Assert.*;

import org.junit.Test;

public class IceTemperatureSensorTest {

  @Test
  public void testReadTemperature() {
    IceTemperatureSensor sensor = new IceTemperatureSensor();
    assertEquals(11.92903, sensor.readTemperature(), 0.0001);
  }
}
