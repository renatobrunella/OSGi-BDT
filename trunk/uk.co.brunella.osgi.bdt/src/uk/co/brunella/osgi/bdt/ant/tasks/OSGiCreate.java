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
import java.io.IOException;

import org.apache.tools.ant.BuildException;

import uk.co.brunella.osgi.bdt.repository.Deployer;
import uk.co.brunella.osgi.bdt.repository.profile.Profile;

public class OSGiCreate extends AbstractOSGiTask {

  private File repository;
  private boolean verbose = false;
  private String profileName = "OSGi/Minimum-1.0";

  public OSGiCreate() {
    setTaskName("osgi-create");
    setDescription("Creates a new bundle repository");
  }
  
  public void execute() {
    Deployer deployer = new Deployer(repository);
    deployer.setVerbose(verbose);
    try {
      deployer.create(profileName);
      log(deployer.getLogMessages());
    } catch (IOException e) {
      throw new BuildException("Creation of repository failed: " + e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
      throw new BuildException(t.getMessage());
    }
  }
  
  public void setRepository(File repository) {
    this.repository = repository;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setProfileName(String profileName) {
    if (!Profile.isValidProfileName(profileName)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Invalid profile name: ").append(profileName).append('\n');
      sb.append("Allowed are:\n");
      for (String name : Profile.getProfileNameList()) {
        sb.append("  ").append(name).append("\n");
      }
      throw new BuildException(sb.toString());
    }
    this.profileName = profileName;
  }
}
