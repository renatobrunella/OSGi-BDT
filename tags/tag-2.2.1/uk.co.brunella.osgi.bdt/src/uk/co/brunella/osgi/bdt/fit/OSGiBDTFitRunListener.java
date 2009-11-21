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
package uk.co.brunella.osgi.bdt.fit;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class OSGiBDTFitRunListener extends RunListener {

  private List<OSGiTestResult> results = new ArrayList<OSGiTestResult>();
  private long startTime;
  
  public List<OSGiTestResult> getResults() {
    return results;
  }

  public void testStarted(Description description) throws Exception {
    startTime = System.currentTimeMillis();
  }

  public void testFinished(Description description) throws Exception {
    long stopTime = System.currentTimeMillis();
    results.add(OSGiTestResult.pass(className(description), methodName(description), startTime - stopTime));
  }

  public void testFailure(Failure failure) throws Exception {
    long stopTime = System.currentTimeMillis();
    results.add(OSGiTestResult.fail(className(failure.getDescription()), 
        methodName(failure.getDescription()), failure.getException(), startTime - stopTime));
  }

  private String className(Description description) {
    return description.getDisplayName().substring(description.getDisplayName().indexOf('(') + 1, description.getDisplayName().length() - 1);
  }
  
  private String methodName(Description description) {
    return description.getDisplayName().substring(0, description.getDisplayName().indexOf('('));
  }
}
