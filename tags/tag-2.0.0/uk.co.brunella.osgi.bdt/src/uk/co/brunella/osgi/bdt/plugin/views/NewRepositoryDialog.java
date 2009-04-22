/*
 * Copyright 2008 brunella ltd
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
package uk.co.brunella.osgi.bdt.plugin.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;

public class NewRepositoryDialog extends Dialog {

  private Combo combo;
  private String repositoryPath;
  private String profileName;

  public NewRepositoryDialog(Shell parentShell, String repositoryPath) {
    super(parentShell);
    this.repositoryPath = repositoryPath;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    final Label thereIsNoLabel = new Label(container, SWT.NONE);
    thereIsNoLabel.setLayoutData(new GridData());
    thereIsNoLabel.setText("There is no repository at " + repositoryPath);

    final Label pleaseSelectALabel = new Label(container, SWT.NONE);
    pleaseSelectALabel.setText("Please select a Java profile:");

    combo = new Combo(container, SWT.READ_ONLY);
    combo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent e) {
        profileName = combo.getItem(combo.getSelectionIndex());
      }
    });
    combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    String[] profileNames = BundleRepository.getProfileNameList();
    for (String profileName : profileNames) {
      combo.add(profileName);
    }
    combo.select(0);
    profileName = combo.getItem(combo.getSelectionIndex());
    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
        true);
    createButton(parent, IDialogConstants.CANCEL_ID,
        IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected Point getInitialSize() {
    return new Point(331, 174);
  }
  
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Add repository");
  }

  public String getProfileName() {
    return profileName;
  }
}
