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
package uk.co.brunella.osgi.bdt.junit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import uk.co.brunella.osgi.bdt.junit.runner.OSGiBDTJUnitRunner;

/**
 * Annotation used to describe an OSGi BDT JUnit test class. The test must 
 * be run with {@link OSGiBDTJUnitRunner}. The annotation specifies how
 * a test bundle is created, the OSGi framework used and the bundles
 * that need to be installed.
 * 
 * <p>Example:</p>
 * <p><blockquote><pre>
 * &#64;RunWith(OSGiBDTJUnitRunner.class)
 * &#64;OSGiBDTTest(
 *   baseDir = ".",
 *   manifest = "META-INF/MANIFEST.MF",
 *   buildIncludes = { &#64;Include(source = "bin", dest = "") },
 *   requiredBundles = { "org.eclipse.osgi.services", 
 *     "org.eclipse.equinox.log" }
 * )
 * public class BundleTestClass {
 * 
 *   &#64;OSGiBundleContext
 *   private BundleContext bundleContext;
 * 
 *   &#64;OSGiService(serviceName = "com.example.MyService")
 *   private MyService myService;
 * 
 *   &#64;Test
 *   public void testService() {
 *      ...
 *   }
 * }
 * </pre></blockquote></p>
 * 
 * @see uk.co.brunella.osgi.bdt.junit.runner.OSGiBDTJUnitRunner
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface OSGiBDTTest {
  
  /**
   * The base directory. Defaults to "."
   */
  String baseDir() default ".";
  
  /**
   * The manifest file relative to the base directory.
   * Defaults to "META-INF/MANIFEST.MF"
   */
  String manifest() default "META-INF/MANIFEST.MF";
  
  /**
   * The OSGi framework used. Defaults to <code>Framework.EQUINOX</code>
   * 
   * @see Framework
   */
  Framework framework() default Framework.EQUINOX;
  
  /**
   * The framework start policy. Defaults to <code>StartPolicy.ONCE_PER_TEST_CLASS</code>
   * 
   * @see StartPolicy
   */
  StartPolicy frameworkStartPolicy() default StartPolicy.ONCE_PER_TEST_CLASS;
  
  /**
   * The OSGi BDT Bundle repository. Must either be an absolut path to
   * the repository or an environment variable name ${ENV_VARIABLE}.
   * Defaults to ${OSGI_REPOSITORY}.
   */
  String repository() default "${OSGI_REPOSITORY}";
  
  /**
   * Specifies which directories to include into the test bundle relative
   * to the base directory.
   * 
   * @see Include
   */
  Include[] buildIncludes();
  
  /**
   * The OSGi system bundle. Defaults to "org.eclipse.osgi". An optional 
   * version range (for syntax see OSGi documentation) can be specified
   * using ";version=" directive.
   * 
   * <p>Example:</p>
   * Version must be greater or equal to 3.4.0:
   * <br><code>"org.eclipse.osgi;version=3.4.0"</code>
   * <p> 
   * Version must be greater or equal to 3.4.0 but smaller than 4.0.0:
   * <br><code>"org.eclipse.osgi;version=[3.4.0,4.0.0)"</code>
   * 
   */
  String systemBundle() default "";
  
  /**
   * An array of required bundles. The bundles specified are installed and started
   * in the order they are specified. An optional version range (for syntax see OSGi 
   * documentation) can be specified using ";version=" directive.
   * 
   * <p>Example:</p>
   * No version specified:
   * <br><code>{ "org.eclipse.osgi.services", "org.eclipse.equinox.log" }</code>
   * <p>With version range:
   * <br><code>{ "org.eclipse.osgi.services;version=[1.4.0,2.0.0)", 
   *    "org.eclipse.equinox.log;version=1.3.0" }</code>
   * 
   */
  String[] requiredBundles();
}
