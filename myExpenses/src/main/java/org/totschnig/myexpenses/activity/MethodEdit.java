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
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.ui.SpinnerHelper;

import java.util.NoSuchElementException;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.gridlayout.widget.GridLayout;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class MethodEdit extends EditActivity implements CompoundButton.OnCheckedChangeListener {
  private EditText mLabelText;
  private GridLayout mAccountTypesGrid;
  private CheckBox mIsNumberedCheckBox;
  private SpinnerHelper mPaymentTypeSpinner;
  private PaymentMethod mMethod;

  @Override
  int getDiscardNewMessage() {
    return R.string.dialog_confirm_discard_new_method;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
        
    setContentView(R.layout.one_method);
    setupToolbar();

    mLabelText = findViewById(R.id.Label);
    mAccountTypesGrid = findViewById(R.id.AccountTypeGrid);

    mPaymentTypeSpinner = new SpinnerHelper(findViewById(R.id.TaType));

    mIsNumberedCheckBox = findViewById(R.id.IsNumbered);
    populateFields();
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupListeners();
  }

  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {
    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
          : 0;
    int paymentType;
    if (rowId != 0) {
      mNewInstance = false;
      mMethod = PaymentMethod.getInstanceFromDb(rowId);

      setTitle(R.string.menu_edit_method);
      mLabelText.setText(mMethod.getLabel());
      paymentType = mMethod.getPaymentType();
      mIsNumberedCheckBox.setChecked(mMethod.isNumbered);
    } else {
      mMethod = new PaymentMethod();
      setTitle(R.string.menu_create_method);
      paymentType = PaymentMethod.NEUTRAL;
    }
    mPaymentTypeSpinner.setSelection(paymentType +1);
    //add one checkbox for each account type
    AppCompatCheckBox cb;
    for (AccountType accountType : AccountType.values()) {
      cb = new AppCompatCheckBox(this);
      cb.setText(accountType.toStringRes());
      cb.setTag(accountType);
      cb.setChecked(mMethod.isValidForAccountType(accountType));
      //setting Id makes state be retained on orientation change 
      cb.setId(getCheckBoxId(accountType));
      cb.setOnCheckedChangeListener(this);
      mAccountTypesGrid.addView(cb);
    }
    linkInputsWithLabels();
  }

  private @IdRes int getCheckBoxId(AccountType accountType) {
    switch (accountType) {
      case CASH:
        return R.id.AccountTypeCheckboxCash;
      case BANK:
        return R.id.AccountTypeCheckboxBank;
      case CCARD:
        return R.id.AccountTypeCheckboxCcard;
      case ASSET:
        return R.id.AccountTypeCheckboxAsset;
      case LIABILITY:
        return R.id.AccountTypeCheckboxLiability;
    }
    throw new NoSuchElementException();
  }

  protected void saveState() {
    String label = mLabelText.getText().toString();
    if (label.equals("")) {
      mLabelText.setError(getString(R.string.no_title_given));
      return;
    }

    mMethod.setLabel(label);

    mMethod.setPaymentType(mPaymentTypeSpinner.getSelectedItemPosition()-1);
    for (AccountType accountType : AccountType.values()) {
      CheckBox cb = mAccountTypesGrid.findViewWithTag(accountType);
      if (cb.isChecked()) {
        mMethod.addAccountType(accountType);
      } else {
        mMethod.removeAccountType(accountType);
      }
    }
    mMethod.isNumbered = mIsNumberedCheckBox.isChecked();
    //EditActivity.saveState calls DbWriteFragment
    super.saveState();
  }
  @Override
  public Model getObject() {
    return mMethod;
  }
  @Override
  public void onPostExecute(Uri result) {
    setResult(RESULT_OK);
    finish();
    //no need to call super after finish
  }
  protected void setupListeners() {
    mLabelText.addTextChangedListener(this);
    mPaymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setDirty();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    mIsNumberedCheckBox.setOnCheckedChangeListener(this);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setDirty();
  }
}
