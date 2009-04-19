package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.lang.reflect.InvocationTargetException;

public abstract class ReflectiveCallable {
  
  public Object run() throws Throwable {
    try {
      return runReflectiveCall();
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }

  protected abstract Object runReflectiveCall() throws Throwable;
}