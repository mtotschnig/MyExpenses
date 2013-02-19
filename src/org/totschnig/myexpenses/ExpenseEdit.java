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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
public class ExpenseEdit extends EditActivity {

  private Button mDateButton;
  private Button mTimeButton;
  private EditText mCommentText;
  private Button mCategoryButton;
  private Button mMethodButton;
  private Button mTypeButton;
  private AutoCompleteTextView mPayeeText;
  private TextView mPayeeLabel;
  private long mRowId;
  private long mTemplateId;
  private long mAccountId;
  private Account mAccount;
  private ExpensesDbAdapter mDbHelper;
  private Calendar mCalendar = Calendar.getInstance();
  private final java.text.DateFormat mTitleDateFormat = java.text.DateFormat.
      getDateInstance(java.text.DateFormat.FULL);
  private long mCatId;
  private long mMethodId = 0;
  private String mLabel;
  private Transaction mTransaction;
  
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  private boolean mType = EXPENSE;
  //normal transaction or transfer
  private boolean mOperationType;


  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;
  static final int ACCOUNT_DIALOG_ID = 2;
  static final int METHOD_DIALOG_ID = 3;
  
/*  private int monkey_state = 0;

  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event) {
    if (keyCode == MyApplication.BACKDOOR_KEY) {
      switch (monkey_state) {
      case 0:
        mAmountText.setText("50");
        monkey_state = 1;
        return true;
      case 1:
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

    Bundle extras = getIntent().getExtras();
    mRowId = extras.getLong(ExpensesDbAdapter.KEY_ROWID,0);
    mTemplateId = extras.getLong("template_id",0);
    
    //1. fetch the transaction or create a new instance
    if (mRowId != 0) {
      mTransaction = Transaction.getInstanceFromDb(mRowId);
      mAccountId = mTransaction.accountId;
      mOperationType = mTransaction.transfer_peer == 0;
    } else if (mTemplateId != 0) {
      mTransaction = Transaction.getInstanceFromTemplate(mTemplateId);
      mAccountId = mTransaction.accountId;
      mOperationType = mTransaction.transfer_peer == 0;
    } else {
      mOperationType = extras.getBoolean("operationType");
      mAccountId = extras.getLong(ExpensesDbAdapter.KEY_ACCOUNTID);
      mTransaction = Transaction.getTypedNewInstance(mOperationType,mAccountId);
    }
    
    setContentView(R.layout.one_expense);
    changeEditTextBackground((ViewGroup)findViewById(android.R.id.content));
    MyApplication.updateUIWithAccountColor(this);
    configAmountInput();

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

    mCommentText = (EditText) findViewById(R.id.Comment);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Revert);
    
    
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
      findViewById(R.id.PayeeRow).setVisibility(View.GONE);
      View MethodContainer = findViewById(R.id.MethodRow);
      if (MethodContainer == null)
        MethodContainer = findViewById(R.id.Method);
      MethodContainer.setVisibility(View.GONE);
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
        
    mTypeButton = (Button) findViewById(R.id.TaType);
    mTypeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        mType = ! mType;
        //we need to empty payment method, since they are different for expenses and incomes
        if (mMethodButton != null) {
          mMethodId = 0;
          mMethodButton.setText(R.string.select);
        }
        configureType();
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
    } else {
      mMethodButton = (Button) findViewById(R.id.Method);
      mMethodButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
          showDialog(METHOD_DIALOG_ID);
        }
      });
    }
    //category button and amount label are further set up in populateFields, since it depends on data
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
      mCalendar.set(year, monthOfYear, dayOfMonth);
      setDate();
    }
  };
  /**
   * listens on changes in the time dialog and sets the time on hte button
   */
  private TimePickerDialog.OnTimeSetListener mTimeSetListener =
    new TimePickerDialog.OnTimeSetListener() {
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
      mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
      mCalendar.set(Calendar.MINUTE,minute);
      setTime();
    }
  };
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case DATE_DIALOG_ID:
      return new DatePickerDialog(this,
          mDateSetListener,
          mCalendar.get(Calendar.YEAR),
          mCalendar.get(Calendar.MONTH),
          mCalendar.get(Calendar.DAY_OF_MONTH)
      );
    case TIME_DIALOG_ID:
      return new TimePickerDialog(this,
          mTimeSetListener,
          mCalendar.get(Calendar.HOUR_OF_DAY),
          mCalendar.get(Calendar.MINUTE),
          true
      );
    case ACCOUNT_DIALOG_ID:
      final Cursor otherAccounts = mDbHelper.fetchAccountOther(mTransaction.accountId,true);
      final String[] accountLabels = Utils.getStringArrayFromCursor(otherAccounts, "label");
      final Long[] accountIds = Utils.getLongArrayFromCursor(otherAccounts, ExpensesDbAdapter.KEY_ROWID);
      otherAccounts.close();
      return new  AlertDialog.Builder(this)
        .setTitle(R.string.dialog_title_select_account)
        .setSingleChoiceItems(accountLabels,
            java.util.Arrays.asList(accountIds).indexOf(mCatId),
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              mCatId = accountIds[item];
              mLabel = accountLabels[item];
              setCategoryButton();
              dismissDialog(ACCOUNT_DIALOG_ID);
            }
          }
        ).create();
    case METHOD_DIALOG_ID:
      Cursor paymentMethods;
      paymentMethods = mDbHelper.fetchPaymentMethodsFiltered(mType,mAccount.type);
      final String[] methodLabels = new String[paymentMethods.getCount()];
      final Long[] methodIds = new Long[paymentMethods.getCount()];
      PaymentMethod pm;
      if(paymentMethods.moveToFirst()){
       for (int i = 0; i < paymentMethods.getCount(); i++){
         methodIds[i] = paymentMethods.getLong(paymentMethods.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
         try {
          pm = PaymentMethod.getInstanceFromDb(methodIds[i]);
        } catch (DataObjectNotFoundException e) {
          // this should not happen, since we got the id from db
          e.printStackTrace();
          throw new RuntimeException(e);
        }
         methodLabels[i] = pm.getDisplayLabel(this);
         paymentMethods.moveToNext();
       }
      } else {
        //TODO create resource string and fill with types
        Toast.makeText(this,getString(
              R.string.no_valid_payment_methods,
              mAccount.type.getDisplayName(this),
              getString(mType == EXPENSE ? R.string.expense : R.string.income)
            ), Toast.LENGTH_LONG).show();
        return null;
      }
      paymentMethods.close();
      return new  AlertDialog.Builder(this)
        .setTitle(R.string.dialog_title_select_method)
        .setSingleChoiceItems(methodLabels,
            java.util.Arrays.asList(methodIds).indexOf(mMethodId), 
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int item) {
                mMethodId = methodIds[item];
                mMethodButton.setText(methodLabels[item]);
                removeDialog(METHOD_DIALOG_ID);
              }
            }
        )
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            removeDialog(METHOD_DIALOG_ID);
          }
        })

        .create();      
    }
    return null;
  }
  
  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {
    Cursor otherAccounts = null;
    int otherAccountsCount = 0;
    try {
      mAccount = Account.getInstanceFromDb(mAccountId);
    } catch (DataObjectNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    //2. get info about other accounts if we are editing a transfer
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      otherAccounts = mDbHelper.fetchAccountOther(mAccountId,true);
      otherAccountsCount = otherAccounts.getCount();
    }
    //TableLayout mScreen = (TableLayout) findViewById(R.id.Table);
    if (mRowId != 0 || mTemplateId != 0) {
      //3 handle edit existing transaction or new one from template
      //3a. fill amount
      BigDecimal amount;
      if (mMinorUnitP) {
        amount = new BigDecimal(mTransaction.amount.getAmountMinor());
      } else {
        amount = mTransaction.amount.getAmountMajor();
      }
      if (amount.signum() == -1) {
        amount = amount.abs();
      } else {
        mType = INCOME;
        configureType();
      }
      
      mAmountText.setText(nfDLocal.format(amount));
      //3b  fill comment
      mCommentText.setText(mTransaction.comment);
      //3c set title based on type
      if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
        setTitle(R.string.menu_edit_ta);
        mPayeeText.setText(mTransaction.payee);
        mMethodId = mTransaction.methodId;
        try {
          if (mMethodId != 0) {
            mMethodButton.setText(PaymentMethod.getInstanceFromDb(mMethodId).getDisplayLabel(this));
          }
        } catch (DataObjectNotFoundException e) {
          //the methodId no longer exists in DB, we set it to 0
          mMethodId = 0;
        }
      } else {
        setTitle(R.string.menu_edit_transfer);
      }
      //3d fill label (category or account) we got from database, if we are a transfer we prefix 
      //with transfer direction
      mCatId = mTransaction.catId;
      mLabel =  mTransaction.label;
    } else {
      //4. handle edit new transaction
      //4a set title based on type
      setTitle(mOperationType == MyExpenses.TYPE_TRANSFER ? 
          R.string.menu_insert_transfer : R.string.menu_insert_ta);
      //4b if we are a transfer, and we have only one other account
      //we point the transfer to that account
      if (mOperationType == MyExpenses.TYPE_TRANSFER && otherAccountsCount == 1) {
        otherAccounts.moveToFirst();
        mCatId = otherAccounts.getInt(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
        mLabel = otherAccounts.getString(otherAccounts.getColumnIndex("label"));
      }
    }
    setCategoryButton();
    //5.configure button behavior
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      //5a if we are a transfer
      if (otherAccountsCount == 1) {
        //we disable the button, if there is only one account
        mCategoryButton.setEnabled(false);
      } else {
        //otherwise show dialog to select account
        mCategoryButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View view) {
            showDialog(ACCOUNT_DIALOG_ID);
          }
        });
      }
      //by the way: this is a good time to close the cursor
      otherAccounts.close();
    } else {
      //5b if we are a transaction we start select category activiy
      mCategoryButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            startSelectCategory();
        }
      });
      //5c we hide the method button if there are no valid methods, we check for both incomes and expenses
      if (mDbHelper.getPaymentMethodsCount(mAccount.type) == 0) {
        View MethodContainer = findViewById(R.id.MethodRow);
        if (MethodContainer == null)
          MethodContainer = findViewById(R.id.Method);
        MethodContainer.setVisibility(View.GONE);
      }
    }
    setDateTime(mTransaction.date);
    
    //add currency label to amount label
    TextView amountLabel = (TextView) findViewById(R.id.AmountLabel);    
    String currencySymbol;
    try {
      Account account = Account.getInstanceFromDb(mTransaction.accountId);
      currencySymbol = account.currency.getSymbol();
      if (mMinorUnitP) {
        switch (account.currency.getDefaultFractionDigits()) {
        case 2:
          currencySymbol += "¢";
          break;
        case 3:
          currencySymbol += "/1000";
        }
      }
    } catch (DataObjectNotFoundException e) {
      currencySymbol = "?";
    }
    amountLabel.setText(getString(R.string.amount) + " ("+currencySymbol+")");    
  }
  /**
   * extracts the fields from a date object for setting them on the buttons
   * @param date
   */
  private void setDateTime(Date date) {
    mCalendar.setTime(date);

    setDate();
    setTime();
  }
  /**
   * sets date on date button
   */
  private void setDate() {
    mDateButton.setText(mTitleDateFormat.format(mCalendar.getTime()));
  }
  
  /**
   * sets time on time button
   */
  private void setTime() {
    mTimeButton.setText(pad(mCalendar.get(Calendar.HOUR_OF_DAY)) + ":" + pad(mCalendar.get(Calendar.MINUTE)));
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
    BigDecimal amount = Utils.validateNumber(nfDLocal, strAmount);
    if (amount == null) {
      Toast.makeText(this,getString(R.string.invalid_number_format,nfDLocal.format(11.11)), Toast.LENGTH_LONG).show();
      return false;
    }
    if (mType == EXPENSE) {
      amount = amount.negate();
    }
    if (mMinorUnitP) {
      mTransaction.amount.setAmountMinor(amount.longValue());
    } else {
      mTransaction.amount.setAmountMajor(amount);
    }

    mTransaction.comment = mCommentText.getText().toString();
    mTransaction.setDate(mCalendar.getTime());

    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mTransaction.setPayee(mPayeeText.getText().toString());
    } else {
      if (mCatId == 0) {
        Toast.makeText(this,getString(R.string.warning_select_account), Toast.LENGTH_LONG).show();
        return false;
      }
    }
    mTransaction.catId = mCatId;
    mTransaction.methodId = mMethodId;
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
      mCatId = intent.getLongExtra("cat_id",0);
      mLabel = intent.getStringExtra("label");
      mCategoryButton.setText(mLabel);
    }
  }
  /**
   * updates interface based on type (EXPENSE or INCOME)
   */
  private void configureType() {
    mTypeButton.setText(mType ? "+" : "-");
    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mPayeeLabel.setText(mType ? R.string.payer : R.string.payee);
    }
    setCategoryButton();
  }
  /**
   *  for a transfer append an indicator of direction to the label on the category button 
   */
  private void setCategoryButton() {
    if (mLabel != null && mLabel.length() != 0) {
      String label = mLabel;
      if (mOperationType == MyExpenses.TYPE_TRANSFER) {
        label = (mType == EXPENSE ? MyExpenses.TRANSFER_EXPENSE  : MyExpenses.TRANSFER_INCOME) +
            label;
      }
      mCategoryButton.setText(label);
    }
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("type", mType);
    outState.putSerializable("calendar", mCalendar);
    outState.putLong("catId", mCatId);
    outState.putLong("methodId", mMethodId);
    outState.putString("label", mLabel);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mType = savedInstanceState.getBoolean("type");
    mCalendar = (Calendar) savedInstanceState.getSerializable("calendar");
    mLabel = savedInstanceState.getString("label");
    mCatId = savedInstanceState.getLong("catId");
    mMethodId = savedInstanceState.getLong("methodId");
    configureType();
    setDate();
    setTime();
    try {
      mMethodButton.setText(PaymentMethod.getInstanceFromDb(mMethodId).getDisplayLabel(this));
    } catch (DataObjectNotFoundException e) {
      //the methodId no longer exists in DB, we set it to 0
      mMethodId = 0;
    }
  }
}
