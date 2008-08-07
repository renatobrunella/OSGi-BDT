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
package uk.co.brunella.osgi.bdt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.osgi.framework.Constants;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.bundle.Version;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.util.FileUtils;

public class Deployer {

  private static final int MAX_WAIT_TIME_IN_MILLIS = 10000;
  private static final String CLASS_FILE_EXTENSION = ".class";
  private static final String JAR_FILE_EXTENSION = ".jar";

  private File repositoryDirectory;
  private File bundleDirectory;
  private File bundleExtractedDirectory;
  private File packageDirectory;
  private File tempDirectory;
  private BundleRepositoryPersister persister;
  private List<String> messages;
  private boolean verbose;

  public Deployer(File repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
    bundleDirectory = new File(repositoryDirectory, "bundles");
    bundleExtractedDirectory = new File(repositoryDirectory, "extracted");
    packageDirectory = new File(repositoryDirectory, "packages");
    tempDirectory = new File(repositoryDirectory, "temp");
    persister = new BundleRepositoryPersister(repositoryDirectory);
    messages = new ArrayList<String>();
    verbose = false;
  }

  public boolean deploy(File sourceBundleFile) throws IOException {
    logClear();
    log("Deploying bundle " + sourceBundleFile);
    // try to lock the repository
    if (persister.lock(MAX_WAIT_TIME_IN_MILLIS)) {
      try {
        // copy the bundle to the bundles directory
        File bundleFile = copyBundle(sourceBundleFile);
        // get the bundle descriptor
        JarFile bundleJarFile = new JarFile(bundleFile);
        BundleDescriptor descriptor = new BundleDescriptor(bundleFile.getName(), bundleJarFile.getManifest());
        bundleJarFile.close();
        // unpack the bundle to extracted directory
        File extractDirectory = new File(bundleExtractedDirectory, descriptor.getBundleSymbolicName() + "_" + descriptor.getBundleVersion());
        FileUtils.deleteDir(extractDirectory);
        extractBundle(bundleFile, extractDirectory, false);
        // unpack the bundle to extracted directory
        File tempExtractDirectory = new File(tempDirectory, bundleFile.getName());
        extractBundle(bundleFile, tempExtractDirectory, true);
        // process all export package directives
        processExportedPackages(tempExtractDirectory, descriptor);
        // cleanup
        FileUtils.deleteDir(tempExtractDirectory);
        // update the repository
        updateRepository(descriptor);
      } finally {
        persister.unlock();
      }
      return true;
    } else {
      logError("Cannot lock the repository");
      return false;
    }
  }

  public boolean undeploy(String bundleSymbolicName, Version bundleVersion) throws IOException {
    logClear();
    log("Undeploying bundle " + bundleSymbolicName + " [" + bundleVersion + "]");
    // try to lock the repository
    if (persister.lock(MAX_WAIT_TIME_IN_MILLIS)) {
      try {
        BundleRepository repository = persister.load();
        BundleDescriptor descriptor = repository.removeBundleDescriptor(bundleSymbolicName, bundleVersion);
        if (descriptor != null) {
          persister.save(repository);
          File bundleJar = new File(bundleDirectory, descriptor.getBundleJarFileName());
          bundleJar.delete();
          File packageDir = new File(packageDirectory, descriptor.getBundleSymbolicName() + File.separator
              + descriptor.getBundleVersion());
          FileUtils.deleteDir(packageDir);
          File[] children = packageDir.getParentFile().listFiles();
          if (children == null || children.length == 0) {
            FileUtils.deleteDir(packageDir.getParentFile());
          }
        } else {
          logError("No bundle found");
        }
      } finally {
        persister.unlock();
      }
      return true;
    } else {
      logError("Cannot lock the repository");
      return false;
    }
  }

  public boolean create(String profile) throws IOException {
    logClear();
    log("Creating new repository in " + repositoryDirectory);
    if (persister.isLocked()) {
      logError("Repository is in use");
      return false;
    } else {
      if (repositoryDirectory.exists()) {
        logInfo("Deleting existing repository");
        FileUtils.deleteDir(repositoryDirectory);
      }
      logInfo("Creating new repository in " + repositoryDirectory);
      repositoryDirectory.mkdirs();
      logInfo("Creating new repository file");
      persister.save(new BundleRepository(profile));
      logInfo("Creating subdirectories");
      bundleDirectory.mkdirs();
      packageDirectory.mkdirs();
      tempDirectory.mkdirs();
      bundleExtractedDirectory.mkdirs();
      logInfo("New repository created at " + repositoryDirectory);
      return true;
    }
  }

  public List<BundleDescriptor> list() throws IOException {
    logClear();
    log("Listing bundle directory");
    BundleRepository repository = persister.load();
    return repository.getBundleDescriptors();
  }

  public List<String> getLogMessages() {
    return messages;
  }
  
  private void logClear() {
    messages.clear();
  }
  
  private void log(String message) {
    messages.add("[LOG] " + message);
  }
  
  private void logInfo(String message) {
    if (verbose) {
      messages.add("[INFO] " + message);
    }
  }
  
  private void logError(String message) {
    messages.add("[ERROR] " + message);
  }
  
  private File copyBundle(File sourceBundleFile) throws IOException {
    File bundleFile = new File(bundleDirectory, sourceBundleFile.getName());
    FileUtils.copyFile(sourceBundleFile, bundleFile);
    return bundleFile;
  }

