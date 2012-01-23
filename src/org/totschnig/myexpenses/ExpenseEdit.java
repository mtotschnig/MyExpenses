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
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

public class ExpenseEdit extends Activity {

  private Button DateButton;
  private Button TimeButton;
  private EditText mAmountText;
  private EditText mCommentText;
  private Button categoryButton;
  private Button typeButton;
  private AutoCompleteTextView mPayeeText;
  private TextView PayeeLabel;
  private Long mRowId;
  private int mAccountId;
  private ExpensesDbAdapter mDbHelper;
  private int cat_id;
  private int mYear;
  private int mMonth;
  private int mDay;
  private int mHours;
  private int mMinutes;
  
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  private boolean type = EXPENSE;

  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();

    setContentView(R.layout.one_expense);

    PayeeLabel = (TextView) findViewById(R.id.PayeeLabel);
    DateButton = (Button) findViewById(R.id.Date);
    DateButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(DATE_DIALOG_ID);
      }
    });

    TimeButton = (Button) findViewById(R.id.Time);
    TimeButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(TIME_DIALOG_ID);
      }
    });
    mAmountText = (EditText) findViewById(R.id.Amount);
    mCommentText = (EditText) findViewById(R.id.Comment);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Cancel);
    
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

    mRowId = savedInstanceState != null ? savedInstanceState.getLong(ExpensesDbAdapter.KEY_ROWID) 
        : null;
    Bundle extras = getIntent().getExtras();
    if (mRowId == null) {
      mRowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID) 
          : 0;
      if (extras != null) {
        mAccountId = extras.getInt(ExpensesDbAdapter.KEY_ACCOUNTID);
      }
    }

    confirmButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        setResult(RESULT_OK);
        saveState();
        finish();
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        finish();
      }
    });
    categoryButton = (Button) findViewById(R.id.Category);
    categoryButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        startSelectCategory();
      } 
    });
    typeButton = (Button) findViewById(R.id.TaType);
    typeButton.setOnClickListener(new View.OnClickListener() {

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
      mPayeeText.setText(note.getString(
          note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE)));
      cat_id = note.getInt(note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
      String label =  note.getString(note.getColumnIndexOrThrow("label"));
      if (label != null && label.length() != 0) {
        categoryButton.setText(label);
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
    DateButton.setText(mYear + "-" + pad(mMonth + 1) + "-" + pad(mDay));
  }
  private void setTime() {
    TimeButton.setText(pad(mHours) + ":" + pad(mMinutes));
  }
  private static String pad(int c) {
    if (c >= 10)
      return String.valueOf(c);
    else
      return "0" + String.valueOf(c);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(ExpensesDbAdapter.KEY_ROWID, mRowId);
  }

  private void saveState() {
    String amount = mAmountText.getText().toString();
    String comment = mCommentText.getText().toString();
    String strDate = DateButton.getText().toString() + " " + TimeButton.getText().toString() + ":00.0";
    String payee = mPayeeText.getText().toString();
    if (type == EXPENSE) {
      amount = "-"+ amount;
    }
    if (mRowId == 0) {
      long id = mDbHelper.createExpense(strDate, amount, comment,String.valueOf(cat_id),String.valueOf(mAccountId),payee);
      if (id > 0) {
        mRowId = id;
      }
    } else {
      mDbHelper.updateExpense(mRowId, strDate, amount, comment,String.valueOf(cat_id),payee);
    }
    mDbHelper.createPayeeOrIgnore(payee);
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    if (intent != null) {
      //Here we will have to set the category for the expense
      cat_id = intent.getIntExtra("cat_id",0);
      categoryButton.setText(intent.getStringExtra("label"));
    }
  }
  private void toggleType() {
    type = ! type;
    typeButton.setText(type ? "+" : "-");
    PayeeLabel.setText(type ? R.string.payer : R.string.payee);
  }
}
