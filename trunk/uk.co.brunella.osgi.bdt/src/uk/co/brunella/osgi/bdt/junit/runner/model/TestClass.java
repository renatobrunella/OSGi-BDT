package uk.co.brunella.osgi.bdt.junit.runner.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;


public class TestClass {
  
  private final Class<?> fClass;

  private Map<String, List<FrameworkMethod>> fMethodsForAnnotations = new HashMap<String, List<FrameworkMethod>>();
  private Map<String, List<FrameworkField>> fFieldsForAnnotations = new HashMap<String, List<FrameworkField>>();
  
  public TestClass(Class<?> clazz) {
    fClass = clazz;
    if (clazz != null && clazz.getConstructors().length > 1)
      throw new IllegalArgumentException("Test class can only have one constructor");

    for (Class<?> eachClass : getSuperClasses(fClass)) {
      for (Method eachMethod : eachClass.getDeclaredMethods()) {
        addToAnnotationLists(new FrameworkMethod(eachMethod));
      }
      for (Field eachField : eachClass.getDeclaredFields()) {
        addToAnnotationLists(new FrameworkField(eachField));
      }
    }
  }

  private void addToAnnotationLists(FrameworkMethod testMethod) {
    for (Annotation each : computeAnnotations(testMethod))
      addToAnnotationList(each.annotationType(), testMethod);
  }
  
  private void addToAnnotationLists(FrameworkField testField) {
    for (Annotation each : computeAnnotations(testField))
      addToAnnotationList(each.annotationType(), testField);
  }

  protected Annotation[] computeAnnotations(FrameworkMethod testMethod) {
    return testMethod.getAnnotations();
  }
  
  protected Annotation[] computeAnnotations(FrameworkField testField) {
    return testField.getAnnotations();
  }

  private void addToAnnotationList(Class<? extends Annotation> annotation,
      FrameworkMethod testMethod) {
    List<FrameworkMethod> methods = getAnnotatedMethods(annotation.getName());
    if (testMethod.isShadowedBy(methods))
      return;
    if (runsTopToBottom(annotation))
      methods.add(0, testMethod);
    else
      methods.add(testMethod);
  }
  
  private void addToAnnotationList(Class<? extends Annotation> annotation, FrameworkField testField) {
    List<FrameworkField> methods = getAnnotatedFields(annotation.getName());
    methods.add(testField);
  }

  public List<FrameworkMethod> getAnnotatedMethods(String annotationClassName) {
    if (!fMethodsForAnnotations.containsKey(annotationClassName)) {
      fMethodsForAnnotations.put(annotationClassName, new ArrayList<FrameworkMethod>());
    }
    return fMethodsForAnnotations.get(annotationClassName);
  }
  
  public List<FrameworkField> getAnnotatedFields(String annotationClassName) {
    if (!fFieldsForAnnotations.containsKey(annotationClassName)) {
      fFieldsForAnnotations.put(annotationClassName, new ArrayList<FrameworkField>());
    }
    return fFieldsForAnnotations.get(annotationClassName);
  }

  private boolean runsTopToBottom(Class<? extends Annotation> annotation) {
    return annotation.equals(Before.class) || annotation.equals(BeforeClass.class);
  }

  private List<Class<?>> getSuperClasses(Class<?> testClass) {
    ArrayList<Class<?>> results = new ArrayList<Class<?>>();
    Class<?> current = testClass;
    while (current != null) {
      results.add(current);
      current = current.getSuperclass();
    }
    return results;
  }

  public Class<?> getJavaClass() {
    return fClass;
  }

  public String getName() {
    if (fClass == null)
      return "null";
    return fClass.getName();
  }

  public Constructor<?> getOnlyConstructor() {
    Constructor<?>[] constructors = fClass.getConstructors();
    Assert.assertEquals(1, constructors.length);
    return constructors[0];
  }

  public Annotation[] getAnnotations() {
    if (fClass == null)
      return new Annotation[0];
    return fClass.getAnnotations();
  }

  public FrameworkMethod getTestMethod(String name) {
    for (List<FrameworkMethod> methods : fMethodsForAnnotations.values()) {
      for (FrameworkMethod method : methods) {
        if (method.getName().equals(name)) {
          return method;
        }
      }
    }
    return null;
  }
}
