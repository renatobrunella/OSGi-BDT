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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import uk.co.brunella.osgi.bdt.Deployer;

public class OSGiDeploy extends AbstractOSGiTask {

  private File repository;
  private File bundle;
  private boolean verbose;
  private List<FileSet> fileSets = new ArrayList<FileSet>();
  private Set<File> deployedBundles = new HashSet<File>();

  public OSGiDeploy() {
    setTaskName("osgi-deploy");
    setDescription("Deploys a bundle to the bundle repository");
  }

  public void execute() {
    Deployer deployer = new Deployer(repository);
    deployer.setVerbose(verbose);
    
    if (bundle != null) {
      deployFile(deployer, bundle);
    }
    
    if (fileSets != null) {
      for (FileSet fileSet : fileSets) {
        DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
          File file = new File(ds.getBasedir(), includedFile);
          deployFile(deployer, file);
        }
      }
    }
  }
  
  private void deployFile(Deployer deployer, File bundle) {
    if (!deployedBundles.contains(bundle)) {
      deployedBundles.add(bundle);
      try {
        deployer.deploy(bundle);
        log(deployer.getLogMessages());
      } catch (IOException e) {
        throw new BuildException("Deployment of bundle failed: " + e.getMessage());
      } catch (Throwable t) {
        t.printStackTrace();
        throw new BuildException(t.getMessage());
      }
    }
  }

  public void setRepository(File repository) {
    this.repository = repository;
  }

  public void setBundle(File bundle) {
    this.bundle = bundle;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
  
  public void addFileSet(FileSet fileSet) {
    fileSets.add(fileSet);
  }

}
