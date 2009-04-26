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

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;

public class KnopflerfishFrameworkStarter extends AbstractOSGiFrameworkStarter {

  public KnopflerfishFrameworkStarter(BundleRepository bundleRepository) {
    super(bundleRepository);
  }

  public String systemBundleName() {
    throw new RuntimeException("not supported yet");
  }

  public String[] defaultArguments() {
    throw new RuntimeException("not supported yet");
  }

  public void startFramework(String systemBundleName, String[] arguments) throws Exception {
    throw new RuntimeException("not supported yet");    
  }

  public void stopFramework() throws Exception {
    throw new RuntimeException("not supported yet");    
  }
 }
