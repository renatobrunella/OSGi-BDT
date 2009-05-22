package uk.co.brunella.osgi.bdt.example.sensor.temperature.engine;

import static org.junit.Assert.*;

import org.junit.Test;

public class EngineTemperatureSensorTest {

  @Test
  public void testReadTemperature() {
    EngineTemperatureSensor sensor = new EngineTemperatureSensor();
    assertEquals(93.096778, sensor.readTemperature(), 0.0001);
  }
}
