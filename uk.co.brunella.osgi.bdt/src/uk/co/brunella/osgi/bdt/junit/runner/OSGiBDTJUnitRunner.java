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

import static org.osgi.framework.Constants.FRAGMENT_HOST;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
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
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.osgi.framework.Bundle;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.framework.OSGiFrameworkStarter;
import uk.co.brunella.osgi.bdt.framework.OSGiFrameworkStarterFactory;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;
import uk.co.brunella.osgi.bdt.junit.annotation.Include;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBDTTest;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBundleContext;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiParameter;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiService;
import uk.co.brunella.osgi.bdt.junit.annotation.StartPolicy;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkField;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkMethod;
import uk.co.brunella.osgi.bdt.junit.runner.model.OSGiBDTTestWrapper;
import uk.co.brunella.osgi.bdt.junit.runner.model.TestClass;
import uk.co.brunella.osgi.bdt.junit.runner.model.TestRunNotifier;
import uk.co.brunella.osgi.bdt.junit.runner.statement.ExpectExceptionStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.FailOnTimeoutStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.FailStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InjectBundleContextStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InjectParametersStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InjectServicesStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.InvokeMethodStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.ReflectiveCallable;
import uk.co.brunella.osgi.bdt.junit.runner.statement.RunAftersStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.RunBeforesStatement;
import uk.co.brunella.osgi.bdt.junit.runner.statement.Statement;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;

/**
 * The OSGi BDT JUnit runner. Allows to run an OSGiBDTTest under JUnit 4.
 * Is used with the <code>RunWith</code> JUnit annotation.
 * 
 * <p>Example:</p>
 * <p><blockquote><pre>
 * <b>&#64;RunWith(OSGiBDTJUnitRunner.class)</b>
 * <b>&#64;OSGiBDTTest</b>(
 *   baseDir = ".",
 *   manifest = "META-INF/MANIFEST.MF",
 *   buildIncludes = { &#64;Include(source = "bin", dest = "") },
 *   requiredBundles = { "org.eclipse.osgi.services", 
 *     "org.eclipse.equinox.log" }
 * )
 * public class BundleTestClass {
 * 
 *   &#64;OSGiBundleContext
 *   private BundleContext bundleContext;
 * 
 *   &#64;OSGiService(serviceName = "com.example.MyService")
 *   private MyService myService;
 * 
 *   &#64;Test
 *   public void testService() {
 *      ...
 *   }
 * }
 * </pre></blockquote></p>
 * 
 * @see uk.co.brunella.osgi.bdt.junit.annotation.OSGiBDTTest
 * @see org.junit.runner.RunWith
 */
public class OSGiBDTJUnitRunner extends Runner {

  public static final String OSGI_BDT_RUNNER_BUNDLE_NAME = "uk.co.brunella.osgi.bdt";
  
  private final String testClassName;
  private final TestClass testClass;
  private final Description testClassDescription;
  private final OSGiBDTTestWrapper testClassAnnotation;
  private final BundleRepository[] repositories;
  private final OSGiFrameworkStarter[] frameworkStarters;
  private final File testBundleFile;
  private final Map<String, String> parameters;
  private TestClass osgiTestClass;
  private Bundle osgiTestBundle;

  public OSGiBDTJUnitRunner(Class<?> testClass) throws InitializationError {
    this(testClass, new OSGiBDTTestWrapper(testClass.getAnnotation(OSGiBDTTest.class)), null, null);
  }

  public OSGiBDTJUnitRunner(Class<?> testClass, OSGiBDTTestWrapper testClassAnnotation, File testBundleFile, 
      Map<String, String> parameters) throws InitializationError {
    if (testClassAnnotation == null) {
      throw new InitializationError("OSGiBDTTest annotation is missing");
    }
    testClassName = testClass.getName();
    this.testClassAnnotation = testClassAnnotation;
    this.testClass = new TestClass(testClass);
    repositories = loadRepositories(testClassAnnotation.repositories());
    testClassDescription = createDescription(this.testClass, repositories);
    
    frameworkStarters = createFrameworkStarter(repositories, testClassAnnotation.framework());
    if (testBundleFile == null) {
      this.testBundleFile = createJar(repositories, testClassAnnotation);
    } else {
      this.testBundleFile = testBundleFile;
    }
    this.parameters = parameters;
  }

