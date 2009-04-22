package uk.co.brunella.osgi.bdt.example.test.dictionary;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import uk.co.brunella.osgi.bdt.example.dictionary.DictionaryService;
import uk.co.brunella.osgi.bdt.osgitestrunner.Description;
import uk.co.brunella.osgi.bdt.osgitestrunner.OSGiTestCase;
import uk.co.brunella.osgi.bdt.osgitestrunner.OSGiTestCaseUtils;
import uk.co.brunella.osgi.bdt.osgitestrunner.OSGiTestParameter;

@Description("Test case for the french dictionary")
public class FrenchDictionaryTestCase implements OSGiTestCase, OSGiTestParameter {

  private BundleContext context;
  private ServiceTracker dictionaryTracker;

  public FrenchDictionaryTestCase(BundleContext context) {
    this.context = context;
  }

  public void setParameters(Map<String, String> arg0) {
    
  }

  public void setUp() throws InvalidSyntaxException {
    String filterContent = "(&(objectClass=" + DictionaryService.class.getName() + ")(Language=French))";
    Filter filter = context.createFilter(filterContent);
    dictionaryTracker = new ServiceTracker(context, filter, null);
    dictionaryTracker.open();
  }
  
  public void tearDown() {
    dictionaryTracker.close();
  }
  
  @Description("Test that translates a german word into english")
  public void testLookup() throws Throwable {
    OSGiTestCaseUtils.waitForService(dictionaryTracker, 2000);
    DictionaryService service = (DictionaryService) dictionaryTracker.getService();
    assertNotNull(service);
    String word = service.lookup("Chien");
    assertEquals("dog", word);
  }
  
  @Description("Test that translates an english word into german")
  public void testInverseLookup() throws Throwable {
    OSGiTestCaseUtils.waitForService(dictionaryTracker, 2000);
    DictionaryService service = (DictionaryService) dictionaryTracker.getService();
    assertNotNull(service);
    String word = service.inverseLookup("cat");
    assertEquals("chat", word);
  }
}
