/*
 * Copyright 2008 - 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.framework;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.junit.annotation.Framework;

public class OSGiFrameworkStarterFactory {

  private static final Map<Framework, Class<? extends OSGiFrameworkStarter>> OSGI_FRAMEWORK_STARTERS;
  static {
    OSGI_FRAMEWORK_STARTERS = new HashMap<Framework, Class<? extends OSGiFrameworkStarter>>();
    OSGI_FRAMEWORK_STARTERS.put(Framework.EQUINOX, EquinoxFrameworkStarter.class);
    OSGI_FRAMEWORK_STARTERS.put(Framework.FELIX, FelixFrameworkStarter.class);
    OSGI_FRAMEWORK_STARTERS.put(Framework.KNOPFLERFISH, KnopflerfishFrameworkStarter.class);
  }
  
  public static OSGiFrameworkStarter create(BundleRepository bundleRepository, Framework framework) {
    if (framework == null) {
      throw new RuntimeException("Framework cannot be null");
    }
    Class<? extends OSGiFrameworkStarter> frameworkStarterClass = OSGI_FRAMEWORK_STARTERS.get(framework);
    try {
      Constructor<? extends OSGiFrameworkStarter> constructor = frameworkStarterClass.getConstructor(BundleRepository.class);
      return constructor.newInstance(bundleRepository);
    } catch (Exception e) {
      throw new RuntimeException("Could not create framework starter for " + framework);
    }
  }
  
  public static OSGiFrameworkStarter create(BundleRepository bundleRepository, String frameworkName) {
    Framework framework = Framework.valueOf(frameworkName.toUpperCase());
    return create(bundleRepository, framework);
  }
}
