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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.repository.Deployer;
import uk.co.brunella.osgi.bdt.util.FileUtils;

public class KnopflerfishFrameworkStarter extends AbstractOSGiFrameworkStarter {

  public static final String KNOPFLERFISH_SYSTEM_BUNDLE_NAME = "frameworkbundle"; 
  public static final String KNOPFLERFISH_SYSTEM_BUNDLE_SYMBOLIC_NAME = "org.knopflerfish.framework"; 
  
  private Class<?> starterClass;
  private Object starterObject;

  public KnopflerfishFrameworkStarter(BundleRepository bundleRepository) {
    super(bundleRepository);
  }

  public String systemBundleName() {
    return KNOPFLERFISH_SYSTEM_BUNDLE_SYMBOLIC_NAME;
  }

  public String[] defaultArguments() {
    return new String[] {};
  }

  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    URL systemBundleUrl = bundleFileUrl(systemBundleName);
    System.setProperty("org.knopflerfish.osgi.registerserviceurlhandler", "false");
    URLClassLoader classLoader = new URLClassLoader(new URL[] { systemBundleUrl }, getClass().getClassLoader());
    starterClass = classLoader.loadClass("org.knopflerfish.framework.Framework");
    
    Method setPropertyMethod = starterClass.getDeclaredMethod("setProperty", String.class, String.class);
    File tempDir = new File(getBundleRepository().getLocation(), Deployer.TEMP_DIRECTORY);
    File knopflerfishTempDir = new File(tempDir, "knopflerfish");
    if (knopflerfishTempDir.exists()) {
      FileUtils.deleteDir(knopflerfishTempDir);
    }
    knopflerfishTempDir.mkdir();
    setPropertyMethod.invoke(null, "org.osgi.framework.dir", knopflerfishTempDir.toString());
    
    Constructor<?> constructor = starterClass.getConstructor(Object.class);
    starterObject = constructor.newInstance(new Object[] { null });
    Method launchMethod = starterClass.getDeclaredMethod("launch", long.class);
    launchMethod.invoke(starterObject, 0L);
    Method getSystemBundleContextMethod = starterClass.getDeclaredMethod("getSystemBundleContext");
    BundleContextWrapper bundleContext = new BundleContextWrapper(getSystemBundleContextMethod.invoke(starterObject));
    setSystemBundleContext(bundleContext);
  }

  public void stopFramework() throws Exception {
    Method shutdownMethod = starterClass.getDeclaredMethod("shutdown");
    shutdownMethod.invoke(starterObject);
  }
 }
