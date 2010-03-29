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
import java.io.IOException;

import org.apache.tools.ant.BuildException;

import uk.co.brunella.osgi.bdt.bundle.Version;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public class OSGiUndeploy extends AbstractOSGiTask {

  private File repository;
  private String bundleName;
  private String version = "";
  private String range = "";
  private boolean verbose;

  public OSGiUndeploy() {
    setTaskName("osgi-undeploy");
    setDescription("Undeploys a bundle from the bundle repository");
  }

  public void execute() {
    Deployer deployer = new Deployer(repository);
    deployer.setVerbose(verbose);
    version = version.trim();
    range = range.trim();
    if (version.length() > 0 && range.length() > 0) {
      throw new BuildException("Only version or range can be set not both");
    }
    try {
      if (version.length() > 0) {
        Version bundleVersion = Version.parseVersion(version);
        deployer.undeploy(bundleName, bundleVersion, true);
      } else {
        VersionRange bundleVersionRange = VersionRange.parseVersionRange(range);
        deployer.undeploy(bundleName, bundleVersionRange, true);
      }
      log(deployer.getLogMessages());
    } catch (IOException e) {
      throw new BuildException("Undeployment of bundle failed: " + e.getMessage());
    } catch (Throwable t) {
      throw new BuildException(t.getMessage());
    }
  }
  
  public void setRepository(File repository) {
    this.repository = repository;
  }

  public void setBundleName(String bundleName) {
    this.bundleName = bundleName;
  }

  public void setVersion(String version) {
    this.version = version;
  }
  
  public void setRange(String range) {
    this.range = range;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
}
