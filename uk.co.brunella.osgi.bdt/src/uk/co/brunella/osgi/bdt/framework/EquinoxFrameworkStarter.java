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
package uk.co.brunella.osgi.bdt.framework;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class EquinoxFrameworkStarter implements OSGiFrameworkStarter {

  private Class<?> eclipseStarterClass;

  public String systemBundleName() {
    return "org.eclipse.osgi";
  }

  public String[] defaultArguments() {
    return new String[] { "-clean" };
  }
  
  public Object startFramework(URL systemBundleLocation, String[] arguments) throws Exception {
    // load the EclipseStarter class and call startup
    URLClassLoader classLoader = new URLClassLoader(new URL[] { systemBundleLocation }, getClass().getClassLoader());
    eclipseStarterClass = classLoader.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");
    Method startupMethod = eclipseStarterClass.getDeclaredMethod("startup", String[].class, Runnable.class);
    return startupMethod.invoke(null, arguments, null);
    // return org.eclipse.core.runtime.adaptor.EclipseStarter.startup(getOSGiArguments(), null);
  }

  public void stopFramework() throws Exception {
    Method shutdownMethod = eclipseStarterClass.getDeclaredMethod("shutdown");
    shutdownMethod.invoke(null);
    eclipseStarterClass = null;
    // org.eclipse.core.runtime.adaptor.EclipseStarter.shutdown();
  }
}
