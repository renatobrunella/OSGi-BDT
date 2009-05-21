package uk.co.brunella.osgi.bdt.example.sensor.temperature.ice;

import java.util.Random;

import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class EngineTemperatureSensor implements TemperatureSensor {

  private Random r = new Random(0L);
  
  public double readTemperature() {
    return r.nextDouble() * 100.0 + 20.0;
  }

}
