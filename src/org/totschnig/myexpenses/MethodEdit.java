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

package org.totschnig.myexpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class MethodEdit extends Activity {
  protected static final int TYPE_DIALOG_ID = 0;
  private EditText mLabelText;
  Button mTypeButton;
  PaymentMethod mMethod;
  String[] mTypes = new String[3];
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
        
    setContentView(R.layout.one_method);

    mLabelText = (EditText) findViewById(R.id.Label);
    
    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Revert);
    
    mTypeButton = (Button) findViewById(R.id.TaType);
    mTypeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        showDialog(TYPE_DIALOG_ID);
      }
    });

    confirmButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (saveState()) {
          Intent intent=new Intent();
          intent.putExtra("method_id", mMethod.id);
          setResult(RESULT_OK,intent);
          finish();
        }
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
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
    long rowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      try {
        mMethod = PaymentMethod.getInstanceFromDb(rowId);
      } catch (DataObjectNotFoundException e) {
        e.printStackTrace();
        setResult(RESULT_CANCELED);
        finish();
      }
      setTitle(R.string.menu_edit_method);
      mLabelText.setText(mMethod.getDisplayLabel(this));
      mTypeButton.setText(mTypes[mMethod.getType()+1]);
      if (mMethod.predef != null) {
        mLabelText.setEnabled(false);
      }
    } else {
      mMethod = new PaymentMethod();
      setTitle(R.string.menu_insert_method);
    }
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case TYPE_DIALOG_ID:
        int checked = mMethod.getType() + 1;
        return new AlertDialog.Builder(this)
          .setTitle(R.string.dialog_title_select_type)
          .setSingleChoiceItems(mTypes, checked, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              mTypeButton.setText(mTypes[item]);
              mMethod.setType(item - 1 );
              dismissDialog(TYPE_DIALOG_ID);
            }
          }).create();
    }
    return null;
  }


  private boolean saveState() {
    if (mMethod.predef == null) {
      mMethod.setLabel(mLabelText.getText().toString());
    }
    mMethod.save();
    return true;
  }
}
