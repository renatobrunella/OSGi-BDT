package uk.co.brunella.osgi.bdt.example.scheduler.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import uk.co.brunella.osgi.bdt.example.scheduler.SchedulerCallback;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

  private static final long DEFAULT_PERIOD_IN_MILLISECONDS = 5000L;

  private ServiceTracker tracker;
  private BundleContext context;
  private Timer timer;
  private Map<ServiceReference, TimerTask> references = new HashMap<ServiceReference, TimerTask>();
  
  public void start(BundleContext context) throws Exception {
    this.context = context;
    timer = new Timer();
    tracker = new ServiceTracker(context, SchedulerCallback.class.getName(), this);
    tracker.open();
  }

  public void stop(BundleContext context) throws Exception {
    timer.cancel();
    timer = null;
    tracker.close();
  }
  
  public Object addingService(ServiceReference reference) {
    System.out.println("addingService " + reference);
    if (!references.containsKey(reference)) {
      SchedulerCallback service = (SchedulerCallback) context.getService(reference);
      TimerTask task = new ServiceTimerTask(service);
      references.put(reference, task);
      Long period = (Long) reference.getProperty(SchedulerCallback.SCHEDULE_PERIOD_IN_MILLISECOND);
      if (period == null) {
        period = DEFAULT_PERIOD_IN_MILLISECONDS;
      }
      scheduleTask(task, period);
      return service;
    } else {
      return null;
    }
  }
  
  public void modifiedService(ServiceReference reference, Object service) {
    System.out.println("modifiedService " + reference);
    if (references.containsKey(reference)) {
      TimerTask task = references.get(reference);
      task.cancel();
      task = new ServiceTimerTask((SchedulerCallback) service);
      references.put(reference, task);
      Long period = (Long) reference.getProperty(SchedulerCallback.SCHEDULE_PERIOD_IN_MILLISECOND);
      if (period == null) {
        period = DEFAULT_PERIOD_IN_MILLISECONDS;
      }
      scheduleTask(task, period);
      timer.purge();
    }
  }
  
  protected void scheduleTask(TimerTask task, Long period) {
    timer.scheduleAtFixedRate(task, 0L, period);
  }

  public void removedService(ServiceReference reference, Object service) {
    System.out.println("removedService " + reference);
    if (references.containsKey(reference)) {
      TimerTask task = references.get(reference);
      task.cancel();
      timer.purge();
      references.remove(reference);
    }
  }
  
  private static class ServiceTimerTask extends TimerTask {
    
    private SchedulerCallback service;

    protected ServiceTimerTask(SchedulerCallback service) {
      this.service = service;
    }
     
    @Override
    public void run() {
      service.callback();
    }
    
  }
}
