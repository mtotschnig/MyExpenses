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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.fragment.DbWriteFragment;

import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class EditActivity extends ProtectedFragmentActivity implements
    DbWriteFragment.TaskCallbacks, ConfirmationDialogFragment.ConfirmationDialogListener, TextWatcher {

  protected boolean mIsSaving = false, mIsDirty = false;
  protected boolean mNewInstance = true;
  private int primaryColor;
  private int accentColor;

  abstract int getDiscardNewMessage();

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {

  }

  @Override
  public void afterTextChanged(Editable s) {
    mIsDirty = true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    TypedValue typedValue = new TypedValue();
    TypedArray a = obtainStyledAttributes(typedValue.data,
        new int[] { android.R.attr.textColorSecondary });
    primaryColor = a.getColor(0, 0);
    a.recycle();
    a = obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorAccent });
    accentColor = a.getColor(0, 0);
  }

  protected Toolbar setupToolbar() {
    Toolbar toolbar = super.setupToolbar(true);
    getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
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
    if (mIsDirty && item.getItemId()==android.R.id.home) {
      showDiscardDialog();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showDiscardDialog() {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(
        mNewInstance ? getDiscardNewMessage() : R.string.dialog_confirm_discard_changes));
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, android.R.id.home);
    b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.dialog_confirm_button_discard);
    b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
    ConfirmationDialogFragment.newInstance(b)
        .show(getSupportFragmentManager(), "AUTO_FILL_HINT");
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case R.id.SAVE_COMMAND:
      if (!mIsSaving) {
        saveState();
      }
      return true;
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  public void onPositive(Bundle args) {
    dispatchCommand(args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE),null);
  }

  @Override
  public void onNegative(Bundle args) {
  }

  @Override
  public void onDismissOrCancel(Bundle args) {

  }

  protected void saveState() {
    mIsSaving = true;
    startDbWriteTask(false);
  }

  protected void changeEditTextBackground(ViewGroup root) {
    //not needed in HOLO
    if (Build.VERSION.SDK_INT > 10) {
      return;
    }
    if (MyApplication.PrefKey.UI_THEME_KEY.getString("dark").equals("dark")) {
      int c = getResources().getColor(R.color.theme_dark_button_color);
      for(int i = 0; i <root.getChildCount(); i++) {
        View v = root.getChildAt(i);
        if(v instanceof EditText) {
          Utils.setBackgroundFilter(v, c);
        } else if(v instanceof ViewGroup) {
          changeEditTextBackground((ViewGroup)v);
        }
      }
    }
  }
  @Override
  public void onPostExecute(Object result) {
    mIsSaving = false;
    super.onPostExecute(result);
  }

  @Override
  public void onBackPressed() {
    if (mIsDirty) {
      showDiscardDialog();
    } else {
      super.onBackPressed();
    }
  }
  protected void linkInputWithLabel(final View input, final View label) {
    //setting this in XML does not work for Spinners
    input.setFocusable(true);
    input.setFocusableInTouchMode(true);
    input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        ((TextView) label).setTextColor(hasFocus ? accentColor : primaryColor);
        if (hasFocus && (input instanceof Button || input instanceof Spinner)) {
          input.performClick();
        }
      }
    });
  }
}