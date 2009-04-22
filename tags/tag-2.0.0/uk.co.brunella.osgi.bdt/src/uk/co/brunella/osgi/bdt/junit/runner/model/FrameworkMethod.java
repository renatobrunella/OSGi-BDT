/*
 * Copyright 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.junit.runner.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import uk.co.brunella.osgi.bdt.junit.runner.statement.ReflectiveCallable;

public class FrameworkMethod {
  
  private final Method fMethod;

  public FrameworkMethod(Method method) {
    fMethod = method;
  }

  public Method getMethod() {
    return fMethod;
  }

  public Object invokeExplosively(final Object target, final Object... params)
      throws Throwable {
    return new ReflectiveCallable() {
      @Override
      protected Object runReflectiveCall() throws Throwable {
        return fMethod.invoke(target, params);
      }
    }.run();
  }

  public String getName() {
    return fMethod.getName();
  }

  public void validatePublicVoidNoArg(boolean isStatic, List<Throwable> errors) {
    validatePublicVoid(isStatic, errors);
    if (fMethod.getParameterTypes().length != 0)
      errors.add(new Exception("Method " + fMethod.getName()
          + " should have no parameters"));
  }

  public void validatePublicVoid(boolean isStatic, List<Throwable> errors) {
    if (Modifier.isStatic(fMethod.getModifiers()) != isStatic) {
      String state = isStatic ? "should" : "should not";
      errors.add(new Exception("Method " + fMethod.getName() + "() " + state
          + " be static"));
    }
    if (!Modifier.isPublic(fMethod.getDeclaringClass().getModifiers()))
      errors.add(new Exception("Class " + fMethod.getDeclaringClass().getName()
          + " should be public"));
    if (!Modifier.isPublic(fMethod.getModifiers()))
      errors.add(new Exception("Method " + fMethod.getName()
          + "() should be public"));
    if (fMethod.getReturnType() != Void.TYPE)
      errors.add(new Exception("Method " + fMethod.getName()
          + "() should be void"));
  }

  boolean isShadowedBy(List<FrameworkMethod> results) {
    for (FrameworkMethod each : results)
      if (isShadowedBy(each))
        return true;
    return false;
  }

  private boolean isShadowedBy(FrameworkMethod each) {
    if (!each.getName().equals(getName()))
      return false;
    if (each.getParameterTypes().length != getParameterTypes().length)
      return false;
    for (int i = 0; i < each.getParameterTypes().length; i++)
      if (!each.getParameterTypes()[i].equals(getParameterTypes()[i]))
        return false;
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (!FrameworkMethod.class.isInstance(obj))
      return false;
    return ((FrameworkMethod) obj).fMethod.equals(fMethod);
  }

  @Override
  public int hashCode() {
    return fMethod.hashCode();
  }

  public boolean producesType(Class<?> type) {
    return getParameterTypes().length == 0
        && type.isAssignableFrom(fMethod.getReturnType());
  }

  private Class<?>[] getParameterTypes() {
    return fMethod.getParameterTypes();
  }

  public Annotation[] getAnnotations() {
    return fMethod.getAnnotations();
  }

  public Annotation getAnnotation(String annotationTypeName) {
    for (Annotation annotation : fMethod.getDeclaredAnnotations()) {
      if (annotation.annotationType().getName().equals(annotationTypeName)) {
        return annotation;
      }
    }
    return null;
  }
}