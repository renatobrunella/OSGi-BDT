package uk.co.brunella.osgi.bdt.example.dictionary.french;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.brunella.osgi.bdt.example.dictionary.DictionaryService;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
	  Properties props = new Properties();
      props.put("Language", "French");
      context.registerService(
          DictionaryService.class.getName(), new DictionaryServiceImpl(), props);
	}

	public void stop(BundleContext context) throws Exception {
	}

	private static class DictionaryServiceImpl implements DictionaryService {

	  private final static String[][] DICTIONARY_ENTRIES = 
	  {
	    { "chien", "dog" },
	    { "chat", "cat" },
	    { "oiseau", "bird" }
	  };
	  
	  private final static int FRENCH = 0;
	  private final static int ENGLISH = 1;
	  
    public String inverseLookup(String word) {
      word = word.toLowerCase();
      for (int i = 0; i < DICTIONARY_ENTRIES.length; i++) {
        if (DICTIONARY_ENTRIES[i][ENGLISH].equals(word)) {
          return DICTIONARY_ENTRIES[i][FRENCH];
        }
      }
      return null;
    }

    public String lookup(String word) {
      word = word.toLowerCase();
      for (int i = 0; i < DICTIONARY_ENTRIES.length; i++) {
        if (DICTIONARY_ENTRIES[i][FRENCH].equals(word)) {
          return DICTIONARY_ENTRIES[i][ENGLISH];
        }
      }
      return null;
    }
	  
	}
}