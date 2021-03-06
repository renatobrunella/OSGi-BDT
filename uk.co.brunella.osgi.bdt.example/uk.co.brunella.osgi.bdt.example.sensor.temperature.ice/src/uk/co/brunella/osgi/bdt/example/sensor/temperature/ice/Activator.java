package uk.co.brunella.osgi.bdt.example.sensor.temperature.ice;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class Activator implements BundleActivator {

  public void start(BundleContext context) throws Exception {
    TemperatureSensor sensor = new IceTemperatureSensor();
    Dictionary<Object, Object> properties = new Hashtable<Object, Object>();
    properties.put(TemperatureSensor.SENSOR_NAME, "Ice Sensor");
    properties.put(TemperatureSensor.MIN_TEMPERATURE, 0.0);
    properties.put(TemperatureSensor.MAX_TEMPERATURE, Double.MAX_VALUE);
    context.registerService(TemperatureSensor.class.getName(), sensor, properties);
  }

  public void stop(BundleContext context) throws Exception {
    
  }

}
