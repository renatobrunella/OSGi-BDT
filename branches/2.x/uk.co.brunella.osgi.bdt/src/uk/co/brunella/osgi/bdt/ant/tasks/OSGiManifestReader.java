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
package uk.co.brunella.osgi.bdt.ant.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.osgi.framework.Constants;

public class OSGiManifestReader extends AbstractOSGiTask {

  private Manifest manifest;
  private String propertyBundleName = "bundle.name";
  private String propertyBundleVersionName = "bundle.version";
  private String propertyPrefix = null;
  private boolean verbose = false;
  

  public OSGiManifestReader() {
    setTaskName("osgi-manifest");
    setDescription("Sets properties with bundle symbolic name and version and read main manifest section");
  }

  public void setBundleName(String bundleName) {
    this.propertyBundleName = bundleName;
  }

  public void setBundleVersionName(String bundleVersionName) {
    this.propertyBundleVersionName = bundleVersionName;
  }
  
  public void setPropertyPrefix(String propertyPrefix) {
    this.propertyPrefix = propertyPrefix;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setBundle(File bundleJarFile) {
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(bundleJarFile);
      manifest = jarFile.getManifest();
    } catch (IOException e) {
      throw new BuildException(e.getMessage(), e);
    } finally {
      if (jarFile != null) {
        try {
          jarFile.close();
        } catch (IOException e) {
          throw new BuildException(e.getMessage(), e);
        }
      }
    }
  }

  public void setManifest(File manifestFile) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(manifestFile);
      manifest = new Manifest(fis);
    } catch (IOException e) {
      throw new BuildException(e.getMessage(), e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          throw new BuildException(e.getMessage(), e);
        }
      }
    }
  }
  
  public void execute() {
    Attributes attributes = manifest.getMainAttributes();
    String bundleName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
    if (bundleName.indexOf(';') != -1) {
      bundleName = bundleName.substring(0, bundleName.indexOf(';'));
    }
    getProject().setNewProperty(propertyBundleName, bundleName);
    String bundleVersion = attributes.getValue(Constants.BUNDLE_VERSION);
    getProject().setNewProperty(propertyBundleVersionName, bundleVersion);
    if (verbose) {
      log("Setting " + propertyBundleName + "=" + bundleName);
      log("Setting " + propertyBundleVersionName + "=" + bundleVersion);
    }
    
    if (propertyPrefix != null) {
      log("Setting manifest entries to properties with prefix " + propertyPrefix);
      for (Entry<Object, Object> entry : attributes.entrySet()) {
        String name = ((Attributes.Name) entry.getKey()).toString();
        String value = (String) entry.getValue();
        getProject().setNewProperty(propertyPrefix + name, value);
        if (verbose) {
          log("Setting " + propertyPrefix + name + "=" + value);
        }
      }
    }
  }
}
