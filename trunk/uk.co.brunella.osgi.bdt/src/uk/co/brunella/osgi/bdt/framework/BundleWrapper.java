/*
 * Copyright 2008 - 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.framework;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class BundleWrapper implements Bundle {

  private final Object bundle;

  public BundleWrapper(Object bundle) {
    this.bundle = bundle;
  }
  
  public Enumeration<?> findEntries(String path, String filePattern, boolean recurse) {
    return (Enumeration<?>) invokeNoException("findEntries", parameterTypes(String.class, String.class, boolean.class), path, filePattern, recurse);
  }

  public BundleContext getBundleContext() {
    return (BundleContext) invokeNoException("getBundleContext", parameterTypes());
  }

  public long getBundleId() {
    return (Long) invokeNoException("getBundleId", parameterTypes());
  }

  public URL getEntry(String path) {
    return (URL) invokeNoException("getEntry", parameterTypes(String.class), path);
  }

  public Enumeration<?> getEntryPaths(String path) {
    return (Enumeration<?>) invokeNoException("getEntryPaths", parameterTypes(String.class), path);
  }

  public Dictionary<?, ?> getHeaders() {
    return (Dictionary<?, ?>) invokeNoException("getHeaders", parameterTypes());
  }

  public Dictionary<?, ?> getHeaders(String locale) {
    return (Dictionary<?, ?>) invokeNoException("getHeaders", parameterTypes(String.class), locale);
  }

  public long getLastModified() {
    return (Long) invokeNoException("getLastModified", parameterTypes());
  }

  public String getLocation() {
    return (String) invokeNoException("getLocation", parameterTypes());
  }

  public ServiceReference[] getRegisteredServices() {
    return (ServiceReference[]) invokeNoException("getRegisteredServices", parameterTypes());
  }

  public URL getResource(String name) {
    return (URL) invokeNoException("getResource", parameterTypes(String.class), name);
  }

  public Enumeration<?> getResources(String name) throws IOException {
    try {
      return (Enumeration<?>) invokeThrowable("getResources", parameterTypes(String.class), name);
    } catch (Throwable e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public ServiceReference[] getServicesInUse() {
    return (ServiceReference[]) invokeNoException("getServicesInUse", parameterTypes());
  }

  public int getState() {
    return (Integer) invokeNoException("getState", parameterTypes());
  }

  public String getSymbolicName() {
    return (String) invokeNoException("getSymbolicName", parameterTypes());
  }

  public boolean hasPermission(Object permission) {
    return (Boolean) invokeNoException("hasPermission", parameterTypes(Object.class), permission);
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    try {
      return (Class<?>) invokeThrowable("loadClass", parameterTypes(String.class), name);
    } catch (Throwable e) {
      if (e instanceof ClassNotFoundException) {
        throw (ClassNotFoundException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public void start() throws BundleException {
    invoke("start", parameterTypes());
  }

  public void start(int options) throws BundleException {
    invoke("start", parameterTypes(int.class), options);
  }

  public void stop() throws BundleException {
    invoke("stop", parameterTypes());
  }

  public void stop(int options) throws BundleException {
    invoke("stop", parameterTypes(int.class), options);
  }

  public void uninstall() throws BundleException {
    invoke("uninstall", parameterTypes());
  }

  public void update() throws BundleException {
    invoke("update", parameterTypes());
  }

  public void update(InputStream in) throws BundleException {
    invoke("update", parameterTypes(InputStream.class), in);
  }
  
  
  private Object invoke(String method, Class<?>[] parameterTypes, Object... arguments) throws BundleException {
    try {
      return bundle.getClass().getMethod(method, parameterTypes).invoke(bundle, arguments);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        throw (BundleException) ((InvocationTargetException) e).getTargetException();
      } else {
        return null;
      }
    }
  }
  
  private Object invokeThrowable(String method, Class<?>[] parameterTypes, Object... arguments) throws Throwable {
    try {
      return bundle.getClass().getMethod(method, parameterTypes).invoke(bundle, arguments);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        throw ((InvocationTargetException) e).getTargetException();
      } else {
        throw e;
      }
    }
  }
  
  private Object invokeNoException(String method, Class<?>[] parameterTypes, Object... arguments) {
    try {
      return bundle.getClass().getMethod(method, parameterTypes).invoke(bundle, arguments);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Class<?>[] parameterTypes(Class<?>... parameterTypes) {
    return parameterTypes;
  }
}
