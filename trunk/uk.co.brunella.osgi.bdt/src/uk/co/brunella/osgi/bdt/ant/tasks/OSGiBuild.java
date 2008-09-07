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

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;

public class OSGiBuild extends AbstractOSGiTask {

  public static void main(String[] args) {
  
    BundleRepository repository = new BundleRepository("J2SE-1.5");
    
    repository.addBundleDescriptor(createDescriptor(new File("...\\META-INF\\MANIFEST.MF"), true));
  
    List<BuildBundleDescriptor> buildOrder = calculateBuildOrder(repository);
    for (BuildBundleDescriptor descriptor : buildOrder) {
      System.out.println(descriptor + " " + (descriptor.isDirty));
    }
  }

  private static List<BuildBundleDescriptor> calculateBuildOrder(BundleRepository repository) {
    Set<BundleDescriptor> processed = new HashSet<BundleDescriptor>();
    Set<BundleDescriptor> processing = new HashSet<BundleDescriptor>();
    List<BuildBundleDescriptor> buildOrder = new ArrayList<BuildBundleDescriptor>();
    for (BundleDescriptor descriptor : repository.getBundleDescriptors()) {
      addToBuildOrder(repository, (BuildBundleDescriptor) descriptor, buildOrder, processed, processing);
    }
    return buildOrder;
  }

  private static boolean addToBuildOrder(BundleRepository repository, BuildBundleDescriptor descriptor, List<BuildBundleDescriptor> buildOrder,
      Set<BundleDescriptor> processed, Set<BundleDescriptor> processing) {
    if (processed.contains(descriptor)) {
      return false;
    }
    if (processing.contains(descriptor)) {
      throw new BuildException("Circular reference " + processing);
    }
    boolean isDirty = descriptor.isDirty();
    processing.add(descriptor);
    Set<BundleDescriptor> dependencies = repository.getBundleDependencies(descriptor);
    for (BundleDescriptor dependency : dependencies) {
      isDirty = isDirty || addToBuildOrder(repository, (BuildBundleDescriptor) dependency, buildOrder, processed, processing);
    }
    processing.remove(descriptor);
    processed.add(descriptor);
    if (isDirty) {
      buildOrder.add(descriptor);
    }
    return isDirty;
  }

  private static BuildBundleDescriptor createDescriptor(File manifestFile, boolean isDirty) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(manifestFile);
      Manifest manifest = new Manifest(fis);
      return new BuildBundleDescriptor(manifest, isDirty);
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
  
  
  public static class BuildBundleDescriptor extends BundleDescriptor {

    private static final long serialVersionUID = 8482828526871445532L;

    private boolean isDirty;
    
    public BuildBundleDescriptor(Manifest manifest, boolean isDirty) throws RuntimeException {
      super("", manifest);
      this.isDirty = isDirty;
    }

    public boolean isDirty() {
      return isDirty;
    }
    
  }

}
