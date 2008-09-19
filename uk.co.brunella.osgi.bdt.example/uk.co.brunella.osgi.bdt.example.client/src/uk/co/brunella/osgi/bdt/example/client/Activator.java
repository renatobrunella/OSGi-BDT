package uk.co.brunella.osgi.bdt.example.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import uk.co.brunella.osgi.bdt.example.translation.TranslationService;

public class Activator implements BundleActivator {

  public void start(BundleContext context) throws Exception {
    ServiceReference reference = context
        .getServiceReference(TranslationService.class.getName());

    try {
      System.out.println("Enter a blank line to exit.");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String phrase = "";

      while (true) {
        System.out.print("Enter an english phrase (cat dog bird): ");
        phrase = in.readLine();

        if (phrase.length() == 0) {
          break;
        }
        TranslationService service = (TranslationService) context
            .getService(reference);
        if (service != null) {
          System.out.println("In french: " + service.translate("English", "French", phrase));
          System.out.println("In german: " + service.translate("English", "German", phrase));
          context.ungetService(reference);
        } else {
          System.out.println("Translation service is not available");
        }

      }
      System.out.println("Bye");
    } catch (IOException e) {
    }
  }

  public void stop(BundleContext context) throws Exception {
  }

}
