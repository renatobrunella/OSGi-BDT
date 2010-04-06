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
package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.util.List;

import org.osgi.framework.Bundle;

import uk.co.brunella.osgi.bdt.framework.BundleContextWrapper;
import uk.co.brunella.osgi.bdt.junit.runner.model.FrameworkField;

public class InjectBundleContextStatement extends Statement {

  private final Bundle fTestBundle;
  private final Statement fNext;
  private final Object fTarget;
  private final List<FrameworkField> fFields;

  public InjectBundleContextStatement(Bundle testBundle, Statement next, List<FrameworkField> fields, Object target) {
    fTestBundle = testBundle;
    fNext = next;
    fFields = fields;
    fTarget = target;
  }

  @Override
  public void evaluate() throws Throwable {
    if (fFields.size() > 0) {
      Object bundleContext = fTestBundle.getBundleContext();
      if (bundleContext instanceof BundleContextWrapper) {
        bundleContext = ((BundleContextWrapper) bundleContext).unwrap();
      }
      for (FrameworkField field : fFields) {
        field.set(fTarget, bundleContext);
      }
    }
    fNext.evaluate();
  }

}