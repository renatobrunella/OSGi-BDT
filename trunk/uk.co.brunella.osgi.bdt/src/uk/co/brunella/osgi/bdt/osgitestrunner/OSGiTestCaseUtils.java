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

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


@Deprecated
public class OSGiTestCaseUtils {

  private static final int WAIT_TIME = 250;

  /**
   * Registers a <code>IntegrationTestCase</code> and runs the tests.
   * 
   * @param context     a bundle context
   * @param testSuite   the test suite
   */
  public static void run(BundleContext context, OSGiTestCase testSuite) {
    context.registerService(OSGiTestCase.class.getName(), testSuite, null);
  }

  /**
   * Waits till a service reference for a given service class is available or
   * the request times out.
   * 
   * @param bundleContext   a bundle context
   * @param serviceClass    the service class name
   * @param timeout         timeout in milli seconds
   * @return                a service reference or null
   */
  public static ServiceReference waitForServiceReference(BundleContext bundleContext, String serviceClass, long timeout) {
    ServiceReference reference = bundleContext.getServiceReference(serviceClass);
    while ((null == reference) && (0 < timeout)) {
      try {
        timeout -= WAIT_TIME;
        Thread.sleep(WAIT_TIME);
      } catch (InterruptedException e) {
        // do nothing
      }
      reference = bundleContext.getServiceReference(serviceClass);
    }
    return reference;
  }

  /**
   * Waits till service references for a given service class and filter are 
   * available or the request times out.
   * 
   * @param bundleContext   a bundle context
   * @param serviceClass    the service class name
   * @param filter          a filter or null
   * @param timeout         timeout in milli seconds
   * @return                an array of service references or null
   */
  public static ServiceReference[] waitForServiceReferences(BundleContext bundleContext, String serviceClass, String filter, long timeout) {
    try {
      ServiceReference[] references;
      references = bundleContext.getServiceReferences(serviceClass, filter);
      while ((references == null) && (timeout > 0)) {
        try {
          timeout -= WAIT_TIME;
          Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
          // do nothing
        }
        references = bundleContext.getServiceReferences(serviceClass, filter);
      }
      return references;
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Waits till a service from a service tracker is available or the request
   * times out.
   * 
   * @param serviceTracker  a service tracker
   * @param timeout         timeout in milli seconds
   * @return                a service object or null
   */
  public static Object waitForService(ServiceTracker serviceTracker, long timeout) {
    Object service = serviceTracker.getService();
    while (service == null && timeout > 0) {
      try {
        timeout -= WAIT_TIME;
        Thread.sleep(WAIT_TIME);
      } catch (InterruptedException e) {
        // do nothing
      }
      service = serviceTracker.getService();
    }
    return service;
  }

  /**
   * Waits till a service for a given service class is available or the request
   * times out.
   * 
   * @param bundleContext   a bundle context
   * @param serviceClass    the service class name
   * @param timeout         timeout in milli seconds
   * @return                a service object or null
   */
  public static Object waitForService(BundleContext bundleContext, String serviceClass, long timeout) {
    ServiceReference reference = waitForServiceReference(bundleContext, serviceClass, timeout);
    if (reference != null) {
      return bundleContext.getService(reference);
    } else {
      return null;
    }
  }
  
  /**
   * Waits till a service for a given service class and filter is available or 
   * the request times out. If more than one service is available the first
   * service will be returned.
   * 
   * @param bundleContext   a bundle context
   * @param serviceClass    the service class name
   * @param filter          a filter or null
   * @param timeout         timeout in milli seconds
   * @return                a service object or null
   */
  public static Object waitForService(BundleContext bundleContext, String serviceClass, String filter, long timeout) {
    ServiceReference[] references = waitForServiceReferences(bundleContext, serviceClass, filter, timeout);
    if (references != null) {
      return bundleContext.getService(references[0]);
    } else {
      return null;
    }
  }
}
