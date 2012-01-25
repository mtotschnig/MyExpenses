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

import java.sql.Timestamp;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;

public class ExpenseEdit extends Activity {

  private Button mDateButton;
  private Button mTimeButton;
  private EditText mAmountText;
  private EditText mCommentText;
  private Button mCategoryButton;
  private Button mTypeButton;
  private AutoCompleteTextView mPayeeText;
  private TextView mPayeeLabel;
  private Long mRowId;
  private int mAccountId;
  private ExpensesDbAdapter mDbHelper;
  //for transfers mCatId stores the peer account
  private int mCatId;
  private int mYear;
  private int mMonth;
  private int mDay;
  private int mHours;
  private int mMinutes;
  
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  private boolean mType = EXPENSE;
  //normal transaction or transfer
  private boolean mOperationType;

  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();

    Bundle extras = getIntent().getExtras();
    mRowId = extras.getLong(ExpensesDbAdapter.KEY_ROWID,0);
    mAccountId = extras.getInt(ExpensesDbAdapter.KEY_ACCOUNTID);
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
      mCategoryButton.setText(R.string.account);
    }
    mCategoryButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
          startSelectCategory();
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(ExpenseEdit.this);
          builder.setTitle("Pick an account");
          final Cursor otherAccounts = mDbHelper.fetchAccountOtherWithCurrency(mAccountId);
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
                mCatId = otherAccounts.getInt(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
                mCategoryButton.setText("=>" + accounts[item]);
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
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
  }
  private void startSelectCategory() {
    Intent i = new Intent(this, SelectCategory.class);
    //i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    startActivityForResult(i, 0);
  }
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
  private void populateFields() {
    float amount;
    //TableLayout mScreen = (TableLayout) findViewById(R.id.Table);
    if (mRowId != 0) {
      setTitle(R.string.menu_edit_ta);
      Cursor note = mDbHelper.fetchExpense(mRowId);
      startManagingCursor(note);
      String dateString = note.getString(
          note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE));
      Timestamp date = Timestamp.valueOf(dateString);
      setDateTime(date);
      try {
        amount = Float.valueOf(note.getString(
            note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)));
      } catch (NumberFormatException e) {
        amount = 0;
      }
      if (amount < 0) {
        amount = 0 - amount;
      } else {
        toggleType();
      }

      
      mAmountText.setText(Float.toString(amount));
      mCommentText.setText(note.getString(
          note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)));
      if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
        mPayeeText.setText(note.getString(
            note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE)));
      }
      mCatId = note.getInt(note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
      String label =  note.getString(note.getColumnIndexOrThrow("label"));
      if (label != null && label.length() != 0) {
        mCategoryButton.setText(label);
      }
    } else {
      Date date =  new Date();
      setDateTime(date);
      setTitle(R.string.menu_insert_ta);
    }
  }
  private void setDateTime(Date date) {
    mYear = date.getYear()+1900;
    mMonth = date.getMonth();
    mDay = date.getDate();
    mHours = date.getHours();
    mMinutes = date.getMinutes();

    setDate();
    setTime();
  }
  private void setDate() {
    mDateButton.setText(mYear + "-" + pad(mMonth + 1) + "-" + pad(mDay));
  }
  private void setTime() {
    mTimeButton.setText(pad(mHours) + ":" + pad(mMinutes));
  }
  private static String pad(int c) {
    if (c >= 10)
      return String.valueOf(c);
    else
      return "0" + String.valueOf(c);
  }

//  //I am not sure if this needed
//  @Override
//  protected void onSaveInstanceState(Bundle outState) {
//    outState.putLong(ExpensesDbAdapter.KEY_ROWID, mRowId);
//    super.onSaveInstanceState(outState);
//  }

  private boolean saveState() {
    long id;
    float amount;
    try {
      amount = Float.valueOf(mAmountText.getText().toString());
    } catch (NumberFormatException e) {
      amount = 0;
    }
    String comment = mCommentText.getText().toString();
    String strDate = mDateButton.getText().toString() + " " + mTimeButton.getText().toString() + ":00.0";
    if (mType == EXPENSE) {
      amount = 0 - amount;
    }
    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      String payee = mPayeeText.getText().toString();
      mDbHelper.createPayeeOrIgnore(payee);
      if (mRowId == 0) {
        id = mDbHelper.createExpense(strDate, amount, comment,mCatId,mAccountId,payee);
        if (id > 0) {
          mRowId = id;
        }
      } else {
        mDbHelper.updateExpense(mRowId, strDate, amount, comment,mCatId,payee);
      }
    } else {
      if (mRowId == 0) {
        if (mCatId == 0) {
          Toast.makeText(this,getString(R.string.warning_select_account), Toast.LENGTH_LONG).show();
          return false;
        }
        id = mDbHelper.createTransfer(strDate, amount, comment,mCatId,mAccountId);
        if (id > 0) {
          mRowId = id;
        }
      } else {
        mDbHelper.updateTransfer(mRowId, strDate, amount, comment,mCatId);
      }
    }
    return true;
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    if (intent != null) {
      //Here we will have to set the category for the expense
      mCatId = intent.getIntExtra("cat_id",0);
      mCategoryButton.setText(intent.getStringExtra("label"));
    }
  }
  private void toggleType() {
    mType = ! mType;
    mTypeButton.setText(mType ? "+" : "-");
    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mPayeeLabel.setText(mType ? R.string.payer : R.string.payee);
    }
  }
}
