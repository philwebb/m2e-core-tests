/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.Messages;


public class MavenPropertyDialog extends Dialog {

  private final String title;
  
  private final String[] initialValues;
  
  private final boolean variables;
  
  private Text nameText;

  private Text valueText;

  private String name;
  
  private String value;
  
  public MavenPropertyDialog(Shell shell, String title, String[] initialValues, boolean variables) {
    super(shell);
    this.title = title;
    this.initialValues = initialValues;
    this.variables = variables;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  protected Control createDialogArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    comp.setLayout(gridLayout);

    Label nameLabel = new Label(comp, SWT.NONE);
    nameLabel.setText(Messages.getString("launch.propertyDialog.name")); //$NON-NLS-1$;
    nameLabel.setFont(comp.getFont());

    nameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    nameText.setLayoutData(gd);
    nameText.setFont(comp.getFont());
    if(initialValues.length >= 1) {
      nameText.setText(initialValues[0]);
    }
    nameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateButtons();
      }
    });

    Label valueLabel = new Label(comp, SWT.NONE);
    valueLabel.setText(Messages.getString("launch.propertyDialog.value")); //$NON-NLS-1$;
    valueLabel.setFont(comp.getFont());

    valueText = new Text(comp, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    valueText.setLayoutData(gd);
    valueText.setFont(comp.getFont());
    if(initialValues.length >= 2) {
      valueText.setText(initialValues[1]);
    }
    valueText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateButtons();
      }
    });

    if(variables) {
      Button variablesButton = new Button(comp, SWT.PUSH);
      variablesButton.setText(Messages.getString("launch.propertyDialog.browseVariables")); //$NON-NLS-1$;
      gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
      gd.horizontalSpan = 2;
      int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
      gd.widthHint = Math.max(widthHint, variablesButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
      variablesButton.setLayoutData(gd);
      variablesButton.setFont(comp.getFont());
  
      variablesButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent se) {
          getVariable();
        }
      });
    }
    
    return comp;
  }

  protected void getVariable() {
    StringVariableSelectionDialog variablesDialog = new StringVariableSelectionDialog(getShell());
    int returnCode = variablesDialog.open();
    if(returnCode == IDialogConstants.OK_ID) {
      String variable = variablesDialog.getVariableExpression();
      if(variable != null) {
        valueText.insert(variable.trim());
      }
    }
  }

  /**
   * Return the name/value pair entered in this dialog. If the cancel button was hit, both will be <code>null</code>.
   */
  public String[] getNameValuePair() {
    return new String[] {name, value};
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
   */
  protected void buttonPressed(int buttonId) {
    if(buttonId == IDialogConstants.OK_ID) {
      name = nameText.getText();
      value = valueText.getText();
    } else {
      name = null;
      value = null;
    }
    super.buttonPressed(buttonId);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if(title != null) {
      shell.setText(title);
    }
//    if (fInitialValues[0].length() == 0) {
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.ADD_PROPERTY_DIALOG);
//    } else {
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.EDIT_PROPERTY_DIALOG);
//    }
  }

  /**
   * Enable the OK button if valid input
   */
  protected void updateButtons() {
    String name = nameText.getText().trim();
    String value = valueText.getText().trim();
    getButton(IDialogConstants.OK_ID).setEnabled((name.length() > 0) && (value.length() > 0));
  }

  /**
   * Enable the buttons on creation.
   * 
   * @see org.eclipse.jface.window.Window#create()
   */
  public void create() {
    super.create();
    updateButtons();
  }
}