package uk.co.brunella.osgi.bdt.example.dashboard;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import uk.co.brunella.osgi.bdt.example.scheduler.SchedulerCallback;
import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;

public class Activator implements BundleActivator {

  private ServiceTracker tracker;

  public void start(BundleContext context) throws Exception {
    tracker = new ServiceTracker(context, TemperatureSensor.class.getName(), null);
    tracker.open();
    SchedulerCallback service = new SensorScheduledService(tracker);
    Dictionary<Object, Object> properties = new Hashtable<Object, Object>();
    properties.put(SchedulerCallback.SCHEDULE_PERIOD_IN_MILLISECOND, 1000L);
    context.registerService(SchedulerCallback.class.getName(), service, properties);
  }

  public void stop(BundleContext context) throws Exception {
    tracker.close();
  }

}
