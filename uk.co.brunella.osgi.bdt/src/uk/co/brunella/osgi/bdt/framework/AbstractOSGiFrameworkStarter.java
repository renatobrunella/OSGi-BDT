package uk.co.brunella.osgi.bdt.framework;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.BundleException;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public abstract class AbstractOSGiFrameworkStarter implements OSGiFrameworkStarter {

  private final BundleRepository bundleRepository;
  private BundleContextWrapper systemBundleContext;

  protected AbstractOSGiFrameworkStarter(BundleRepository bundleRepository) {
    this.bundleRepository = bundleRepository;
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

}
