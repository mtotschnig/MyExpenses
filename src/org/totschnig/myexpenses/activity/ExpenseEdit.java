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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.FilterCursorWrapper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;

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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Activity for editing a transaction
 * @author Michael Totschnig
 */
public class ExpenseEdit extends AmountActivity implements
    OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>,ContribIFace {

  private Button mDateButton;
  private Button mTimeButton;
  private EditText mCommentText, mTitleText, mReferenceNumberText;
  private Button mCategoryButton, mPlanButton;
  private Spinner mMethodSpinner, mAccountSpinner, mTransferAccountSpinner, mStatusSpinner;
  private SimpleCursorAdapter mMethodsAdapter, mAccountsAdapter, mTransferAccountsAdapter;
  private ArrayAdapter<String>  mPayeeAdapter;
  private FilterCursorWrapper mTransferAccountCursor;
  private AutoCompleteTextView mPayeeText;
  private TextView mPayeeLabel, mAmountLabel;
  private ToggleButton mPlanToggleButton;
  public Long mRowId = 0L;
  private Long mTemplateId;
  private Account[] mAccounts;
  private Calendar mCalendar = Calendar.getInstance();
  private final java.text.DateFormat mDateFormat = DateFormat.getDateInstance(
      DateFormat.FULL);
  private final java.text.DateFormat mTimeFormat = DateFormat.getTimeInstance(
      DateFormat.SHORT);
  private Long mCatId = null, mPlanId = null, mMethodId = null,
      mAccountId = null, mTransferAccountId;
  private String mLabel;
  private Transaction mTransaction;
  private Cursor mMethodsCursor;
  private Plan mPlan;

  private long mPlanInstanceId,mPlanInstanceDate;
  /**
   *   transaction, transfer or split
   */
  private int mOperationType;


  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;
  //CALCULATOR_REQUEST in super = 0
  private static final int EDIT_SPLIT_REQUEST = 1;
  private static final int SELECT_CATEGORY_REQUEST = 2;
  protected static final int EDIT_EVENT_REQUEST = 4;

  public static final int PAYEES_CURSOR=1;
  public static final int METHODS_CURSOR=2;
  public static final int ACCOUNTS_CURSOR=3;
  private static final int EVENT_CURSOR = 4;
  public static final int TRANSACTION_CURSOR = 5;
  public static final int SUM_CURSOR = 6;
  private LoaderManager mManager;

  private boolean mNewInstance = true, mCreateNew = false, mLaunchPlanView = false, mSavedInstance = false;

  public enum HelpVariant {
    transaction,transfer,split,template,splitPartCategory,splitPartTransfer
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.one_expense);
    mManager= getSupportLoaderManager();
    changeEditTextBackground((ViewGroup)findViewById(android.R.id.content));
    mTypeButton = (Button) findViewById(R.id.TaType);
    //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
    mTypeButton.setEnabled(false);
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
    mTransferAccountSpinner = (Spinner) findViewById(R.id.TransferAccount);
    mStatusSpinner = (Spinner) findViewById(R.id.Status);
    mPlanToggleButton = (ToggleButton) findViewById(R.id.togglebutton);
    mAmountLabel = (TextView) findViewById(R.id.AmountLabel);
    TextPaint paint = mPlanToggleButton.getPaint();
    int automatic = (int) paint.measureText(getString(R.string.plan_automatic));
    int manual = (int) paint.measureText(getString(R.string.plan_manual));
    mPlanToggleButton.setWidth(
        (automatic > manual ? automatic : manual) +
        + mPlanToggleButton.getPaddingLeft()
        + mPlanToggleButton.getPaddingRight());

    Bundle extras = getIntent().getExtras();
    mRowId = extras.getLong(DatabaseConstants.KEY_ROWID,0);
    if (mRowId != 0L) {
      mNewInstance = false;
    }
    //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
    if (savedInstanceState != null) {
      mSavedInstance = true;
      mRowId = savedInstanceState.getLong("rowId");

      mCalendar = (Calendar) savedInstanceState.getSerializable("calendar");
      mPlan = (Plan) savedInstanceState.getSerializable("plan");
      if (mPlan != null) {
        mPlanId = mPlan.id;
        configurePlan();
      }
      mLabel = savedInstanceState.getString("label");
      if ((mCatId = savedInstanceState.getLong("catId")) == 0L)
        mCatId = null;
      setDate();
      setTime();
      if ((mMethodId = savedInstanceState.getLong("methodId")) == 0L)
        mMethodId = null;
      if ((mAccountId = savedInstanceState.getLong("accountId")) == 0L)
        mAccountId = null;
      if ((mTransferAccountId = savedInstanceState.getLong("transferAccountId")) == 0L)
        mTransferAccountId = null;
      mType = savedInstanceState.getBoolean("type");
      configureType();
    }
    mTemplateId = extras.getLong("template_id",0);
    //were we called from a notification
    int notificationId = extras.getInt("notification_id", 0);
    if (notificationId > 0) {
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
    }


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
      }
    };
    sAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
    mStatusSpinner.setAdapter(sAdapter);
    
    //1. fetch the transaction or create a new instance
    if (mRowId != 0 || mTemplateId != 0) {
      int taskId;
      Long objectId;
      if (mRowId != 0) {
        taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION;
        objectId = mRowId;
      } else {
        objectId = mTemplateId;
        //are we editing the template or instantiating a new one
        if ((mPlanInstanceId = extras.getLong("instance_id")) != 0L) {
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE;
          mPlanInstanceDate = extras.getLong("instance_date");
        } else {
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE;
        }
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
      if (mTransaction == null) {
        Toast.makeText(this,"Error instantiating transaction for account "+accountId,Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      //Split transactions are returned persisted to db and already have an id
      mRowId = mTransaction.id;
      setup();
    }
  }
  private void setup() {
    configAmountInput();
    // Spinner for account and transfer account
    mAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
        new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    if (mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer) {
        disableAccountSpinner();
    }
    mAccountSpinner.setOnItemSelectedListener(this);

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
    } else {
      findViewById(R.id.PayeeRow).setVisibility(View.GONE);
      View MethodContainer = findViewById(R.id.MethodRow);
      MethodContainer.setVisibility(View.GONE);
    }
    
    View categoryContainer = findViewById(R.id.CategoryRow);
    if (categoryContainer == null)
      categoryContainer = findViewById(R.id.Category);
    TextView accountLabelTv = (TextView) findViewById(R.id.AccountLabel);
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      mTypeButton.setVisibility(View.GONE);
      categoryContainer.setVisibility(View.GONE);
      View accountContainer = findViewById(R.id.TransferAccountRow);
      if (accountContainer == null)
        accountContainer = findViewById(R.id.TransferAccount);
      accountContainer.setVisibility(View.VISIBLE);
      if (getResources().getConfiguration().orientation ==  android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
        accountLabelTv.setText(getString(R.string.transfer_from_account) + " / " + getString(R.string.transfer_to_account));
      } else {
        accountLabelTv.setText(R.string.transfer_from_account);
      }
      mTransferAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
          new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
      mTransferAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mTransferAccountSpinner.setAdapter(mTransferAccountsAdapter);
    } else if (getResources().getConfiguration().orientation ==  android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
        accountLabelTv.setText(getString(R.string.account) + " / " + getString(R.string.category));
    }

    mManager.initLoader(ACCOUNTS_CURSOR, null, this);

    if (mTransaction instanceof Template) {
      findViewById(R.id.TitleRow).setVisibility(View.VISIBLE);
      findViewById(R.id.PlannerRow).setVisibility(View.VISIBLE);
      setTitle(mTransaction.id == 0 ? R.string.menu_create_template : R.string.menu_edit_template);
      helpVariant = HelpVariant.template;
    } else if (mTransaction instanceof SplitTransaction) {
      setTitle(mNewInstance ? R.string.menu_create_split : R.string.menu_edit_split);
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
      if (getResources().getConfiguration().orientation ==  android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
        TextView DateTimeLabelTv = (TextView) findViewById(R.id.DateTimeLabel);
        if (DateTimeLabelTv != null) {
          DateTimeLabelTv.setText(getString(R.string.date) + " / " + getString(R.string.time));
        }
      }
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

    //when we have a savedInstance, fields have already been populated
    if (!mSavedInstance) {
      populateFields();
    }

    if (mType == INCOME && mOperationType == MyExpenses.TYPE_TRANSFER) {
      switchAccountViews();
    }

    setCategoryButton();
    if (mOperationType != MyExpenses.TYPE_TRANSFER) {
      mCategoryButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            startSelectCategory();
        }
      });
    }

    mPlanButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (mPlanId == null) {
          if (getIntent().getExtras().getBoolean("newPlanEnabled")) {
            if (syncStateAndValidate()) {
              launchNewPlan();
            }
          } else {
            CommonCommands.showContribDialog(ExpenseEdit.this,Feature.PLANS_UNLIMITED, null);
          }
          return;
       }
       launchPlanView();
     }
    });
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
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.split, menu);
    } else if (!(mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer)) {
      MenuItemCompat.setShowAsAction(
          menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
            .setIcon(R.drawable.save_and_new_icon),
          Build.VERSION.SDK_INT < 11 ?
              MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT :
              MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      MenuItemCompat.setShowAsAction(
          menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
          .setIcon(R.drawable.invert_transfer_icon), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
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
    case R.id.CREATE_TRANSACTION_COMMAND:
      createRow(MyExpenses.TYPE_TRANSACTION);
      return true;
    case R.id.CREATE_TRANSFER_COMMAND:
      createRow(MyExpenses.TYPE_TRANSFER);
      return true;
    case R.id.CREATE_COMMAND:
      //create calendar
      getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_NEW_CALENDAR,0L,null), "ASYNC_TASK")
      .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_create_calendar),"PROGRESS")
      .commit();
      return true;
    case R.id.INVERT_TRANSFER_COMMAND:
      mType = ! mType;
      switchAccountViews();
    }
    return super.dispatchCommand(command, tag);
  }
  private void createRow(int type) {
    int accountPosition = mAccountSpinner.getSelectedItemPosition();
    if (accountPosition == AdapterView.INVALID_POSITION) {
      //silently do nothing if accounts are not yet loaded
      return;
    }
    if (type == MyExpenses.TYPE_TRANSFER &&
        !mAccounts[accountPosition].transferEnabled) {
      MessageDialogFragment.newInstance(
          R.string.dialog_title_menu_command_disabled,
          getString(R.string.dialog_command_disabled_insert_transfer,
              mAccounts[accountPosition].currency.getCurrencyCode()),
          MessageDialogFragment.Button.okButton(),
          null,null)
       .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
    } else {
      Intent i = new Intent(this, ExpenseEdit.class);
      i.putExtra("operationType", type);
      i.putExtra(KEY_ACCOUNTID,mAccountSpinner.getSelectedItemId());
      i.putExtra(KEY_PARENTID,mTransaction.id);
      startActivityForResult(i, EDIT_SPLIT_REQUEST);
    }
  }
  /**
   * calls the activity for selecting (and managing) categories
   */
  private void startSelectCategory() {
    Intent i = new Intent(this, ManageCategories.class);
    //we pass the currently selected category in to prevent
    //it from being deleted, which can theoretically lead
    //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
    i.putExtra(DatabaseConstants.KEY_ROWID, mCatId);
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
          android.text.format.DateFormat.is24HourFormat(this)
      );
    }
    return null;
  }

  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {
    mStatusSpinner.setSelection(mTransaction.crStatus.ordinal());
    if (mRowId != 0 || mTemplateId != 0) {
      //3 handle edit existing transaction or new one from template
      //3b  fill comment
      mCommentText.setText(mTransaction.comment);
      if (mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory)) {
        mPayeeText.setText(mTransaction.payee);
      }
    }
    if (mTransaction instanceof Template) {
      mTitleText.setText(((Template) mTransaction).title);
      if (mPlanId !=null) {
        //we need data from the cursor when launching the view intent
        //hence need to disable button until data is loaded
        mPlanButton.setEnabled(false);
        mManager.initLoader(EVENT_CURSOR, null, this);
      }
      mPlanToggleButton.setChecked(((Template) mTransaction).planExecutionAutomatic);
    } else
      mReferenceNumberText.setText(mTransaction.referenceNumber);
    if (!(mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer))
      setDateTime(mTransaction.getDate());

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
    mDateButton.setText(mDateFormat.format(mCalendar.getTime()));
  }

  /**
   * sets time on time button
   */
  private void setTime() {
    mTimeButton.setText(mTimeFormat.format(mCalendar.getTime()));
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

  protected void saveState() {
    if (syncStateAndValidate()) {
      //we are not interested in receiving onLoadFinished about
      //the updated plan, it will cause problems if we are in SAVE_AND_NEW
      //since we reset the plan to null in that case
      mManager.destroyLoader(EVENT_CURSOR);
      getSupportFragmentManager().beginTransaction()
        .add(DbWriteFragment.newInstance(true), "SAVE_TASK")
        .commit();
    } else {
      //prevent this flag from being sticky if form was not valid
      mCreateNew = false;
    }
  }
  /**
   * sets the state of the UI on mTransaction
   * @return false if any data is not valid, also informs user through toast
   */
  protected boolean syncStateAndValidate() {
    boolean validP = true;
    String title = "";
    //account cursor not yet loaded
    if (mAccounts == null)
      return false;
    Account account = mAccounts[mAccountSpinner.getSelectedItemPosition()];
    BigDecimal amount = validateAmountInput(true);

    if (amount == null) {
      //Toast is shown in validateAmountInput
      validP = false;
    } else {
      if (mType == EXPENSE) {
        amount = amount.negate();
      }
      mTransaction.amount.setCurrency(account.currency);
      mTransaction.amount.setAmountMajor(amount);
    }
    mTransaction.accountId = account.id;

    mTransaction.comment = mCommentText.getText().toString();

    if (mTransaction instanceof Template) {
      title = mTitleText.getText().toString();
      if (title.equals("")) {
        mTitleText.setError(getString(R.string.no_title_given));
        validP = false;
      }
      ((Template) mTransaction).title = title;
      ((Template) mTransaction).planId = mPlanId;
    } else
      mTransaction.referenceNumber = mReferenceNumberText.getText().toString();
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
        mTransaction.transfer_account = mTransferAccountSpinner.getSelectedItemId();
    }
    mTransaction.crStatus = (Transaction.CrStatus) mStatusSpinner.getSelectedItem();
    return validP;
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
      Log.i(MyApplication.TAG,"deleting unused plan " + mPlanId);
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
      mPlanButton.setText(Plan.prettyTimeInfo(this,mPlan.rrule, mPlan.dtstart));
      if (mTitleText.getText().toString().equals(""))
        mTitleText.setText(mPlan.title);
      mPlanToggleButton.setVisibility(View.VISIBLE);
    }
    mPlanButton.setEnabled(true);
  }
  private void configureStatusSpinner() {
    mStatusSpinner.setVisibility((
        mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer ||
        getCurrentAccount().type.equals(Type.CASH)) ?
      View.GONE : View.VISIBLE);
  }
  /**
   *  set label on category button
   */
  private void setCategoryButton() {
    if (mLabel != null && mLabel.length() != 0) {
      mCategoryButton.setText(mLabel);
    }
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("type", mType);
    outState.putSerializable("calendar", mCalendar);
    //restored in onCreate
    if (mRowId != 0)
      outState.putLong("rowId", mRowId);
    if (mCatId != null)
      outState.putLong("catId", mCatId);
    outState.putString("label", mLabel);
    if (mPlan != null)
      outState.putSerializable("plan",mPlan);
    long methodId = mMethodSpinner.getSelectedItemId();
    if (methodId != android.widget.AdapterView.INVALID_POSITION)
      outState.putLong("methodId", methodId);
    outState.putLong("accountId", mAccountSpinner.getSelectedItemId());
    if (mOperationType == MyExpenses.TYPE_TRANSFER)
      outState.putLong("transferAccountId", mTransferAccountSpinner.getSelectedItemId());
  }

  private void switchAccountViews() {
    ViewGroup accountParent = (ViewGroup) findViewById(R.id.AccountParent);
    if (getResources().getConfiguration().orientation ==  android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
      if (mType == INCOME) {
        accountParent.removeView(mAccountSpinner);
        accountParent.addView(mAccountSpinner);
      } else {
        accountParent.removeView(mTransferAccountSpinner);
        accountParent.addView(mTransferAccountSpinner);
      }
    } else {
      ViewGroup transferAccountRow = (ViewGroup) findViewById(R.id.TransferAccountRow);
      if (mType == INCOME) {
        accountParent.removeView(mAccountSpinner);
        transferAccountRow.removeView(mTransferAccountSpinner);
        accountParent.addView(mTransferAccountSpinner);
        transferAccountRow.addView(mAccountSpinner);
      } else {
        accountParent.removeView(mTransferAccountSpinner);
        transferAccountRow.removeView(mAccountSpinner);
        accountParent.addView(mAccountSpinner);
        transferAccountRow.addView(mTransferAccountSpinner);
      }
    }
  }
  public Money getAmount() {
    if (getCurrentAccount() == null)
      return null;
    Money result = new Money(getCurrentAccount().currency,0L);
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
  /*
   * callback of TaskExecutionFragment
   */
  @Override
  public void onPostExecute(int taskId,Object o) {
    switch(taskId) {
    case TaskExecutionFragment.TASK_NEW_PLAN:
      mPlanId = (Long) o;
      //unable to create new plan, inform user
      if (mPlanId == null) {
        MessageDialogFragment.Button createNewButton;
        int message;
        int selectButtonLabel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          createNewButton =
                new MessageDialogFragment.Button(
                    R.string.dialog_setup_planner_button_create_new,
                    R.id.CREATE_COMMAND,
                    null);
          message = R.string.planner_setup_info_jb;
          selectButtonLabel = R.string.dialog_setup_planner_button_select_existing;
        } else {
          createNewButton = null;
          message = R.string.planner_setup_info;
          selectButtonLabel = android.R.string.yes;
        }
        MessageDialogFragment.newInstance(
            R.string.dialog_title_planner_setup_info,
            message,
            new MessageDialogFragment.Button(
                selectButtonLabel,
                R.id.SETTINGS_COMMAND,
                null),
            createNewButton,
            MessageDialogFragment.Button.noButton())
         .show(getSupportFragmentManager(),"CALENDAR_SETUP_INFO");
      } else if (mPlanId == 0L) {
        mPlanId = null;
        Toast.makeText(
            this,
            "Unable to create plan. Need WRITE_CALENDAR permission.",
            Toast.LENGTH_LONG).show();
      } else {
        mLaunchPlanView = true;
        if (mManager.getLoader(EVENT_CURSOR) != null && !mManager.getLoader(EVENT_CURSOR).isReset())
          mManager.restartLoader(EVENT_CURSOR, null, ExpenseEdit.this);
        else
          mManager.initLoader(EVENT_CURSOR, null, ExpenseEdit.this);
      }
      break;
    case TaskExecutionFragment.TASK_NEW_CALENDAR:
      boolean success= (Boolean) o;
      Toast.makeText(
          this,
          success ? R.string.planner_create_calendar_success : R.string.planner_create_calendar_failure,
          Toast.LENGTH_LONG).show();
      //if we successfully created the calendar, we set up the plan immediately
      if (success) {
        launchNewPlan();
      }
      break;
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
      if (o==null) {
        Toast.makeText(this, R.string.save_transaction_template_deleted,Toast.LENGTH_LONG).show();
        finish();
        return;
      }
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
    case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
      if (o==null) {
        Toast.makeText(this, "Object has been deleted from db",Toast.LENGTH_LONG).show();
        finish();
        return;
      }
      mTransaction = (Transaction) o;
      if (taskId == TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE) {
        if (mPlanInstanceId > 0L) {
          mTransaction.originPlanInstanceId = mPlanInstanceId;
        }
        if (mPlanInstanceDate != 0L) {
          mTransaction.setDate(new Date(mPlanInstanceDate));
        }
      }
      if (mTransaction instanceof SplitTransaction) {
        mOperationType = MyExpenses.TYPE_SPLIT;
      }
      else if (mTransaction instanceof Template) {
        mOperationType = ((Template) mTransaction).isTransfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
        mPlanId = ((Template) mTransaction).planId;
      }
      else
        mOperationType = mTransaction instanceof Transfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
      //if catId has already been set by onRestoreInstanceState, the value might have been edited by the user and has precedence
      if (mCatId == null) {
        mCatId = mTransaction.catId;
        mLabel =  mTransaction.label;
      }
      setup();
      supportInvalidateOptionsMenu();
      break;
    }
    super.onPostExecute(taskId, o);
  }
  public Account getCurrentAccount() {
    if (mAccounts == null) {
        return null;
    }
    int selected = mAccountSpinner.getSelectedItemPosition();
    if (selected == android.widget.AdapterView.INVALID_POSITION) {
      return null;
    }
    return mAccounts[selected];
  }
  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
      long id) {
    switch(parent.getId()) {
    case R.id.Method:
      if (id>0) {
        //ignore first row "no method" merged in
        mMethodsCursor.moveToPosition(position-1);
        if (!(mTransaction instanceof Template))
          mReferenceNumberText.setVisibility(mMethodsCursor.getInt(mMethodsCursor.getColumnIndexOrThrow(KEY_IS_NUMBERED))>0 ?
              View.VISIBLE : View.INVISIBLE);
      }
      else {
        mTransaction.methodId = null;
        mReferenceNumberText.setVisibility(View.INVISIBLE);
      }
      break;
    case R.id.Account:
      mAmountLabel.setText(getString(R.string.amount) + " ("+mAccounts[position].currency.getSymbol()+")");
      if (mOperationType == MyExpenses.TYPE_TRANSFER) {
        setTransferAccountFilterMap();
      }
      if (mTransaction instanceof SplitTransaction) {
        ((SplitPartList) getSupportFragmentManager().findFragmentByTag("SPLIT_PART_LIST")).updateBalance();
      }
      configureStatusSpinner();
    }
  }
  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // TODO Auto-generated method stub    
  }

  /*
   * callback of DbWriteFragment
   */
  @Override
  public void onPostExecute(Object result) {
    Long sequenceCount = (Long) result;
    if (sequenceCount == -1L) {
      if (mTransaction instanceof Template) {
        //for the moment, the only case where saving will fail
        //if the unique constraint for template titles is violated
        //TODO: we should probably validate the title earlier
        mTitleText.setError(getString(R.string.template_title_exists,((Template) mTransaction).title));
        mCreateNew = false;
      } else {
        Toast.makeText(this, "Unknown error while saving transaction", Toast.LENGTH_SHORT).show();
      }
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
        //no need to call super after finish
        return;
      }
    }
    super.onPostExecute(result);
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
          .appendPath(getCurrentAccount().type.name())
          .build(), null, null, null, null);
    case ACCOUNTS_CURSOR:
        String selection = (mOperationType == MyExpenses.TYPE_TRANSFER) ? 
            "(select count(*) from accounts t where currency = accounts.currency)>1" :
            null;
      return new CursorLoader(this,TransactionProvider.ACCOUNTS_BASE_URI,
          null,
          selection,null,null);
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
      View methodContainer = findViewById(R.id.MethodRow);
      if (!data.moveToFirst()) {
        methodContainer.setVisibility(View.GONE);
      } else {
        methodContainer.setVisibility(View.VISIBLE);
        MatrixCursor extras = new MatrixCursor(new String[] { KEY_ROWID,KEY_LABEL,KEY_IS_NUMBERED });
        extras.addRow(new String[] { "0", "- - - -","0" });
        mMethodsAdapter.swapCursor(new MergeCursor(new Cursor[] {extras,data}));
        if (mSavedInstance) {
          mTransaction.methodId = mMethodId;
        }
        if (mTransaction.methodId != null) {
          while (data.isAfterLast() == false) {
            if (data.getLong(data.getColumnIndex(KEY_ROWID)) == mTransaction.methodId) {
              mMethodSpinner.setSelection(data.getPosition()+1);
              break;
            }
            data.moveToNext();
          }
        } else {
          mMethodSpinner.setSelection(0);
        }
      }
      break;
    case ACCOUNTS_CURSOR:
      if (data.getCount()==0) {
        Toast.makeText(this,"Error loading accounts list ",Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      mAccountsAdapter.swapCursor(data);
      mAccounts = new Account[data.getCount()];
      if (mSavedInstance) {
        mTransaction.accountId = mAccountId;
        mTransaction.transfer_account = mTransferAccountId;
      }
      data.moveToFirst();
      while (data.isAfterLast() == false) {
        int position = data.getPosition();
        long _id = data.getLong(data.getColumnIndex(KEY_ROWID));
        mAccounts[position] = Account.isInstanceCached(_id) ?
            Account.getInstanceFromDb(_id):
            new Account(data);
        if(mTransaction.accountId != null && _id == mTransaction.accountId) {
          mAccountSpinner.setSelection(position);
        }
        data.moveToNext();
      }
      //if the accountId we have been passed does not exist, we select the first entry
      if (mAccountSpinner.getSelectedItemPosition() == android.widget.AdapterView.INVALID_POSITION) {
        mAccountSpinner.setSelection(0);
        mTransaction.accountId = mAccounts[0].id;
      }
      if (mOperationType == MyExpenses.TYPE_TRANSFER) {
        mTransferAccountCursor = new FilterCursorWrapper(data);
        int selectedPosition = setTransferAccountFilterMap();
        mTransferAccountsAdapter.swapCursor(mTransferAccountCursor);
        mTransferAccountSpinner.setSelection(selectedPosition);
      } else {
        //the methods cursor is based on the current account,
        //hence it is loaded only after the accounts cursor is loaded
        if (!(mTransaction instanceof SplitPartCategory)) {
          mManager.initLoader(METHODS_CURSOR, null, this);
        }
      }
      mTypeButton.setEnabled(true);
      configureType();
      configureStatusSpinner();
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
              title,
              "" // we do not need the description stored in the event
              );
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
  private int setTransferAccountFilterMap() {
    Account fromAccount = mAccounts[mAccountSpinner.getSelectedItemPosition()];
    ArrayList<Integer> list = new ArrayList<Integer>();
    int position = 0,selectedPosition = 0;
    for (int i = 0; i < mAccounts.length; i++) {
      if (fromAccount.id != mAccounts[i].id &&
          fromAccount.currency.equals(mAccounts[i].currency)) {
        list.add(i);
        if (mTransaction.transfer_account != null && mTransaction.transfer_account == mAccounts[i].id) {
          selectedPosition = position;
        }
        position++;
      }
    }
    mTransferAccountCursor.setFilterMap(list);
    mTransferAccountsAdapter.notifyDataSetChanged();
    return selectedPosition;
  }
  private void launchPlanView() {
    mLaunchPlanView = false;
    //unfortunately ACTION_EDIT does not work see http://code.google.com/p/android/issues/detail?id=39402
    Intent intent = new Intent (Intent.ACTION_VIEW);
    intent.setData(ContentUris.withAppendedId(Events.CONTENT_URI, mPlanId));
    //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan.dtstart);
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan.dtstart);
    if (Utils.isIntentAvailable(this, intent)) {
      //TODO on the Xperia X8 the calendar app started with this intent crashes
      //can we catch such a crash and inform the user?
      startActivityForResult (intent, EDIT_EVENT_REQUEST);
    } else {
      Log.w(MyApplication.TAG,"no intent found for viewing event in calendar");
    }
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
  private void launchNewPlan() {
    String description = ((Template) mTransaction).compileDescription(ExpenseEdit.this);
    getSupportFragmentManager().beginTransaction()
    .add(TaskExecutionFragment.newInstance(
        TaskExecutionFragment.TASK_NEW_PLAN,
        0L,
        new Plan(
            0,
            System.currentTimeMillis(),
            "",
            ((Template) mTransaction).title,
            description)),
        "ASYNC_TASK")
    .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_create_plan),"PROGRESS")
    .commit();
  }
  public void disableAccountSpinner() {
    mAccountSpinner.setEnabled(false);
  }
}
