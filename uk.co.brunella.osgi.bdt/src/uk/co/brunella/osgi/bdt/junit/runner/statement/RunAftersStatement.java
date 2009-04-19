package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.util.ArrayList;
import java.util.List;

import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkMethod;

public class RunAftersStatement extends Statement {

  private final Statement fNext;
  private final Object fTarget;
  private final List<FrameworkMethod> fAfters;

  public RunAftersStatement(Statement next, List<FrameworkMethod> afters, Object target) {
    fNext = next;
    fAfters = afters;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    List<Throwable> fErrors = new ArrayList<Throwable>();
    fErrors.clear();
    try {
      fNext.evaluate();
    } catch (Throwable e) {
      fErrors.add(e);
    } finally {
      for (FrameworkMethod each : fAfters)
        try {
          each.invokeExplosively(fTarget);
        } catch (Throwable e) {
          fErrors.add(e);
        }
    }
    if (fErrors.isEmpty())
      return;
    if (fErrors.size() == 1)
      throw fErrors.get(0);
    throw new MultipleFailureException(fErrors);
  }
}