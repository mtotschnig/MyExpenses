/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.fragment.FolderList;

public class FolderBrowser extends ProtectedFragmentActivity implements
    EditTextDialogListener {

  public static final String PATH = "PATH";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.folder_browser);
    setupToolbar(true);
  }

  @Override
  public void onFinishEditDialog(Bundle args) {
    ((FolderList) getSupportFragmentManager().findFragmentById(R.id.folder_list))
        .createNewFolder(args.getString(EditTextDialog.KEY_RESULT));
  }

  @Override
  public void onCancelEditDialog() {
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    setResult(RESULT_CANCELED);
    finish();
  }
}
