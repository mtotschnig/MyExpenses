/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

import java.text.NumberFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for editing a transaction
 * @author Michael Totschnig
 */
public class ExpenseEdit extends Activity {

  private Button mDateButton;
  private Button mTimeButton;
  private EditText mAmountText;
  private EditText mCommentText;
  private Button mCategoryButton;
  private Button mTypeButton;
  private AutoCompleteTextView mPayeeText;
  private TextView mPayeeLabel;
  private long mRowId;
  private long mAccountId;
  private ExpensesDbAdapter mDbHelper;
  private int mYear;
  private int mMonth;
  private int mDay;
  private int mHours;
  private int mMinutes;
  private Transaction mTransaction;
  
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  private boolean mType = EXPENSE;
  //normal transaction or transfer
  private boolean mOperationType;
  private NumberFormat nfDLocal;

  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;
  
/*  private int monkey_state = 0;

  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENVELOPE) {
      switch (monkey_state) {
      case 0:
        mAmountText.setText("50");
        setResult(RESULT_OK);
        saveState();
        finish();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }*/

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = MyApplication.db();
    nfDLocal = NumberFormat.getNumberInstance();
    nfDLocal.setGroupingUsed(false);

    Bundle extras = getIntent().getExtras();
    mRowId = extras.getLong(ExpensesDbAdapter.KEY_ROWID,0);
    mAccountId = extras.getLong(ExpensesDbAdapter.KEY_ACCOUNTID);
    mOperationType = extras.getBoolean("operationType");
    
    setContentView(R.layout.one_expense);