  private Description createDescription(TestClass testClass, BundleRepository[] repositories) {
    if (repositories.length == 1) {
      Description testClassDescription = Description.createSuiteDescription(testClass.getJavaClass());
      for (FrameworkMethod method : testClass.getAnnotatedMethods(Test.class.getName())) {
        testClassDescription.addChild(methodDescription(testClass, method, 0));
      }
      return testClassDescription;
    } else {
      Description testParentDescription = Description.createSuiteDescription(testClass.getJavaClass());
      int index = 0;
      for (BundleRepository repository : repositories) {
        index++;
        Description testClassDescription = Description.createSuiteDescription(repository.getLocation().toString());
        testParentDescription.addChild(testClassDescription);
        for (FrameworkMethod method : testClass.getAnnotatedMethods(Test.class.getName())) {
          testClassDescription.addChild(methodDescription(testClass, method, index));
        }
      }
      return testParentDescription;
    }
  }

  @Override
  public Description getDescription() {
    return testClassDescription;
  }

  @Override
  public void run(RunNotifier notifier) {
    List<FrameworkMethod> testMethods = computeTestMethods(testClass);
    runTests(notifier, testMethods);
  }

  private OSGiFrameworkStarter[] createFrameworkStarter(BundleRepository[] bundleRepositories, Framework framework) {
    OSGiFrameworkStarter[] starters = new OSGiFrameworkStarter[bundleRepositories.length];
    for (int i = 0; i < starters.length; i++) {
      starters[i] = OSGiFrameworkStarterFactory.create(bundleRepositories[i], framework);
    }
    return starters;
  }
  
  private void startFramework(OSGiFrameworkStarter frameworkStarter) {
    try {
      String systemBundleName = testClassAnnotation.systemBundle();
      if ("".equals(systemBundleName)) {
        systemBundleName = frameworkStarter.systemBundleName();
      }
      String[] frameworkParameters = frameworkStarter.defaultArguments();
      frameworkStarter.startFramework(systemBundleName, frameworkParameters);

      frameworkStarter.installBundle(OSGI_BDT_RUNNER_BUNDLE_NAME).start();
      
      List<Bundle> bundleList = new ArrayList<Bundle>();
      
      for (String bundleName : testClassAnnotation.requiredBundles()) {
        bundleList.add(frameworkStarter.installBundle(bundleName));
      }

      osgiTestBundle = frameworkStarter.installBundle(testBundleFile);
      String fragmentHost = getFragmentHost(osgiTestBundle);

      if (fragmentHost == null) {
        for (Bundle bundle : bundleList) {
          if (getFragmentHost(bundle) == null) {
            bundle.start();
          }
        }
        osgiTestBundle.start();
      } else {
        for (Bundle bundle : bundleList) {
          if (getFragmentHost(bundle) == null) {
            bundle.start();
          }
          if (bundle.getSymbolicName().equals(fragmentHost)) {
            osgiTestBundle = bundle;
          }
        }
      }
    } catch (Exception e) { 
      throw new RuntimeException(e);
    }
    
    try {
      osgiTestClass = new TestClass(osgiTestBundle.loadClass(testClassName));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String getFragmentHost(Bundle bundle) {
    @SuppressWarnings("unchecked") Dictionary<String, String> headers = bundle.getHeaders();
    String fragmentHost = headers.get(FRAGMENT_HOST);
    if (fragmentHost != null) {
      fragmentHost = fragmentHost.trim();
      if (fragmentHost.indexOf(';') >= 0) {
        fragmentHost = fragmentHost.substring(0, fragmentHost.indexOf(';')).trim();
      }
    }
    return fragmentHost;
  }

  private BundleRepository[] loadRepositories(String[] repositoryLocations) throws InitializationError {
    List<BundleRepository> repositories = new ArrayList<BundleRepository>(repositoryLocations.length); 
    for (String repositoryLocation : repositoryLocations) {
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
        repositories.add(persister.load());
      } catch (IOException e) {
        List<Throwable> list = new ArrayList<Throwable>(1);
        list.add(e);
        throw new InitializationError(list);
      }
    }
    return (BundleRepository[]) repositories.toArray(new BundleRepository[repositories.size()]);
  }

  private void stopFramework(OSGiFrameworkStarter frameworkStarter) {
    try {
      frameworkStarter.stopFramework();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runTests(RunNotifier notifier, List<FrameworkMethod> testMethods) {
    int index = 0;
    for (OSGiFrameworkStarter frameworkStarter : frameworkStarters) {
      if (frameworkStarters.length > 1) {
        index++;
      }
      if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST_CLASS) {
        startFramework(frameworkStarter);
        runBeforeClass(notifier);
      }
      for (FrameworkMethod testMethod : testMethods) {
        if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST) {
          startFramework(frameworkStarter);
        }
  
        runTest(notifier, testMethod, index);
  
        if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST) {
          stopFramework(frameworkStarter);
        }
      }
      if (testClassAnnotation.frameworkStartPolicy() == StartPolicy.ONCE_PER_TEST_CLASS) {
        runAfterClass(notifier);
        stopFramework(frameworkStarter);
      }
    }
  }

