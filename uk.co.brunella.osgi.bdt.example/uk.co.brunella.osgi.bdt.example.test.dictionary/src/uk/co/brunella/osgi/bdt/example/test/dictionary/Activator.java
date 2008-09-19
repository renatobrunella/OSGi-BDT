package uk.co.brunella.osgi.bdt.example.test.dictionary;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.brunella.osgi.bdt.osgitestrunner.OSGiTestCaseUtils;

public class Activator implements BundleActivator {

  public void start(BundleContext context) throws Exception {
    OSGiTestCaseUtils.run(context, new GermanDictionaryTestCase(context));
    OSGiTestCaseUtils.run(context, new FrenchDictionaryTestCase(context));
  }

  public void stop(BundleContext context) throws Exception {
  }
}
