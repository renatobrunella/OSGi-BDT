package uk.co.brunella.osgi.bdt.example.sensor.temperature.engine.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import uk.co.brunella.osgi.bdt.example.sensor.temperature.TemperatureSensor;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;
import uk.co.brunella.osgi.bdt.junit.annotation.Include;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBDTTest;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBundleContext;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiService;
import uk.co.brunella.osgi.bdt.junit.annotation.StartPolicy;
import uk.co.brunella.osgi.bdt.junit.runner.OSGiBDTJUnitRunner;

@RunWith(OSGiBDTJUnitRunner.class)
@OSGiBDTTest(
    repositories = { "../build/repository", "../build/repository-instrumented" },
    manifest = "integration-test/META-INF/MANIFEST.MF",
    framework = Framework.EQUINOX,
    buildIncludes = { @Include(source = "bin-integration-test", dest = "") },
    frameworkStartPolicy = StartPolicy.ONCE_PER_TEST,
    requiredBundles = { 
        "uk.co.brunella.osgi.bdt.example.sensor.temperature", 
        "uk.co.brunella.osgi.bdt.example.sensor.temperature.engine" }
)
public class EngineTemperatureSensorFrameworkTest {

  @Before
  public void setup() {
    System.setProperty("emma.coverage.out.file", "./coverage/coverage.emma");
    System.setProperty("emma.coverage.out.merge", "true");
  }
  
  @OSGiBundleContext
  private BundleContext context;
  
  @OSGiService(serviceClass = TemperatureSensor.class)
  private TemperatureSensor sensor;
  
  @Test
  public void testSensorAvailable() {
    assertNotNull("Sensor service not available", sensor);
    double temp = sensor.readTemperature();
    assertTrue("Invalid temperature", temp > -273.0);
  }
  
  @Test
  public void testSensorRegistrationIsCorrect() throws BundleException {
    assertNotNull("Sensor service not available", context.getServiceReference(TemperatureSensor.class.getName()));
    Bundle bundle = findSensorBundle();
    bundle.stop();
    assertNull("Sensor service is available", context.getServiceReference(TemperatureSensor.class.getName()));
    bundle.start();
    assertNotNull("Sensor service not available", context.getServiceReference(TemperatureSensor.class.getName()));
  }
  
  private Bundle findSensorBundle() {
    for (Bundle bundle : context.getBundles()) {
      if ("uk.co.brunella.osgi.bdt.example.sensor.temperature.engine".equals(bundle.getSymbolicName())) {
        return bundle;
      }
    }
    fail("could not find bundle");
    return null;
  }
}
