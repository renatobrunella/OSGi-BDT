package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.util.List;

import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkMethod;

public class RunBeforesStatement extends Statement {

  private final Statement fNext;
  private final Object fTarget;
  private final List<FrameworkMethod> fBefores;

  public RunBeforesStatement(Statement next, List<FrameworkMethod> befores, Object target) {
    fNext = next;
    fBefores = befores;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    for (FrameworkMethod before : fBefores)
      before.invokeExplosively(fTarget);
    fNext.evaluate();
  }
}