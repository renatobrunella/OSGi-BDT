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
package uk.co.brunella.osgi.bdt.runner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.runner.framework.OSGiFrameworkStarter;
import uk.co.brunella.osgi.bdt.runner.framework.OSGiFrameworkStarterFactory;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResult;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResultService;

public class OSGiTestRunner {

  private static final OSGiBundleDescriptor TEST_RUNNER_BUNDLE = new OSGiBundleDescriptor(
      "uk.co.brunella.osgi.bdt.osgitestrunner", VersionRange.parseVersionRange(""));
  
  private BundleRepository repository;
  private OSGiFrameworkStarter frameworkStarter;
  private OSGiBundleDescriptor systemBundleDescriptor;
  private OSGiBundleDescriptor testRunnerBundleDescriptor;
  private List<OSGiBundleDescriptor> requiredBundles;
  private List<OSGiBundleDescriptor> testBundles;
  private List<String> arguments;

  public OSGiTestRunner() {
    frameworkStarter = OSGiFrameworkStarterFactory.create("equinox");
    testRunnerBundleDescriptor = TEST_RUNNER_BUNDLE;
    requiredBundles = new ArrayList<OSGiBundleDescriptor>();
    testBundles = new ArrayList<OSGiBundleDescriptor>();
    arguments = new ArrayList<String>();
  }