  private void extractBundle(File bundleFile, File directory, boolean extractEmbeddedJars) throws IOException {
    FileUtils.deleteDir(directory);
    directory.mkdir();
    JarFile bundleJarFile = new JarFile(bundleFile);
    FileUtils.extractJar(bundleJarFile, directory, extractEmbeddedJars);
    bundleJarFile.close();
  }

  private void processExportedPackages(File extractedDirectory, BundleDescriptor descriptor) throws IOException {
    String[] bundleClassPaths = descriptor.getBundleClassPath();
    Set<File> baseClassPaths = new HashSet<File>();
    for (int i = 0; i < bundleClassPaths.length; i++) {
      String path = bundleClassPaths[i];
      if (path.toLowerCase().endsWith(JAR_FILE_EXTENSION)) {
        //TODO change to path.lastIndexOf('\\')
        int j = path.length() - 1;
        while (j >= 0) {
          if (path.charAt(j) == '\\' || path.charAt(j) == '/') {
            path = path.substring(0, j);
            break;
          }
          j--;
        }
        if (j < 0) {
          path = ".";
        }
      }
      File basePath = new File(extractedDirectory, path + File.separator);
      baseClassPaths.add(basePath);
    }

    File packageDirectory = new File(this.packageDirectory, descriptor.getBundleSymbolicName() + File.separator
        + descriptor.getBundleVersion().toString());
    FileUtils.deleteDir(packageDirectory);
    packageDirectory.mkdirs();

    ExportPackage[] exportedPackages = descriptor.getExportPackages();
    for (ExportPackage exportPackage : exportedPackages) {
      processExportedPackage(baseClassPaths, exportPackage, packageDirectory);
    }
  }

  private void processExportedPackage(Set<File> classPaths, ExportPackage exportPackage, File packageDirectory)
      throws IOException {
    ExportClassFilter classFilter = new ExportClassFilter(exportPackage); 
    String packageName = exportPackage.getName();
    Version packageVersion = exportPackage.getVersion();
    File directory = new File(packageDirectory, packageName + File.separator + packageVersion.toString());
    directory.mkdirs();
    String namePath = packageName.replace('.', File.separatorChar);
    for (File basePath : classPaths) {
      File packagePath = new File(basePath, namePath);
      File[] children = packagePath.listFiles();
      File destPath = new File(directory, namePath);
      destPath.mkdirs();
      if (children != null) {
        for (File child : children) {
          if (child.isFile() && child.getName().endsWith(CLASS_FILE_EXTENSION)) {
            if (classFilter.isClassFileExported(child.getName())) {
              File dest = new File(destPath, child.getName());
              FileUtils.copyFile(child, dest);
            }
          }
        }
      }
    }
  }

  private static class ExportClassFilter {
    
    private String[] includes = null;
    private String[] excludes = null;
    
    public ExportClassFilter(ExportPackage exportPackage) {
      List<String> includeList = new ArrayList<String>(5);
      List<String> excludeList = new ArrayList<String>(5);
      Map<String, Object> directives = exportPackage.getDirectives();
      if (directives != null) {
        addClassesToList(includeList, (String[]) directives.get(Constants.INCLUDE_DIRECTIVE));
        addClassesToList(excludeList, (String[]) directives.get(Constants.EXCLUDE_DIRECTIVE));
      }
      includes = (String[]) includeList.toArray(new String[includeList.size()]);
      excludes = (String[]) excludeList.toArray(new String[excludeList.size()]);
    }

    private void addClassesToList(List<String> list, String[] directives) {
      if (directives != null) {
        for (String directive : directives) {
          String[] classes = directive.split(",");
          for (String cls : classes) {
            list.add(cls);
          }
        }
      }
    }
    
    public boolean isClassFileExported(String name) {
      if (includes.length == 0 && excludes.length == 0) {
        return true;
      } else {
        // strip .class
        if (name.endsWith(CLASS_FILE_EXTENSION)) {
          name = name.substring(0, name.length() - CLASS_FILE_EXTENSION.length());
        }
        // strip inner classes
        //TODO is this correct?
        int dollar = name.indexOf('$');
        if (dollar >= 0) {
          name = name.substring(0, dollar);
        }
        boolean included;
        if (includes.length > 0) {
          included = matches(includes, name);
        } else {
          included = true;
        }
        if (excludes.length > 0) {
          included = included && !matches(excludes, name);
        }
        return included;
      }
    }
    
    private boolean matches(String[] classFilters, String name) {
      for (String filter : classFilters) {
        int len = filter.length();
        if (len == 0) {
          continue;
        }
        if (len == 1 && filter.charAt(0) == '*') {
          return true;
        } else if (filter.charAt(len - 1) == '*') {
          if (name.startsWith(filter.substring(0, len - 1))) {
            return true;
          }
        } else if (filter.charAt(0) == '*') {
          if (name.endsWith(filter.substring(1, len))) {
            return true;
          }
        } else if (filter.equals(name)) {
          return true;
        }
      }
      return false;
    }
  }
  
  private void updateRepository(BundleDescriptor descriptor) throws IOException {
    BundleRepository repository = persister.load();
    repository.removeBundleDescriptor(descriptor.getBundleSymbolicName(), descriptor.getBundleVersion());
    repository.addBundleDescriptor(descriptor);
    persister.save(repository);
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
}
