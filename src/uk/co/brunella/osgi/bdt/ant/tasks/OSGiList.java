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
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import uk.co.brunella.osgi.bdt.Deployer;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ImportPackage;

public class OSGiList extends Task {

  private File repository;
  private boolean verbose = false;

  public OSGiList() {
    setTaskName("osgi-list");
    setDescription("Lists all bundles in the bundle repository");
  }

  public void execute() {
    if (verbose) {
      System.out.println("Listing repository in " + repository);
    }
    Deployer deployer = new Deployer(repository);
    try {
      List<BundleDescriptor> bundles = deployer.list();
      for (BundleDescriptor descriptor : bundles) {
        System.out.println(descriptor.getBundleSymbolicName() + " " + descriptor.getBundleVersion() + 
            " [" + descriptor.getBundleJarFileName() + "]");
        if (verbose) {
          System.out.println("Export-Package:");
          ExportPackage[] exportPackages = descriptor.getExportPackages();
          for (ExportPackage exportPackage : exportPackages) {
            System.out.println("\t" + exportPackage.getName() + " " + exportPackage.getVersion());
          }
          System.out.println("Import-Package:");
          ImportPackage[] importPackages = descriptor.getImportPackages();
          for (ImportPackage importPackage : importPackages) {
            System.out.println("\t" + importPackage.getName() + " " + importPackage.getVersionRange());
          }
          System.out.println();
        }
      }
    } catch (IOException e) {
      throw new BuildException("Listing of repository failed: " + e.getMessage());
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
}
