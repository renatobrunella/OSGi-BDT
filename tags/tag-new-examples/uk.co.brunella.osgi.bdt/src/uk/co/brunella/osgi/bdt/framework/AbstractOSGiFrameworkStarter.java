package uk.co.brunella.osgi.bdt.framework;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osgi.framework.BundleException;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;
import uk.co.brunella.osgi.bdt.repository.Deployer;
import uk.co.brunella.osgi.bdt.util.FileUtils;

public abstract class AbstractOSGiFrameworkStarter implements OSGiFrameworkStarter {

  private final BundleRepository bundleRepository;
  private final Framework framework;
  private final File tempDirectory;
  private BundleContextWrapper systemBundleContext;

  protected AbstractOSGiFrameworkStarter(Framework framework, BundleRepository bundleRepository) {
    this.bundleRepository = bundleRepository;
    this.framework = framework;
    this.tempDirectory = getTempDirectory(framework);
  }
  
  public BundleRepository getBundleRepository() {
    return bundleRepository;
  }
  
  protected void setSystemBundleContext(BundleContextWrapper systemBundleContext) {
    this.systemBundleContext = systemBundleContext;
  }
  
  public BundleContextWrapper getSystemBundleContext() {
    return systemBundleContext;
  }
  
  public BundleWrapper installBundle(String bundleName) throws BundleException {
    return installBundle(findBundle(bundleName));
  }
  
  public BundleWrapper installBundle(File bundleFile) throws BundleException {
    String bundleLocation = "file:" + bundleFile.toString();
    return new BundleWrapper(systemBundleContext.installBundle(bundleLocation));
  }
  
  public BundleWrapper installBundle(BundleDescriptor bundleDescriptor) throws BundleException {
    String bundleLocation = "file:" + bundleFile(bundleDescriptor).toString();
    return new BundleWrapper(systemBundleContext.installBundle(bundleLocation));
  }

  protected BundleDescriptor findBundle(String bundleName) {
    String name;
    VersionRange versionRange;
    if (bundleName.contains(";version=")) {
      name = bundleName.substring(0, bundleName.indexOf(';'));
      versionRange = VersionRange.parseVersionRange(bundleName.substring(bundleName.indexOf(';') + ";version=".length()));
    } else {
      name = bundleName;
      versionRange = VersionRange.parseVersionRange("");
    }
    BundleDescriptor[] descriptors = bundleRepository.resolveBundle(name, versionRange, true);
    if (descriptors.length > 0) {
      return descriptors[0];
    } else {
      throw new RuntimeException("Cannot find bundle " + bundleName);
    }
  }

  protected File bundleFile(BundleDescriptor bundleDescriptor) {
    return new File(bundleRepository.getLocation(), Deployer.BUNDLES_DIRECTORY + File.separator + bundleDescriptor.getBundleJarFileName());
  }
  
  protected URL bundleFileUrl(String bundleName) throws MalformedURLException {
    File bundleFile = bundleFile(findBundle(bundleName));
    return new URL("file:" + bundleFile);
  }

  protected File getTempDirectory() {
    return tempDirectory;
  }
  
  protected Class<?> loadStarterClass(String systemBundleName, String className) throws Exception {
    URL systemBundleUrl = bundleFileUrl(systemBundleName);
    URLClassLoader classLoader = new URLClassLoader(new URL[] { systemBundleUrl }, getClass().getClassLoader());
    return classLoader.loadClass(className);
  }
  
  private File getTempDirectory(Framework framework) { 
    File tempDir = new File(getBundleRepository().getLocation(), Deployer.TEMP_DIRECTORY);
    File frameworkTempDir = new File(tempDir, framework.toString().toLowerCase());
    frameworkTempDir.mkdir();
    return frameworkTempDir;
  }

  protected void cleanTempDirectory() {
    FileUtils.deleteDir(tempDirectory);
    tempDirectory.mkdir();
  }
  
  protected Property getProperty(String nameValuePair) {
    String name;
    String value;
    
    if (nameValuePair.startsWith("-D")) {
      name = nameValuePair.substring(2, nameValuePair.indexOf('='));
      value = nameValuePair.substring(nameValuePair.indexOf('=') + 1);
      if (value.length() > 2 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}') {
        value = resolveSymbol(value.substring(1, value.length() - 1));
      }
    } else {
      name = nameValuePair;
      value = null;
    }
    
    return new Property(name, value);
  }
  
  private String resolveSymbol(String symbol) {
    if ("framework.tempdir".equals(symbol)) {
      return tempDirectory.toString();
    } else if ("framework.tempdir.url".equals(symbol)) {
      return "file:/" + tempDirectory.toString();
    }
    throw new RuntimeException("Unknown symbol " + symbol);
  }
  
  protected Framework getFramework() {
    return framework;
  }

  static class Property {
    private String name;
    private String value;

    public Property(String name, String value) {
      this.name = name;
      this.value = value;
    }
    
    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }
}
