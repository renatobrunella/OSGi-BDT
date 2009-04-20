/*
 * Copyright 2008 - 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt;

import java.io.File;
import java.io.IOException;
import java.util.List;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.Version;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public class Main {

  public static void main(String[] args) throws IOException {
    
    if (args.length < 1) {
      printHelp();
      System.exit(-1);
    }
    String command = args[0];
    if ("-help".equals(command)) {
      printHelp();
    } else if ("-create".equals(command)) {
      if (args.length == 3) {
        if (!create(args[1], args[2])) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else if ("-deploy".equals(command)) {
      if (args.length == 3) {
        if (!deploy(args[1], args[2])) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else if ("-undeploy".equals(command)) {
      if (args.length == 4) {
        if (!undeploy(args[1], args[2], args[3])) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else if ("-list".equals(command)) {
      if (args.length == 2) {
        if (!list(args[1])) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else if ("-listprofiles".equals(command)) {
      if (args.length == 1) {
        if (!listProfiles()) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else if ("-resolve".equals(command)) {
      if (args.length == 4) {
        if (!resolve(args[1], args[2], args[3])) {
          System.exit(-1);
        }
      } else {
        printHelp();
      }
    } else {
      printHelp();
      System.exit(-1);
    }
  }

  private static boolean deploy(String bundle, String repositoryDir) throws IOException {
    File bundleFile = new File(bundle);
    if (!bundleFile.exists()) {
      System.out.println("Cannot find bundle " + bundleFile);
      return false;
    }
    File repositoryDirectory = new File(repositoryDir);
    if (!checkRepository(repositoryDirectory)) {
      return false;
    }
    Deployer deployer = new Deployer(repositoryDirectory);
    deployer.deploy(bundleFile);
    System.out.println("Deployed bundle " + bundleFile + " to repository " + repositoryDirectory);
    return true;
  }

  private static boolean undeploy(String bundleSymbolicName, String bundleVersion, String repositoryDir) throws IOException {
    File repositoryDirectory = new File(repositoryDir);
    if (!checkRepository(repositoryDirectory)) {
      return false;
    }
    Deployer deployer = new Deployer(repositoryDirectory);
    Version version = Version.parseVersion(bundleVersion);
    deployer.undeploy(bundleSymbolicName, version);
    System.out.println("Undeployed bundle " + bundleSymbolicName + " version " + version.toString() + 
        " from repository " + repositoryDirectory);
    return true;
  }

  private static boolean checkRepository(File repositoryDirectory) {
    return new BundleRepositoryPersister(repositoryDirectory).checkRepository();
  }

  private static boolean create(String repositoryDir, String profile) throws IOException {
    File repositoryDirectory = new File(repositoryDir);
    Deployer deployer = new Deployer(repositoryDirectory);
    deployer.create(profile);
    return true;
  }

  private static boolean list(String repositoryDir) throws IOException {
    File repositoryDirectory = new File(repositoryDir);
    if (!checkRepository(repositoryDirectory)) {
      return false;
    }
    Deployer deployer = new Deployer(repositoryDirectory);
    List<BundleDescriptor> bundles = deployer.list();
    
    for (BundleDescriptor descriptor : bundles) {
      System.out.println(descriptor.getBundleSymbolicName() + " " + descriptor.getBundleVersion() + 
          " [" + descriptor.getBundleJarFileName() + "]" );
    }
    return true;
  }

  private static boolean listProfiles() throws IOException {
    for (String profileName : BundleRepository.getProfileNameList()) {
      System.out.println(profileName);
    }
    return true;
  }

  private static boolean resolve(String packageName, String range, String repositoryDir) throws IOException {
    File repositoryDirectory = new File(repositoryDir);
    if (!checkRepository(repositoryDirectory)) {
      return false;
    }
    VersionRange versionRange = VersionRange.parseVersionRange(range);
    BundleRepositoryPersister persister = new BundleRepositoryPersister(repositoryDirectory);
    BundleRepository repository = persister.load();
    
    ExportPackage[] exportPackages = repository.resolve(packageName, versionRange, true);
    if (exportPackages.length == 0) {
      System.out.println("No match found");
    } else {
      for (ExportPackage exportPackage : exportPackages) {
        BundleDescriptor descriptor = exportPackage.getBundleDescriptor();
        System.out.println(exportPackage.getName() + " " + exportPackage.getVersion() + 
            " [" + descriptor.getBundleSymbolicName() + " " + descriptor.getBundleVersion() + "]");
      }
    }
    
    return true;
  }

  private static void printHelp() {
    System.out.println("OSGiBDT:");
    System.out.println("\t-help");
    System.out.println("\t-create repository profilename");
    System.out.println("\t-deploy bundle repository");
    System.out.println("\t-undeploy bundlename bundleversion repository");
    System.out.println("\t-list repository");
    System.out.println("\t-listprofiles");
    System.out.println("\t-resolve packagename versionrange repository");
  }

}
