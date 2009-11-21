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
import java.util.HashMap;
import java.util.Map;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;

public class FelixFrameworkStarter extends AbstractOSGiFrameworkStarter {

  private static final String FELIX_STARTER_CLASS_NAME = "org.apache.felix.framework.Felix";

  public FelixFrameworkStarter(BundleRepository bundleRepository) {
    super(Framework.FELIX, bundleRepository);
  }

  private Class<?> felixStarterClass;
  private Object felixStarterObject;
  
  public String systemBundleName() {
    return "org.apache.felix.main";
  }

  public String[] defaultArguments() {
    return new String[] { 
        "-Dfelix.embedded.execution=true", 
        "-Dorg.osgi.framework.storage={framework.tempdir}",
        "-Dorg.osgi.framework.storage.clean=onFirstInit" };
  }

  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    cleanTempDirectory();
    felixStarterClass = loadStarterClass(systemBundleName, FELIX_STARTER_CLASS_NAME);
    Constructor<?> felixConstructor = felixStarterClass.getConstructor(Map.class);
    felixStarterObject = felixConstructor.newInstance(getFelixConfiguration(arguments));
    Method startMethod = felixStarterClass.getDeclaredMethod("start");
    startMethod.invoke(felixStarterObject);
    BundleContextWrapper bundleContext = new BundleWrapper(felixStarterObject).getBundleContext();
    setSystemBundleContext(bundleContext);
  }

  private Map<String, String> getFelixConfiguration(String[] arguments) {
    Map<String, String> configMap = new HashMap<String, String>();
    for (String argument : arguments) {
      Property property = getProperty(argument);
      if (property.getValue() != null) {
        configMap.put(property.getName(), property.getValue());
      }
    }
    return configMap;
  }

  public void stopFramework() throws Exception {
    Method stopMethod = felixStarterClass.getDeclaredMethod("stop");
    stopMethod.invoke(felixStarterObject);
    felixStarterClass = null;
    felixStarterObject = null;
  }

}