    mDateButton = (Button) findViewById(R.id.Date);
    mDateButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(DATE_DIALOG_ID);
      }
    });

    mTimeButton = (Button) findViewById(R.id.Time);
    mTimeButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(TIME_DIALOG_ID);
      }
    });

    mAmountText = (EditText) findViewById(R.id.Amount);
    mCommentText = (EditText) findViewById(R.id.Comment);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Cancel);
    
    
    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mPayeeLabel = (TextView) findViewById(R.id.PayeeLabel);
      Cursor allPayees = mDbHelper.fetchPayeeAll();
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
          android.R.layout.simple_dropdown_item_1line);
      allPayees.moveToFirst();
      while(!allPayees.isAfterLast()) {
           adapter.add(allPayees.getString(allPayees.getColumnIndex("name")));
           allPayees.moveToNext();
      }
      allPayees.close();
      mPayeeText = (AutoCompleteTextView) findViewById(R.id.Payee);
      mPayeeText.setAdapter(adapter);
    } else {
      View v = findViewById(R.id.PayeeRow);
      v.setVisibility(View.GONE);
    }
    
    confirmButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        setResult(RESULT_OK);
        if (saveState())
          finish();
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        finish();
      }
    });
    mCategoryButton = (Button) findViewById(R.id.Category);
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      //if there is a label for the category input (portrait), we adjust it,
      //otherwise directly the button (landscape)
      TextView categoryLabel = (TextView) findViewById(R.id.CategoryLabel);
      if (categoryLabel != null)
        categoryLabel.setText(R.string.account);
      else
        mCategoryButton.setText(R.string.account);
    }
    mCategoryButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
          startSelectCategory();
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(ExpenseEdit.this);
          builder.setTitle(R.string.dialog_title_select_account);
          final Cursor otherAccounts = mDbHelper.fetchAccountOtherWithCurrency(mTransaction.account_id);
          final String[] accounts = new String[otherAccounts.getCount()];
          if(otherAccounts.moveToFirst()){
           for (int i = 0; i < otherAccounts.getCount(); i++){
             accounts[i] = otherAccounts.getString(otherAccounts.getColumnIndex("label"));
             otherAccounts.moveToNext();
           }
          }
          builder.setSingleChoiceItems(accounts, -1, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int item) {
                otherAccounts.moveToPosition(item);
                mTransaction.cat_id = otherAccounts.getLong(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
                mCategoryButton.setText(
                    (mType == EXPENSE ? MyExpenses.TRANSFER_EXPENSE  : MyExpenses.TRANSFER_EXPENSE) + 
                    accounts[item]
                );
                otherAccounts.close();
                dialog.cancel();
              }
          });
          builder.show();
        }
      }
    });
    mTypeButton = (Button) findViewById(R.id.TaType);
    mTypeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        toggleType();
      } 
    });
    populateFields();
  }

  /**
   * calls the activity for selecting (and managing) categories
   */
  private void startSelectCategory() {
    Intent i = new Intent(this, SelectCategory.class);
    //i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    startActivityForResult(i, 0);
  }
  /**
   * listens on changes in the date dialog and sets the date on the button
   */
  private DatePickerDialog.OnDateSetListener mDateSetListener =
    new DatePickerDialog.OnDateSetListener() {

    public void onDateSet(DatePicker view, int year, 
        int monthOfYear, int dayOfMonth) {
      mYear = year;
      mMonth = monthOfYear;
      mDay = dayOfMonth;
      setDate();
    }
  };
  /**
   * listens on changes in the time dialog and sets the time on hte button
   */
  private TimePickerDialog.OnTimeSetListener mTimeSetListener =
    new TimePickerDialog.OnTimeSetListener() {
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
      mHours = hourOfDay;
      mMinutes = minute;
      setTime();
    }
  };
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case DATE_DIALOG_ID:
      return new DatePickerDialog(this,
          mDateSetListener,
          mYear, mMonth, mDay);
    case TIME_DIALOG_ID:
      return new TimePickerDialog(this,
          mTimeSetListener,
          mHours, mMinutes, true);
    }
    return null;
  }
  
  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {
    //TableLayout mScreen = (TableLayout) findViewById(R.id.Table);
    if (mRowId != 0) {
      mTransaction = Transaction.getInstanceFromDb(mDbHelper, mRowId);
      float amount;
      if (mTransaction.amount < 0) {
        amount = 0 - mTransaction.amount;
      } else {
        amount = mTransaction.amount;
        toggleType();
      }      
      mAmountText.setText(nfDLocal.format(amount));
      mCommentText.setText(mTransaction.comment);
      if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
        setTitle(R.string.menu_edit_ta);
        mPayeeText.setText(mTransaction.payee);
      } else {
        setTitle(R.string.menu_edit_transfer);
      }
      String label =  mTransaction.label;
      if (label != null && label.length() != 0) {
        if (mOperationType == MyExpenses.TYPE_TRANSFER)
          label = (mType == EXPENSE ? 
              MyExpenses.TRANSFER_EXPENSE : 
              MyExpenses.TRANSFER_EXPENSE) 
              + label;
        mCategoryButton.setText(label);
      }
    } else {
      mTransaction = Transaction.getTypedNewInstance(mDbHelper,mOperationType);
      mTransaction.account_id = mAccountId;
      setTitle(mOperationType == MyExpenses.TYPE_TRANSFER ? 
          R.string.menu_insert_transfer : R.string.menu_insert_ta);
    }
    setDateTime(mTransaction.date);
    
  }
  /**
   * extracts the fields from a date object for setting them on the buttons
   * @param date
   */
  private void setDateTime(Date date) {
    mYear = date.getYear()+1900;
    mMonth = date.getMonth();
    mDay = date.getDate();
    mHours = date.getHours();
    mMinutes = date.getMinutes();

    setDate();
    setTime();
  }
  /**
   * sets date on date button
   */
  private void setDate() {
    mDateButton.setText(mYear + "-" + pad(mMonth + 1) + "-" + pad(mDay));
  }
  
  /**
   * sets time on time button
   */
  private void setTime() {
    mTimeButton.setText(pad(mHours) + ":" + pad(mMinutes));
  }
  /**
   * helper for padding integer values smaller than 10 with 0
   * @param c
   * @return
   */
  private static String pad(int c) {
    if (c >= 10)
      return String.valueOf(c);
    else
      return "0" + String.valueOf(c);
  }

  /**
   * validates (is number interpretable as float in current locale,
   * is account selected for transfers) and saves
   * @return true upon success, false if validation fails
   */
  private boolean saveState() {
    String strAmount = mAmountText.getText().toString();
    Float amount = Utils.validateNumber(strAmount);
    if (amount == null) {
      Toast.makeText(this,getString(R.string.invalid_number_format,nfDLocal.format(11.11)), Toast.LENGTH_LONG).show();
      return false;
    }
    if (mType == EXPENSE) {
      amount = 0 - amount;
    }
    
    mTransaction.amount = amount;
    mTransaction.comment = mCommentText.getText().toString();
    mTransaction.setDate(mDateButton.getText().toString() + 
        " " + mTimeButton.getText().toString() + ":00.0");

    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mTransaction.setPayee(mPayeeText.getText().toString());
    } else {
      if (mTransaction.cat_id == 0) {
        Toast.makeText(this,getString(R.string.warning_select_account), Toast.LENGTH_LONG).show();
        return false;
      }
    }
    mTransaction.save();
    return true;
  }
  /* (non-Javadoc)
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    if (intent != null) {
      mTransaction.cat_id = intent.getLongExtra("cat_id",0);
      mCategoryButton.setText(intent.getStringExtra("label"));
    }
  }
  /**
   * updates interface if type is toggled between EXPENSE and INCOME
   */
  private void toggleType() {
    mType = ! mType;
    mTypeButton.setText(mType ? "+" : "-");
    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mPayeeLabel.setText(mType ? R.string.payer : R.string.payee);
    }
  }
}