  public void setRepositoryDirectory(String repositoryDirectory) {
    BundleRepositoryPersister persister = new BundleRepositoryPersister(new File(repositoryDirectory));
    try {
      repository = persister.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void setSystemBundle(String bundleSymbolicName, String bundleVersionRange) {
    systemBundleDescriptor = new OSGiBundleDescriptor(bundleSymbolicName, parseVersionRange(bundleVersionRange));
  }

  public void setTestRunnerBundle(String bundleSymbolicName, String bundleVersionRange) {
    testRunnerBundleDescriptor = new OSGiBundleDescriptor(bundleSymbolicName, parseVersionRange(bundleVersionRange));
  }
  
  public void addRequiredBundle(String bundleSymbolicName, String bundleVersionRange) {
    requiredBundles.add(new OSGiBundleDescriptor(bundleSymbolicName, parseVersionRange(bundleVersionRange)));
  }
  
  public void addTestBundle(String bundleSymbolicName, String bundleVersionRange) {
    testBundles.add(new OSGiBundleDescriptor(bundleSymbolicName, parseVersionRange(bundleVersionRange)));
  }

  private static VersionRange parseVersionRange(String bundleVersionRange) {
    if (bundleVersionRange == null) {
      bundleVersionRange = "";
    }
    VersionRange versionRange = VersionRange.parseVersionRange(bundleVersionRange);
    return versionRange;
  }
  
  public void addOSGiArgument(String argument) {
    arguments.add(argument);
  }
  
  public List<OSGiTestResult> runTests() throws Exception {
    Object systemBundleContext = startFramework();
    
    // list of Bundle
    List<Object> bundleList = new ArrayList<Object>();
    try {
      // add required bundles defined in test
      for (OSGiBundleDescriptor descriptor : testBundles) {
        addRequiredBundleFromTest(descriptor);
      }
      
      Object testRunnerBundle = installBundle(systemBundleContext, testRunnerBundleDescriptor);
      bundleList.add(testRunnerBundle);
      for (OSGiBundleDescriptor descriptor : requiredBundles) {
        Object bundle = installBundle(systemBundleContext, descriptor);
        if (bundle != null) {
          bundleList.add(bundle);
        }
      }
      List<Object> testBundleList = new ArrayList<Object>();
      for (OSGiBundleDescriptor descriptor : testBundles) {
        Object bundle = installBundle(systemBundleContext, descriptor);
        if (bundle != null) {
          testBundleList.add(bundle);
        }
      }
      // start all required bundles
      for (Object bundle : bundleList) {
        BundleWrapper.start(bundle);
      }
      // start the test bundles
      for (Object bundle : testBundleList) {
        BundleWrapper.start(bundle);
      }
      List<OSGiTestResult> testResults = getTestResults(BundleWrapper.getBundleContext(testRunnerBundle)); 
      return testResults;
    } finally {
      stopFramework();
    }
  }
  
  private void addRequiredBundleFromTest(OSGiBundleDescriptor descriptor) {
    BundleDescriptor bundleDescriptor = resolveBundle(descriptor);
    File bundleFile = bundleFile(bundleDescriptor);
    try {
      JarFile bundleJarFile = new JarFile(bundleFile);
      ZipEntry entry = bundleJarFile.getEntry("META-INF/osgi-bdt-testcase.xml");
      if (entry != null) {
        InputStream is = bundleJarFile.getInputStream(entry);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLHandler handler = new XMLHandler();
        parser.parse(is, handler);
        for (OSGiBundleDescriptor required : handler.getRequiredBundles()) {
          requiredBundles.add(required);
        }
      }
      bundleJarFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }

  private Object installBundle(Object systemBundleContext, OSGiBundleDescriptor descriptor) throws Exception {
    BundleDescriptor bundleDescriptor = resolveBundle(descriptor);
    String bundleFileName = bundleFileLocation(bundleDescriptor);
    Object bundle = BundleContextWrapper.installBundle(systemBundleContext, bundleFileName);
    if (bundleDescriptor.getFragmentHost() == null) {
      return bundle;
    } else {
      return null;
    }
  }

  private String bundleFileLocation(BundleDescriptor bundleDescriptor) {
    return "file:" + bundleFile(bundleDescriptor).toString();
  }

  private File bundleFile(BundleDescriptor bundleDescriptor) {
    return new File(repository.getLocation(), "bundles" + File.separator + bundleDescriptor.getBundleJarFileName());
  }
  
  private BundleDescriptor resolveBundle(OSGiBundleDescriptor descriptor) {
    BundleDescriptor[] resolvedBundles = repository.resolveBundle(descriptor.getBundleSymbolicName(), descriptor.getBundleVersionRange(), true);
    if (resolvedBundles.length == 0) {
      throw new RuntimeException("Cannot resolve bundle " + descriptor);
    }
    return resolvedBundles[0];
  }

  public void setFramework(String frameworkName) {
    frameworkStarter = OSGiFrameworkStarterFactory.create(frameworkName);
  }

  private Object startFramework() throws Exception {
    // find the system bundle in the repository
    if (systemBundleDescriptor == null) {
      systemBundleDescriptor = new OSGiBundleDescriptor(frameworkStarter.systemBundleName(), VersionRange.parseVersionRange(""));
    }
    BundleDescriptor systemBundle = resolveBundle(systemBundleDescriptor);
    String systemBundleLocation = bundleFileLocation(systemBundle);

    // create arguments
    String[] defaults = frameworkStarter.defaultArguments();
    String[] args = new String[arguments.size() + defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      args[i] = defaults[i];
    }
    for (int i = 0; i < arguments.size(); i++) {
      args[i + defaults.length] = arguments.get(i);
    }
    
    return frameworkStarter.startFramework(new URL(systemBundleLocation), args);
  }
  
  private void stopFramework() throws Exception {
    frameworkStarter.stopFramework();
  }

  @SuppressWarnings("unchecked")
  private List<OSGiTestResult> getTestResults(Object systemBundleContext) throws Exception {
    Object reference = BundleContextWrapper.getServiceReference(systemBundleContext, OSGiTestResultService.class.getName());
    Object service = BundleContextWrapper.getService(systemBundleContext, reference);
//    Object service = systemBundleContext.getService(systemBundleContext.getServiceReference(OSGiTestResultService.class.getName()));
    Method getTestResultsMethod = service.getClass().getDeclaredMethod("getTestResults");
    byte[] serialized = (byte[]) getTestResultsMethod.invoke(service);
    BundleContextWrapper.ungetService(systemBundleContext, reference);
    
    ByteArrayInputStream bais = null;
    ObjectInputStream ois = null;
    try {
      bais = new ByteArrayInputStream(serialized);
      ois = new ObjectInputStream(bais);
      return (List<OSGiTestResult>) ois.readObject();
    } finally {
      ois.close();
      bais.close();
    }
  }

  private static class OSGiBundleDescriptor {
    private String bundleSymbolicName;
    private VersionRange bundleVersionRange;

    public OSGiBundleDescriptor(String bundleSymbolicName, VersionRange bundleVersionRange) {
      super();
      this.bundleSymbolicName = bundleSymbolicName;
      this.bundleVersionRange = bundleVersionRange;
    }

    public String getBundleSymbolicName() {
      return bundleSymbolicName;
    }

    public VersionRange getBundleVersionRange() {
      return bundleVersionRange;
    }
    
    public String toString() {
      return bundleSymbolicName + " " + bundleVersionRange;
    }
  }

  private static class BundleWrapper {
    
    public static void start(Object bundle) throws Exception {
      bundle.getClass().getMethod("start").invoke(bundle);
    }
    
    public static void stop(Object bundle) throws Exception {
      bundle.getClass().getMethod("stop").invoke(bundle);
    }
    
    public static Object getBundleContext(Object bundle) throws Exception {
      return bundle.getClass().getMethod("getBundleContext").invoke(bundle);
    }
  }
  
  private static class BundleContextWrapper {
    
    public static Object installBundle(Object context, String location) throws Exception {
      return context.getClass().getMethod("installBundle", String.class).invoke(context, location);
    }
    
    public static Object getServiceReference(Object context, String serviceName) throws Exception {
      return context.getClass().getMethod("getServiceReference", String.class).invoke(context, serviceName);
    }
    
    public static Object getService(Object context, Object serviceReference) throws Exception {
      Method[] methods = context.getClass().getMethods();
      Method getServiceMethod = null;
      for (Method method : methods) {
        if (method.getName().equals("getService")) {
          getServiceMethod = method;
          break;
        }
      }
      return getServiceMethod.invoke(context, serviceReference);
    }
    
    public static boolean ungetService(Object context, Object serviceReference) throws Exception {
      Method[] methods = context.getClass().getMethods();
      Method getServiceMethod = null;
      for (Method method : methods) {
        if (method.getName().equals("ungetService")) {
          getServiceMethod = method;
          break;
        }
      }
      return (Boolean) getServiceMethod.invoke(context, serviceReference);
    }
  }

  //TODO add validation
  private static class XMLHandler extends DefaultHandler {

    private List<OSGiBundleDescriptor> requiredBundles = new ArrayList<OSGiBundleDescriptor>();
    
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      if ("bundle".equals(qName)) {
        String bundleName = attributes.getValue("name");
        String bundleVersionRange = attributes.getValue("version");
        OSGiBundleDescriptor descriptor = new OSGiBundleDescriptor(bundleName, parseVersionRange(bundleVersionRange));
        requiredBundles.add(descriptor);
      }
    }

    public List<OSGiBundleDescriptor> getRequiredBundles() {
      return requiredBundles;
    }
  }
}
