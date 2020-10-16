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

import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.ui.AmountInput;

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
  private int primaryColor;
  private int accentColor;

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
    TypedValue typedValue = new TypedValue();
    TypedArray a = obtainStyledAttributes(typedValue.data,
        new int[]{android.R.attr.textColorSecondary});
    primaryColor = a.getColor(0, 0);
    a.recycle();
    a = obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
    accentColor = a.getColor(0, 0);
    a.recycle();
    if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_IS_DIRTY)) {
      setDirty();
    }
  }

  protected Toolbar setupToolbar() {
    Toolbar toolbar = super.setupToolbar(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_close_clear_cancel);
    return toolbar;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.one, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (isDirty() && item.getItemId() == android.R.id.home) {
      showDiscardDialog();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showDiscardDialog() {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(
        mNewInstance ? getDiscardNewMessage() : R.string.dialog_confirm_discard_changes));
    //noinspection InlinedApi
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, android.R.id.home);
    b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes);
    b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no);
    ConfirmationDialogFragment.newInstance(b)
        .show(getSupportFragmentManager(), "DISCARD");
  }

  @Override
  public boolean dispatchCommand(int command, @Nullable Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    switch (command) {
      case R.id.SAVE_COMMAND:
        doSave(false);
        return true;
      case R.id.SAVE_AND_NEW_COMMAND:
        doSave(true);
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

  public void linkInputWithLabel(final View input, final View label) {
    setOnFocusChangeListenerRecursive(input, (v, hasFocus) ->
        ((TextView) label).setTextColor(hasFocus ? accentColor : primaryColor));
  }

  private void setOnFocusChangeListenerRecursive(View view, View.OnFocusChangeListener listener) {
    if (view instanceof ViewGroup && !view.isFocusable()) {
      ViewGroup group = ((ViewGroup) view);
      for (int i = 0; i < group.getChildCount(); i++) {
        setOnFocusChangeListenerRecursive(group.getChildAt(i), listener);
      }
    } else {
      view.setOnFocusChangeListener(listener);
    }
  }

  public boolean isDirty() {
    return mIsDirty;
  }

  private void setDirty(boolean isDirty) {
    this.mIsDirty = isDirty;
  }

  public void setDirty() {
    setDirty(true);
  }

  @Override
  protected int getSnackbarContainerId() {
    return R.id.edit_container;
  }
}