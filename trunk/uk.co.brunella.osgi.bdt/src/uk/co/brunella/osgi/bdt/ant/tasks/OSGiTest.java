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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import uk.co.brunella.osgi.bdt.runner.OSGiTestRunner;
import uk.co.brunella.osgi.bdt.runner.formatter.TestResultFormatter;
import uk.co.brunella.osgi.bdt.runner.formatter.TestResultFormatterFactory;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResult;

public class OSGiTest extends AbstractOSGiTask {

  private String repository;
  private String frameworkName = "equinox";
  private BundleDescriptor systemBundle;
  private BundleDescriptor runnerBundle;
  private BundleDescriptorList requiredBundles = new BundleDescriptorList();
  private BundleDescriptorList testBundles = new BundleDescriptorList();
  private String outputFormat;
  private String outputDirectory;
  private String failurePropertyName;

  public void execute() {
    // Workaround to run ant from Eclipse. Otherwise we get an error while starting the first bundle:
    //   org.osgi.framework.BundleException: The bundle could not be resolved. 
    //   Reason: Missing Permission: (org.osgi.framework.PackagePermission org.osgi.framework export,import), 
    //   Missing Constraint: Import-Package: org.osgi.framework; version="1.3.0"
    System.setSecurityManager(null);
    
    try {
      OSGiTestRunner runner = new OSGiTestRunner();
      runner.setFramework(frameworkName);
      runner.setRepositoryDirectory(repository);
      if (systemBundle != null) {
        runner.setSystemBundle(systemBundle.getName(), systemBundle.getVersion());
      }
      if (runnerBundle != null) {
        runner.setTestRunnerBundle(runnerBundle.getName(), runnerBundle.getVersion());
      }
      for (BundleDescriptor bundle : requiredBundles.getBundleDescriptors()) {
        runner.addRequiredBundle(bundle.getName(), bundle.getVersion());
      }
      for (BundleDescriptor bundle : testBundles.getBundleDescriptors()) {
        runner.addTestBundle(bundle.getName(), bundle.getVersion());
      }
      try {
        List<OSGiTestResult> results;
        PrintStream oldOutStream = System.out;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream newOutStream = new PrintStream(outStream);
        PrintStream oldErrStream = System.err;
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream newErrStream = new PrintStream(errStream);
        String out;
        String err;
        try {
          System.setErr(newErrStream);
          results = runner.runTests();
        } finally {
          out = outStream.toString();
          err = errStream.toString();
          newOutStream.close();
          System.setOut(oldOutStream);
          newErrStream.close();
          System.setErr(oldErrStream);
        }
        boolean failed = false;
        for (OSGiTestResult result : results) {
          if (!result.hasPassed() && !result.isInfo()) {
            failed = true;
          }
          if (result.isInfo() || result.hasPassed()) {
            System.out.println(result);
          } else {
            System.err.println(result);
          }
        }
        
        // set the failure property
        if (failed && failurePropertyName != null) {
          getProject().setNewProperty(failurePropertyName, "true");
        }
  
        createReport(results, out, err);
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void createReport(List<OSGiTestResult> results, String out, String err) {
    if (outputFormat != null) {
      TestResultFormatter formatter = TestResultFormatterFactory.createTestResultFormatter(outputFormat);
      formatter.setOutputDirectory(outputDirectory);
      formatter.formatTestResults(results, out, err);
    }
  }
  
  public void setRepository(String repository) {
    this.repository = repository;
  }
  
  public void setFramework(String frameworkName) {
    this.frameworkName = frameworkName;
  }

  public BundleDescriptor createSystemBundle() {
    systemBundle = new BundleDescriptor(); 
    return systemBundle;
  }
  
  public BundleDescriptor createRunnerBundle() {
    runnerBundle = new BundleDescriptor(); 
    return runnerBundle;
  }

  public BundleDescriptorList createRequired() {
    requiredBundles = new BundleDescriptorList();
    return requiredBundles;
  }
  
  public BundleDescriptorList createTests() {
    testBundles = new BundleDescriptorList();
    return testBundles;
  }
  
  public static class BundleDescriptor {

    private String name;
    private String version;
    
    public String getName() {
      return name;
    }
    
    public String getVersion() {
      return version;
    }
    
    public void setName(String name) {
      this.name = name;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }
  
  public static class BundleDescriptorList {
    
    private List<BundleDescriptor> bundleDescriptors = new ArrayList<BundleDescriptor>();
    
    public BundleDescriptor createBundle() {
      BundleDescriptor desciptor = new BundleDescriptor();
      bundleDescriptors.add(desciptor);
      return desciptor;
    }
    
    public List<BundleDescriptor> getBundleDescriptors() {
      return bundleDescriptors;
    }
  }
  
  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }
  
  public void setOutputDir(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }
  
  public void setFailureProperty(String name) {
    this.failurePropertyName = name;
  }
}
