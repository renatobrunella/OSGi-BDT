package uk.co.brunella.osgi.bdt.example.sensor.temperature.ice;

import java.util.Random;

import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class IceTemperatureSensor implements TemperatureSensor {

  private Random r = new Random(0L);
  
  public double readTemperature() {
    return r.nextDouble() * 30.0 - 10.0;
  }

}
