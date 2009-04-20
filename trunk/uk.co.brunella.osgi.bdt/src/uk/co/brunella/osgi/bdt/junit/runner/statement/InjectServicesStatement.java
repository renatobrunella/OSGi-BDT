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
package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.lang.annotation.Annotation;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import uk.co.brunella.osgi.bdt.junit.annotation.OSGiService;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkField;

public class InjectServicesStatement extends Statement {

  private final static long WAIT_TIME_IN_MILLIS = 100L;
  
  private final Bundle fTestBundle;
  private final Statement fNext;
  private final Object fTarget;
  private final List<FrameworkField> fFields;

  public InjectServicesStatement(Bundle testBundle, Statement next, List<FrameworkField> fields, Object target) {
    fTestBundle = testBundle;
    fNext = next;
    fFields = fields;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    // get services and inject
    for (FrameworkField field : fFields) {
      Annotation annotation = field.getAnnotation(OSGiService.class.getName());
      String serviceClassName = getServiceClassName(annotation);
      String filter = getServiceFilter(annotation);
      long timeout = getServiceTimeout(annotation);
      Object service = getService(serviceClassName, filter, timeout);
      field.set(fTarget, service);
    }
    fNext.evaluate();
    
    // unget services
    for (FrameworkField field : fFields) {
      Annotation annotation = field.getAnnotation(OSGiService.class.getName());
      String serviceClassName = getServiceClassName(annotation);
      String filter = getServiceFilter(annotation);
      ungetService(serviceClassName, filter);
    }
  }

  private Object getService(String serviceClassName, String filter, long timeout) {
    BundleContext bundleContext = fTestBundle.getBundleContext();
    ServiceReference reference = null;
    if (timeout == 0L) {
      reference = getServiceReference(bundleContext, serviceClassName, filter);
    } else {
      long currentTime = System.currentTimeMillis();
      reference = getServiceReference(bundleContext, serviceClassName, filter);
      while (reference == null && System.currentTimeMillis() < currentTime + timeout) {
        try {
          Thread.sleep(WAIT_TIME_IN_MILLIS);
        } catch (InterruptedException e) { }
        reference = getServiceReference(bundleContext, serviceClassName, filter);
      }
    }
    if (reference == null) {
      throw new AssertionError("Could not inject service \"" + serviceClassName + "\" " + filter);
    }
    return bundleContext.getService(reference);
  }
  
  private void ungetService(String serviceClassName, String filter) {
    BundleContext bundleContext = fTestBundle.getBundleContext();
    ServiceReference reference = getServiceReference(bundleContext, serviceClassName, filter);
    if (reference != null) {
      bundleContext.ungetService(reference);
    }
  }
  
  private ServiceReference getServiceReference(BundleContext bundleContext, String serviceClassName, String filter) {
    if (filter == null || filter.equals("")) {
      return bundleContext.getServiceReference(serviceClassName);
    } else {
      ServiceReference[] references;
      try {
        references = bundleContext.getServiceReferences(serviceClassName, filter);
      } catch (InvalidSyntaxException e) {
        throw new RuntimeException(e);
      }
      if (references == null || references.length == 0) {
        return null;
      } else {
        return references[0];
      }
    }
  }

  private String getServiceClassName(Annotation annotation) {
    Class<?> serviceClass = (Class<?>) getAnnoationValue(annotation, "serviceClass");
    if (serviceClass != null && !serviceClass.getName().equals(Void.class.getName())) {
      return serviceClass.getName();
    } else {
      return (String) getAnnoationValue(annotation, "serviceName");
    }
  }
  
  private String getServiceFilter(Annotation annotation) {
    return (String) getAnnoationValue(annotation, "filter");
  }
  
  private long getServiceTimeout(Annotation annotation) {
    return (Long) getAnnoationValue(annotation, "timeout");
  }

  private Object getAnnoationValue(Annotation annotation, String methodName) {
    if (annotation == null) {
      return null;
    }
    try {
      return annotation.annotationType().getDeclaredMethod(methodName).invoke(annotation);
    } catch (Exception e) {
      return null;
    }
  }
}