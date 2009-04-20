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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service injection annotation. This annotation is used
 * to inject an OSGi service into a field. The service is
 * specified by the service class name and an optional 
 * filter. If the service is not available then the test(s)
 * will fail. A timeout can be specified to allow more time
 * for bundles to register their services. 
  * <p>Example:</p>
 * <p>
 * <p><blockquote><pre>
 *   &#64;OSGiService(serviceName = "org.osgi.service.log.LogService")
 *   private LogService logService;
 * </pre></blockquote></p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OSGiService {
  
  /**
   * The service class name. 
   */
  String serviceName() default "";
  
  /**
   * The service class. 
   */
  Class<?> serviceClass() default Void.class;
  
  /**
   * Optional service filter. See OSGi documentation for syntax. 
   */
  String filter() default "";
  
  /**
   * Timeout in milliseconds after which the test runner
   * stops trying to get a matching service.
   */
  long timeout() default 0;
}
