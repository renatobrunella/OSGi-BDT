package uk.co.brunella.osgi.bdt.junit.runner.statement;

import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkMethod;

public class InvokeMethodStatement extends Statement {

  private final FrameworkMethod fTestMethod;
  private Object fTarget;

  public InvokeMethodStatement(FrameworkMethod testMethod, Object target) {
    fTestMethod = testMethod;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    fTestMethod.invokeExplosively(fTarget);
  }
}