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
import java.util.HashMap;
import java.util.Map;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public class FelixFrameworkStarter extends AbstractOSGiFrameworkStarter {

  public FelixFrameworkStarter(BundleRepository bundleRepository) {
    super(bundleRepository);
  }

  private Class<?> felixStarterClass;
  private Object felixStarterObject;
  
  public String systemBundleName() {
    return "org.apache.felix.main";
  }

  public String[] defaultArguments() {
    return new String[] {};
  }

  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    URL systemBundleUrl = bundleFileUrl(systemBundleName);
    URLClassLoader classLoader = new URLClassLoader(new URL[] { systemBundleUrl }, getClass().getClassLoader());
    felixStarterClass = classLoader.loadClass("org.apache.felix.framework.Felix");
    Constructor<?> felixConstructor = felixStarterClass.getConstructor(Map.class);
    felixStarterObject = felixConstructor.newInstance(getFelixConfiguration());
    Method startMethod = felixStarterClass.getDeclaredMethod("start");
    startMethod.invoke(felixStarterObject);
    BundleContextWrapper bundleContext = new BundleWrapper(felixStarterObject).getBundleContext();
    setSystemBundleContext(bundleContext);
  }

  @SuppressWarnings("unchecked")
  private Map getFelixConfiguration() {
    Map configMap = new HashMap();
    configMap.put("felix.embedded.execution", "true");
    File tempDir = new File(getBundleRepository().getLocation(), Deployer.TEMP_DIRECTORY);
    File felixTempDir = new File(tempDir, "felix");
    configMap.put("org.osgi.framework.storage", felixTempDir.toString());
    configMap.put("org.osgi.framework.storage.clean", "onFirstInit");
    return configMap;
  }

  public void stopFramework() throws Exception {
    Method stopMethod = felixStarterClass.getDeclaredMethod("stop");
    stopMethod.invoke(felixStarterObject);
    felixStarterClass = null;
    felixStarterObject = null;
  }

}
