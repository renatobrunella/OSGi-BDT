package uk.co.brunella.osgi.bdt.example.translation;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import uk.co.brunella.osgi.bdt.example.dictionary.DictionaryService;

public class Activator implements BundleActivator {

  private BundleContext context;
  private ServiceTracker serviceTracker;
  private Map<String, DictionaryService> supportedLanguages = new HashMap<String, DictionaryService>();
  
  public void start(BundleContext context) throws Exception {
    this.context = context;
    serviceTracker = new ServiceTracker(context, DictionaryService.class.getName(), new Customizer());
    serviceTracker.open();
    
    context.registerService(TranslationService.class.getName(), new TranslationServiceImpl(), null);
  }

  public void stop(BundleContext context) throws Exception {
    serviceTracker.close();
  }

  private class TranslationServiceImpl implements TranslationService {

    public String translate(String from, String to, String phrase) {
      if (from.equals(to)) {
        return phrase;
      }
      if (from == null || !("English".equals(from) || supportedLanguages.containsKey(from))) {
        throw new IllegalArgumentException("Unsupported language " + from);
      }
      if (to == null || !("English".equals(to) || supportedLanguages.containsKey(to))) {
        throw new IllegalArgumentException("Unsupported language " + to);
      }
      TranslationStrategy strategy = null;
      if (from.equals("English")) {
        final DictionaryService service = supportedLanguages.get(to);
        strategy = new TranslationStrategy() {
          public String lookup(String word) {
            return service.inverseLookup(word);
          }
        };
      } else if (to.equals("English")) {
        final DictionaryService service = supportedLanguages.get(from);
        strategy = new TranslationStrategy() {
          public String lookup(String word) {
            return service.lookup(word);
          }
        };
      } else {
        final DictionaryService fromService = supportedLanguages.get(from);
        final DictionaryService toService = supportedLanguages.get(to);
        strategy = new TranslationStrategy() {
          public String lookup(String word) {
            String englishWord = fromService.lookup(word);
            return toService.inverseLookup(englishWord);
          }
        };
     }
      return strategy.translate(phrase);
    }
    
  }
  
  private abstract class TranslationStrategy {
    protected abstract String lookup(String word);
    
    public String translate(String phrase) {
      String[] words = phrase.split(" ");
      StringBuilder sb = new StringBuilder();
      for (String word : words) {
        sb.append(lookup(word)).append(' ');
      }
      return sb.toString();
    }
  }
  
  private class Customizer implements ServiceTrackerCustomizer {

    public Object addingService(ServiceReference reference) {
      String language = (String) reference.getProperty("Language");
      if (!supportedLanguages.containsKey(language)) {
        DictionaryService service = (DictionaryService) context.getService(reference);
        supportedLanguages.put(language, service);
        return service;
      }
      return null;
    }

    public void modifiedService(ServiceReference reference, Object service) {
      // ignore
    }

    public void removedService(ServiceReference reference, Object service) {
      String language = (String) reference.getProperty("Language");
      supportedLanguages.remove(language);
    }
    
  }
}
