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
package uk.co.brunella.osgi.bdt.bundle;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.PACKAGE_SPECIFICATION_VERSION;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_MANDATORY;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.VISIBILITY_DIRECTIVE;
import static org.osgi.framework.Constants.VISIBILITY_REEXPORT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

import uk.co.brunella.osgi.bdt.manifest.AttributeElement;
import uk.co.brunella.osgi.bdt.manifest.ManifestAttributeParser;

public class BundleDescriptor implements Serializable {

  private static final long serialVersionUID = -8270432208629234472L;

  private final static String[] ATTRIBUTE_NAMES = new String[] {
    BUNDLE_MANIFESTVERSION, BUNDLE_SYMBOLICNAME, BUNDLE_VERSION, BUNDLE_CLASSPATH, 
    EXPORT_PACKAGE, IMPORT_PACKAGE, REQUIRE_BUNDLE, FRAGMENT_HOST
  };
  
  private String bundleJarFileName;
  private String bundleSymbolicName;
  private Version bundleVersion;
  private String[] bundleClassPath;
  private ExportPackage[] exportPackages;
  private ImportPackage[] importPackages;
  private RequireBundle[] requireBundles;
  private String fragmentHost;
  private VersionRange fragmentHostVersionRange;
  
  public BundleDescriptor(String bundleJarFileName, Manifest manifest) throws RuntimeException {
    this.bundleJarFileName = bundleJarFileName;
    ManifestAttributeParser parser = new ManifestAttributeParser(manifest);
    Map<String, AttributeElement[]> attributes = parser.parseAttributes(ATTRIBUTE_NAMES);

    setBundleSymbolicName(attributes);
    setBundleVersion(attributes);
    setBundleClassPath(attributes);
    setImportPackages(attributes);
    setExportPackages(attributes);
    setRequiredBundles(attributes);
    setFragementHost(attributes);
  }

  private void setRequiredBundles(Map<String, AttributeElement[]> attributes) {
    AttributeElement[] elements = getOptional(REQUIRE_BUNDLE, attributes, true);
    if (elements == null) {
      requireBundles = new RequireBundle[0];
    } else {
      requireBundles = new RequireBundle[elements.length];
      for (int i = 0; i < elements.length; i++) {
        AttributeElement element = elements[i];
        String name = element.getValues().get(0);
        VersionRange versionRange;
        String[] versionString = element.getAttributes().get(BUNDLE_VERSION_ATTRIBUTE);
        if (versionString == null || versionString.length != 1) {
          versionRange = VersionRange.parseVersionRange("");
        } else {
          versionRange = VersionRange.parseVersionRange(versionString[0]);
        }
        String[] visibility = element.getDirectiveValues(VISIBILITY_DIRECTIVE);
        boolean reexport = true;
        if (visibility != null && visibility.length == 1) {
          reexport = VISIBILITY_REEXPORT.equals(visibility[0]);
        }
        String[] resolution = element.getDirectiveValues(RESOLUTION_DIRECTIVE);
        boolean mandatory = true;
        if (resolution != null && resolution.length == 1) {
          mandatory = RESOLUTION_MANDATORY.equals(resolution[0]);
        }
        requireBundles[i] = new RequireBundle(this, name, versionRange, reexport, mandatory);
      }
    }
  }

  private void setFragementHost(Map<String, AttributeElement[]> attributes) {
    AttributeElement[] elements = getOptional(FRAGMENT_HOST, attributes, false);
    if (elements == null) {
      fragmentHost = null;
      fragmentHostVersionRange = null;
    } else {
      fragmentHost = elements[0].getValues().get(0);
      String[] versionString = elements[0].getAttributeValues(BUNDLE_VERSION_ATTRIBUTE);
      if (versionString == null || versionString.length != 1) {
        fragmentHostVersionRange = VersionRange.parseVersionRange("");
      } else {
        fragmentHostVersionRange = VersionRange.parseVersionRange(versionString[0]);
      }
    }
  }
  
