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

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ButtonWithDialog;
import org.totschnig.myexpenses.util.FormAccentUtilKt;

import java.math.BigDecimal;

import icepick.State;

public abstract class EditActivity extends ProtectedFragmentActivity implements TextWatcher, ButtonWithDialog.Host {

  protected boolean mIsSaving = false;
  @State
  protected boolean mIsDirty = false;
  @State
  protected boolean mNewInstance = true;

  protected BigDecimal validateAmountInput(AmountInput input, boolean showToUser) {
    return input.getTypedValue(true, showToUser);
  }

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

  protected void setupToolbarWithClose() {
    setupToolbar(true, R.drawable.ic_menu_close_clear_cancel);
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
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            mNewInstance ? getString(R.string.discard) +"?" :
                    getString(R.string.dialog_confirm_discard_changes));
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

  @Override
  public void onCurrencySelectionChanged(CurrencyUnit currencyUnit) {
    setDirty();
  }

  @Override
  public void onValueSet(@NonNull View view) {
    setDirty();
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
  protected int getSnackBarContainerId() {
    return R.id.edit_container;
  }

}