/*
 * Copyright 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.junit.runner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runner.notification.RunNotifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.framework.BundleContextWrapper;
import uk.co.brunella.osgi.bdt.framework.EquinoxFrameworkStarter;
import uk.co.brunella.osgi.bdt.framework.OSGiFrameworkStarter;
import uk.co.brunella.osgi.bdt.junit.annotation.Include;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBundleContext;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiService;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiTest;
import uk.co.brunella.osgi.bdt.junit.annotation.StartPolicy;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkField;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkMethod;
import uk.co.brunella.osgi.bdt.junit.runner.model.TestClass;
import uk.co.brunella.osgi.bdt.junit.runner.model.TestRunNotifier;
import uk.co.brunella.osgi.bdt.junit.runner.statement.ExpectExceptionStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.FailOnTimeoutStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.FailStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InjectBundleContextStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InjectServicesStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InvokeMethodStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.ReflectiveCallable;
import uk.co.brunella.osgi.bdt.junit.runner.statement.RunAftersStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.RunBeforesStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.Statement;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public class OSGiBDTJUnitRunner extends Runner {

  public static final String OSGI_BDT_RUNNER_BUNDLE_NAME = "uk.co.brunella.osgi.bdt.junit.runner";
  
  private String testClassName;
  private TestClass testClass;
  private TestClass osgiTestClass;
  private Bundle osgiTestBundle;
  private Description testClassDescription;
  private OSGiTest testClassAnnotation;
  private OSGiFrameworkStarter frameworkStarter;
  private BundleRepository repository;
  private File testBundleFile;

  public OSGiBDTJUnitRunner(Class<?> testClass) throws InitializationError {
    validate(testClass);
    testClassName = testClass.getName();
    testClassAnnotation = testClass.getAnnotation(OSGiTest.class);
    this.testClass = new TestClass(testClass);
    repository = loadRepository(testClassAnnotation.repository());
    createDescription(this.testClass);
    testBundleFile = createJar(repository, testClassAnnotation);
  }

  private void validate(Class<?> testClass) throws InitializationError {
    if (!testClass.isAnnotationPresent(OSGiTest.class)) {
      throw new InitializationError("OSGiTest annotation is missing");
    }
  }
  
  private void createDescription(TestClass testClass) {
    testClassDescription = Description.createSuiteDescription(testClass.getJavaClass());
    for (FrameworkMethod method : testClass.getAnnotatedMethods(Test.class.getName())) {
      testClassDescription.addChild(Description.createTestDescription(testClass.getJavaClass(), method.getName(), method.getAnnotations()));
    }
  }

  @Override
  public Description getDescription() {
    return testClassDescription;
  }

  @Override
  public void run(RunNotifier notifier) {
    frameworkStarter = createFrameworkStarter();
    List<FrameworkMethod> testMethods = computeTestMethods(testClass);
    runTests(notifier, testMethods);
  }

  private EquinoxFrameworkStarter createFrameworkStarter() {
    EquinoxFrameworkStarter frameworkStarter = new EquinoxFrameworkStarter();
    return frameworkStarter;
  }
  
  private void startFramework() {
    System.out.println("startFramework");
    try {
      String systemBundleLocation = findBundle(testClassAnnotation.systemBundle()); 
      String[] frameworkParameters = new String[] { "-clean" };
      BundleContext systemBundleContext = new BundleContextWrapper(frameworkStarter.startFramework(new URL(systemBundleLocation), frameworkParameters));

      installAndStartBundle(systemBundleContext, OSGI_BDT_RUNNER_BUNDLE_NAME);
      
      for (String bundleName : testClassAnnotation.requiredBundles()) {
        installAndStartBundle(systemBundleContext, bundleName);
      }
      
      osgiTestBundle = systemBundleContext.installBundle("file:/" + testBundleFile.toString());
      osgiTestBundle.start();
    } catch (Exception e) { 
      throw new RuntimeException(e);
    }
    
    try {
      osgiTestClass = new TestClass(osgiTestBundle.loadClass(testClassName));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  private Bundle installAndStartBundle(BundleContext systemBundleContext, String bundleName) throws BundleException {
    Bundle bundle = systemBundleContext.installBundle(findBundle(bundleName));
    bundle.start();
    return bundle;
  }

  private String findBundle(String bundleName) {
    String name;
    VersionRange versionRange;
    if (bundleName.contains(";version=")) {
      name = bundleName.substring(0, bundleName.indexOf(';'));
      versionRange = VersionRange.parseVersionRange(bundleName.substring(bundleName.indexOf(';') + ";version=".length()));
    } else {
      name = bundleName;
      versionRange = VersionRange.parseVersionRange("");
    }
    BundleDescriptor[] descriptors = repository.resolveBundle(name, versionRange, true);
    if (descriptors.length > 0) {
      File bundleFiles = new File(repository.getLocation(), Deployer.BUNDLES_DIRECTORY);
      File bundleFile = new File(bundleFiles, descriptors[0].getBundleJarFileName());
      return "file:/" + bundleFile;
    } else {
      throw new RuntimeException("Cannot find bundle " + bundleName);
    }
  }

  private BundleRepository loadRepository(String repositoryLocation) throws InitializationError {
    File repositoryDirectory;
    if (repositoryLocation.startsWith("${") && repositoryLocation.endsWith("}")) {
      String location = System.getenv(repositoryLocation.substring(2, repositoryLocation.length() - 1));
      if (location == null) {
        throw new InitializationError("Repository environment variable " + repositoryLocation + " is not defined");
      }
      repositoryDirectory = new File(location);
    } else {
      repositoryDirectory = new File(repositoryLocation);
    }
    if (!repositoryDirectory.exists() || !repositoryDirectory.isDirectory()) {
      throw new InitializationError("Repository " + repositoryDirectory + " does not exist or is not a directory");
    }
    BundleRepositoryPersister persister = new BundleRepositoryPersister(repositoryDirectory);
    try {
      return persister.load();
    } catch (IOException e) {
      List<Throwable> list = new ArrayList<Throwable>(1);
      list.add(e);
      throw new InitializationError(list);
    }
  }

  private void stopFramework() {
    System.out.println("stopFramework");
    try {
      frameworkStarter.stopFramework();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runTests(RunNotifier notifier, List<FrameworkMethod> testMethods) {
    if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST_CLASS) {
      startFramework();
      runBeforeClass(notifier);
    }
    for (FrameworkMethod testMethod : testMethods) {
      if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST) {
        startFramework();
      }

      runTest(notifier, testMethod);

      if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST) {
        stopFramework();
      }
    }
    if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST_CLASS) {
      runAfterClass(notifier);
      stopFramework();
    }
  }

  private void runTest(RunNotifier notifier, FrameworkMethod testMethod) {
    TestRunNotifier testNotifier = new TestRunNotifier(notifier, methodDescription(testClass, testMethod));
    if (testMethod.getAnnotation(Ignore.class.getName()) != null) {
      testNotifier.fireTestIgnored();
    } else {
      testNotifier.fireTestStarted();
      try {
        FrameworkMethod osgiTestMethod = osgiTestClass.getTestMethod(testMethod.getName());
        makeTestStatement(osgiTestMethod).evaluate();
      } catch (AssumptionViolatedException e) {
        testNotifier.addFailedAssumption(e);
      } catch (Throwable e) {
        testNotifier.addFailure(e);
      } finally {
        testNotifier.fireTestFinished();
      }

    }
  }

  private void runBeforeClass(RunNotifier notifier) {
    for (FrameworkMethod method : osgiTestClass.getAnnotatedMethods(BeforeClass.class.getName())) {
      try {
        method.invokeExplosively(osgiTestClass);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
  
  private void runAfterClass(RunNotifier notifier) {
    for (FrameworkMethod method : osgiTestClass.getAnnotatedMethods(AfterClass.class.getName())) {
      try {
        method.invokeExplosively(osgiTestClass);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  private Statement makeTestStatement(FrameworkMethod testMethod) {
    Object test;
    try {
      test = new ReflectiveCallable() {
        @Override
        protected Object runReflectiveCall() throws Throwable {
          return osgiTestClass.getOnlyConstructor().newInstance();
        }
      }.run();
    } catch (Throwable e) {
      return new FailStatement(e);
    }

    Statement statement = methodInvoker(testMethod, test);
    statement = possiblyExpectingExceptions(testMethod, test, statement);
    statement = withPotentialTimeout(testMethod, test, statement);
    statement = withBefores(testMethod, test, statement);
    statement = withAfters(testMethod, test, statement);
    statement = withServiceInjection(testMethod, test, statement);
    statement = withBundleContextInjection(testMethod, test, statement);
    return statement;
  }

  private Statement methodInvoker(FrameworkMethod testMethod, Object test) {
    return new InvokeMethodStatement(testMethod, test);
  }
  
  private Statement possiblyExpectingExceptions(FrameworkMethod testMethod,
      Object test, Statement next) {
    @SuppressWarnings("unchecked") Class<? extends Throwable> expectedException = 
      (Class<? extends Throwable>) getAnnoationValue(testMethod.getAnnotation(Test.class.getName()), "expected");
    if (expectedException != null && !expectedException.getName().equals(org.junit.Test.None.class.getName())) {
      return new ExpectExceptionStatement(next, expectedException);
    } else {
      return next;
    }
  }
  
  private Statement withPotentialTimeout(FrameworkMethod testMethod,
      Object test, Statement next) {
    Long timeout = (Long) getAnnoationValue(testMethod.getAnnotation(Test.class.getName()), "timeout");
    if (timeout != null && timeout > 0) {
      return new FailOnTimeoutStatement(next, timeout);
    } else {
      return next;
    }
  }
  
  private Statement withBefores(FrameworkMethod testMethod,
      Object test, Statement next) {
    List<FrameworkMethod> befores = osgiTestClass.getAnnotatedMethods(Before.class.getName());
    return new RunBeforesStatement(next, befores, test);
  }

  private Statement withAfters(FrameworkMethod testMethod,
      Object test, Statement next) {
    List<FrameworkMethod> afters= osgiTestClass.getAnnotatedMethods(After.class.getName());
    return new RunAftersStatement(next, afters, test);
  }
  
  private Statement withServiceInjection(FrameworkMethod testMethod,
      Object test, Statement next) {
    List<FrameworkField> befores = osgiTestClass.getAnnotatedFields(OSGiService.class.getName());
    return new InjectServicesStatement(osgiTestBundle, next, befores, test);
  }
  
  private Statement withBundleContextInjection(FrameworkMethod testMethod,
      Object test, Statement next) {
    List<FrameworkField> befores = osgiTestClass.getAnnotatedFields(OSGiBundleContext.class.getName());
    return new InjectBundleContextStatement(osgiTestBundle, next, befores, test);
  }

  private Object getAnnoationValue(Annotation annotation, String methodName) {
    if (annotation == null) {
      return null;
    }
    try {
      return annotation.annotationType().getDeclaredMethod(methodName).invoke(annotation);
    } catch (Exception e) {
      return null;
    }
  }
  
  protected List<FrameworkMethod> computeTestMethods(TestClass testClass) {
    return testClass.getAnnotatedMethods(Test.class.getName());
  }
  
  private Description methodDescription(TestClass testClass, FrameworkMethod testMethod) {
    return Description.createTestDescription(testClass.getJavaClass(),
        testMethod.getName(), testMethod.getAnnotations());
  }

  
  private File createJar(BundleRepository repository, OSGiTest annotation) throws InitializationError {
    File tempDirectory = new File(repository.getLocation(), Deployer.TEMP_DIRECTORY);
    File testBundleFile = new File(tempDirectory, "testjar.jar");
    try {
      File baseDir = new File(annotation.baseDir());
      File manifestFile = new File(baseDir, annotation.manifest());
      Manifest manifest = null;
      InputStream is = new FileInputStream(manifestFile);
      try {
        manifest = new Manifest(is);
      } finally {
        is.close();
      }
      OutputStream os = new FileOutputStream(testBundleFile);
      JarOutputStream jos = new JarOutputStream(os, manifest);
      try {
        for (Include buildInclude : annotation.buildIncludes()) {
          File include = new File(baseDir, buildInclude.source());
          if (buildInclude.dest().equals("")) {
            for (File file : include.listFiles()) {
              addFileToJar(jos, file, file.getName());
            }
          } else {
            addFileToJar(jos, include, buildInclude.dest());
          }
        }
      } finally {
        jos.close();
      }
      return testBundleFile;
    } catch (IOException e) {
      List<Throwable> list = new ArrayList<Throwable>(1);
      list.add(e);
      throw new InitializationError(list);
    }
  }

  private final static int BUFFERSIZE = 1024;

  private void addFileToJar(JarOutputStream jarout, File f, String dest) throws IOException {
    if ("META-INF/MANIFEST.MF".equals(dest.toUpperCase())) {
      return;
    }

    String sep = "/";

    if (f.isFile()) {
      BufferedInputStream origin = null;

      byte data[] = new byte[BUFFERSIZE];

      FileInputStream fi = new FileInputStream(f);
      origin = new BufferedInputStream(fi, BUFFERSIZE);

      JarEntry entry = new JarEntry(dest);
      jarout.putNextEntry(entry);

      int count;

      while ((count = origin.read(data, 0, BUFFERSIZE)) != -1) {
        jarout.write(data, 0, count);
      }

      jarout.flush();
      jarout.closeEntry();
      origin.close();
    } else if (f.isDirectory()) {
      File files[] = f.listFiles();

      for (int i = 0; i < files.length; i++) {
        addFileToJar(jarout, files[i], dest + sep + files[i].getName());
      }
    }
  }

}
