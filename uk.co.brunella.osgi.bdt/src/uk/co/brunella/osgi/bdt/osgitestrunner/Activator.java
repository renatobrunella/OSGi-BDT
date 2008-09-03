/*
 * Copyright 2008 brunella ltd
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
package uk.co.brunella.osgi.bdt.osgitestrunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResult;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResultService;

public class Activator implements BundleActivator, ServiceListener, OSGiTestResultService {

  private static Activator instance;

  private BundleContext bundleContext;
  private Set<ServiceReference> registeredServices = new HashSet<ServiceReference>();
  private ErrorLogListener errorLogListener;
  private List<OSGiTestResult> testResults = new ArrayList<OSGiTestResult>();
  private Map<String, String> testParameters;

  public Activator() {
    instance = this;
  }
  
  public static Activator getInstance() {
    return instance;
  }
  
  public void start(BundleContext context) throws Exception {
    testResults.clear();
    bundleContext = context;
    
    errorLogListener = createErrorLogListener();

    context.registerService(OSGiTestResultService.class.getName(), this, null);
    
    synchronized (this) {
      String filter = "(objectclass=" + OSGiTestCase.class.getName() + ")";
      context.addServiceListener(this, filter);
      ServiceReference[] refs = context.getServiceReferences(OSGiTestCase.class.getName(), null);
      for (int i = 0; refs != null && i < refs.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refs[i]));
      }
    }
  }

  public void stop(BundleContext context) throws Exception {
    if (errorLogListener != null) {
      errorLogListener.close();
    }
    bundleContext = null;
  }
  
  private ErrorLogListener createErrorLogListener() throws InvalidSyntaxException {
    try {
      // is the org.osgi.service.log package resolved?
      bundleContext.getBundle().loadClass("org.osgi.service.log.LogReaderService");
      errorLogListener = new ErrorLogListener(bundleContext);
      return errorLogListener;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public void serviceChanged(ServiceEvent event) {
    ServiceReference serviceReference = event.getServiceReference();
    switch (event.getType()) {
    case ServiceEvent.REGISTERED:
      if (!registeredServices.contains(serviceReference)) {
        registeredServices.add(serviceReference);
        OSGiTestCase integrationTestService = (OSGiTestCase) bundleContext.getService(serviceReference);
        runTests(integrationTestService);
      }
      break;
    }
  }

  protected void runTests(OSGiTestCase testService) {
    // set test parameters
    if (testParameters != null && testService instanceof OSGiTestParameter) {
      try {
        ((OSGiTestParameter)testService).setParameters(testParameters);
      } catch (Exception e) {
        // ignore
      }
    }
    
    try {
      Method setUp = testService.getClass().getDeclaredMethod("setUp");
      setUp.invoke(testService);
    } catch (Exception e) {
      // ignore
    }
    
    OSGiTestResult info;
    if (testService.getClass().isAnnotationPresent(Description.class)) {
      String description = testService.getClass().getAnnotation(Description.class).value();
      info = OSGiTestResult.info(testService.getClass().getName(), description);
      testResults.add(info);
    } else {
      info = OSGiTestResult.info(testService.getClass().getName(), "");
      testResults.add(info);
    }
    
    long startTime = System.currentTimeMillis();
    
    Method[] methods = testService.getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (method.getName().startsWith("test") && method.getParameterTypes().length == 0) {
        runTest(testService, method);
      }
    }

    long endTime = System.currentTimeMillis();
    info.setTime(endTime - startTime);
    
    try {
      Method tearDown = testService.getClass().getDeclaredMethod("tearDown");
      tearDown.invoke(testService);
    } catch (Exception e) {
      // ignore
    }
  }

  protected void runTest(OSGiTestCase testService, Method method) {
    OSGiTestResult testResult;
    String description = "";
    if (method.isAnnotationPresent(Description.class)) {
      description = method.getAnnotation(Description.class).value();
    }
    long startTime = System.currentTimeMillis();
    try {
      method.invoke(testService);
      testResult = OSGiTestResult.pass(method, description, System.currentTimeMillis() - startTime);
    } catch (InvocationTargetException ite) {
      Throwable t = ite.getTargetException();
      if (t instanceof junit.framework.AssertionFailedError ||
          t instanceof junit.framework.ComparisonFailure || 
          t instanceof org.junit.internal.ArrayComparisonFailure ||
          t instanceof java.lang.AssertionError ||
          t instanceof org.junit.ComparisonFailure) {
        testResult = OSGiTestResult.fail(method, t, description, System.currentTimeMillis() - startTime);
      } else {
        testResult = OSGiTestResult.error(method, t, description, System.currentTimeMillis() - startTime);
      }
    } catch (Throwable t) {
      testResult = OSGiTestResult.error(method, t, description, System.currentTimeMillis() - startTime);
    }
    testResults.add(testResult);
  }
  
  public byte[] getTestResults() throws IOException {
    ByteArrayOutputStream baos = null;
    ObjectOutputStream oos = null;
    try {
      baos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(baos);
      oos.writeObject(testResults);
      return baos.toByteArray();
    } finally {
      oos.close();
      baos.close();
    }
  }

  public void setTestParameters(Map<String, String> parameters) {
    this.testParameters = parameters;
  }
}
