package uk.co.brunella.osgi.bdt.example.scheduler;

public interface SchedulerCallback {

  static final String SCHEDULE_PERIOD_IN_MILLISECOND = "PERIOD_IN_MILLISECOND";
  
  void callback();
}
