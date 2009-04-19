package uk.co.brunella.osgi.bdt.junit.runner.statement;

public class FailStatement extends Statement {

  private final Throwable fError;

  public FailStatement(Throwable e) {
    fError = e;
  }

  @Override
  public void evaluate() throws Throwable {
    throw fError;
  }
}
