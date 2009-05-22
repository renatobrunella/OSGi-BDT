package uk.co.brunella.osgi.bdt.example.dashboard;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import uk.co.brunella.osgi.bdt.example.scheduler.SchedulerCallback;
import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class SensorScheduledService implements SchedulerCallback {

  private ServiceTracker tracker;

  public SensorScheduledService(ServiceTracker tracker) {
    this.tracker = tracker;
  }

  public void callback() {
    ServiceReference[] references = tracker.getServiceReferences();
    if (references != null) {
      for (ServiceReference reference : references) {
        TemperatureSensor sensor = (TemperatureSensor) tracker.getService(reference);
        Double minTemperature = (Double) reference.getProperty(TemperatureSensor.MIN_TEMPERATURE); 
        Double maxTemperature = (Double) reference.getProperty(TemperatureSensor.MAX_TEMPERATURE);
        String sensorName = (String) reference.getProperty(TemperatureSensor.SENSOR_NAME);
        
        double temperature = sensor.readTemperature();
        if (temperature >= minTemperature && temperature <= maxTemperature) {
          reportNormal(sensorName, temperature);
        } else if (temperature < minTemperature) {
          reportTooLow(sensorName, temperature);
        } else {
          reportTooHigh(sensorName, temperature);
        }
      }
      System.out.println();
    }
  }
  
  protected void reportNormal(String sensorName, double temperature) {
    System.out.println(String.format("%s is normal (%3.2f)", sensorName, temperature));
  }

  protected void reportTooHigh(String sensorName, double temperature) {
    System.err.println(String.format("WARNING: %s is too high (%3.2f)", sensorName, temperature));
  }

  protected void reportTooLow(String sensorName, double temperature) {
    System.err.println(String.format("WARNING: %s is too low (%3.2f)", sensorName, temperature));
  }

}
