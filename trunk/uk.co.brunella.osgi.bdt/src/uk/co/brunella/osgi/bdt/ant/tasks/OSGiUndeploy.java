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

import uk.co.brunella.osgi.bdt.Deployer;
import uk.co.brunella.osgi.bdt.bundle.Version;

public class OSGiUndeploy extends AbstractOSGiTask {

  private File repository;
  private String bundleName;
  private String version = "";
  private boolean verbose;

  public OSGiUndeploy() {
    setTaskName("osgi-undeploy");
    setDescription("Undeploys a bundle from the bundle repository");
  }

  public void execute() {
    Deployer deployer = new Deployer(repository);
    deployer.setVerbose(verbose);
    try {
      Version bundleVersion = version == null ? null : Version.parseVersion(version);
      deployer.undeploy(bundleName, bundleVersion);
      log(deployer.getLogMessages());
    } catch (IOException e) {
      throw new BuildException("Undeployment of bundle failed: " + e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
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
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
}
