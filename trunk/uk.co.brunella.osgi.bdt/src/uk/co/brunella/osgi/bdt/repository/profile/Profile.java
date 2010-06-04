/*
 * Copyright 2010 brunella ltd
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
package uk.co.brunella.osgi.bdt.repository.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;

public class Profile {

  private final static String PROFILE_DIRECTORY = "profiles";
  
  private static Map<String, Properties> profiles;
  
  static
  {
    profiles = getProfiles();
  }
  
  public static String[] getProfileNameList() {
    Set<String> names = profiles.keySet();
    String[] nameList = (String[]) names.toArray(new String[names.size()]);
    Arrays.sort(nameList);
    return nameList;
  }
  
  public static boolean isValidProfileName(String profileName) {
    return profiles.containsKey(profileName);
  }

  public static Properties getProfile(String profileName) {
    return profiles.get(profileName);
  }

  private static Properties readProperties(String fileName) {
    InputStream is = BundleRepository.class.getClassLoader().getResourceAsStream(PROFILE_DIRECTORY + "/" + fileName);
    Properties properties = new Properties();
    try {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      return null;
    }
  }

  private static Map<String, Properties> getProfiles() {
    Properties profiles = readProperties("profile.list");
    if (profiles == null) {
      throw new RuntimeException("Could not read profiles properties");
    }
    String[] profileList = ((String)profiles.get("profiles")).trim().split(",");
    Map<String, Properties> profilesMap = new HashMap<String, Properties>(profileList.length);
    for (int i = 0; i < profileList.length; i++) {
      Properties profileProperties = readProperties(profileList[i]);
      String profileName = (String) profileProperties.get("osgi.java.profile.name");
      profilesMap.put(profileName, profileProperties);
    }
    
    return profilesMap;
  }
}
