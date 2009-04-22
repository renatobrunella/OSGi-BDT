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
package uk.co.brunella.osgi.bdt.ant.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ImportPackage;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;

public class OSGiPath extends Path {

  private File repositoryDirectory;
  private BundleDescriptor descriptor;
  private boolean failOnUnresolved = false;
  private boolean failOnUnresolvedMandatory = true;
  private boolean verbose = false;
  private int resolveLevel = 0;
  private String resolve = "package";

  public OSGiPath(Project p) {
    super(p);
    setDescription("Adds dependent packages from the bundle repository to the classpath");
  }

  public OSGiPath(Project p, String path) {
    super(p, path);
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
    if (repositoryDirectory != null) {
      try {
        resolveBundle();
      } catch (IOException e) {
        throw new BuildException(e.getMessage());
      }
    }
  }
  
  public void setRepository(File repository) {
    repositoryDirectory = repository;
    if (descriptor != null) {
      try {
        resolveBundle();
      } catch (IOException e) {
        throw new BuildException(e.getMessage());
      }
    }
  }

  private Set<ImportPackage> unresolved;
  private Set<ExportPackage> resolved;
  private Set<BundleDescriptor> resolvedBundles;
  
  private void resolveBundle() throws IOException {
    BundleRepositoryPersister persister = new BundleRepositoryPersister(repositoryDirectory);
    BundleRepository repository = persister.load();

    unresolved = new HashSet<ImportPackage>();
    resolved = new HashSet<ExportPackage>();
    resolvedBundles = new HashSet<BundleDescriptor>();

    boolean resolveToBundle = "bundle".equals(resolve);

    Set<BundleDescriptor> importedBundles = new HashSet<BundleDescriptor>();
    
    // add fragment host bundle
    if (descriptor.getFragmentHost() != null) {
      BundleDescriptor[] hosts = repository.resolveBundle(descriptor.getFragmentHost(), descriptor.getFragmentHostVersionRange(), true);
      if (hosts.length > 0) {
        BundleDescriptor fragmentHost = hosts[0];
        importedBundles.add(fragmentHost);
        // resolve the fragment host
        resolveBundle(0, fragmentHost, repository);
      } else {
        if (failOnUnresolved || failOnUnresolvedMandatory) {
          throw new BuildException("Cannot resolve fragment host bundle for " + descriptor.getFragmentHost());
        }
      }
    }

    resolveBundle(0, descriptor, repository);
    
    if (unresolved.size() > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append("Could not resolve imports:\n");
      boolean mandatoryUnresolved = false;
      for (ImportPackage importPackage : unresolved) {
        if (importPackage.isMandatory()) {
          mandatoryUnresolved = true;
          sb.append('\t').append(importPackage.getName()).append(' ').append(importPackage.getVersionRange()).append('\n');
        } else {
          sb.append('\t').append(importPackage.getName()).append(' ').append(importPackage.getVersionRange()).append(" [optional]\n");
        }
      }
      if (failOnUnresolved || (mandatoryUnresolved && failOnUnresolvedMandatory)) {
        throw new BuildException(sb.toString());
      } else {
        System.err.println(sb.toString());
      }
    }
    if (verbose) {
      System.out.println("Classpath elements:");
    }
    
    if (resolveToBundle) {
      for (ExportPackage exportPackage : resolved) {
        BundleDescriptor exportDescriptor = exportPackage.getBundleDescriptor();
        if (exportDescriptor != null) {
          importedBundles.add(exportDescriptor);
        }
      }
    } else {
      for (ExportPackage exportPackage : resolved) {
        BundleDescriptor exportDescriptor = exportPackage.getBundleDescriptor();
        // bundle descriptor is null for system packages
        if (exportDescriptor != null) {
          File path = new File(repositoryDirectory, "packages" + File.separator + 
              exportDescriptor.getBundleSymbolicName() + File.separator + exportDescriptor.getBundleVersion() +
              File.separator + exportPackage.getName() + File.separator + exportPackage.getVersion());
          if (verbose) {
            System.out.println("\t" + path.toString());
          }
          createPathElement().setPath(path.toString());
        }
      }
    }
    
    // create classpath entries for bundles
    for (BundleDescriptor descriptor : importedBundles) {
      File path = new File(repositoryDirectory, "extracted" + File.separator + 
          descriptor.getBundleSymbolicName() + "_" + descriptor.getBundleVersion());
      String basePath = path.toString() + File.separator;
      String[] classpath = descriptor.getBundleClassPath();
      for (int i = 0; i < classpath.length; i++) {
        String bundlePath = basePath + classpath[i];
        if (verbose) {
          System.out.println("\t" + bundlePath);
        }
        createPathElement().setPath(bundlePath);
      }
    }
    if (verbose) {
      System.out.println(" ");
    }
  }

  private void resolveBundle(int level, BundleDescriptor descriptor, BundleRepository repository) throws IOException {
    if (resolvedBundles.contains(descriptor)) {
      return;
    } else {
      resolvedBundles.add(descriptor);
    }
    if (verbose) {
      System.out.println("Resolving imports for bundle " + descriptor.getBundleSymbolicName() + " " + descriptor.getBundleVersion());
    }
    Set<BundleDescriptor> unresolvedBundles = new HashSet<BundleDescriptor>();
    for (ImportPackage importPackage : descriptor.getImportPackages()) {
      ExportPackage[] exportPackages = repository.resolve(importPackage.getName(), importPackage.getVersionRange(), true);
      if (exportPackages.length > 0) {
        resolved.add(exportPackages[0]); // first one has the highest version number
        // bundle descriptor for system bundles is null!!!
        if (exportPackages[0].getBundleDescriptor() != null) {
          unresolvedBundles.add(exportPackages[0].getBundleDescriptor());
        }
      } else {
        unresolved.add(importPackage);
      }
    }
    if (level < resolveLevel) {
      for (BundleDescriptor unresolvedBundle : unresolvedBundles) {
        resolveBundle(level + 1, unresolvedBundle, repository);
      }
    }
  }

  public void setFailOnUnresolved(boolean failOnUnresolved) {
    this.failOnUnresolved = failOnUnresolved;
  }

  public void setFailOnUnresolvedMandatory(boolean failOnUnresolvedMandatory) {
    this.failOnUnresolvedMandatory = failOnUnresolvedMandatory;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setResolveLevel(int resolveLevel) {
    this.resolveLevel = resolveLevel;
  }
  
  public void setResolve(String resolve) {
    if ("package".equals(resolve) || "bundle".equals(resolve)) {
      this.resolve = resolve;
    } else {
      throw new BuildException(resolve + " is not a valid option. Only package and bundle are supported");
    }
  }

}
