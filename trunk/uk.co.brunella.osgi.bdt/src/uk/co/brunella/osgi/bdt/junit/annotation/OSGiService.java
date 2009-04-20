package uk.co.brunella.osgi.bdt.junit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OSGiService {
  String serviceName() default "";
  Class<?> serviceClass() default Void.class;
  String filter() default "";
  long timeout() default 0;
}
