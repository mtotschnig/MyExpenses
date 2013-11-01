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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.CheckBox;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class MethodEdit extends EditActivity {
  protected static final int TYPE_DIALOG_ID = 0;
  private EditText mLabelText;
  private TableLayout mTable;
  Button mPaymentTypeButton;
  PaymentMethod mMethod;
  private int mPaymentType;
  String[] mTypes = new String[3];
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
        
    setContentView(R.layout.one_method);
    changeEditTextBackground((ViewGroup)findViewById(android.R.id.content));

    mLabelText = (EditText) findViewById(R.id.Label);
    mTable = (TableLayout)findViewById(R.id.Table);
    
    
    mPaymentTypeButton = (Button) findViewById(R.id.TaType);
    mPaymentTypeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        showDialog(TYPE_DIALOG_ID);
      }
    });

    mTypes[0] = getString(R.string.pm_type_debit);
    mTypes[1] = getString(R.string.pm_type_neutral);
    mTypes[2] = getString(R.string.pm_type_credit);
    
    populateFields();
  }
  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {
    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      mMethod = PaymentMethod.getInstanceFromDb(rowId);

      setTitle(R.string.menu_edit_method);
      mLabelText.setText(mMethod.getDisplayLabel());
      mPaymentType = mMethod.getPaymentType();
      mPaymentTypeButton.setText(mTypes[mPaymentType+1]);
      if (mMethod.predef != null) {
        mLabelText.setFocusable(false);
        mLabelText.setEnabled(false);
      }
    } else {
      mMethod = new PaymentMethod();
      setTitle(R.string.menu_create_method);
    }
    //add one row with checkbox for each account type
    TableRow tr;
    TextView tv;
    CheckBox cb;
    int cbId = 1;
    for (Account.Type accountType : Account.Type.values()) {
      /* Create a new row to be added. */
     tr = new TableRow(this);
  /*    tr.setLayoutParams(new LayoutParams(
                     LayoutParams.FILL_PARENT,
                     LayoutParams.WRAP_CONTENT));*/
           /* Create a Button to be the row-content. */
      tv = new TextView(this);
      tv.setText(accountType.getDisplayName());
      tv.setTextAppearance(this, R.style.form_label);
      cb = new CheckBox(this);
      cb.setTag(accountType);
      cb.setChecked(mMethod.isValidForAccountType(accountType));
      //setting Id makes state be retained on orientation change 
      cb.setId(cbId);
      tr.addView(tv);
      tr.addView(cb);
      mTable.addView(tr);
      cbId++;
    }
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case TYPE_DIALOG_ID:
        int checked = mMethod.getPaymentType() + 1;
        return new AlertDialog.Builder(this)
          .setTitle(R.string.dialog_title_select_type)
          .setSingleChoiceItems(mTypes, checked, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              mPaymentTypeButton.setText(mTypes[item]);
              mPaymentType = item - 1 ;
              dismissDialog(TYPE_DIALOG_ID);
            }
          }).create();
    }
    return null;
  }

  protected void saveState() {
    if (mMethod.predef == null) {
      mMethod.setLabel(mLabelText.getText().toString());
    }
    mMethod.setPaymentType(mPaymentType);
    for (Account.Type accountType : Account.Type.values()) {
      CheckBox cb = (CheckBox) mTable.findViewWithTag(accountType);
      if (cb.isChecked()) {
        mMethod.addAccountType(accountType);
      } else {
        mMethod.removeAccountType(accountType);
      }
    }
    //EditActivity.saveState calls DbWriteFragment
    super.saveState();
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("type", mPaymentType);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mPaymentType = savedInstanceState.getInt("type");
    mPaymentTypeButton.setText(mTypes[mPaymentType+1]);
  }
  @Override
  public Model getObject() {
    // TODO Auto-generated method stub
    return mMethod;
  }
}
