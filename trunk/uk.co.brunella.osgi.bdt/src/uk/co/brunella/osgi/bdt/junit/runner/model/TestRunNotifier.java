package uk.co.brunella.osgi.bdt.junit.runner.model;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import uk.co.brunella.osgi.bdt.junit.runner.statement.MultipleFailureException;

public class TestRunNotifier {

  private final RunNotifier fNotifier;
  private final Description fDescription;

  public TestRunNotifier(RunNotifier notifier, Description description) {
    fNotifier = notifier;
    fDescription = description;
  }

  public void addFailure(Throwable targetException) {
    if (targetException instanceof MultipleFailureException) {
      MultipleFailureException mfe = (MultipleFailureException) targetException;
      for (Throwable each : mfe.getFailures())
        addFailure(each);
      return;
    }
    fNotifier.fireTestFailure(new Failure(fDescription, targetException));
  }

  public void addFailedAssumption(AssumptionViolatedException e) {
    fNotifier.fireTestAssumptionFailed(new Failure(fDescription, e));
  }

  public void fireTestFinished() {
    fNotifier.fireTestFinished(fDescription);
  }

  public void fireTestStarted() {
    fNotifier.fireTestStarted(fDescription);
  }

  public void fireTestIgnored() {
    fNotifier.fireTestIgnored(fDescription);
  }
}