/*
 * Copyright 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.fit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;
import uk.co.brunella.osgi.bdt.junit.annotation.StartPolicy;
import uk.co.brunella.osgi.bdt.junit.runner.OSGiBDTJUnitRunner;
import uk.co.brunella.osgi.bdt.junit.runner.model.OSGiBDTTestWrapper;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;
import fit.Fixture;
import fit.Parse;

public class OSGiBDTFitTest extends Fixture {

  private Framework framework;
  private String systemBundle;
  private List<String> requiredBundles = new ArrayList<String>();
  private String testBundle;
  private Map<String, Object> parameters = new HashMap<String, Object>();
  private BundleRepository repository;

  public OSGiBDTFitTest() {
  }

  public void doCells(Parse cells) {
    String action = cells.text().toLowerCase();
    if ("framework".equals(action)) {
      setFramework(cells);
    } else if ("system".equals(action)) {
      setSystemBundle(cells);
    } else if ("required".equals(action)) {
      addRequired(cells);
    } else if ("test".equals(action)) {
      setTestBundle(cells);
    } else if ("parameter".equals(action)) {
      addParameter(cells);
    } else if ("run".equals(action) || "run tests".equals(action)) {
      runTests(cells);
    } else {
      exception(cells, new RuntimeException("Invalid"));
    }
  }

  private void setFramework(Parse cells) {
    if (framework != null) {
      exception(cells, new RuntimeException("Framework can only be set once!"));
    }
    checkColumns(cells, 2, 2);
    String frameworkName = cells.at(1).text().trim().toUpperCase();
    try {
      framework = Framework.valueOf(frameworkName);
    } catch (IllegalArgumentException e) {
      exception(cells, new RuntimeException("Invalid framework name: " + cells.at(1).text()));
    }
  }

  private void setSystemBundle(Parse cells) {
    if (systemBundle != null) {
      exception(cells, new RuntimeException("System bundle can only be set once!"));
    }
    systemBundle = getBundleNameAndVersion(cells);
  }

  private void addRequired(Parse cells) {
    String requiredBundle = getBundleNameAndVersion(cells);
    if (!requiredBundles.contains(requiredBundle)) {
      requiredBundles.add(requiredBundle);
    }
  }

  private void setTestBundle(Parse cells) {
    if (testBundle != null) {
      exception(cells, new RuntimeException("Test bundle can only be set once!"));
    }
    testBundle = getBundleNameAndVersion(cells);
  }

  private void addParameter(Parse cells) {
    checkColumns(cells, 3, 3);
    String name = cells.at(1).text();
    String value;
    if(cells.at(2).text().indexOf("=") == 0) {
      String symbol = cells.at(2).text().substring(1, cells.at(2).text().length()); 
      value = getSymbol(symbol).toString();  
    } else {
      value = cells.at(2).text();
    }
    if (parameters.containsKey(name)) {
      exception(cells, new RuntimeException("Parameter " + name + " was set twice"));
    }
    parameters.put(name, value);
  }

  private void runTests(Parse cells) {
    checkColumns(cells, 2, 2);
    if (repository == null) {
      if (args.length < 1) {
        exception(cells, new RuntimeException("Repository not set as argument"));
      }
      try {
        loadRepository(args[0]);
      } catch (IOException e) {
        exception(cells, new RuntimeException("Repository cannot be loaded: " + e.getMessage()));
      }
    }
    
    String testClassName = cells.at(1).text().trim();
    Class<?> testClass = loadTestClass(cells, testClassName);

    String[] arguments = new String[] {};
    String[] repositoryLocations = new String[] { repository.getLocation().toString() };
    String[] required = (String[]) requiredBundles.toArray(new String[requiredBundles.size()]); 
    
    OSGiBDTTestWrapper testArguments = new OSGiBDTTestWrapper(null, null, framework, StartPolicy.ONCE_PER_TEST_CLASS, 
        repositoryLocations, null, systemBundle, required, arguments);
    
    try {
      OSGiBDTJUnitRunner runner = new OSGiBDTJUnitRunner(testClass, testArguments, findBundle(testBundle), parameters);
      RunNotifier notifier = new RunNotifier();
      OSGiBDTFitRunListener listener = new OSGiBDTFitRunListener();
      notifier.addListener(listener);
      runner.run(notifier);
      addTestResultRows(cells, listener.getResults());
    } catch (InitializationError e) {
      exception(cells, e);
    }
    
    parameters.clear();
    
//    try {
//      List<OSGiTestResult> testResults = testRunner.runTests();
//      
//      addTestResultRows(cells, testResults);
//
//      
//    } catch (Exception e) {
//      exception(cells, e);
//    }
  }

  private void loadRepository(String repositoryLocation) throws IOException {
    BundleRepositoryPersister persister = new BundleRepositoryPersister(new File(repositoryLocation));
    repository = persister.load();
  }

  private Class<?> loadTestClass(Parse cells, String testClassName) {
    List<URL> jarUrls = new ArrayList<URL>();
    try {
      jarUrls.add(findBundle(OSGiBDTJUnitRunner.OSGI_BDT_RUNNER_BUNDLE_NAME).toURL());
      for (String requiredBunde : requiredBundles) {
        jarUrls.add(findBundle(requiredBunde).toURL());
      }
      jarUrls.add(findBundle(testBundle).toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    try {
      URLClassLoader classLoader = new URLClassLoader((URL[]) jarUrls.toArray(new URL[jarUrls.size()]), getClass().getClassLoader());
      return classLoader.loadClass(testClassName);
    } catch (ClassNotFoundException e) {
      exception(cells, e);
      return null;
    }
  }

  private String getBundleNameAndVersion(Parse cells) {
    String bundleSymbolicName;
    checkColumns(cells, 2, 3);
    bundleSymbolicName = cells.at(1).text().trim();
    if (cells.size() == 3) {
      String versionRange = cells.at(2).text().trim();
      bundleSymbolicName = bundleSymbolicName + ";version=" + versionRange;
    }
    return bundleSymbolicName;
  }

  private void checkColumns(Parse cells, int min, int max) {
    if (cells.size() < min || cells.size() > max) {
      exception(cells, new RuntimeException("Invalid number of columns. Expected min=" + min + " and max=" + max));
    }
  }

  private File findBundle(String bundleName) {
    String name;
    VersionRange versionRange;
    if (bundleName.contains(";version=")) {
      name = bundleName.substring(0, bundleName.indexOf(';'));
      versionRange = VersionRange.parseVersionRange(bundleName.substring(bundleName.indexOf(';') + ";version=".length()));
    } else {
      name = bundleName;
      versionRange = VersionRange.parseVersionRange("");
    }
    BundleDescriptor[] descriptors = repository.resolveBundle(name, versionRange, true);
    if (descriptors.length > 0) {
      BundleDescriptor descriptor = descriptors[0];
      File bundleJarFile = new File(repository.getLocation(), Deployer.BUNDLES_DIRECTORY + File.separator + descriptor.getBundleJarFileName());
      return bundleJarFile;
    } else {
      throw new RuntimeException("Cannot find bundle " + bundleName);
    }
  }

  private void addTestResultRows(Parse cells, List<OSGiTestResult> testResults) {
    Parse last = cells.last();
    last.more = buildHeaderRow();
    last = cells.last();
    last.more = buildRows(testResults);
  }

  private Parse buildHeaderRow() {
    Parse root = new Parse(null, null, null, null);
    Parse first = new Parse("td", "Status", null, null);
    Parse next = first.more = new Parse("td", "Test Name", null, null);
    next = next.more = new Parse("td", "Message", null, null);
    root.more = new Parse("tr", null, first, null);
    return root.more;
  }
  
  private Parse buildRows(List<OSGiTestResult> testResults) {
    Parse root = new Parse(null, null, null, null);
    Parse next = root;
    for (int i = 0; i < testResults.size(); i++) {
      next = next.more = new Parse("tr", null, buildCells(testResults.get(i)), null);
    }
    return root.more;
  }

  private Parse buildCells(OSGiTestResult testResult) {
    Parse root = new Parse(null, null, null, null);
    Parse next = root;
    if (testResult.hasPassed()) {
      next = next.more = new Parse("td", "Pass", null, null);
      right(next);
    } else if (testResult.hasFailed()) {
      next = next.more = new Parse("td", "Fail", null, null);
      wrong(next);
    } else if (testResult.hasErrored()) {
      next = next.more = new Parse("td", "Error", null, null);
      exception(next, testResult.getThrowable());
    }
    String testName = testResult.getMethodName(); 
    next = next.more = new Parse("td", testName, null, null);
    if (!testResult.hasPassed()) {
      next = next.more = new Parse("td", escape(testResult.getMessage()), null, null);
    } else {
      next = next.more = new Parse("td", "&nbsp;", null, null);
    }
    return root.more;
  }

}
