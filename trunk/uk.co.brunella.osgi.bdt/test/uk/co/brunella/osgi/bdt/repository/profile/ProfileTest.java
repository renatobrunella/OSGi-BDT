package uk.co.brunella.osgi.bdt.repository.profile;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

public class ProfileTest {

  @Test
  public void getProfileNameList() {
    String[] profiles = Profile.getProfileNameList();
    assertNotNull(profiles);
    assertEquals(10, profiles.length);
    assertEquals("[CDC-1.0/Foundation-1.0, CDC-1.1/Foundation-1.1, " +
    		"J2SE-1.2, J2SE-1.3, J2SE-1.4, J2SE-1.5, JRE-1.1, JavaSE-1.6, " +
    		"OSGi/Minimum-1.0, OSGi/Minimum-1.1]", Arrays.toString(profiles));
  }
  
  @Test
  public void isValidProfileName() {
    assertTrue(Profile.isValidProfileName("J2SE-1.5"));
    assertFalse(Profile.isValidProfileName("INVALID"));
  }
  
  @Test
  public void getProfile() {
    Properties profile = Profile.getProfile("J2SE-1.5");
    assertNotNull(profile);
    assertNull(Profile.getProfile("INVALID"));
  }
}
