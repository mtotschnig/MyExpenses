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

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.support.v7.widget.GridLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.ui.SpinnerHelper;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class MethodEdit extends EditActivity implements CompoundButton.OnCheckedChangeListener {
  protected static final int TYPE_DIALOG_ID = 0;
  private EditText mLabelText;
  private GridLayout mAccountTypesGrid;
  CheckBox mIsNumberedCheckBox;
  SpinnerHelper mPaymentTypeSpinner;
  PaymentMethod mMethod;
  String[] mTypes = new String[3];

  @Override
  int getDiscardNewMessage() {
    return R.string.dialog_confirm_discard_new_method;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
        
    setContentView(R.layout.one_method);
    setupToolbar();
    changeEditTextBackground((ViewGroup) findViewById(android.R.id.content));

    mLabelText = (EditText) findViewById(R.id.Label);
    mAccountTypesGrid = (GridLayout)findViewById(R.id.AccountTypeGrid);

    mPaymentTypeSpinner = new SpinnerHelper(findViewById(R.id.TaType));

    mIsNumberedCheckBox = (CheckBox) findViewById(R.id.IsNumbered);
    linkInputsWithLabels();
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
      mMethod = PaymentMethod.getInstanceFromDb(rowId);

      setTitle(R.string.menu_edit_method);
      mLabelText.setText(mMethod.getLabel());
      paymentType = mMethod.getPaymentType();
      mIsNumberedCheckBox.setChecked(mMethod.isNumbered);
    } else {
      mMethod = new PaymentMethod();
      setTitle(R.string.menu_create_method);
      paymentType = PaymentMethod.NEUTRAL;
      mNewInstance = true;
    }
    mPaymentTypeSpinner.setSelection(paymentType +1);
    //add one checkbox for each account type
    CheckBox cb;
    int cbId = 1;
    TextView accountTypesLabel = (TextView) findViewById(R.id.AccountTypesLabel);
    for (Account.Type accountType : Account.Type.values()) {
      cb = new CheckBox(this);
      cb.setText(accountType.toString());
      cb.setTag(accountType);
      cb.setChecked(mMethod.isValidForAccountType(accountType));
      //setting Id makes state be retained on orientation change 
      cb.setId(cbId);
      cb.setOnCheckedChangeListener(this);
      linkInputWithLabel(cb,accountTypesLabel);
      mAccountTypesGrid.addView(cb);
      cbId++;
    }
  }

  protected void saveState() {
    String label = mLabelText.getText().toString();
    if (label.equals("")) {
      mLabelText.setError(getString(R.string.no_title_given));
      return;
    }

    mMethod.setLabel(label);

    mMethod.setPaymentType(mPaymentTypeSpinner.getSelectedItemPosition()-1);
    for (Account.Type accountType : Account.Type.values()) {
      CheckBox cb = (CheckBox) mAccountTypesGrid.findViewWithTag(accountType);
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
  public void onPostExecute(Object result) {
    setResult(RESULT_OK);
    finish();
    //no need to call super after finish
  }
  protected void setupListeners() {
    mLabelText.addTextChangedListener(this);
    mPaymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mIsDirty = true;
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    mIsNumberedCheckBox.setOnCheckedChangeListener(this);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    mIsDirty = true;
  }

  protected void linkInputsWithLabels() {
    linkInputWithLabel(mLabelText,findViewById(R.id.LabelLabel));
    linkInputWithLabel(mPaymentTypeSpinner.getSpinner(),findViewById(R.id.TypeLabel));
    linkInputWithLabel(mIsNumberedCheckBox,findViewById(R.id.IsNumberedLabel));
  }
}
