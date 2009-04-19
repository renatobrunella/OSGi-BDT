package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkField;

public class InjectBundleContextStatement extends Statement {

  private final Bundle fTestBundle;
  private final Statement fNext;
  private final Object fTarget;
  private final List<FrameworkField> fFields;

  public InjectBundleContextStatement(Bundle testBundle, Statement next, List<FrameworkField> fields, Object target) {
    fTestBundle = testBundle;
    fNext = next;
    fFields = fields;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    if (fFields.size() > 0) {
      BundleContext bundleContext = fTestBundle.getBundleContext(); 
      for (FrameworkField field : fFields) {
        field.set(fTarget, bundleContext);
      }
    }
    fNext.evaluate();
  }

}