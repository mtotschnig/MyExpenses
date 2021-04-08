/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.util.FormAccentUtilKt;

import java.math.BigDecimal;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import icepick.State;

public abstract class EditActivity extends ProtectedFragmentActivity implements TextWatcher {

  private static final String KEY_IS_DIRTY = "isDirty";
  protected boolean mIsSaving = false;
  @State
  protected boolean mIsDirty = false;
  @State
  protected boolean mNewInstance = true;

  abstract int getDiscardNewMessage();

  protected BigDecimal validateAmountInput(AmountInput input, boolean showToUser) {
    return input.getTypedValue(true, showToUser);
  }

  protected abstract void setupListeners();

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {

  }

  @Override
  public void afterTextChanged(Editable s) {
    setDirty();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_IS_DIRTY)) {
      setDirty();
    }
  }

  @Override
  public void setContentView(View view) {
    super.setContentView(view);
    requireFloatingActionButton();
  }

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    requireFloatingActionButton();
  }

  protected Toolbar setupToolbar() {
    Toolbar toolbar = super.setupToolbar(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_close_clear_cancel);
    return toolbar;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home)
      if (isDirty()) {
        showDiscardDialog();
        return true;
      } else {
        hideKeyboard();
      }
    return super.onOptionsItemSelected(item);
  }

  private void showDiscardDialog() {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(
        mNewInstance ? getDiscardNewMessage() : R.string.dialog_confirm_discard_changes));
    //noinspection InlinedApi
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, android.R.id.home);
    b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes);
    b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no);
    ConfirmationDialogFragment.newInstance(b)
        .show(getSupportFragmentManager(), "DISCARD");
  }

  @Override
  public boolean dispatchCommand(int command, @Nullable Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.CREATE_COMMAND) {
      doSave(false);
      return true;
    }
    return false;
  }

  protected void doSave(boolean andNew) {
    if (!mIsSaving) {
      saveState();
    }
  }

  protected void saveState() {
    mIsSaving = true;
    startDbWriteTask();
  }

  @Override
  public void onPostExecute(@Nullable Uri result) {
    mIsSaving = false;
    super.onPostExecute(result);
  }

  @Override
  public void onBackPressed() {
    if (isDirty()) {
      showDiscardDialog();
    } else {
      dispatchOnBackPressed();
    }
  }

  protected void dispatchOnBackPressed() {
    super.onBackPressed();
  }

  protected void linkInputsWithLabels() {
    FormAccentUtilKt.linkInputsWithLabels(findViewById(R.id.Table));
  }

  public boolean isDirty() {
    return mIsDirty;
  }

  public void setDirty() {
    mIsDirty = true;
  }

  protected void clearDirty() {
    mIsDirty = false;
  }

  @Override
  protected int getSnackbarContainerId() {
    return R.id.edit_container;
  }

  protected void hideKeyboard() {
    InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    im.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
  }
}