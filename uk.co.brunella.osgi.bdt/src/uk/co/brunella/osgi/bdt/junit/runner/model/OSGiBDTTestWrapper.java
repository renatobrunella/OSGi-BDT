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

import uk.co.brunella.osgi.bdt.junit.annotation.Framework;
import uk.co.brunella.osgi.bdt.junit.annotation.Include;
import uk.co.brunella.osgi.bdt.junit.annotation.OSGiBDTTest;
import uk.co.brunella.osgi.bdt.junit.annotation.StartPolicy;


public class OSGiBDTTestWrapper {

  private final String baseDir;
  private final String manifest;
  private final Framework framework;
  private final StartPolicy frameworkStartPolicy;
  private final String[] repositories;
  private final Include[] buildIncludes;
  private final String systemBundle;
  private final String[] requiredBundles;
  private final String[] arguments;

  public OSGiBDTTestWrapper(OSGiBDTTest annotation) {
    baseDir = annotation.baseDir();
    manifest = annotation.manifest();
    framework = annotation.framework();
    frameworkStartPolicy = annotation.frameworkStartPolicy();
    repositories = annotation.repositories();
    buildIncludes = annotation.buildIncludes();
    systemBundle = annotation.systemBundle();
    requiredBundles = annotation.requiredBundles();
    arguments = annotation.arguments();
  }
  
  public OSGiBDTTestWrapper(String baseDir, String manifest, Framework framework, StartPolicy frameworkStartPolicy,
      String[] repositories, Include[] buildIncludes, String systemBundle, String[] requiredBundles, String[] arguments) {
    this.baseDir = baseDir;
    this.manifest = manifest;
    this.framework = framework;
    this.frameworkStartPolicy = frameworkStartPolicy;
    this.repositories = repositories;
    this.buildIncludes = buildIncludes;
    this.systemBundle = systemBundle;
    this.requiredBundles = requiredBundles;
    this.arguments = arguments;
  }

  public String baseDir() {
    return baseDir;
  }
  
  public String manifest() {
    return manifest;
  }

  public Framework framework() {
    return framework;
  }

  public StartPolicy frameworkStartPolicy() {
    return frameworkStartPolicy;
  }
  
  public String[] repositories() {
    return repositories;
  }
  
  public Include[] buildIncludes() {
    return buildIncludes;
  }
  
  public String systemBundle() {
    return systemBundle;
  }

  public String[] requiredBundles() {
    return requiredBundles;
  }
  
  public String[] arguments() {
    return arguments;
  }
}