  @SuppressWarnings("deprecation")
  private void setExportPackages(Map<String, AttributeElement[]> attributes) throws RuntimeException {
    AttributeElement[] elements = getOptional(EXPORT_PACKAGE, attributes, true);
    if (elements == null) {
      exportPackages = new ExportPackage[0];
    } else {
      exportPackages = new ExportPackage[elements.length];
      for (int i = 0; i < elements.length; i++) {
        AttributeElement element = elements[i];
        String packageName = element.getValues().get(0);
        Version version;
        String[] versionString = element.getAttributes().get(VERSION_ATTRIBUTE);
        if (versionString == null || versionString.length != 1) {
          versionString = element.getAttributes().get(PACKAGE_SPECIFICATION_VERSION);
        }
        
        if (versionString == null || versionString.length != 1) {
          version = Version.parseVersion("");
        } else {
          version = Version.parseVersion(versionString[0]);
        }
        ExportPackage exportPackage = new ExportPackage(this, packageName, version);
        exportPackage.addDirective(Constants.INCLUDE_DIRECTIVE, element.getDirectiveValues(Constants.INCLUDE_DIRECTIVE));
        exportPackage.addDirective(Constants.EXCLUDE_DIRECTIVE, element.getDirectiveValues(Constants.EXCLUDE_DIRECTIVE));
        exportPackages[i] = exportPackage;
      }
    }
  }

  private void setImportPackages(Map<String, AttributeElement[]> attributes) throws RuntimeException {
    AttributeElement[] elements = getOptional(IMPORT_PACKAGE, attributes, true);
    if (elements == null) {
      importPackages = new ImportPackage[0];
    } else {
      importPackages = new ImportPackage[elements.length];
      for (int i = 0; i < elements.length; i++) {
        AttributeElement element = elements[i];
        String packageName = element.getValues().get(0);
        VersionRange versionRange;
        String[] versionString = element.getAttributes().get(VERSION_ATTRIBUTE);
        
        if (versionString == null || versionString.length != 1) {
          versionRange = VersionRange.parseVersionRange("");
        } else {
          versionRange = VersionRange.parseVersionRange(versionString[0]);
        }
        String[] resolution = element.getDirectiveValues(RESOLUTION_DIRECTIVE);
        boolean mandatory = true;
        if (resolution != null && resolution.length == 1) {
          mandatory = RESOLUTION_MANDATORY.equals(resolution[0]);
        }
        importPackages[i] = new ImportPackage(this, packageName, versionRange, mandatory);
      }
    }
  }

  private void setBundleClassPath(Map<String, AttributeElement[]> attributes) throws RuntimeException {
    AttributeElement[] elements = getOptional(BUNDLE_CLASSPATH, attributes, true);
    if (elements == null) {
      bundleClassPath = new String[] { "." };
    } else {
      bundleClassPath = new String[elements.length];
      for (int i = 0; i < elements.length; i++) {
        bundleClassPath[i] = elements[i].getValues().get(0);
      }
    }
  }

  private void setBundleVersion(Map<String, AttributeElement[]> attributes) throws RuntimeException {
    AttributeElement[] elements = getOptional(BUNDLE_VERSION, attributes, false);
    if (elements == null) {
      bundleVersion = Version.parseVersion("");
    } else {
      bundleVersion = Version.parseVersion(elements[0].getValues().get(0));
    }
  }

  private void setBundleSymbolicName(Map<String, AttributeElement[]> attributes) throws RuntimeException {
    AttributeElement element = getMandatory(BUNDLE_SYMBOLICNAME, attributes, false)[0];
    bundleSymbolicName = element.getValues().get(0);
  }

  private AttributeElement[] getMandatory(String name, Map<String, AttributeElement[]> attributes, boolean multiValue) throws RuntimeException {
    AttributeElement[] elements = attributes.get(name);
    if (elements == null) {
      throw new RuntimeException(name + " is missing");
    }
    if (!multiValue && elements.length != 1) {
      throw new RuntimeException(name + " has multiple values");
    }
    return elements;
  }

