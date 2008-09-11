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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.types.DirSet;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.BundleRepositoryPersister;

public class OSGiBuild extends AbstractOSGiTask {

  private File repository;
  private List<DirSet> projectDirectories = new ArrayList<DirSet>();
  private String manifestFilePath = "./META-INF/MANIFEST.MF";
  private String buildFilePath = "./build.xml";
  private String buildTarget;
  private boolean fullRebuild = false;
  private boolean inheritDirty = false;
  private boolean verbose = false;
  private BundleRepository buildRepository;
  private BundleRepository deployRepository;
  private long dirtyThreshold = 2000;
  private boolean inheritAll = false;
  private boolean inheritRefs = false;

  public void execute() {
    if (repository == null || fullRebuild) {
      fullRebuild = true;
    } else {
      try {
        deployRepository = new BundleRepositoryPersister(repository).load();
      } catch (Exception e) {
        throw new BuildException(e.getMessage(), e);
      }
    }
    if (deployRepository != null) {
      buildRepository = new BundleRepository(deployRepository.getProfileName());
    } else {
      buildRepository = new BundleRepository("J2SE-1.5");
    }
    for (DirSet fileSet : projectDirectories) {
      DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
      String[] projectDirectories = ds.getIncludedDirectories();
      for (String projectDirectoryName : projectDirectories) {
        File projectDirectory = new File(ds.getBasedir(), projectDirectoryName);
        if (!projectDirectory.isDirectory()) {
          throw new BuildException(projectDirectory + " does not exist or is not a directory");
        }
        File buildFile = new File(projectDirectory, buildFilePath);
        if (!buildFile.isFile()) {
          throw new BuildException(buildFile + " does not exist or is not a file");
        }
        File manifestFile = new File(projectDirectory, manifestFilePath);
        if (!manifestFile.isFile()) {
          throw new BuildException(manifestFile + " does not exist or is not a file");
        }
        BuildBundleDescriptor descriptor = createDescriptor(projectDirectory, buildFile, manifestFile);
        buildRepository.addBundleDescriptor(descriptor);
      }
    }

    List<BuildBundleDescriptor> buildOrder = calculateBuildOrder();
    if (verbose) {
      if (buildOrder.size() > 0) {
        System.out.println("Build order:");
        for (BuildBundleDescriptor descriptor : buildOrder) {
          System.out.println(descriptor);
        }
      } else {
        System.out.println("Nothing to build");
      }
      System.out.println();
    }
    
    for (BuildBundleDescriptor descriptor : buildOrder) {
      Ant antTask = new Ant(this);
      File buildFile = descriptor.getBuildFile();
      System.out.println("Building " + buildFile);
      antTask.setAntfile(buildFile.toString());
      antTask.setDir(buildFile.getParentFile());
      if (buildTarget != null) {
        antTask.setTarget(buildTarget);
      }
      antTask.setInheritAll(inheritAll);
      antTask.setInheritRefs(inheritRefs);
      antTask.execute();
    }
    
  }

  public void setRepository(File repository) {
    this.repository = repository;
  }

  public void setFullRebuild(boolean fullRebuild) {
    this.fullRebuild = fullRebuild;
  }
  
  public void addDirSet(DirSet dirSet) {
    projectDirectories.add(dirSet);
  }
  
  public void setManifestFile(String manifestFile) {
    this.manifestFilePath = manifestFile;
  }

  public void setBuildFile(String buildFile) {
    this.buildFilePath = buildFile;
  }
  
  public void setBuildTarget(String buildTarget) {
    this.buildTarget = buildTarget;
  }

  public void setInheritDirty(boolean inheritDirty) {
    this.inheritDirty = inheritDirty;
  }
  
  public void setInheritAll(boolean inheritAll) {
    this.inheritAll = inheritAll;
  }
  
  public void setInheritRefs(boolean inheritRefs) {
    this.inheritRefs = inheritRefs;
  }

