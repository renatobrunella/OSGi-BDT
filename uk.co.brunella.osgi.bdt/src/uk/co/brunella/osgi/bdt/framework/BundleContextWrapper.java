package uk.co.brunella.osgi.bdt.framework;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class BundleContextWrapper implements BundleContext {

  private final Object bundleContext;

  public BundleContextWrapper(Object bundleContext) {
    this.bundleContext = bundleContext;
  }
  
  public void addBundleListener(BundleListener listener) {
    invoke("addBundleListener", parameterTypes(BundleListener.class), listener);    
  }

  public void addFrameworkListener(FrameworkListener listener) {
    invoke("addFrameworkListener", parameterTypes(FrameworkListener.class), listener);    
  }

  public void addServiceListener(ServiceListener listener) {
    invoke("addServiceListener", parameterTypes(ServiceListener.class), listener);    
  }

  public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
    try {
      invokeThrowable("addServiceListener", parameterTypes(ServiceListener.class, String.class), listener, filter);
    } catch (Throwable e) {
      if (e instanceof InvalidSyntaxException) {
        throw (InvalidSyntaxException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Filter createFilter(String filter) throws InvalidSyntaxException {
    try {
      return (Filter) invokeThrowable("createFilter", parameterTypes(String.class), filter);
    } catch (Throwable e) {
      if (e instanceof InvalidSyntaxException) {
        throw (InvalidSyntaxException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
    try {
      return (ServiceReference[]) invokeThrowable("getAllServiceReferences", parameterTypes(String.class, String.class), clazz, filter);
    } catch (Throwable e) {
      if (e instanceof InvalidSyntaxException) {
        throw (InvalidSyntaxException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Bundle getBundle() {
    return (Bundle) invoke("getBundle", parameterTypes());
  }

  public Bundle getBundle(long id) {
    return (Bundle) invoke("getBundle", parameterTypes(long.class), id);
  }

  public Bundle[] getBundles() {
    return (Bundle[]) invoke("getBundles", parameterTypes());
  }

  public File getDataFile(String filename) {
    return (File) invoke("getDataFile", parameterTypes(String.class), filename);
  }

  public String getProperty(String key) {
    return (String) invoke("getProperty", parameterTypes(String.class), key);
  }

  public Object getService(ServiceReference reference) {
    return invoke("getService", parameterTypes(ServiceReference.class), reference);
  }

  public ServiceReference getServiceReference(String clazz) {
    return (ServiceReference) invoke("getServiceReference", parameterTypes(String.class), clazz);
  }

  public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
    try {
      return (ServiceReference[]) invokeThrowable("getServiceReferences", parameterTypes(String.class, String.class), clazz, filter);
    } catch (Throwable e) {
      if (e instanceof InvalidSyntaxException) {
        throw (InvalidSyntaxException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Bundle installBundle(String location) throws BundleException {
    try {
      return (Bundle) invokeThrowable("installBundle", parameterTypes(String.class), location);
    } catch (Throwable e) {
      if (e instanceof BundleException) {
        throw (BundleException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Bundle installBundle(String location, InputStream input) throws BundleException {
    try {
      return (Bundle) invokeThrowable("installBundle", parameterTypes(String.class, InputStream.class), location, input);
    } catch (Throwable e) {
      if (e instanceof BundleException) {
        throw (BundleException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
    return (ServiceRegistration) invoke("registerService", parameterTypes(String[].class, Object.class, Dictionary.class), clazzes, service, properties);
  }

  @SuppressWarnings("unchecked")
  public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
    return (ServiceRegistration) invoke("registerService", parameterTypes(String.class, Object.class, Dictionary.class), clazz, service, properties);
  }

  public void removeBundleListener(BundleListener listener) {
    invoke("removeBundleListener", parameterTypes(BundleListener.class), listener);
  }

  public void removeFrameworkListener(FrameworkListener listener) {
    invoke("removeFrameworkListener", parameterTypes(FrameworkListener.class), listener);
  }

  public void removeServiceListener(ServiceListener listener) {
    invoke("removeServiceListener", parameterTypes(ServiceListener.class), listener);
  }

  public boolean ungetService(ServiceReference reference) {
    return (Boolean) invoke("ungetService", parameterTypes(ServiceReference.class), reference);
  }
  
  
  private Object invoke(String method, Class<?>[] parameterTypes, Object... arguments) {
    try {
      return bundleContext.getClass().getMethod(method, parameterTypes).invoke(bundleContext, arguments);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private Object invokeThrowable(String method, Class<?>[] parameterTypes, Object... arguments) throws Throwable {
    try {
      return bundleContext.getClass().getMethod(method, parameterTypes).invoke(bundleContext, arguments);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        throw ((InvocationTargetException) e).getTargetException();
      } else {
        throw e;
      }
    }
  }

  private Class<?>[] parameterTypes(Class<?>... parameterTypes) {
    return parameterTypes;
  }

}
