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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;

public class EquinoxFrameworkStarter extends AbstractOSGiFrameworkStarter {

  private static final String EQUINOX_STARTER_CLASS_NAME = "org.eclipse.core.runtime.adaptor.EclipseStarter";

  public EquinoxFrameworkStarter(BundleRepository bundleRepository) {
    super(Framework.EQUINOX, bundleRepository);
  }

  private Class<?> eclipseStarterClass;

  public String systemBundleName() {
    return "org.eclipse.osgi";
  }

  public String[] defaultArguments() {
    return new String[] { "-clean", "-Dosgi.install.area={framework.tempdir.url}" };
  }
  
  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    cleanTempDirectory();
    String[] eclipseArguments = processArguments(arguments);
    eclipseStarterClass = loadStarterClass(systemBundleName, EQUINOX_STARTER_CLASS_NAME);
    Method startupMethod = eclipseStarterClass.getDeclaredMethod("startup", String[].class, Runnable.class);
    BundleContextWrapper bundleContext =  new BundleContextWrapper(startupMethod.invoke(null, eclipseArguments, null));
    setSystemBundleContext(bundleContext);
  }
 
  private String[] processArguments(String[] arguments) {
    List<String> eclipseArguments = new ArrayList<String>();
    for (String argument : arguments) {
      Property property = getProperty(argument);
      if (property.getValue() != null) {
        System.setProperty(property.getName(), property.getValue());
      } else {
        eclipseArguments.add(property.getName());
      }
    }
    return eclipseArguments.toArray(new String[eclipseArguments.size()]);
  }

  public void stopFramework() throws Exception {
    Method shutdownMethod = eclipseStarterClass.getDeclaredMethod("shutdown");
    shutdownMethod.invoke(null);
    eclipseStarterClass = null;
  }
}
