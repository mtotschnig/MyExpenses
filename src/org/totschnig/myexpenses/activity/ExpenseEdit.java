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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.calendar.CalendarContractCompat;
import com.android.calendar.EventRecurrenceFormatter;
import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendarcommon2.EventRecurrence;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Activity for editing a transaction
 * @author Michael Totschnig
 */
public class ExpenseEdit extends AmountActivity implements TaskExecutionFragment.TaskCallbacks,
    OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>,ContribIFace {

  private Button mDateButton;
  private Button mTimeButton;
  private EditText mCommentText, mTitleText, mReferenceNumberText;
  private Button mCategoryButton, mPlanButton;
  private Spinner mMethodSpinner, mAccountSpinner;
  private SimpleCursorAdapter mMethodsAdapter, mAccountsAdapter;
  private AutoCompleteTextView mPayeeText;
  private TextView mPayeeLabel;
  private ToggleButton mPlanToggleButton;
  public Long mRowId;
  private Long mTemplateId;
  private Account mAccount;
  private Calendar mCalendar = Calendar.getInstance();
  private final java.text.DateFormat mTitleDateFormat = java.text.DateFormat.
      getDateInstance(java.text.DateFormat.FULL);
  private Long mCatId = null, mPlanId = null;
  private String mLabel;
  private Transaction mTransaction;
  private boolean mTransferEnabled = false;
  private Cursor mMethodsCursor;
  private Plan mPlan;

  /**
   *   transaction, transfer or split
   */
  private int mOperationType;


  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;
  //CALCULATOR_REQUEST in super = 0
  private static final int ACTIVITY_EDIT_SPLIT = 1;
  private static final int SELECT_CATEGORY_REQUEST = 2;
  protected static final int ACTIVITY_EDIT_EVENT = 4;

  public static final int PAYEES_CURSOR=1;
  public static final int METHODS_CURSOR=2;
  public static final int ACCOUNTS_CURSOR=3;
  private static final int EVENT_CURSOR = 4;
  private LoaderManager mManager;

  private boolean mCreateNew = false, mLaunchPlanView = false;

  public enum HelpVariant {
    transaction,transfer,split,template,splitPartCategory,splitPartTransfer
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle extras = getIntent().getExtras();
    //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
    if ((mRowId = (savedInstanceState == null ? 0L : savedInstanceState.getLong("rowId"))) == 0L)
      mRowId = extras.getLong(DatabaseConstants.KEY_ROWID,0);
    mTemplateId = extras.getLong("template_id",0);
    mTransferEnabled = extras.getBoolean("transferEnabled",false);
    //were we called from a notification
    int notificationId = extras.getInt("notification_id", 0);
    if (notificationId > 0) {
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
    }
    setContentView(R.layout.one_expense);
    changeEditTextBackground((ViewGroup)findViewById(android.R.id.content));
    mTypeButton = (Button) findViewById(R.id.TaType);
    mCommentText = (EditText) findViewById(R.id.Comment);
    mTitleText = (EditText) findViewById(R.id.Title);
    mReferenceNumberText = (EditText) findViewById(R.id.Number);
    mDateButton = (Button) findViewById(R.id.Date);
    mTimeButton = (Button) findViewById(R.id.Time);
    mPayeeLabel = (TextView) findViewById(R.id.PayeeLabel);
    mPayeeText = (AutoCompleteTextView) findViewById(R.id.Payee);
    mCategoryButton = (Button) findViewById(R.id.Category);
    mPlanButton = (Button) findViewById(R.id.Plan);
    mMethodSpinner = (Spinner) findViewById(R.id.Method);
    mAccountSpinner = (Spinner) findViewById(R.id.Account);
    mPlanToggleButton = (ToggleButton) findViewById(R.id.togglebutton);
    TextPaint paint = mPlanToggleButton.getPaint();
    int automatic = (int) paint.measureText(getString(R.string.plan_automatic));
    int manual = (int) paint.measureText(getString(R.string.plan_manual));
    mPlanToggleButton.setWidth(
        (automatic > manual ? automatic : manual) +
        + mPlanToggleButton.getPaddingLeft()
        + mPlanToggleButton.getPaddingRight());
    mManager= getSupportLoaderManager();

    //1. fetch the transaction or create a new instance
    if (mRowId != 0 || mTemplateId != 0) {
      int taskId;
      Long objectId;
      if (mRowId != 0) {
        taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION;
        objectId = mRowId;
      }
      else {
        objectId = mTemplateId;
        //are we editing the template or instantiating a new one
        if (extras.getBoolean("instantiate"))
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE;
        else
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE;
      }
      FragmentManager fm = getSupportFragmentManager();
      if (fm.findFragmentByTag("ASYNC_TASK") == null) {
        fm.beginTransaction()
          .add(TaskExecutionFragment.newInstance(taskId,objectId, null), "ASYNC_TASK")
          .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_loading),"PROGRESS")
          .commit();
      }
    } else {
      mOperationType = extras.getInt("operationType");
      Long accountId = extras.getLong(KEY_ACCOUNTID);
      Long parentId = extras.getLong(KEY_PARENTID);
      if (extras.getBoolean("newTemplate",false))
        mTransaction = Template.getTypedNewInstance(mOperationType, accountId);
      else
        mTransaction = Transaction.getTypedNewInstance(mOperationType,accountId,parentId);
      //Split transactions are returned persisted to db and already have an id
      mRowId = mTransaction.id;
      setup();
    }
  }
  private void setup() {
    configAmountInput();

    View categoryContainer = findViewById(R.id.CategoryRow);
    if (categoryContainer == null)
      categoryContainer = findViewById(R.id.Category);
    //Spinner for setting Status
    Spinner statusSpinner = (Spinner) findViewById(R.id.Status);
    if (getmAccount().type.equals(Type.CASH) ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer)
      statusSpinner.setVisibility(View.GONE);
    else {
      ArrayAdapter<Transaction.CrStatus> sAdapter = new ArrayAdapter<Transaction.CrStatus>(
          DialogUtils.wrapContext1(this),
          R.layout.custom_spinner_item, android.R.id.text1,Transaction.CrStatus.values()) {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          View row = super.getView(position, convertView, parent);
          setColor(position,row);
          row.findViewById(android.R.id.text1).setVisibility(View.GONE);
          return row;
        }
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
          View row = super.getDropDownView(position, convertView, parent);
          setColor(position,row);
          return row;
        }
        private void setColor(int position, View row) {
          View color = row.findViewById(R.id.color1);
          color.setBackgroundColor(getItem(position).color);
          LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(20,20);
          lps.setMargins(10, 0, 0, 0);
          color.setLayoutParams(lps);
        }
      };
      sAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
      statusSpinner.setAdapter(sAdapter);
      statusSpinner.setSelection(mTransaction.crStatus.ordinal());
      statusSpinner.setOnItemSelectedListener(this);
    }

    if (mTransaction instanceof Template) {
      findViewById(R.id.TitleRow).setVisibility(View.VISIBLE);
      findViewById(R.id.PlannerRow).setVisibility(View.VISIBLE);
      setTitle(mTransaction.id == 0 ? R.string.menu_create_template : R.string.menu_edit_template);
      helpVariant = HelpVariant.template;
    } else if (mTransaction instanceof SplitTransaction) {
      setTitle(mTransaction.id == 0 ? R.string.menu_create_split : R.string.menu_edit_split);
      //SplitTransaction are always instantiated with status uncommitted,
      //we save them to DB as uncommitted, before working with them
      //when the split transaction is saved the split and its parts are committed
      categoryContainer.setVisibility(View.GONE);
      //add split list
      FragmentManager fm = getSupportFragmentManager();
      SplitPartList f = (SplitPartList) fm.findFragmentByTag("SPLIT_PART_LIST");
      if (f == null) {
        fm.beginTransaction()
          .add(R.id.OneExpense,SplitPartList.newInstance(mTransaction.id,mTransaction.accountId),"SPLIT_PART_LIST")
          .commit();
        fm.executePendingTransactions();
      }
      helpVariant = HelpVariant.split;
    } else {
      if (mTransaction instanceof SplitPartCategory) {
        setTitle(mTransaction.id == 0 ?
            R.string.menu_create_split_part_category : R.string.menu_edit_split_part_category  );
        helpVariant = HelpVariant.splitPartCategory;
        mTransaction.status = STATUS_UNCOMMITTED;
      }
      else if (mTransaction instanceof SplitPartTransfer) {
        setTitle(mTransaction.id == 0 ?
            R.string.menu_create_split_part_transfer : R.string.menu_edit_split_part_transfer );
        helpVariant = HelpVariant.splitPartTransfer;
        mTransaction.status = STATUS_UNCOMMITTED;
      }
      else if (mTransaction instanceof Transfer) {
        setTitle(mTransaction.id == 0 ?
            R.string.menu_create_transfer : R.string.menu_edit_transfer );
        helpVariant = HelpVariant.transfer;
      }
      else if (mTransaction instanceof Transaction) {
        setTitle(mTransaction.id == 0 ?
            R.string.menu_create_transaction : R.string.menu_edit_transaction );
        helpVariant = HelpVariant.transaction;
      }
    }

    if (mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer) {
      findViewById(R.id.DateRow).setVisibility(View.GONE);
      //in portrait orientation we have a separate row for time
      View timeRow = findViewById(R.id.TimeRow);
      if (timeRow != null)
        timeRow.setVisibility(View.GONE);
    } else {
      mDateButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          showDialog(DATE_DIALOG_ID);
        }
      });

      mTimeButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          showDialog(TIME_DIALOG_ID);
        }
      });
    }

    if (mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory)) {
      mManager.initLoader(PAYEES_CURSOR, null, this);

      // Spinner for methods
      mMethodsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
          new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0) {
        @Override
        public void setViewText(TextView v, String text) {
          super.setViewText(v, PaymentMethod.getDisplayLabel(text));
        }
      };
      mMethodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mMethodSpinner.setAdapter(mMethodsAdapter);
      mMethodSpinner.setOnItemSelectedListener(this);
      mManager.initLoader(METHODS_CURSOR, null, this);
    } else {
      findViewById(R.id.PayeeRow).setVisibility(View.GONE);
      View MethodContainer = findViewById(R.id.MethodRow);
      MethodContainer.setVisibility(View.GONE);
    }

    mTypeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        mType = ! mType;
        configureType();
        if (mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory)) {
          mTransaction.methodId = null;
          mManager.restartLoader(METHODS_CURSOR, null, ExpenseEdit.this);
        }
      } 
    });
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      categoryContainer.setVisibility(View.GONE);
      View accountContainer = findViewById(R.id.AccountRow);
      if (accountContainer == null)
        accountContainer = findViewById(R.id.Account);
      accountContainer.setVisibility(View.VISIBLE);

      // Spinner for accounts
      mAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
          new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
      mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mAccountSpinner.setAdapter(mAccountsAdapter);
      mAccountSpinner.setOnItemSelectedListener(this);
      mManager.initLoader(ACCOUNTS_CURSOR, null, this);
    }
    //category button and amount label are further set up in populateFields, since it depends on data
    populateFields();
  }
  @Override
  protected void configAmountInput() {
    super.configAmountInput();
    if (mTransaction instanceof SplitTransaction) {
      mAmountText.addTextChangedListener(new TextWatcher(){
        public void afterTextChanged(Editable s) {
          ((SplitPartList) getSupportFragmentManager().findFragmentByTag("SPLIT_PART_LIST")).updateBalance();
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
      });
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (mTransaction instanceof SplitTransaction) {
      MenuInflater inflater = getSupportMenuInflater();
      inflater.inflate(R.menu.split, menu);
      if (!mTransferEnabled)
        menu.findItem(R.id.INSERT_TRANSFER_COMMAND).setVisible(false);
    } else if (!(mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer))
      menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
        .setIcon(R.drawable.save_and_new_icon)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    return true;
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case android.R.id.home:
      //TODO Strict mode
      if (mTransaction instanceof SplitTransaction) {
        ((SplitTransaction) mTransaction).cleanupCanceledEdit();
      } else if (mTransaction instanceof Template) {
        deleteUnusedPlan();
      }
      //handled in super
      break;
    case R.id.Confirm:
      if (mTransaction instanceof SplitTransaction &&
        !((SplitPartList) getSupportFragmentManager().findFragmentByTag("SPLIT_PART_LIST")).splitComplete()) {
          Toast.makeText(this,getString(R.string.unsplit_amount_greater_than_zero),Toast.LENGTH_SHORT).show();
          return true;
      }
      //handled in super
      break;
    case R.id.SAVE_AND_NEW_COMMAND:
      mCreateNew = true;
      saveState();
      return true;
    case R.id.INSERT_TA_COMMAND:
      createRow(MyExpenses.TYPE_TRANSACTION);
      return true;
    case R.id.INSERT_TRANSFER_COMMAND:
      createRow(MyExpenses.TYPE_TRANSFER);
      return true;
    }
    return super.dispatchCommand(command, tag);
  }
  private void createRow(int type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("operationType", type);
    i.putExtra(KEY_ACCOUNTID,mTransaction.accountId);
    i.putExtra(KEY_PARENTID,mTransaction.id);
    startActivityForResult(i, ACTIVITY_EDIT_SPLIT);
  }
  /**
   * calls the activity for selecting (and managing) categories
   */
  private void startSelectCategory() {
    Intent i = new Intent(this, ManageCategories.class);
    //i.putExtra(DatabaseConstants.KEY_ROWID, id);
    startActivityForResult(i, SELECT_CATEGORY_REQUEST);
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
  private ArrayAdapter<String>  mPayeeAdapter;
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
    }
    return null;
  }

  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {

    if (mRowId != 0 || mTemplateId != 0) {
      //3 handle edit existing transaction or new one from template
      //3b  fill comment
      mCommentText.setText(mTransaction.comment);
      mReferenceNumberText.setText(mTransaction.referenceNumber);
      if (mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory)) {
        mPayeeText.setText(mTransaction.payee);
      }
      //3d fill label (category or account) we got from database, if we are a transfer we prefix 
      //with transfer direction
      mCatId = mTransaction.catId;
      mLabel =  mTransaction.label;
    }
    setCategoryButton();
    //5.configure button behavior
    if (mOperationType != MyExpenses.TYPE_TRANSFER) {
      //5b if we are a transaction we start select category activity
      mCategoryButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            startSelectCategory();
        }
      });
    }
    if (mTransaction instanceof Template) {
      mTitleText.setText(((Template) mTransaction).title);
      mPlanId = ((Template) mTransaction).planId;
      if (mPlanId !=null) {
        //we need data from the cursor when launching the view intent
        //hence need to disable button until data is loaded
        mPlanButton.setEnabled(false);
        mManager.initLoader(EVENT_CURSOR, null, this);
      }
      mPlanButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
          if (mPlanId == null) {
            String title = mTitleText.getText().toString();
            if (MyApplication.getInstance().isContribEnabled ||
                Template.countWithPlan() < 3) {
              mLaunchPlanView = true;
              getSupportFragmentManager().beginTransaction()
              .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_NEW_PLAN,null, title), "ASYNC_TASK")
              .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_loading),"PROGRESS")
              .commit();
            } else {
              CommonCommands.showContribDialog(ExpenseEdit.this,Feature.PLANS_UNLIMITED, null);
            }
            return;
         }
         launchPlanView();
       }
      });
      mPlanToggleButton.setChecked(((Template) mTransaction).planExecutionAutomatic);
    }
    if (!(mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer))
      setDateTime(mTransaction.date);

    //add currency label to amount label
    TextView amountLabel = (TextView) findViewById(R.id.AmountLabel);    
    String currencySymbol;
    Account account = Account.getInstanceFromDb(mTransaction.accountId);
    currencySymbol = account.currency.getSymbol();
    amountLabel.setText(getString(R.string.amount) + " ("+currencySymbol+")");
    //fill amount
    BigDecimal amount = mTransaction.amount.getAmountMajor();
    int signum = amount.signum();
    switch(signum) {
    case -1:
      amount = amount.abs();
      break;
    case 1:
      mType = INCOME;
    }
    configureType();
    if (signum != 0)
      mAmountText.setText(nfDLocal.format(amount));
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
  protected void saveState() {
    String title = "";
    BigDecimal amount = validateAmountInput(true);
    if (amount == null) {
      return;
    }
    if (mType == EXPENSE) {
      amount = amount.negate();
    }

    mTransaction.amount.setAmountMajor(amount);

    mTransaction.comment = mCommentText.getText().toString();
    mTransaction.referenceNumber = mReferenceNumberText.getText().toString();
    if (mTransaction instanceof Template) {
      title = mTitleText.getText().toString();
      if (title.equals("")) {
        Toast.makeText(this, R.string.no_title_given, Toast.LENGTH_LONG).show();
        return;
      }
      ((Template) mTransaction).title = title;
      ((Template) mTransaction).planId = mPlanId;
    }
    if (!(mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer))
      mTransaction.setDate(mCalendar.getTime());

    if (mOperationType == MyExpenses.TYPE_TRANSACTION)
      mTransaction.catId = mCatId;
    if (mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory)) {
        mTransaction.setPayee(mPayeeText.getText().toString());
        long selected = mMethodSpinner.getSelectedItemId();
        mTransaction.methodId = (selected != AdapterView.INVALID_ROW_ID && selected > 0) ?
            selected : null;
    }

    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      mTransaction.transfer_account = mAccountSpinner.getSelectedItemId();
    }
    getSupportFragmentManager().beginTransaction()
    .add(DbWriteFragment.newInstance(true), "SAVE_TASK")
    .commit();
  }
  /* (non-Javadoc)
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SELECT_CATEGORY_REQUEST && intent != null) {
      mCatId = intent.getLongExtra("cat_id",0);
      mLabel = intent.getStringExtra("label");
      mCategoryButton.setText(mLabel);
    }
  }
  @Override
  public void onBackPressed() {
    if (mTransaction instanceof SplitTransaction) {
      ((SplitTransaction) mTransaction).cleanupCanceledEdit();
    } else if (mTransaction instanceof Template) {
      deleteUnusedPlan();
    }
    super.onBackPressed();
  }
  /**
   * when we have created a new plan without saving the template, we delete the plan
   */
  private void deleteUnusedPlan() {
    if (mPlanId != null && !mPlanId.equals(((Template) mTransaction).planId)) {
      Log.i("DEBUG","deleting unused plan");
      Plan.delete(mPlanId);
    }
  }
  /**
   * updates interface based on type (EXPENSE or INCOME)
   */
  protected void configureType() {
    super.configureType();
    if (mPayeeLabel != null) {
      mPayeeLabel.setText(mType ? R.string.payer : R.string.payee);
    }
    if (mTransaction instanceof SplitTransaction) {
      ((SplitPartList) getSupportFragmentManager().findFragmentByTag("SPLIT_PART_LIST")).updateBalance();
    }
    setCategoryButton();
  }
  private void configurePlan() {
    if (mPlan == null) {
      mPlanButton.setText(R.string.menu_create);
      mPlanToggleButton.setVisibility(View.GONE);
    } else {
      if (mPlan.rrule != null) {
        EventRecurrence eventRecurrence = new EventRecurrence();
        eventRecurrence.parse(mPlan.rrule);
        Time date = new Time();
        date.set(mPlan.dtstart);
        eventRecurrence.setStartDate(date);
        mPlanButton.setText(EventRecurrenceFormatter.getRepeatString(this,getResources(), eventRecurrence,true));
      } else {
        mPlanButton.setText(mTitleDateFormat.format(new Date(mPlan.dtstart)));
      }
      if (mTitleText.getText().toString().equals(""))
        mTitleText.setText(mPlan.title);
      mPlanToggleButton.setVisibility(View.VISIBLE);
    }
    mPlanButton.setEnabled(true);
  }
  /**
   *  for a transfer append an indicator of direction to the label on the category button 
   */
  private void setCategoryButton() {
    if (mLabel != null && mLabel.length() != 0) {
      mCategoryButton.setText(mLabel);
    }
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable("calendar", mCalendar);
    //restored in onCreate
    if (mRowId != 0)
      outState.putLong("rowId", mRowId);
    if (mCatId != null)
      outState.putLong("catId", mCatId);
    outState.putString("label", mLabel);
    if (mPlan != null)
      outState.putSerializable("plan",mPlan);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mCalendar = (Calendar) savedInstanceState.getSerializable("calendar");
    mPlan = (Plan) savedInstanceState.getSerializable("plan");
    if (mPlan != null) {
      mPlanId = mPlan.id;
      configurePlan();
    }
    mLabel = savedInstanceState.getString("label");
    if ((mCatId = savedInstanceState.getLong("catId")) == 0L)
      mCatId = null;
    configureType();
  }

  public Money getAmount() {
    if (getmAccount() == null)
      return null;
    Money result = new Money(getmAccount().currency,0L);
    BigDecimal amount = validateAmountInput(false);
    if (amount == null) {
      return result;
    }
    if (mType == EXPENSE) {
      amount = amount.negate();
    }
    result.setAmountMajor(amount);
    return result;
  }
  @Override
  public void onPreExecute() {
    // TODO Auto-generated method stub

  }
  @Override
  public void onProgressUpdate(int percent) {
    // TODO Auto-generated method stub

  }
  @Override
  public void onCancelled() {
    // TODO Auto-generated method stub

  }
  @Override
  public void onPostExecute(int taskId,Object o) {
    switch(taskId) {
    case TaskExecutionFragment.TASK_NEW_PLAN:
      mPlanId = (Long) o;
      if (mPlanId == null) {
        Log.i("DEBUG", "Could not create new plan");
        MessageDialogFragment.newInstance(R.string.dialog_title_planner_setup_info,
            R.string.planner_setup_info,R.id.SETTINGS_COMMAND,null)
          .show(getSupportFragmentManager(),"CALENDAR_SETUP_INFO");
      } else {
        if (mManager.getLoader(EVENT_CURSOR) != null && !mManager.getLoader(EVENT_CURSOR).isReset())
          mManager.restartLoader(EVENT_CURSOR, null, ExpenseEdit.this);
        else
          mManager.initLoader(EVENT_CURSOR, null, ExpenseEdit.this);
      }
      break;
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
    case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
      mTransaction = (Transaction) o;
      if (mTransaction instanceof SplitTransaction)
        mOperationType = MyExpenses.TYPE_SPLIT;
      else if (mTransaction instanceof Template)
        mOperationType = ((Template) mTransaction).isTransfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
      else
        mOperationType = mTransaction instanceof Transfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
      setup();
      supportInvalidateOptionsMenu();
      break;
    }
    super.onPostExecute(taskId, o);
  }
  public Account getmAccount() {
    if (mAccount == null) {
      if (mTransaction == null)
        return null;
      mAccount = Account.getInstanceFromDb(mTransaction.accountId);
    }
    return mAccount;
  }
  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
      long id) {
    switch(parent.getId()) {
    case R.id.Status:
      mTransaction.crStatus = (Transaction.CrStatus) parent.getItemAtPosition(position);
      break;
    case R.id.Method:
      if (id>0) {
        mTransaction.methodId = id;
        //ignore first row "no method" merged in
        mMethodsCursor.moveToPosition(position-1);
        mReferenceNumberText.setVisibility(mMethodsCursor.getInt(mMethodsCursor.getColumnIndexOrThrow(KEY_IS_NUMBERED))>0 ?
            View.VISIBLE : View.INVISIBLE);
      }
      else {
        mTransaction.methodId = null;
        mReferenceNumberText.setVisibility(View.INVISIBLE);
      }
    }
  }
  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // TODO Auto-generated method stub    
  }

  @Override
  public void onPostExecute(Object result) {
    Long sequenceCount = (Long) result;
    if (sequenceCount == -1L && mTransaction instanceof Template) {
      //for the moment, the only case where saving will fail
      //if the unique constraint for template titles is violated
      //TODO: we should probably validate the title earlier
      Toast.makeText(this,getString(R.string.template_title_exists,((Template) mTransaction).title), Toast.LENGTH_LONG).show();
      mCreateNew = false;
    } else {
      if (mCreateNew) {
        mCreateNew = false;
        mTransaction.id = 0L;
        mRowId = 0L;
        if (mTransaction instanceof Template) {
          setTitle(R.string.menu_create_template);
          mTitleText.setText("");
          mPlanId = null;
          mPlan = null;
          configurePlan();
        } else {
          setTitle(mOperationType == MyExpenses.TYPE_TRANSACTION ?
              R.string.menu_create_transaction : R.string.menu_create_transfer);
        }
        mAmountText.setText("");
        Toast.makeText(this,getString(R.string.save_transaction_and_new_success),Toast.LENGTH_SHORT).show();
      } else {
        Intent intent=new Intent();
        intent.putExtra("sequence_count", sequenceCount);
        setResult(RESULT_OK,intent);
        finish();
      }
    }
  }
  @Override
  public Model getObject() {
    return mTransaction;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch(id){
    case PAYEES_CURSOR:
      return new CursorLoader(this, TransactionProvider.PAYEES_URI, null, null, null, null);
    case METHODS_CURSOR:
      return new CursorLoader(this,
          TransactionProvider.METHODS_URI.buildUpon()
          .appendPath("typeFilter")
          .appendPath(mType == INCOME ? "1" : "-1")
          .appendPath(getmAccount().type.name())
          .build(), null, null, null, null);
    case ACCOUNTS_CURSOR:
      return new CursorLoader(this,TransactionProvider.ACCOUNTS_URI,
          new String[] {DatabaseConstants.KEY_ROWID, "label"},
          DatabaseConstants.KEY_ROWID + " != ? AND currency = ?",
          new String[] {String.valueOf(mTransaction.accountId),getmAccount().currency.getCurrencyCode()},null);
    case EVENT_CURSOR:
      return new CursorLoader(
          this,
          ContentUris.withAppendedId(Events.CONTENT_URI, mPlanId),
          new String[]{
              Events._ID,
              Events.DTSTART,
              Events.RRULE,
              Events.TITLE},
          null,
          null,
          null);
    }
    return null;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    int id = loader.getId();
    switch(id) {
    case PAYEES_CURSOR:
      data.moveToFirst();
      if (mPayeeAdapter == null)
        mPayeeAdapter = new ArrayAdapter<String>(this,
          android.R.layout.simple_dropdown_item_1line);
      else
        mPayeeAdapter.clear();
      while(!data.isAfterLast()) {
        mPayeeAdapter.add(data.getString(data.getColumnIndex("name")));
        data.moveToNext();
      }
      mPayeeText.setAdapter(mPayeeAdapter);
      break;
    case METHODS_CURSOR:
      mMethodsCursor = data;
      View MethodContainer = findViewById(R.id.MethodRow);
      if (!data.moveToFirst()) {
        MethodContainer.setVisibility(View.GONE);
      } else {
        MethodContainer.setVisibility(View.VISIBLE);
        MatrixCursor extras = new MatrixCursor(new String[] { KEY_ROWID,KEY_LABEL,KEY_IS_NUMBERED });
        extras.addRow(new String[] { "0", "No method","0" });
        mMethodsAdapter.swapCursor(new MergeCursor(new Cursor[] {extras,data}));
        if (mTransaction.methodId != null) {
          while (data.isAfterLast() == false) {
            if (data.getLong(data.getColumnIndex(KEY_ROWID)) == mTransaction.methodId) {
              mMethodSpinner.setSelection(data.getPosition()+1);
              break;
            }
            data.moveToNext();
          }
        } else
          mMethodSpinner.setSelection(0);
      }
      break;
    case ACCOUNTS_CURSOR:
      mAccountsAdapter.swapCursor(data);
      if (mTransaction.transfer_account != null) {
        for (int position = 0; position < mAccountsAdapter.getCount(); position++) {
          if(mAccountsAdapter.getItemId(position) == mTransaction.transfer_account) {
            mAccountSpinner.setSelection(position);
            break;
          }
        }
      }
      break;
    case EVENT_CURSOR:
      if (data.moveToFirst()) {
        long eventId = data.getLong(data.getColumnIndexOrThrow(Events._ID));
        long dtStart = data.getLong(data.getColumnIndexOrThrow(Events.DTSTART));
        String rRule = data.getString(data.getColumnIndexOrThrow(Events.RRULE));
        String title = data.getString(data.getColumnIndexOrThrow(Events.TITLE));
        if (mPlan == null) {
          mPlan = new Plan(
              eventId,
              dtStart,
              rRule,
              title);
        } else {
          mPlan.id = eventId;
          mPlan.dtstart= dtStart;
          mPlan.rrule = rRule;
          mPlan.title = title;
        }
        if (mLaunchPlanView) {
          launchPlanView();
        }
      } else {
        mPlan = null;
        mPlanId = null;
      }
      configurePlan();
      break;
    }
  }
  private void launchPlanView() {
    mLaunchPlanView = false;
    //unfortunately ACTION_EDIT does not work see http://code.google.com/p/android/issues/detail?id=39402
    Intent intent = new Intent (Intent.ACTION_VIEW);
    intent.setData(ContentUris.withAppendedId(Events.CONTENT_URI, mPlanId));
    //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan.dtstart);
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan.dtstart);
    if (Utils.isIntentAvailable(this, intent))
      startActivityForResult (intent, ACTIVITY_EDIT_EVENT);
    else
      Log.i("DEBUG","could not launch event view in calendar");
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    //should not be necessary to empty the autocompletetextview
    int id = loader.getId();
    switch(id) {
    case METHODS_CURSOR:
      mMethodsCursor = null;
      mMethodsAdapter.swapCursor(null);
      break;
    case ACCOUNTS_CURSOR:
      mAccountsAdapter.swapCursor(null);
      break;
    case EVENT_CURSOR:
      mPlan = null;
      break;
    }
  }
  public void onToggleClicked(View view) {
    ((Template) mTransaction).planExecutionAutomatic = ((ToggleButton) view).isChecked();
  }
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    // not used
  }
  @Override
  public void contribFeatureNotCalled() {
    // nothing to do
  }
}