  private void runTest(RunNotifier notifier, FrameworkMethod testMethod, int index) {
    TestRunNotifier testNotifier = new TestRunNotifier(notifier, methodDescription(testClass, testMethod, index));
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
      e.printStackTrace();
      return new FailStatement(e);
    }

    Statement statement = methodInvoker(testMethod, test);
    statement = possiblyExpectingExceptions(testMethod, test, statement);
    statement = withPotentialTimeout(testMethod, test, statement);
    statement = withBefores(testMethod, test, statement);
    statement = withAfters(testMethod, test, statement);
    statement = withServiceInjection(testMethod, test, statement);
    statement = withBundleContextInjection(testMethod, test, statement);
    statement = withParamterInjection(testMethod, test, statement);
    return statement;
  }

  private Statement methodInvoker(FrameworkMethod testMethod, Object test) {
    return new InvokeMethodStatement(testMethod, test);
  }
  
  private Statement possiblyExpectingExceptions(FrameworkMethod testMethod,
      Object test, Statement next) {
    @SuppressWarnings("unchecked") Class<? extends Throwable> expectedException = 
      (Class<? extends Throwable>) getAnnotationValue(testMethod.getAnnotation(Test.class.getName()), "expected");
    if (expectedException != null && !expectedException.getName().equals(org.junit.Test.None.class.getName())) {
      return new ExpectExceptionStatement(next, expectedException);
    } else {
      return next;
    }
  }
  
  private Statement withPotentialTimeout(FrameworkMethod testMethod,
      Object test, Statement next) {
    Long timeout = (Long) getAnnotationValue(testMethod.getAnnotation(Test.class.getName()), "timeout");
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
    List<FrameworkMethod> afters = osgiTestClass.getAnnotatedMethods(After.class.getName());
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
  
  private Statement withParamterInjection(FrameworkMethod testMethod,
      Object test, Statement next) {
    if (parameters != null) {
      List<FrameworkMethod> befores = osgiTestClass.getAnnotatedMethods(OSGiParameter.class.getName());
      return new InjectParametersStatement(osgiTestBundle, next, befores, test, parameters);
    } else {
      return next;
    }
  }

  private Object getAnnotationValue(Annotation annotation, String methodName) {
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
  
  private Description methodDescription(TestClass testClass, FrameworkMethod testMethod, int index) {
    if (index == 0) {
      return Description.createTestDescription(testClass.getJavaClass(),
          testMethod.getName() /*, testMethod.getAnnotations()*/);
    } else {
      return Description.createTestDescription(testClass.getJavaClass(),
          testMethod.getName() + " [" + index + "]" /*, testMethod.getAnnotations()*/);
    }
  }

  
  private File createJar(BundleRepository[] repositories, OSGiBDTTestWrapper annotation) throws InitializationError {
    Manifest manifest = readManifest(annotation);
    File tempDirectory = new File(repositories[0].getLocation(), Deployer.TEMP_DIRECTORY);
    File testBundleFile = new File(tempDirectory, "testjar.jar");
    try {
      File baseDir = new File(annotation.baseDir());
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

  private Manifest readManifest(OSGiBDTTestWrapper annotation) throws InitializationError {
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
      return manifest;
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
