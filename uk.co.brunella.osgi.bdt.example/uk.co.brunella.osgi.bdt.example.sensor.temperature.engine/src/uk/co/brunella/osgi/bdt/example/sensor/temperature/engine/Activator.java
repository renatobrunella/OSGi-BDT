package uk.co.brunella.osgi.bdt.example.sensor.temperature.engine;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class Activator implements BundleActivator {

  public void start(BundleContext context) throws Exception {
    TemperatureSensor sensor = new EngineTemperatureSensor();
    Dictionary<Object, Object> properties = new Hashtable<Object, Object>();
    properties.put(TemperatureSensor.SENSOR_NAME, "Engine Sensor");
    properties.put(TemperatureSensor.MIN_TEMPERATURE, -10.0);
    properties.put(TemperatureSensor.MAX_TEMPERATURE, 110.0);
    context.registerService(TemperatureSensor.class.getName(), sensor, properties);
  }

  public void stop(BundleContext context) throws Exception {
    
  }

}