  private AttributeElement[] getOptional(String name, Map<String, AttributeElement[]> attributes, boolean multiValue) throws RuntimeException {
    AttributeElement[] elements = attributes.get(name);
    if (!multiValue && elements != null && elements.length > 1) {
      throw new RuntimeException(name + " has multiple values");
    }
    return elements;
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof BundleDescriptor)) {
      return false;
    }
    BundleDescriptor other = (BundleDescriptor) obj;
    return bundleSymbolicName.equals(other.bundleSymbolicName) && bundleVersion.equals(other.bundleVersion);
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bundleSymbolicName == null) ? 0 : bundleSymbolicName.hashCode());
    result = prime * result + ((bundleVersion == null) ? 0 : bundleVersion.hashCode());
    return result;
  }    

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject(); 
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

  public static class ImportPackage implements Serializable {

    private static final long serialVersionUID = -2422541393190832301L;
    
    private BundleDescriptor bundleDescriptor;
    private String name;
    private VersionRange versionRange;
    private boolean mandatory;
    private Map<String, Object> attributes = new HashMap<String, Object>(3);
    private Map<String, Object> directives = new HashMap<String, Object>(3);
    
    public ImportPackage(BundleDescriptor bundleDescriptor, String name, VersionRange versionRange, boolean mandatory) {
      this.bundleDescriptor = bundleDescriptor;
      this.name = name;
      this.versionRange = versionRange;
      this.mandatory = mandatory;
    }

    public String getName() {
      return name;
    }

    public VersionRange getVersionRange() {
      return versionRange;
    }

    public boolean isMandatory() {
      return mandatory;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public Map<String, Object> getDirectives() {
      return directives;
    }

    public BundleDescriptor getBundleDescriptor() {
      return bundleDescriptor;
    }
    
    public void addDirective(String name, Object value) {
      directives.put(name, value);
    }

    //TODO add mandatory?
    public boolean equals(Object object) {
      if (!(object instanceof ImportPackage)) {
        return false;
      }
      ImportPackage other = (ImportPackage) object;
      return name.equals(other.name) && versionRange.equals(other.versionRange);
    }
    
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((versionRange == null) ? 0 : versionRange.hashCode());
      return result;
    }    
    
  }
  
  public static class ExportPackage implements Serializable {

    private static final long serialVersionUID = 3418812598326432855L;
    
    private BundleDescriptor bundleDescriptor;
    private String name;
    private Version version;
    private String[] mandatory;
    private Map<String, Object> attributes = new HashMap<String, Object>(3);
    private Map<String, Object> directives = new HashMap<String, Object>(3);

    
    public ExportPackage(BundleDescriptor bundleDescriptor, String name, Version version) {
      this.bundleDescriptor = bundleDescriptor;
      this.name = name;
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public Version getVersion() {
      return version;
    }

    public String[] getMandatory() {
      return mandatory;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public Map<String, Object> getDirectives() {
      return directives;
    }
    
    public void addDirective(String name, Object value) {
      directives.put(name, value);
    }

    public boolean matches(ImportPackage importPackage) {
      if (name.equals(importPackage.getName()) && 
          importPackage.getVersionRange().isIncluded(version)) {
        return true;
      }
      return false;
    }

    public BundleDescriptor getBundleDescriptor() {
      return bundleDescriptor;
    }

    //TODO add attribute comparison
    public boolean equals(Object object) {
      if (!(object instanceof ExportPackage)) {
        return false;
      }
      ExportPackage other = (ExportPackage) object;
      return name.equals(other.name) && version.equals(other.version);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
    }

  }

  public static class RequireBundle implements Serializable {

    private static final long serialVersionUID = -3003813302348282931L;
    
    private BundleDescriptor bundleDescriptor;
    private String name;
    private VersionRange versionRange;
    private boolean reexport;
    private boolean mandatory;

    public RequireBundle(BundleDescriptor bundleDescriptor, String name, VersionRange versionRange, boolean reexport, boolean mandatory) {
      this.bundleDescriptor = bundleDescriptor;
      this.name = name;
      this.versionRange = versionRange;
      this.reexport = reexport;
      this.mandatory = mandatory;
    }

    public BundleDescriptor getBundleDescriptor() {
      return bundleDescriptor;
    }

    public String getName() {
      return name;
    }

    public VersionRange getVersionRange() {
      return versionRange;
    }

    public boolean isReexport() {
      return reexport;
    }

    public boolean isMandatory() {
      return mandatory;
    }
  }
  
  public String getBundleJarFileName() {
    return bundleJarFileName;
  }

  public String getBundleSymbolicName() {
    return bundleSymbolicName;
  }

  public Version getBundleVersion() {
    return bundleVersion;
  }

  public String[] getBundleClassPath() {
    return bundleClassPath;
  }

  public ExportPackage[] getExportPackages() {
    return exportPackages;
  }

  public ImportPackage[] getImportPackages() {
    return importPackages;
  }

  public RequireBundle[] getRequireBundles() {
    return requireBundles;
  }
  
  public String getFragmentHost() {
    return fragmentHost;
  }
  
  public VersionRange getFragmentHostVersionRange() {
    return fragmentHostVersionRange;
  }
    
  public String toString() {
    return bundleSymbolicName + " [" + bundleVersion + "]";
  }
}
