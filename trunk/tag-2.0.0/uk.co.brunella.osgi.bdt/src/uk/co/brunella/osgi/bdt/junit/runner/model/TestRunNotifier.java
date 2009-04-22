/*
 * Copyright 2009 brunella ltd
 *
 * Licensed under the GPL Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
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