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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.ServiceReference;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.framework.BundleContextWrapper;
import uk.co.brunella.osgi.bdt.framework.BundleWrapper;
import uk.co.brunella.osgi.bdt.framework.OSGiFrameworkStarter;
import uk.co.brunella.osgi.bdt.framework.OSGiFrameworkStarterFactory;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResult;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResultService;

public class OSGiTestRunner {

  private static final OSGiBundleDescriptor TEST_RUNNER_BUNDLE = new OSGiBundleDescriptor(
      "uk.co.brunella.osgi.bdt.osgitestrunner", VersionRange.parseVersionRange(""));
  
  private BundleRepository repository;
  private OSGiFrameworkStarter frameworkStarter;
  private String frameworkName;
  private OSGiBundleDescriptor systemBundleDescriptor;
  private OSGiBundleDescriptor testRunnerBundleDescriptor;
  private List<OSGiBundleDescriptor> requiredBundles;
  private List<OSGiBundleDescriptor> testBundles;
  private List<String> arguments;
  private Map<String, String> testParameters;

  public OSGiTestRunner() {
    frameworkName = "equinox";
    testRunnerBundleDescriptor = TEST_RUNNER_BUNDLE;
    requiredBundles = new ArrayList<OSGiBundleDescriptor>();
    testBundles = new ArrayList<OSGiBundleDescriptor>();
    arguments = new ArrayList<String>();
    testParameters = new HashMap<String, String>();
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

  public void resetTestParameters() {
    testParameters.clear();
  }
  
  public void addTestParameter(String name, String value) {
    testParameters.put(name, value);
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
    startFramework();
    
    // list of Bundle
    List<BundleWrapper> bundleList = new ArrayList<BundleWrapper>();
    try {
      // add required bundles defined in test
      for (OSGiBundleDescriptor descriptor : testBundles) {
        addRequiredBundleFromTest(descriptor);
      }
      
      BundleWrapper testRunnerBundle = frameworkStarter.installBundle(getBundleNameAndVersion(testRunnerBundleDescriptor));
      bundleList.add(testRunnerBundle);
      for (OSGiBundleDescriptor descriptor : requiredBundles) {
        BundleWrapper bundle = frameworkStarter.installBundle(getBundleNameAndVersion(descriptor));
        if (bundle != null) {
          bundleList.add(bundle);
        }
      }
      
      List<BundleWrapper> testBundleList = new ArrayList<BundleWrapper>();
      for (OSGiBundleDescriptor descriptor : testBundles) {
        BundleWrapper bundle = frameworkStarter.installBundle(getBundleNameAndVersion(descriptor));
        if (bundle != null) {
          testBundleList.add(bundle);
        }
      }
      // start all required bundles
      for (BundleWrapper bundle : bundleList) {
        bundle.start();
      }
      
      // set the test parameters
      if (testParameters.size() > 0) {
        setTestParameters(testRunnerBundle.getBundleContext());
      }

      // start the test bundles
      for (BundleWrapper bundle : testBundleList) {
        bundle.start();
      }
      List<OSGiTestResult> testResults = getTestResults(testRunnerBundle.getBundleContext()); 
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

//  private Object installBundle(BundleContextWrapper systemBundleContext, OSGiBundleDescriptor descriptor) throws Exception {
//    BundleDescriptor bundleDescriptor = resolveBundle(descriptor);
//    String bundleFileName = bundleFileLocation(bundleDescriptor);
//    Object bundle = systemBundleContext.installBundle(bundleFileName);
//    if (bundleDescriptor.getFragmentHost() == null) {
//      return bundle;
//    } else {
//      return null;
//    }
//  }

  private File bundleFile(BundleDescriptor bundleDescriptor) {
    return new File(repository.getLocation(), Deployer.BUNDLES_DIRECTORY + File.separator + bundleDescriptor.getBundleJarFileName());
  }
  
  private BundleDescriptor resolveBundle(OSGiBundleDescriptor descriptor) {
    BundleDescriptor[] resolvedBundles = repository.resolveBundle(descriptor.getBundleSymbolicName(), descriptor.getBundleVersionRange(), true);
    if (resolvedBundles.length == 0) {
      throw new RuntimeException("Cannot resolve bundle " + descriptor);
    }
    return resolvedBundles[0];
  }

  public void setFramework(String frameworkName) {
    this.frameworkName = frameworkName;
//    frameworkStarter = OSGiFrameworkStarterFactory.create(frameworkName);
  }

  private void startFramework() throws Exception {
    frameworkStarter = OSGiFrameworkStarterFactory.create(repository, frameworkName);
    // find the system bundle in the repository
    if (systemBundleDescriptor == null) {
      systemBundleDescriptor = new OSGiBundleDescriptor(frameworkStarter.systemBundleName(), VersionRange.parseVersionRange(""));
    }
    String systemBundleName = getBundleNameAndVersion(systemBundleDescriptor);

    // create arguments
    String[] defaults = frameworkStarter.defaultArguments();
    String[] args = new String[arguments.size() + defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      args[i] = defaults[i];
    }
    for (int i = 0; i < arguments.size(); i++) {
      args[i + defaults.length] = arguments.get(i);
    }
    
    frameworkStarter.startFramework(systemBundleName, args);
  }

  private String getBundleNameAndVersion(OSGiBundleDescriptor bundleDescriptor) {
    return bundleDescriptor.getBundleSymbolicName() + ";version=" + bundleDescriptor.getBundleVersionRange();
  }
  
  private void stopFramework() throws Exception {
    frameworkStarter.stopFramework();
  }

  @SuppressWarnings("unchecked")
  private List<OSGiTestResult> getTestResults(BundleContextWrapper bundleContext) throws Exception {
    ServiceReference reference = bundleContext.getServiceReference(OSGiTestResultService.class.getName());
    Object service = bundleContext.getService(reference);
    Method getTestResultsMethod = service.getClass().getDeclaredMethod("getTestResults");
    byte[] serialized = (byte[]) getTestResultsMethod.invoke(service);
    bundleContext.ungetService(reference);
    
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
  
  private void setTestParameters(BundleContextWrapper bundleContext) throws Exception {
    ServiceReference reference = bundleContext.getServiceReference(OSGiTestResultService.class.getName());
    Object service = bundleContext.getService(reference);
    Method setTestParametersMethod = service.getClass().getDeclaredMethod("setTestParameters", Map.class);
    setTestParametersMethod.invoke(service, testParameters);
    bundleContext.ungetService(reference);
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
