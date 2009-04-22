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