  public void setDirtyThreshold(long dirtyThreshold) {
    this.dirtyThreshold = dirtyThreshold;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  private List<BuildBundleDescriptor> calculateBuildOrder() {
    if (!fullRebuild) {
      checkAndSetDirtyFlag();
    }
    Set<BundleDescriptor> processed = new HashSet<BundleDescriptor>();
    Set<BundleDescriptor> processing = new HashSet<BundleDescriptor>();
    List<BuildBundleDescriptor> buildOrder = new ArrayList<BuildBundleDescriptor>();
    for (BundleDescriptor descriptor : buildRepository.getBundleDescriptors()) {
      addToBuildOrder((BuildBundleDescriptor) descriptor, buildOrder, processed, processing);
    }
    return buildOrder;
  }

  private boolean addToBuildOrder(BuildBundleDescriptor descriptor, List<BuildBundleDescriptor> buildOrder,
      Set<BundleDescriptor> processed, Set<BundleDescriptor> processing) {
    if (processed.contains(descriptor)) {
      return descriptor.isDirty();
    }
    if (processing.contains(descriptor)) {
      throw new BuildException("Circular reference " + processing);
    }
    boolean isDirty = descriptor.isDirty();
    processing.add(descriptor);
    Set<BundleDescriptor> dependencies = buildRepository.getBundleDependencies(descriptor);
    for (BundleDescriptor dependency : dependencies) {
      boolean dependencyIsDirty = addToBuildOrder((BuildBundleDescriptor) dependency, buildOrder, processed, processing); 
      isDirty = isDirty || dependencyIsDirty;
    }
    processing.remove(descriptor);
    processed.add(descriptor);
    if (isDirty) {
      if (inheritDirty) {
        descriptor.setDirty(true);
      }
      buildOrder.add(descriptor);
    }
    return isDirty;
  }

  private BuildBundleDescriptor createDescriptor(File projectDirectory, File buildFile, File manifestFile) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(manifestFile);
      Manifest manifest = new Manifest(fis);
      return new BuildBundleDescriptor(projectDirectory, buildFile, manifest);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e.getMessage());
        }
      }
    }
  }
  
  private void checkAndSetDirtyFlag() {
    for (BundleDescriptor descriptor : buildRepository.getBundleDescriptors()) {
      BuildBundleDescriptor buildDescriptor = (BuildBundleDescriptor) descriptor;
      // check if already deployed in deploy repository
      BundleDescriptor deployedDescriptor = deployRepository.findBundleDescriptor(buildDescriptor.getBundleSymbolicName(), buildDescriptor.getBundleVersion());
      if (deployedDescriptor != null) {
        buildDescriptor.setDirty(isDirty(
            new File(deployRepository.getLocation(), "bundles/" + deployedDescriptor.getBundleJarFileName()), 
            buildDescriptor.getProjectDirectory()));
      } else {
        buildDescriptor.setDirty(true);
      }
    }
  }
  
  private boolean isDirty(File deployedFile, File projectDirectory) {
    long deployedTime = deployedFile.lastModified() + dirtyThreshold;
    return isNewer(deployedTime, projectDirectory);
  }
  
  private boolean isNewer(long oldTime, File directory) {
    if (directory.lastModified() > oldTime) {
      return true;
    }
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        if (isNewer(oldTime, file)) {
          return true;
        }
      } else {
        if (file.lastModified() > oldTime) {
          return true;
        }
      }
    }
    
    return false;
  }

  public static class BuildBundleDescriptor extends BundleDescriptor {

    private static final long serialVersionUID = 8482828526871445532L;

    private File projectDirectory;
    private File buildFile;
    private boolean isDirty;
    
    public BuildBundleDescriptor(File projectDirectory, File buildFile, Manifest manifest) throws RuntimeException {
      super("", manifest);
      this.isDirty = true;
      this.projectDirectory = projectDirectory;
      this.buildFile = buildFile;
    }

    public File getProjectDirectory() {
      return projectDirectory;
    }
    
    public File getBuildFile() {
      return buildFile;
    }

    public void setDirty(boolean isDirty) {
      this.isDirty = isDirty;
    }

    public boolean isDirty() {
      return isDirty;
    }
    
  }
}
