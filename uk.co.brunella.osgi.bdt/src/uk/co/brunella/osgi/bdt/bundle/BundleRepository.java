/*
 * Copyright 2008 - 2010 brunella ltd
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
package uk.co.brunella.osgi.bdt.bundle;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ImportPackage;
import uk.co.brunella.osgi.bdt.repository.profile.Profile;

public class BundleRepository implements Serializable {

  private static final long serialVersionUID = -8509546195436191224L;

  private List<BundleDescriptor> bundleDescriptors;
  private String profileName;
  // maps package name to a list of exported packages
  private transient Map<String, List<ExportPackage>> exportedPackages;
  // list of packages supplied by the system (JRE runtime)
  private transient Set<String> systemPackages = new HashSet<String>(0);
  private transient File location;

  public BundleRepository(String profileName) {
    if (!Profile.isValidProfileName(profileName)) {
      throw new IllegalArgumentException("Invalid profile name: " + profileName);
    }
    this.profileName = profileName;
    bundleDescriptors = new ArrayList<BundleDescriptor>();
    readSystemPackages();
    exportedPackages = new HashMap<String, List<ExportPackage>>();
  }

  private void readSystemPackages() {
    Properties profileProperties = Profile.getProfile(profileName);
    String[] packages = ((String) profileProperties.get("org.osgi.framework.system.packages")).split(",");
    systemPackages = new HashSet<String>(packages.length);
    for (String pkg : packages) {
      systemPackages.add(pkg);
    }
  }

  public void addBundleDescriptor(BundleDescriptor descriptor) {
    bundleDescriptors.add(descriptor);
    addExportPackages(descriptor);
  }
  
  public List<BundleDescriptor> getBundleDescriptors() {
    return bundleDescriptors;
  }
  
  public void setSystemPackages(List<String> packageList) {
    systemPackages = new HashSet<String>(packageList.size());
    systemPackages.addAll(packageList);
  }
  
  public BundleDescriptor removeBundleDescriptor(String bundleSymbolicName, VersionRange bundleVersionRange) {
    int index = -1;
    BundleDescriptor descriptor = null;
    for (int i = 0; i < bundleDescriptors.size(); i++) {
      descriptor = bundleDescriptors.get(i);
      if (descriptor.getBundleSymbolicName().equals(bundleSymbolicName) 
          && bundleVersionRange.isIncluded(descriptor.getBundleVersion())) {
        index = i;
        break;
      }
    }
    if (index >= 0) {
      bundleDescriptors.remove(index);
      removeExportPackages(descriptor);
      return descriptor;
    } else {
      return null;
    }
  }

  public BundleDescriptor findBundleDescriptor(String bundleSymbolicName, Version bundleVersion) {
    for (BundleDescriptor descriptor : bundleDescriptors) {
      if (descriptor.getBundleSymbolicName().equals(bundleSymbolicName)
          && descriptor.getBundleVersion().equals(bundleVersion)) {
        return descriptor;
      }
    }
    return null;
  }
  
  private void refreshExportPackages() {
    exportedPackages = new HashMap<String, List<ExportPackage>>();
    for (BundleDescriptor descriptor : bundleDescriptors) {
      addExportPackages(descriptor);
    }
  }
  
  private void addExportPackages(BundleDescriptor descriptor) {
    ExportPackage[] exportPackages = descriptor.getExportPackages();
    for (ExportPackage exportPackage : exportPackages) {
      List<ExportPackage> list = exportedPackages.get(exportPackage.getName());
      if (list == null) {
        list = new ArrayList<ExportPackage>(3);
        exportedPackages.put(exportPackage.getName(), list);
      }
      list.add(exportPackage);
    }
  }
  
  private void removeExportPackages(BundleDescriptor descriptor) {
    ExportPackage[] exportPackages = descriptor.getExportPackages();
    for (ExportPackage exportPackage : exportPackages) {
      List<ExportPackage> list = exportedPackages.get(exportPackage.getName());
      if (list != null) {
        for (int i = list.size() - 1; i >= 0; i--) {
          if (list.get(i).getBundleDescriptor().equals(descriptor)) {
            list.remove(i);
          }
        }
        if (list.size() == 0) {
          exportedPackages.remove(exportPackage.getName());
        }
      }
    }
  }

  public ExportPackage[] resolve(String name, VersionRange versionRange, boolean mandatory) {
    // check if the system packages supply the package
    if (systemPackages != null && systemPackages.contains(name)) {
      return new ExportPackage[] { new ExportPackage(null, name, Version.emptyVersion) };
    }
    
    List<ExportPackage> list = exportedPackages.get(name);
    if (list == null) {
      return new ExportPackage[0];
    }
    List<ExportPackage> matchingList = new ArrayList<ExportPackage>(list.size());
    for (ExportPackage exportPackage : list) {
      if (versionRange.isIncluded(exportPackage.getVersion())) {
        matchingList.add(exportPackage);
      }
    }
    ExportPackage[] result = matchingList.toArray(new ExportPackage[matchingList.size()]);
    // sort in descending order
    Arrays.sort(result, new Comparator<ExportPackage>() {
      public int compare(ExportPackage o1, ExportPackage o2) {
        return -(o1.getVersion().compareTo(o2.getVersion()));
      }});
    return result;
  }
  
  public BundleDescriptor[] resolveBundle(String name, VersionRange versionRange, boolean mandatory) {
    List<BundleDescriptor> list = new ArrayList<BundleDescriptor>();
    
    for (BundleDescriptor descriptor : bundleDescriptors) {
      if (descriptor.getBundleSymbolicName().equals(name) && versionRange.isIncluded(descriptor.getBundleVersion())) {
        list.add(descriptor);
      }
    }
    BundleDescriptor[] result = list.toArray(new BundleDescriptor[list.size()]);
    // sort in descending order
    Arrays.sort(result, new Comparator<BundleDescriptor>() {
      public int compare(BundleDescriptor o1, BundleDescriptor o2) {
        return -(o1.getBundleVersion().compareTo(o2.getBundleVersion()));
      }});
    return result;
  }
  
  public Set<BundleDescriptor> getBundleDependencies(BundleDescriptor descriptor) {
    //TODO add requireBundles
    Set<BundleDescriptor> dependencies = new HashSet<BundleDescriptor>();
    for (ImportPackage importPackage : descriptor.getImportPackages()) {
      ExportPackage[] exportPackages = resolve(importPackage.getName(), importPackage.getVersionRange(), true);
      if (exportPackages.length > 0) {
        BundleDescriptor exportBundle = exportPackages[0].getBundleDescriptor();
        if (exportBundle != null) {
          dependencies.add(exportBundle);
        }
      }
    }
    return dependencies;
  }

  public Properties getProfile() {
    return Profile.getProfile(profileName);
  }
  
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    readSystemPackages();
    refreshExportPackages();
  }

  public String getProfileName() {
    return profileName;
  }
  
  public File getLocation() {
    return location;
  }

  public void setLocation(File repositoryDirectory) {
    this.location = repositoryDirectory;
  }
}
