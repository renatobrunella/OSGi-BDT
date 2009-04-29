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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;

public class KnopflerfishFrameworkStarter extends AbstractOSGiFrameworkStarter {

  public static final String KNOPFLERFISH_SYSTEM_BUNDLE_NAME = "frameworkbundle"; 
  public static final String KNOPFLERFISH_SYSTEM_BUNDLE_SYMBOLIC_NAME = "org.knopflerfish.framework";
  private static final String KNOPFLERFISH_STARTER_CLASS_NAME = "org.knopflerfish.framework.Framework";
  
  private Class<?> starterClass;
  private Object starterObject;

  public KnopflerfishFrameworkStarter(BundleRepository bundleRepository) {
    super(Framework.KNOPFLERFISH, bundleRepository);
  }

  public String systemBundleName() {
    return KNOPFLERFISH_SYSTEM_BUNDLE_SYMBOLIC_NAME;
  }

  public String[] defaultArguments() {
    return new String[] {
        "-Dorg.knopflerfish.osgi.registerserviceurlhandler=false",
        "-Dorg.osgi.framework.dir={framework.tempdir}"
    };
  }

  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    cleanTempDirectory();
    processArguments(arguments);
    starterClass = loadStarterClass(systemBundleName, KNOPFLERFISH_STARTER_CLASS_NAME);
    Constructor<?> constructor = starterClass.getConstructor(Object.class);
    starterObject = constructor.newInstance(new Object[] { null });
    Method launchMethod = starterClass.getDeclaredMethod("launch", long.class);
    launchMethod.invoke(starterObject, 0L);
    Method getSystemBundleContextMethod = starterClass.getDeclaredMethod("getSystemBundleContext");
    BundleContextWrapper bundleContext = new BundleContextWrapper(getSystemBundleContextMethod.invoke(starterObject));
    setSystemBundleContext(bundleContext);
  }

  private void processArguments(String[] arguments) {
    for (String argument : arguments) {
      Property property = getProperty(argument);
      if (property.getValue() != null) {
        System.setProperty(property.getName(), property.getValue());
      }
    }
  }

  public void stopFramework() throws Exception {
    Method shutdownMethod = starterClass.getDeclaredMethod("shutdown");
    shutdownMethod.invoke(starterObject);
  }
 }
