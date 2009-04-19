package uk.co.brunella.osgi.bdt.junit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface OSGiTest {
  String baseDir() default ".";
  String manifest() default "META-INF/MANIFEST.MF";
  Framework framework() default Framework.EQUINOX;
  StartPolicy frameworkStartPolicy() default StartPolicy.ONCE_PER_TEST_CLASS;
  String repository() default "${OSGI_REPOSITORY}";
  Include[] buildIncludes();
  String systemBundle() default "org.eclipse.osgi";
  String[] requiredBundles();
}
