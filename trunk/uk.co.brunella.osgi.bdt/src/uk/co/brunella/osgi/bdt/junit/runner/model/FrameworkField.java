package uk.co.brunella.osgi.bdt.junit.runner.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FrameworkField {

  private Field fField;

  public FrameworkField(Field field) {
    fField = field;
  }
  
  public Field getField() {
    return fField;
  }

  public void set(Object object, Object value) {
    if (!fField.isAccessible()) {
      fField.setAccessible(true);
    }
    try {
      fField.set(object, value);
    } catch (IllegalArgumentException e) {
    } catch (IllegalAccessException e) {
    }
  }
  
  public Annotation[] getAnnotations() {
    return fField.getAnnotations();
  }

  public Annotation getAnnotation(String annotationTypeName) {
    for (Annotation annotation : fField.getDeclaredAnnotations()) {
      if (annotation.annotationType().getName().equals(annotationTypeName)) {
        return annotation;
      }
    }
    return null;
  }

}
