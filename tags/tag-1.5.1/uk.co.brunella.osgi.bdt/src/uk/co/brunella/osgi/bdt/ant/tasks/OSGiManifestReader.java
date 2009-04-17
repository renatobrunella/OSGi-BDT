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
package uk.co.brunella.osgi.bdt.ant.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;

public class OSGiManifestReader extends AbstractOSGiTask {

  private BundleDescriptor descriptor;
  private String bundleName = "bundle.name";
  private String bundleVersionName = "bundle.version";

  public OSGiManifestReader() {
    setTaskName("osgi-manifest");
    setDescription("Sets properties with bundle symbolic name and version");
  }

  public void setBundleName(String bundleName) {
    this.bundleName = bundleName;
  }

  public void setBundleVersionName(String bundleVersionName) {
    this.bundleVersionName = bundleVersionName;
  }

  public void setManifest(File manifestFile) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(manifestFile);
      Manifest manifest = new Manifest(fis);
      descriptor = new BundleDescriptor("", manifest);
    } catch (IOException e) {
      e.printStackTrace();
      throw new BuildException(e.getMessage());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          throw new BuildException(e.getMessage());
        }
      }
    }
  }
  
  public void execute() {
    String name = descriptor.getBundleSymbolicName();
    log("Setting bundle name: " + bundleName + "=" + name);
    getProject().setNewProperty(bundleName, name);
    String version = descriptor.getBundleVersion().toString();
    log("Setting bundle version: " + bundleVersionName + "=" + version);
    getProject().setNewProperty(bundleVersionName, version);
  }
}
