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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;
import com.squareup.picasso.Picasso;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CrStatusAdapter;
import org.totschnig.myexpenses.adapter.OperationTypeAdapter;
import org.totschnig.myexpenses.adapter.RecurrenceAdapter;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitPartCategory;
import org.totschnig.myexpenses.model.SplitPartTransfer;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter.CursorToStringConverter;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.FilterCursorWrapper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT;

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
public class ExpenseEdit extends AmountActivity implements
    OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>,
    ContribIFace, ConfirmationDialogListener {

  private static final String SPLIT_PART_LIST = "SPLIT_PART_LIST";
  public static final String KEY_NEW_TEMPLATE = "newTemplate";
  public static final String KEY_CLONE = "clone";
  private static final String KEY_CALENDAR = "calendar";
  private static final String PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET = "transactionLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET = "transferLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET = "transferLastTransferAccountFromWidget";
  private static final String PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET = "splitLastAccountFromWidget";
  public static final int EXCHANGE_RATE_FRACTION_DIGITS = 5;
  private static final BigDecimal nullValue = new BigDecimal(0);
  private Button mDateButton;
  private Button mTimeButton;
  private EditText mCommentText, mTitleText, mReferenceNumberText;
  private AmountEditText mTransferAmountText, mExchangeRate1Text, mExchangeRate2Text;
  private Button mCategoryButton, mPlanButton;
  private Spinner mMethodSpinner;
  private SpinnerHelper mAccountSpinner, mTransferAccountSpinner, mStatusSpinner,
      mOperationTypeSpinner, mReccurenceSpinner;
  private SimpleCursorAdapter mMethodsAdapter, mAccountsAdapter, mTransferAccountsAdapter, mPayeeAdapter;
  private OperationTypeAdapter mOperationTypeAdapter;
  private FilterCursorWrapper mTransferAccountCursor;
  private AutoCompleteTextView mPayeeText;
  protected TextView mPayeeLabel;
  private ToggleButton mPlanToggleButton;
  private ImageView mAttachPictureButton;
  private FrameLayout mPictureViewContainer;
  public Long mRowId = 0L;
  private Long mTemplateId;
  private Account[] mAccounts;
  private Calendar mCalendar = Calendar.getInstance();
  private DateFormat mDateFormat, mTimeFormat;
  private Long mCatId = null, mMethodId = null,
      mAccountId = null, mTransferAccountId;
  private String mLabel;
  private Transaction mTransaction;
  private Cursor mMethodsCursor;
  private Plan mPlan;
  private Uri mPictureUri, mPictureUriTemp;

  private long mPlanInstanceId, mPlanInstanceDate;
  /**
   * transaction, transfer or split
   */
  private int mOperationType;


  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;

  public static final int METHODS_CURSOR = 2;
  public static final int ACCOUNTS_CURSOR = 3;
  public static final int TRANSACTION_CURSOR = 5;
  public static final int SUM_CURSOR = 6;
  public static final int LAST_EXCHANGE_CURSOR = 7;
  private static final String KEY_PICTURE_URI = "picture_uri";
  private static final String KEY_PICTURE_URI_TMP = "picture_uri_tmp";

  private LoaderManager mManager;

  protected boolean mClone = false;
  protected boolean mCreateNew;
  protected boolean mIsMainTransactionOrTemplate;
  protected boolean mSavedInstance;
  protected boolean mRecordTemplateWidget;
  private boolean mIsResumed;
  boolean isProcessingLinkedAmountInputs = false;
  private ContentObserver pObserver;
  private boolean mPlanUpdateNeeded;

  public enum HelpVariant {
    transaction, transfer, split, templateCategory, templateTransfer, splitPartCategory, splitPartTransfer
  }

  @Override
  int getDiscardNewMessage() {
    return mTransaction instanceof Template ? R.string.dialog_confirm_discard_new_template :
        R.string.dialog_confirm_discard_new_transaction;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.one_expense);


    mDateFormat = android.text.format.DateFormat.getDateFormat(this);
    mTimeFormat = android.text.format.DateFormat.getTimeFormat(this);

    setupToolbar();
    mManager = getSupportLoaderManager();
    //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
    configTypeButton();
    mTypeButton.setEnabled(false);
    mCommentText = (EditText) findViewById(R.id.Comment);
    mTitleText = (EditText) findViewById(R.id.Title);
    mReferenceNumberText = (EditText) findViewById(R.id.Number);
    mDateButton = (Button) findViewById(R.id.DateButton);
    mAttachPictureButton = (ImageView) findViewById(R.id.AttachImage);
    mPictureViewContainer = (FrameLayout) findViewById(R.id.picture_container);
    mTimeButton = (Button) findViewById(R.id.TimeButton);
    mPayeeLabel = (TextView) findViewById(R.id.PayeeLabel);
    mPayeeText = (AutoCompleteTextView) findViewById(R.id.Payee);
    mTransferAmountText = (AmountEditText) findViewById(R.id.TranferAmount);
    mExchangeRate1Text = (AmountEditText) findViewById(R.id.ExchangeRate_1);
    mExchangeRate1Text.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    mExchangeRate1Text.addTextChangedListener(new LinkedExchangeRateTextWatchter(true));
    mExchangeRate2Text = (AmountEditText) findViewById(R.id.ExchangeRate_2);
    mExchangeRate2Text.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    mExchangeRate2Text.addTextChangedListener(new LinkedExchangeRateTextWatchter(false));

    mPayeeAdapter = new SimpleCursorAdapter(this, R.layout.support_simple_spinner_dropdown_item, null,
        new String[]{KEY_PAYEE_NAME},
        new int[]{android.R.id.text1},
        0);
    mPayeeText.setAdapter(mPayeeAdapter);
    mPayeeAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      @SuppressLint("NewApi")
      public Cursor runQuery(CharSequence str) {
        if (str == null) {
          return null;
        }
        String search = Utils.esacapeSqlLikeExpression(Utils.normalize(str.toString()));
        //we accept the string at the beginning of a word
        String selection = KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
            KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
            KEY_PAYEE_NAME_NORMALIZED + " LIKE ?";
        String[] selectArgs = {search + "%", "% " + search + "%", "%." + search + "%"};
        return getContentResolver().query(
            TransactionProvider.PAYEES_URI,
            new String[]{
                KEY_ROWID,
                KEY_PAYEE_NAME,
                "(SELECT max(" + KEY_ROWID
                    + ") FROM " + TABLE_TRANSACTIONS
                    + " WHERE " + WHERE_NOT_SPLIT + " AND "
                    + KEY_PAYEEID + " = " + TABLE_PAYEES + "." + KEY_ROWID + ")"},
            selection, selectArgs, null);
      }
    });

    mPayeeAdapter.setCursorToStringConverter(new CursorToStringConverter() {
      public CharSequence convertToString(Cursor cur) {
        return cur.getString(1);
      }
    });
    mPayeeText.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor) mPayeeAdapter.getItem(position);
        if (c.moveToPosition(position)) {
          mTransaction.updatePayeeWithId(c.getString(1), c.getLong(0));
          if (mNewInstance && mTransaction != null &&
              !(mTransaction instanceof Template || mTransaction instanceof SplitTransaction)) {
            //moveToPosition should not be necessary,
            //but has been reported to not be positioned correctly on samsung GT-I8190N
            if (!c.isNull(2)) {
              if (MyApplication.PrefKey.AUTO_FILL_HINT_SHOWN.getBoolean(false)) {
                if (MyApplication.PrefKey.AUTO_FILL.getBoolean(true)) {
                  startAutoFill(c.getLong(2));
                }
              } else {
                Bundle b = new Bundle();
                b.putLong(KEY_ROWID, c.getLong(2));
                b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information);
                b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string
                    .hint_auto_fill));
                b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND);
                b.putString(ConfirmationDialogFragment.KEY_PREFKEY, MyApplication.PrefKey
                    .AUTO_FILL_HINT_SHOWN.getKey());
                b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes);
                b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no);
                ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(),
                    "AUTO_FILL_HINT");
              }
            }
          }
        }
      }
    });

    mCategoryButton = (Button) findViewById(R.id.Category);
    mPlanButton = (Button) findViewById(R.id.Plan);
    mMethodSpinner = (Spinner) findViewById(R.id.Method);
    mAccountSpinner = new SpinnerHelper(findViewById(R.id.Account));
    mTransferAccountSpinner = new SpinnerHelper(findViewById(R.id.TransferAccount));
    mTransferAccountSpinner.setOnItemSelectedListener(this);
    mStatusSpinner = new SpinnerHelper(findViewById(R.id.Status));
    mReccurenceSpinner = new SpinnerHelper(findViewById(R.id.Recurrence));
    mPlanToggleButton = (ToggleButton) findViewById(R.id.togglebutton);
    TextPaint paint = mPlanToggleButton.getPaint();
    int automatic = (int) paint.measureText(getString(R.string.plan_automatic));
    int manual = (int) paint.measureText(getString(R.string.plan_manual));
    mPlanToggleButton.setWidth(
        (automatic > manual ? automatic : manual) +
            +mPlanToggleButton.getPaddingLeft()
            + mPlanToggleButton.getPaddingRight());

    mRowId = Utils.getFromExtra(getIntent().getExtras(), KEY_ROWID, 0);

    //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
    if (savedInstanceState != null) {
      mSavedInstance = true;
      mRowId = savedInstanceState.getLong(KEY_ROWID);
      mPictureUri = savedInstanceState.getParcelable(KEY_PICTURE_URI);
      mPictureUriTemp = savedInstanceState.getParcelable(KEY_PICTURE_URI_TMP);
      if (mPictureUri != null) {
        setPicture();
      }

      mCalendar = (Calendar) savedInstanceState.getSerializable(KEY_CALENDAR);
      mLabel = savedInstanceState.getString(KEY_LABEL);
      if ((mCatId = savedInstanceState.getLong(KEY_CATID)) == 0L) {
        mCatId = null;
      }
      if ((mMethodId = savedInstanceState.getLong(KEY_METHODID)) == 0L)
        mMethodId = null;
      if ((mAccountId = savedInstanceState.getLong(KEY_ACCOUNTID)) == 0L) {
        mAccountId = null;
      } else {
        //once user has selected account, we no longer want
        //the passed in KEY_CURRENCY to override it in onLoadFinished
        getIntent().removeExtra(KEY_CURRENCY);
      }
      if ((mTransferAccountId = savedInstanceState.getLong(KEY_TRANSFER_ACCOUNT)) == 0L)
        mTransferAccountId = null;
    }
    mTemplateId = getIntent().getLongExtra(KEY_TEMPLATEID, 0);
    //were we called from a notification
    int notificationId = getIntent().getIntExtra(MyApplication.KEY_NOTIFICATION_ID, 0);
    if (notificationId > 0) {
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
    }


    CrStatusAdapter sAdapter = new CrStatusAdapter(this) {

      @Override
      public boolean isEnabled(int position) {
        //if the transaction is reconciled, the status can not be changed
        //otherwise only unreconciled and cleared can be set
        return mTransaction != null && mTransaction.crStatus != CrStatus.RECONCILED && position != CrStatus.RECONCILED.ordinal();
      }
    };
    mStatusSpinner.setAdapter(sAdapter);

    //1. fetch the transaction or create a new instance
    if (mRowId != 0 || mTemplateId != 0) {
      mNewInstance = false;
      int taskId;
      Serializable extra = null;
      Long objectId;
      if (mRowId != 0) {
        taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION;
        //if called with extra KEY_CLONE, we ask the task to clone, but no longer after orientation change
        extra = getIntent().getBooleanExtra(KEY_CLONE, false) && savedInstanceState == null;
        objectId = mRowId;
      } else {
        objectId = mTemplateId;
        //are we editing the template or instantiating a new one
        if ((mPlanInstanceId = getIntent().getLongExtra(KEY_INSTANCEID, 0)) != 0L) {
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE;
          mPlanInstanceDate = getIntent().getLongExtra(KEY_DATE, 0);
          mRecordTemplateWidget =
              getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false) &&
                  !ContribFeature.TEMPLATE_WIDGET.hasAccess();
        } else {
          taskId = TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE;
        }
      }
      FragmentManager fm = getSupportFragmentManager();
      if (fm.findFragmentByTag(ProtectionDelegate.ASYNC_TAG) == null) {
        startTaskExecution(
            taskId,
            new Long[]{objectId},
            extra,
            R.string.progress_dialog_loading);
      }
    } else {
      mOperationType = getIntent().getIntExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSACTION);
      if (!isValidType(mOperationType)) {
        mOperationType = MyExpenses.TYPE_TRANSACTION;
      }
      if (mOperationType == MyExpenses.TYPE_SPLIT && !ContribFeature.SPLIT_TRANSACTION.hasAccess() &&
          ContribFeature.SPLIT_TRANSACTION.usagesLeft() < 1) {
        Toast.makeText(this, ContribFeature.SPLIT_TRANSACTION.buildRequiresString(this),
            Toast.LENGTH_LONG).show();
        finish();
        return;
      }
      final Long parentId = getIntent().getLongExtra(KEY_PARENTID, 0);
      final boolean isNewTemplate = getIntent().getBooleanExtra(KEY_NEW_TEMPLATE, false);
      getSupportActionBar().setDisplayShowTitleEnabled(false);
      View spinner = findViewById(R.id.OperationType);
      mOperationTypeSpinner = new SpinnerHelper(spinner);
      spinner.setVisibility(View.VISIBLE);
      List<Integer> allowedOperationTypes = new ArrayList<>();
      allowedOperationTypes.add(MyExpenses.TYPE_TRANSACTION);
      allowedOperationTypes.add(MyExpenses.TYPE_TRANSFER);
      if (!isNewTemplate && parentId == 0) {
        allowedOperationTypes.add(MyExpenses.TYPE_SPLIT);
      }
      mOperationTypeAdapter = new OperationTypeAdapter(this, allowedOperationTypes,
          isNewTemplate, parentId != 0);
      mOperationTypeSpinner.setAdapter(mOperationTypeAdapter);
      resetOperationType();
      mOperationTypeSpinner.setOnItemSelectedListener(this);
      Long accountId = getIntent().getLongExtra(KEY_ACCOUNTID, 0);
      if (isNewTemplate) {
        mTransaction = Template.getTypedNewInstance(mOperationType, accountId);
      } else {
        switch (mOperationType) {
          case MyExpenses.TYPE_TRANSACTION:
            if (accountId == 0L) {
              accountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, 0L);
            }
            mTransaction = parentId == 0L ?
                Transaction.getNewInstance(accountId) :
                SplitPartCategory.getNewInstance(accountId, parentId);
            break;
          case MyExpenses.TYPE_TRANSFER:
            Long transfer_account = 0L;
            if (accountId == 0L) {
              accountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET, 0L);
              transfer_account = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, 0L);
            }
            mTransaction = parentId == 0L ?
                Transfer.getNewInstance(accountId, transfer_account) :
                SplitPartTransfer.getNewInstance(accountId, parentId, transfer_account);
            break;
          case MyExpenses.TYPE_SPLIT:
            if (accountId == 0L) {
              accountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET, 0L);
            }
            mTransaction = SplitTransaction.getNewInstance(accountId);
            //Split transactions are returned persisted to db and already have an id
            if (mTransaction != null) {
              mRowId = mTransaction.getId();
            }
            break;
        }
      }
      if (mTransaction == null) {
        String errMsg = "Error instantiating transaction for account " + accountId;
        Utils.reportToAcra(new IllegalStateException(errMsg),
            "Extras", getIntent().getExtras().toString());
        Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      setup();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    mIsResumed = true;
    if (mAccounts != null) setupListeners();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (pObserver != null) {
      try {
        ContentResolver cr = getContentResolver();
        cr.unregisterContentObserver(pObserver);
      } catch (IllegalStateException ise) {
        // Do Nothing.  Observer has already been unregistered.
      }
    }
  }

  private void setup() {
    mAmountText.setFractionDigits(Money.getFractionDigits(mTransaction.getAmount().getCurrency()));
    linkInputsWithLabels();
    if (mTransaction instanceof SplitTransaction) {
      mAmountText.addTextChangedListener(new MyTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
          findSplitPartList().updateBalance();
        }
      });
    }
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      mAmountText.addTextChangedListener(new LinkedTransferAmountTextWatcher(true));
      mTransferAmountText.addTextChangedListener(new LinkedTransferAmountTextWatcher(false));
    }
    // Spinner for account and transfer account
    mAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
        new String[]{KEY_LABEL}, new int[]{android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    if (mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer) {
      disableAccountSpinner();
    }
    mIsMainTransactionOrTemplate = mOperationType != MyExpenses.TYPE_TRANSFER && !(mTransaction instanceof SplitPartCategory);

    if (mIsMainTransactionOrTemplate) {

      // Spinner for methods
      mMethodsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
          new String[]{KEY_LABEL}, new int[]{android.R.id.text1}, 0);
      mMethodsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
      mMethodSpinner.setAdapter(mMethodsAdapter);
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
      accountLabelTv.setText(R.string.transfer_from_account);

      mTransferAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
          new String[]{KEY_LABEL}, new int[]{android.R.id.text1}, 0);
      mTransferAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
      mTransferAccountSpinner.setAdapter(mTransferAccountsAdapter);
    }

    mManager.initLoader(ACCOUNTS_CURSOR, null, this);

    if (mTransaction instanceof Template) {
      findViewById(R.id.TitleRow).setVisibility(View.VISIBLE);
      if (!calendarPermissionPermanentlyDeclined()) {
        //if user has denied access and checked that he does not want to be asked again, we do not
        //bother him with a button that is not working
        setPlannerRowVisibility(View.VISIBLE);
        RecurrenceAdapter recurrenceAdapter = new RecurrenceAdapter(this);
        mReccurenceSpinner.setAdapter(recurrenceAdapter);
        mReccurenceSpinner.setOnItemSelectedListener(this);
        mPlanButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View view) {
            if (mPlan == null) {
              showDialog(DATE_DIALOG_ID);
            } else {
              launchPlanView();
            }
          }
        });
      }
      mAttachPictureButton.setVisibility(View.GONE);
      setTitle(
          getString(mTransaction.getId() == 0 ? R.string.menu_create_template : R.string.menu_edit_template)
              + " ("
              + getString(mOperationType == MyExpenses.TYPE_TRANSFER ? R.string.transfer : R.string.transaction)
              + ")");
      helpVariant = mOperationType == MyExpenses.TYPE_TRANSFER ?
          HelpVariant.templateTransfer : HelpVariant.templateCategory;
    } else if (mTransaction instanceof SplitTransaction) {
      setTitle(mNewInstance ? R.string.menu_create_split : R.string.menu_edit_split);
      //SplitTransaction are always instantiated with status uncommitted,
      //we save them to DB as uncommitted, before working with them
      //when the split transaction is saved the split and its parts are committed
      categoryContainer.setVisibility(View.GONE);
      //add split list
      if (findSplitPartList() == null) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
            .add(R.id.OneExpense, SplitPartList.newInstance(mTransaction.getId(), mTransaction.accountId), SPLIT_PART_LIST)
            .commit();
        fm.executePendingTransactions();
      }
      helpVariant = HelpVariant.split;
    } else {
      if (mTransaction instanceof SplitPartCategory) {
        setTitle(mTransaction.getId() == 0 ?
            R.string.menu_create_split_part_category : R.string.menu_edit_split_part_category);
        helpVariant = HelpVariant.splitPartCategory;
        mTransaction.status = STATUS_UNCOMMITTED;
      } else if (mTransaction instanceof SplitPartTransfer) {
        setTitle(mTransaction.getId() == 0 ?
            R.string.menu_create_split_part_transfer : R.string.menu_edit_split_part_transfer);
        helpVariant = HelpVariant.splitPartTransfer;
        mTransaction.status = STATUS_UNCOMMITTED;
      } else if (mTransaction instanceof Transfer) {
        setTitle(mTransaction.getId() == 0 ?
            R.string.menu_create_transfer : R.string.menu_edit_transfer);
        helpVariant = HelpVariant.transfer;
      } else if (mTransaction instanceof Transaction) {
        setTitle(mTransaction.getId() == 0 ?
            R.string.menu_create_transaction : R.string.menu_edit_transaction);
        helpVariant = HelpVariant.transaction;
      }
    }
    if (mClone) {
      setTitle(R.string.menu_clone_transaction);
    }

    if (mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer) {
      findViewById(R.id.DateTimeRow).setVisibility(View.GONE);
    } else {
      //noinspection SetTextI18n
      ((TextView) findViewById(R.id.DateTimeLabel)).setText(getString(
          R.string.date) + " / " + getString(R.string.time));
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

    //when we have a savedInstance, fields have already been populated
    if (!mSavedInstance) {
      populateFields();
    }

    if (!(mTransaction instanceof SplitPartCategory || mTransaction instanceof SplitPartTransfer)) {
      setDateTime();
    }
    //after setdatetime, so that the plan info can override the date
    configurePlan();


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
  }

  private void setPlannerRowVisibility(int visibility) {
    findViewById(R.id.PlannerRow).setVisibility(visibility);
  }

  @Override
  protected void setupListeners() {
    super.setupListeners();
    mCommentText.addTextChangedListener(this);
    mTitleText.addTextChangedListener(this);
    mPayeeText.addTextChangedListener(this);
    mReferenceNumberText.addTextChangedListener(this);
    mAccountSpinner.setOnItemSelectedListener(this);
    mMethodSpinner.setOnItemSelectedListener(this);
    mStatusSpinner.setOnItemSelectedListener(this);
  }

  @Override
  protected void linkInputsWithLabels() {
    super.linkInputsWithLabels();
    linkAccountLabels();
    linkInputWithLabel(mTitleText, findViewById(R.id.TitleLabel));
    linkInputWithLabel(mDateButton, findViewById(R.id.DateTimeLabel));
    linkInputWithLabel(mTimeButton, findViewById(R.id.DateTimeLabel));
    linkInputWithLabel(mPayeeText, mPayeeLabel);
    View commentLabel = findViewById(R.id.CommentLabel);
    linkInputWithLabel(mStatusSpinner.getSpinner(), commentLabel);
    linkInputWithLabel(mAttachPictureButton, commentLabel);
    linkInputWithLabel(mPictureViewContainer, commentLabel);
    linkInputWithLabel(mCommentText, commentLabel);
    linkInputWithLabel(mCategoryButton, findViewById(R.id.CategoryLabel));
    View methodLabel = findViewById(R.id.MethodLabel);
    linkInputWithLabel(mMethodSpinner, methodLabel);
    linkInputWithLabel(mReferenceNumberText, methodLabel);
    linkInputWithLabel(mPlanButton, findViewById(R.id.PlanLabel));
    final View transferAmountLabel = findViewById(R.id.TransferAmountLabel);
    linkInputWithLabel(mTransferAmountText, transferAmountLabel);
    linkInputWithLabel(findViewById(R.id.CalculatorTransfer), transferAmountLabel);
    final View exchangeRateAmountLabel = findViewById(R.id.ExchangeRateLabel);
    linkInputWithLabel(findViewById(R.id.ExchangeRate_1), exchangeRateAmountLabel);
    linkInputWithLabel(findViewById(R.id.ExchangeRate_2), exchangeRateAmountLabel);
  }

  private void linkAccountLabels() {
    final View accountLabel = findViewById(R.id.AccountLabel);
    final View transferAccountLabel = findViewById(R.id.TransferAccountLabel);
    linkInputWithLabel(mAccountSpinner.getSpinner(),
        mType == INCOME ? transferAccountLabel : accountLabel);
    linkInputWithLabel(mTransferAccountSpinner.getSpinner(),
        mType == INCOME ? accountLabel : transferAccountLabel);
  }

  @Override
  protected void onTypeChanged(boolean isClicked) {
    super.onTypeChanged(isClicked);
    if (mTransaction != null && mIsMainTransactionOrTemplate) {
      mTransaction.methodId = null;
      if (mManager.getLoader(METHODS_CURSOR) != null && !mManager.getLoader(METHODS_CURSOR).isReset()) {
        mManager.restartLoader(METHODS_CURSOR, null, this);
      } else {
        mManager.initLoader(METHODS_CURSOR, null, this);
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (!(mTransaction instanceof SplitPartCategory || mTransaction instanceof SplitPartTransfer ||
        mTransaction instanceof Template ||
        (mTransaction instanceof SplitTransaction && !MyApplication.getInstance().isContribEnabled()))) {
      MenuItemCompat.setShowAsAction(
          menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
              .setIcon(R.drawable.ic_action_save_new),
          MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      MenuItemCompat.setShowAsAction(
          menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
              .setIcon(R.drawable.ic_menu_move), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case android.R.id.home:
        cleanup();
        finish();
        return true;
      case R.id.SAVE_COMMAND:
      case R.id.SAVE_AND_NEW_COMMAND:
        if (mTransaction instanceof SplitTransaction &&
            !findSplitPartList().splitComplete()) {
          Toast.makeText(this, getString(R.string.unsplit_amount_greater_than_zero), Toast.LENGTH_SHORT).show();
          return true;
        }
        if (command == R.id.SAVE_COMMAND) {
          //handled in super
          break;
        }
        if (!mIsSaving) {
          mCreateNew = true;
          saveState();
        }
        return true;
      case R.id.CREATE_COMMAND:
        createRow();
        return true;
      case R.id.INVERT_TRANSFER_COMMAND:
        mType = !mType;
        switchAccountViews();
    }
    return super.dispatchCommand(command, tag);
  }

  private boolean checkTransferEnabled(Account account) {
    if (account == null)
      return false;
    if (!(mAccounts.length > 1)) {
      MessageDialogFragment.newInstance(
          0,
          getString(R.string.dialog_command_disabled_insert_transfer),
          MessageDialogFragment.Button.okButton(),
          null, null)
          .show(getSupportFragmentManager(), "BUTTON_DISABLED_INFO");
      return false;
    }
    return true;
  }

  private void createRow() {
    Account account = getCurrentAccount();
    if (account == null) {
      Toast.makeText(this, R.string.account_list_not_yet_loaded, Toast.LENGTH_LONG).show();
      return;
    }
    Intent i = new Intent(this, ExpenseEdit.class);
    forwardDataEntryFromWidget(i);
    i.putExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, account.getId());
    i.putExtra(KEY_PARENTID, mTransaction.getId());
    startActivityForResult(i, EDIT_SPLIT_REQUEST);
  }

  /**
   * calls the activity for selecting (and managing) categories
   */
  private void startSelectCategory() {
    Intent i = new Intent(this, ManageCategories.class);
    forwardDataEntryFromWidget(i);
    //we pass the currently selected category in to prevent
    //it from being deleted, which can theoretically lead
    //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
    i.putExtra(KEY_ROWID, mCatId);
    startActivityForResult(i, SELECT_CATEGORY_REQUEST);
  }

  /**
   * listens on changes in the date dialog and sets the date on the button
   */
  private DatePickerDialog.OnDateSetListener mDateSetListener =
      new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year,
                              int monthOfYear, int dayOfMonth) {
          if (mCalendar.get(Calendar.YEAR) != year ||
              mCalendar.get(Calendar.MONTH) != monthOfYear ||
              mCalendar.get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
            mCalendar.set(year, monthOfYear, dayOfMonth);
            setDate();
            setDirty(true);
          }
        }
      };

  /**
   * listens on changes in the time dialog and sets the time on the button
   */
  private TimePickerDialog.OnTimeSetListener mTimeSetListener =
      new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
          if (mCalendar.get(Calendar.HOUR_OF_DAY) != hourOfDay ||
              mCalendar.get(Calendar.MINUTE) != minute) {
            mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mCalendar.set(Calendar.MINUTE, minute);
            setTime();
            setDirty(true);
          }
        }
      };

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DATE_DIALOG_ID:
        boolean brokenSamsungDevice = isBrokenSamsungDevice();
        @SuppressLint("InlinedApi")
        Context context = brokenSamsungDevice ?
            new ContextThemeWrapper(this,
                MyApplication.getThemeType() == MyApplication.ThemeType.dark ?
                    android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog) :
            this;
        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, mDateSetListener,
            year, month, day);
        if (brokenSamsungDevice) {
          datePickerDialog.setTitle("");
          datePickerDialog.updateDate(year, month, day);
        }
        return datePickerDialog;
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

  private static boolean isBrokenSamsungDevice() {
    return (Build.MANUFACTURER.equalsIgnoreCase("samsung")
        && isBetweenAndroidVersions(
        Build.VERSION_CODES.LOLLIPOP,
        Build.VERSION_CODES.LOLLIPOP_MR1));
  }

  private static boolean isBetweenAndroidVersions(int min, int max) {
    return Build.VERSION.SDK_INT >= min && Build.VERSION.SDK_INT <= max;
  }

  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {
    isProcessingLinkedAmountInputs = true;
    mStatusSpinner.setSelection(mTransaction.crStatus.ordinal(), false);
    if (mClone || mRowId != 0 || mTemplateId != 0) {
      //3 handle edit existing transaction or new one from template
      //3b  fill comment
      mCommentText.setText(mTransaction.comment);
      if (mIsMainTransactionOrTemplate) {
        mPayeeText.setText(mTransaction.payee);
      }
    }
    if (mTransaction instanceof Template) {
      mTitleText.setText(((Template) mTransaction).title);
      mPlanToggleButton.setChecked(((Template) mTransaction).planExecutionAutomatic);
    } else {
      mReferenceNumberText.setText(mTransaction.referenceNumber);
    }

    fillAmount(mTransaction.getAmount().getAmountMajor());

    if (mNewInstance) {
      if (mTransaction instanceof Template) {
        mTitleText.requestFocus();
      } else if (mIsMainTransactionOrTemplate && MyApplication.PrefKey.AUTO_FILL.getBoolean(false)) {
        mPayeeText.requestFocus();
      }
    }

    isProcessingLinkedAmountInputs = false;
  }

  protected void fillAmount(BigDecimal amount) {
    int signum = amount.signum();
    switch (signum) {
      case -1:
        amount = amount.abs();
        break;
      case 1:
        mType = INCOME;
    }
    if (signum != 0) {
      mAmountText.setAmount(amount);
    }
    mAmountText.requestFocus();
    mAmountText.selectAll();
  }

  /**
   * extracts the fields from the transaction date for setting them on the buttons
   */
  private void setDateTime() {
    setDate();
    setTime();
  }

  /**
   * sets date on date button
   */
  private void setDate() {
    (mTransaction instanceof Template ? mPlanButton : mDateButton)
        .setText(mDateFormat.format(mCalendar.getTime()));
  }

  /**
   * sets time on time button
   */
  private void setTime() {
    mTimeButton.setText(mTimeFormat.format(mCalendar.getTime()));
  }

  protected void saveState() {
    if (syncStateAndValidate()) {
      mIsSaving = true;
      startDbWriteTask(true);
      if (getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
        SharedPreferences.Editor e = MyApplication.getInstance().getSettings().edit();
        switch (mOperationType) {
          case MyExpenses.TYPE_TRANSACTION:
            e.putLong(PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, mTransaction.accountId);
            break;
          case MyExpenses.TYPE_TRANSFER:
            e.putLong(PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET, mTransaction.accountId);
            e.putLong(PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, mTransaction.transfer_account);
            break;
          case MyExpenses.TYPE_SPLIT:
            e.putLong(PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET, mTransaction.accountId);
        }
        SharedPreferencesCompat.apply(e);
      }
    } else {
      //prevent this flag from being sticky if form was not valid
      mCreateNew = false;
    }
  }

  /**
   * sets the state of the UI on mTransaction
   *
   * @return false if any data is not valid, also informs user through toast
   */
  protected boolean syncStateAndValidate() {
    boolean validP = true;
    String title;

    Account account = getCurrentAccount();
    if (account == null)
      return false;

    BigDecimal amount = validateAmountInput(true);

    if (amount == null) {
      //Toast is shown in validateAmountInput
      validP = false;
    } else {
      if (mType == EXPENSE) {
        amount = amount.negate();
      }
      mTransaction.getAmount().setCurrency(account.currency);
      mTransaction.getAmount().setAmountMajor(amount);//TODO refactor to better respect encapsulation
    }
    mTransaction.accountId = account.getId();

    mTransaction.comment = mCommentText.getText().toString();

    if (!(mTransaction instanceof SplitPartCategory || mTransaction instanceof SplitPartTransfer)) {
      mTransaction.setDate(mCalendar.getTime());
    }

    if (mOperationType == MyExpenses.TYPE_TRANSACTION) {
      mTransaction.setCatId(mCatId);
    }
    if (mIsMainTransactionOrTemplate) {
      mTransaction.setPayee(mPayeeText.getText().toString());
      long selected = mMethodSpinner.getSelectedItemId();
      mTransaction.methodId = (selected != AdapterView.INVALID_ROW_ID && selected > 0) ?
          selected : null;
    }
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      mTransaction.transfer_account = mTransferAccountSpinner.getSelectedItemId();
      final Account transferAccount = Account.getInstanceFromDb(mTransferAccountSpinner
          .getSelectedItemId());
      boolean isSame = account.currency.equals(transferAccount.currency);
      if (mTransaction instanceof Template) {
        if (!isSame && amount == null) {
          BigDecimal transferAmount = validateAmountInput(mTransferAmountText, true);
          if (transferAmount != null) {
            mTransaction.accountId = transferAccount.getId();
            mTransaction.transfer_account = account.getId();
            if (mType == INCOME) {
              transferAmount = transferAmount.negate();
            }
            mTransaction.setAmount(new Money(transferAccount.currency, transferAmount));
            mAmountText.setError(null);
            validP = true; //we only need either amount or transfer amount
          }
        }
      } else {
        mTransaction.getTransferAmount().setCurrency(transferAccount.currency);
        if (isSame) {
          if (amount != null) mTransaction.getTransferAmount().setAmountMajor(amount.negate());
        } else {
          BigDecimal transferAmount = validateAmountInput(mTransferAmountText, true);

          if (transferAmount == null) {
            //Toast is shown in validateAmountInput
            validP = false;
          } else {
            if (mType == INCOME) {
              transferAmount = transferAmount.negate();
            }
            mTransaction.getTransferAmount().setAmountMajor(transferAmount);
          }
        }
      }
    }
    if (mTransaction instanceof Template) {
      title = mTitleText.getText().toString();
      if (title.equals("")) {
        mTitleText.setError(getString(R.string.no_title_given));
        validP = false;
      }
      ((Template) mTransaction).title = title;
      if (mPlan == null) {
        if (mReccurenceSpinner.getSelectedItemPosition() > 0) {
          String description = ((Template) mTransaction).compileDescription(ExpenseEdit.this);
          mPlan = new Plan(
             mCalendar,
              ((Plan.Recurrence) mReccurenceSpinner.getSelectedItem()).toRrule(),
              ((Template) mTransaction).title,
              description);
          ((Template) mTransaction).setPlan(mPlan);
        }
      } else {
        mPlan.description = ((Template) mTransaction).compileDescription(ExpenseEdit.this);
        mPlan.title = title;
        ((Template) mTransaction).setPlan(mPlan);
      }
    } else {
      mTransaction.referenceNumber = mReferenceNumberText.getText().toString();
    }

    mTransaction.crStatus = (Transaction.CrStatus) mStatusSpinner.getSelectedItem();

    mTransaction.setPictureUri(mPictureUri);
    return validP;
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SELECT_CATEGORY_REQUEST && intent != null) {
      mCatId = intent.getLongExtra("cat_id", 0);
      mLabel = intent.getStringExtra("label");
      mCategoryButton.setText(mLabel);
      setDirty(true);
    }
    if (requestCode == PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
      Uri uri;
      String errorMsg;
      if (intent == null) {
        uri = mPictureUriTemp;
        Log.d(MyApplication.TAG, "got result for PICTURE request, intent null, relying on stored output uri :" + mPictureUriTemp);
      } else if (intent.getData() != null) {
        uri = intent.getData();
        Log.d(MyApplication.TAG, "got result for PICTURE request, found uri in intent data :" + uri.toString());
      } else {
        Log.d(MyApplication.TAG, "got result for PICTURE request, intent != null, getData() null, relying on stored output uri :" + mPictureUriTemp);
        uri = mPictureUriTemp;
      }
      if (uri != null) {
        if (isFileAndNotExists(uri)) {
          errorMsg = "Error while retrieving image: File not found: " + uri;
        } else {
          mPictureUri = uri;
          setPicture();
          setDirty(true);
          return;
        }
      } else {
        errorMsg = "Error while retrieving image: No data found.";
      }
      Utils.reportToAcra(new Exception(errorMsg));
      Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
  }

  protected void setPicture() {
    mPictureViewContainer.setVisibility(View.VISIBLE);
    Picasso.with(this).load(mPictureUri).fit().into((ImageView) mPictureViewContainer.findViewById(R.id.picture));
    mAttachPictureButton.setVisibility(View.GONE);
  }

  @Override
  public void onBackPressed() {
    cleanup();
    super.onBackPressed();
  }

  protected void cleanup() {
    if (mTransaction instanceof SplitTransaction) {
      ((SplitTransaction) mTransaction).cleanupCanceledEdit();
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
      findSplitPartList().updateBalance();
    }
    setCategoryButton();
  }

  private void configurePlan() {
    if (mPlan != null) {
      mPlanButton.setText(Plan.prettyTimeInfo(this, mPlan.rrule, mPlan.dtstart));
      if (mTitleText.getText().toString().equals(""))
        mTitleText.setText(mPlan.title);
      mPlanToggleButton.setVisibility(View.VISIBLE);
      mReccurenceSpinner.getSpinner().setVisibility(View.GONE);
      mPlanButton.setVisibility(View.VISIBLE);
      pObserver = new PlanObserver();
      getContentResolver().registerContentObserver(
          ContentUris.withAppendedId(Events.CONTENT_URI, mPlan.getId()),
          false, pObserver);
    }
  }

  private class PlanObserver extends ContentObserver {
    public PlanObserver() {
      super(new Handler());
    }

    @Override
    public void onChange(boolean selfChange) {
      if (mIsResumed) {
        refreshPlanData();
      }
      else {
        mPlanUpdateNeeded = true;
      }
    }
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    if (mPlanUpdateNeeded) {
      refreshPlanData();
      mPlanUpdateNeeded = false;
    }
  }

  private void refreshPlanData() {
    startTaskExecution(TaskExecutionFragment.TASK_INSTANTIATE_PLAN,
        new Long[]{mPlan.getId()}, null, 0);
  }

  private void configureStatusSpinner() {
    Account a = getCurrentAccount();
    mStatusSpinner.getSpinner().setVisibility((mTransaction instanceof Template ||
        mTransaction instanceof SplitPartCategory ||
        mTransaction instanceof SplitPartTransfer ||
        a == null ||
        a.type.equals(Type.CASH)) ? View.GONE : View.VISIBLE);
  }

  /**
   * set label on category button
   */
  private void setCategoryButton() {
    if (mLabel != null && mLabel.length() != 0) {
      mCategoryButton.setText(mLabel);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_CALENDAR, mCalendar);
    //restored in onCreate
    if (mRowId != 0) {
      outState.putLong(KEY_ROWID, mRowId);
    }
    if (mCatId != null) {
      outState.putLong(KEY_CATID, mCatId);
    }
    outState.putString(KEY_LABEL, mLabel);
    if (mPictureUri != null) {
      outState.putParcelable(KEY_PICTURE_URI, mPictureUri);
    }
    if (mPictureUriTemp != null) {
      outState.putParcelable(KEY_PICTURE_URI_TMP, mPictureUriTemp);
    }
    long methodId = mMethodSpinner.getSelectedItemId();
    if (methodId == android.widget.AdapterView.INVALID_ROW_ID && mMethodId != null) {
      methodId = mMethodId;
    }
    if (methodId != android.widget.AdapterView.INVALID_ROW_ID) {
      outState.putLong(KEY_METHODID, methodId);
    }
    long accountId = mAccountSpinner.getSelectedItemId();
    if (accountId == android.widget.AdapterView.INVALID_ROW_ID && mAccountId != null) {
      accountId = mAccountId;
    }
    if (accountId != android.widget.AdapterView.INVALID_ROW_ID) {
      outState.putLong(KEY_ACCOUNTID, accountId);
    }
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      outState.putLong(KEY_TRANSFER_ACCOUNT, mTransferAccountSpinner.getSelectedItemId());
    }
  }

  private void switchAccountViews() {
    Spinner accountSpinner = mAccountSpinner.getSpinner();
    Spinner transferAccountSpinner = mTransferAccountSpinner.getSpinner();
    ViewGroup accountParent = (ViewGroup) findViewById(R.id.AccountRow);
    ViewGroup transferAccountRow = (ViewGroup) findViewById(R.id.TransferAccountRow);
    TableLayout table = (TableLayout) findViewById(R.id.Table);
    View amountRow = table.findViewById(R.id.AmountRow);
    View transferAmountRow = table.findViewById(R.id.TransferAmountRow);
    table.removeView(amountRow);
    table.removeView(transferAmountRow);
    if (mType == INCOME) {
      accountParent.removeView(accountSpinner);
      transferAccountRow.removeView(transferAccountSpinner);
      accountParent.addView(transferAccountSpinner);
      transferAccountRow.addView(accountSpinner);
      table.addView(transferAmountRow, 2);
      table.addView(amountRow, 4);
    } else {
      accountParent.removeView(transferAccountSpinner);
      transferAccountRow.removeView(accountSpinner);
      accountParent.addView(accountSpinner);
      transferAccountRow.addView(transferAccountSpinner);
      table.addView(amountRow, 2);
      table.addView(transferAmountRow, 4);
    }
    linkAccountLabels();
  }

  public Money getAmount() {
    Account a = getCurrentAccount();
    if (a == null)
      return null;
    Money result = new Money(a.currency, 0L);
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

  /*
   * callback of TaskExecutionFragment
   */
  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    boolean success;
    switch (taskId) {
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2:
        if (o != null) {
          Transaction t = (Transaction) o;
          if (mCatId == null) {
            mCatId = t.getCatId();
            mLabel = t.label;
            setCategoryButton();
          }
          if (TextUtils.isEmpty(mCommentText.getText().toString())) {
            mCommentText.setText(t.comment);
          }
          if (TextUtils.isEmpty(mAmountText.getText().toString())) {
            fillAmount(t.getAmount().getAmountMajor());
            configureType();
          }
        }
        break;
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
        if (o == null) {
          Toast.makeText(this, R.string.save_transaction_template_deleted, Toast.LENGTH_LONG).show();
          finish();
          return;
        }
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
      case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
        if (o == null) {
          Toast.makeText(this, "Object has been deleted from db", Toast.LENGTH_LONG).show();
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
        } else if (mTransaction instanceof Template) {
          mOperationType = ((Template) mTransaction).isTransfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
          mPlan = ((Template) mTransaction).getPlan();
        } else {
          mOperationType = mTransaction instanceof Transfer ? MyExpenses.TYPE_TRANSFER : MyExpenses.TYPE_TRANSACTION;
          if (mPictureUri == null) { // we might have received a picture in onActivityResult before
            // arriving here, in this case it takes precedence
            mPictureUri = mTransaction.getPictureUri();
            if (mPictureUri != null) {
              boolean doShowPicture = true;
              if (isFileAndNotExists(mTransaction.getPictureUri())) {
                Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
                doShowPicture = false;
              }
              if (doShowPicture) {
                setPicture();
              }
            }
          }
        }
        //if catId has already been set by onRestoreInstanceState, the value might have been edited by the user and has precedence
        if (mCatId == null) {
          mCatId = mTransaction.getCatId();
          mLabel = mTransaction.label;
        }
        if (getIntent().getBooleanExtra(KEY_CLONE, false)) {
          if (mTransaction instanceof SplitTransaction) {
            mRowId = mTransaction.getId();
          } else {
            mTransaction.setId(0L);
            mRowId = 0L;
          }
          mTransaction.crStatus = CrStatus.UNRECONCILED;
          mTransaction.status = STATUS_NONE;
          mTransaction.setDate(new Date());
          mClone = true;
        }
        mCalendar.setTime(mTransaction.getDate());
        setup();
        supportInvalidateOptionsMenu();
        break;
      case TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS:
        success = (Boolean) o;
        Account account = mAccounts[mAccountSpinner.getSelectedItemPosition()];
        if (success) {
          updateAccount(account);
        } else {
          for (int i = 0; i < mAccounts.length; i++) {
            if (mAccounts[i].getId() == mTransaction.accountId) {
              mAccountSpinner.setSelection(i);
              break;
            }
          }
          Toast.makeText(
              this,
              getString(R.string.warning_cannot_move_split_transaction, account.label),
              Toast.LENGTH_LONG).show();
        }
        break;
      case TaskExecutionFragment.TASK_INSTANTIATE_PLAN:
        mPlan = ((Plan) o);
        configurePlan();
        break;
    }
  }

  private boolean isFileAndNotExists(Uri uri) {
    if (uri.getScheme().equals("file") && !new File(uri.getPath()).exists()) {
      return true;
    }
    return false;
  }

  public Account getCurrentAccount() {
    if (mAccounts == null) {
      return null;
    }
    int selected = mAccountSpinner.getSelectedItemPosition();
    if (selected == android.widget.AdapterView.INVALID_POSITION ||
        selected >= mAccounts.length) {
      return null;
    }
    return mAccounts[selected];
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
                             long id) {
    if (parent.getId() != R.id.OperationType) {
      setDirty(true);
    }
    switch (parent.getId()) {
      case R.id.Recurrence:
        int visibility = View.GONE;
        if (id > 0)  {
          if (ContextCompat.checkSelfPermission(ExpenseEdit.this,
              Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            if (MyApplication.PrefKey.NEW_PLAN_ENABLED.getBoolean(true)) {
              visibility = View.VISIBLE;
            } else {
              mReccurenceSpinner.setSelection(0);
              CommonCommands.showContribDialog(this, ContribFeature.PLANS_UNLIMITED, null);
            }
          } else {
            ActivityCompat.requestPermissions(ExpenseEdit.this,
                new String[]{Manifest.permission.WRITE_CALENDAR},
                ProtectionDelegate.PERMISSIONS_REQUEST_WRITE_CALENDAR);
          }
        }
        mPlanButton.setVisibility(visibility);
        mPlanToggleButton.setVisibility(visibility);
        break;
      case R.id.Method:
        if (id > 0) {
          //ignore first row "no method" merged in
          mMethodsCursor.moveToPosition(position - 1);
          if (!(mTransaction instanceof Template))
            mReferenceNumberText.setVisibility(mMethodsCursor.getInt(mMethodsCursor.getColumnIndexOrThrow(KEY_IS_NUMBERED)) > 0 ?
                View.VISIBLE : View.INVISIBLE);
        } else {
          mTransaction.methodId = null;
          mReferenceNumberText.setVisibility(View.GONE);
        }
        break;
      case R.id.Account:
        final Account account = mAccounts[position];
        if (mTransaction instanceof SplitTransaction && findSplitPartList().getSplitCount() > 0) {
          //call background task for moving parts to new account
          startTaskExecution(
              TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS,
              new Long[]{mTransaction.getId()},
              account.getId(),
              R.string.progress_dialog_updating_split_parts);
        } else {
          updateAccount(account);
        }
        break;
      case R.id.OperationType:
        int newType = ((Integer) mOperationTypeSpinner.getItemAtPosition(position));
        if (newType != mOperationType && isValidType(newType)) {
          if (newType == MyExpenses.TYPE_TRANSFER && !checkTransferEnabled(getCurrentAccount())) {
            //reset to previous
            resetOperationType();
          } else if (newType == MyExpenses.TYPE_SPLIT) {
            resetOperationType();
            contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null);
          } else {
            restartWithType(newType);
          }
        }
        break;
      case R.id.TransferAccount:
        mTransaction.transfer_account = mTransferAccountSpinner.getSelectedItemId();
        configureTransferInput();
        break;
    }
  }

  private boolean isValidType(int type) {
    return type == MyExpenses.TYPE_SPLIT || type == MyExpenses.TYPE_TRANSACTION ||
        type == MyExpenses.TYPE_TRANSFER;
  }

  private void updateAccount(Account account) {
    mTransaction.accountId = account.getId();
    setAccountLabel(account);
    if (mOperationType == MyExpenses.TYPE_TRANSFER) {
      mTransferAccountSpinner.setSelection(setTransferAccountFilterMap());
      mTransaction.transfer_account = mTransferAccountSpinner.getSelectedItemId();
      configureTransferInput();
    } else {
      if (!(mTransaction instanceof SplitPartCategory)) {
        if (mManager.getLoader(METHODS_CURSOR) != null && !mManager.getLoader(METHODS_CURSOR).isReset()) {
          mManager.restartLoader(METHODS_CURSOR, null, this);
        } else {
          mManager.initLoader(METHODS_CURSOR, null, this);
        }
      }
      if (mTransaction instanceof SplitTransaction) {
        final SplitPartList splitPartList = findSplitPartList();
        splitPartList.updateAccount(account);
      }
    }
    configureStatusSpinner();
    mAmountText.setFractionDigits(Money.getFractionDigits(account.currency));
    //once user has selected account, we no longer want
    //the passed in KEY_CURRENCY to override it in onLoadFinished
    getIntent().removeExtra(KEY_CURRENCY);
  }

  private void configureTransferInput() {
    final Account transferAccount = Account.getInstanceFromDb(
        mTransferAccountSpinner.getSelectedItemId());
    final Currency currency = getCurrentAccount().currency;
    final boolean isSame = currency.equals(transferAccount.currency);
    findViewById(R.id.TransferAmountRow).setVisibility(isSame ? View.GONE : View.VISIBLE);
    findViewById(R.id.ExchangeRateRow).setVisibility(
        isSame || (mTransaction instanceof Template) ? View.GONE : View.VISIBLE);
    final String symbol2 = transferAccount.currency.getSymbol();
    //noinspection SetTextI18n
    addCurrencyToLabel((TextView) findViewById(R.id.TransferAmountLabel), symbol2);
    mTransferAmountText.setFractionDigits(Money.getFractionDigits(transferAccount.currency));
    final String symbol1 = currency.getSymbol();
    ((TextView) findViewById(R.id.ExchangeRateLabel_1_1)).setText(String.format("1 %s =", symbol1));
    ((TextView) findViewById(R.id.ExchangeRateLabel_1_2)).setText(symbol2);
    ((TextView) findViewById(R.id.ExchangeRateLabel_2_1)).setText(String.format("1 %s =", symbol2));
    ((TextView) findViewById(R.id.ExchangeRateLabel_2_2)).setText(symbol1);

    Bundle bundle = new Bundle(2);
    bundle.putStringArray(KEY_CURRENCY, new String[]{currency.getCurrencyCode(), transferAccount
        .currency.getCurrencyCode()});
    if (!isSame && !mSavedInstance && (mNewInstance || mPlanInstanceId == -1) && !(mTransaction instanceof Template)) {
      mManager.restartLoader(LAST_EXCHANGE_CURSOR, bundle, this);
    }
  }

  private void setAccountLabel(Account account) {
    addCurrencyToLabel(mAmountLabel, account.currency.getSymbol());
  }

  private void addCurrencyToLabel(TextView label, String symbol) {
    //noinspection SetTextI18n
    label.setText(getString(R.string.amount) + " (" + symbol + ")");
  }

  private void resetOperationType() {
    mOperationTypeSpinner.setSelection(mOperationTypeAdapter.getPosition(mOperationType));
  }

  private void restartWithType(int newType) {
    cleanup();
    Intent restartIntent = getIntent();
    restartIntent.putExtra(MyApplication.KEY_OPERATION_TYPE, newType);
    finish();
    startActivity(restartIntent);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }

  /*
   * callback of DbWriteFragment
   */
  @Override
  public void onPostExecute(Object result) {
    if (result == null) {
      Toast.makeText(this, "Unknown error while saving transaction", Toast.LENGTH_SHORT).show();
      return;
    }
    Long sequenceCount = (Long) result;
    if (sequenceCount < 0L) {
      if (mTransaction instanceof Template) {
        //for the moment, the only case where saving will fail
        //if the unique constraint for template titles is violated
        //TODO: we should probably validate the title earlier
        mTitleText.setError(getString(R.string.template_title_exists, ((Template) mTransaction).title));
      } else {
        String errorMsg;
        switch (sequenceCount.intValue()) {
          case DbWriteFragment.ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE:
            errorMsg = getString(R.string.external_storage_unavailable);
            break;
          case DbWriteFragment.ERROR_PICTURE_SAVE_UNKNOWN:
            errorMsg = "Error while saving picture";
            break;
          default:
            //possibly the selected category has been deleted
            mCatId = null;
            mCategoryButton.setText(R.string.select);

            errorMsg = "Error while saving transaction";
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
      }
      mCreateNew = false;
    } else {
      if (mRecordTemplateWidget) {
        recordUsage(ContribFeature.TEMPLATE_WIDGET);
        TemplateWidget.showContribMessage(this);
      }
      if (mCreateNew) {
        mCreateNew = false;
        if (mOperationType == MyExpenses.TYPE_SPLIT) {
          mTransaction = SplitTransaction.getNewInstance(mTransaction.accountId);
          mRowId = mTransaction.getId();
          findSplitPartList().updateParent(mRowId);
        } else {
          mTransaction.setId(0L);
          mRowId = 0L;
        }
        //while saving the picture might have been moved from temp to permanent
        mPictureUri = mTransaction.getPictureUri();
        mNewInstance = true;
        mClone = false;
        switch (mOperationType) {
          case MyExpenses.TYPE_TRANSACTION:
            setTitle(R.string.menu_create_transaction);
            break;
          case MyExpenses.TYPE_TRANSFER:
            setTitle(R.string.menu_create_transfer);
            break;
          case MyExpenses.TYPE_SPLIT:
            setTitle(R.string.menu_create_split);
            break;
        }
        isProcessingLinkedAmountInputs = true;
        mAmountText.setText("");
        mTransferAmountText.setText("");
        isProcessingLinkedAmountInputs = false;
        Toast.makeText(this, getString(R.string.save_transaction_and_new_success), Toast.LENGTH_SHORT).show();
      } else {
        //make sure soft keyboard is closed
        InputMethodManager im = (InputMethodManager) this.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        Intent intent = new Intent();
        intent.putExtra(ContribInfoDialogFragment.KEY_SEQUENCE_COUNT, sequenceCount);
        setResult(RESULT_OK, intent);
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
    switch (id) {
      case METHODS_CURSOR:
        Account a = getCurrentAccount();
        if (a == null)
          return null;
        return new CursorLoader(this,
            TransactionProvider.METHODS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                .appendPath(mType == INCOME ? "1" : "-1")
                .appendPath(a.type.name()).build(),
            null, null, null, null);
      case ACCOUNTS_CURSOR:
        return new CursorLoader(this, TransactionProvider.ACCOUNTS_BASE_URI,
            null, null, null, null);
      case LAST_EXCHANGE_CURSOR:
        String[] currencies = args.getStringArray(KEY_CURRENCY);
        return new CursorLoader(this,
            Transaction.CONTENT_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_LAST_EXCHANGE)
                .appendPath(currencies[0])
                .appendPath(currencies[1])
                .build(),
            null, null, null, null);
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (data == null) {
      return;
    }
    int id = loader.getId();
    switch (id) {
      case METHODS_CURSOR:
        mMethodsCursor = data;
        View methodContainer = findViewById(R.id.MethodRow);
        if (mMethodsAdapter == null || !data.moveToFirst()) {
          methodContainer.setVisibility(View.GONE);
        } else {
          methodContainer.setVisibility(View.VISIBLE);
          MatrixCursor extras = new MatrixCursor(new String[]{KEY_ROWID, KEY_LABEL, KEY_IS_NUMBERED});
          extras.addRow(new String[]{"0", "- - - -", "0"});
          mMethodsAdapter.swapCursor(new MergeCursor(new Cursor[]{extras, data}));
          if (mSavedInstance) {
            mTransaction.methodId = mMethodId;
          }
          if (mTransaction.methodId != null) {
            while (data.isAfterLast() == false) {
              if (data.getLong(data.getColumnIndex(KEY_ROWID)) == mTransaction.methodId) {
                mMethodSpinner.setSelection(data.getPosition() + 1);
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
        mAccountsAdapter.swapCursor(data);
        mAccounts = new Account[data.getCount()];
        if (mSavedInstance) {
          mTransaction.accountId = mAccountId;
          mTransaction.transfer_account = mTransferAccountId;
        }
        data.moveToFirst();
        boolean selectionSet = false;
        String currencyExtra = getIntent().getStringExtra(KEY_CURRENCY);
        while (data.isAfterLast() == false) {
          int position = data.getPosition();
          Account a = Account.fromCacheOrFromCursor(data);
          mAccounts[position] = a;
          if (!selectionSet &&
              (a.currency.getCurrencyCode().equals(currencyExtra) ||
                  (currencyExtra == null && a.getId().equals(mTransaction.accountId)))) {
            mAccountSpinner.setSelection(position);
            setAccountLabel(a);
            selectionSet = true;
          }
          data.moveToNext();
        }
        //if the accountId we have been passed does not exist, we select the first entry
        if (mAccountSpinner.getSelectedItemPosition() == android.widget.AdapterView.INVALID_POSITION) {
          mAccountSpinner.setSelection(0);
          mTransaction.accountId = mAccounts[0].getId();
          setAccountLabel(mAccounts[0]);
        }
        if (mOperationType == MyExpenses.TYPE_TRANSFER) {
          mTransferAccountCursor = new FilterCursorWrapper(data);
          int selectedPosition = setTransferAccountFilterMap();
          mTransferAccountsAdapter.swapCursor(mTransferAccountCursor);
          mTransferAccountSpinner.setSelection(selectedPosition);
          mTransaction.transfer_account = mTransferAccountSpinner.getSelectedItemId();
          configureTransferInput();
          if (!mNewInstance && !(mTransaction instanceof Template)) {
            isProcessingLinkedAmountInputs = true;
            mTransferAmountText.setAmount(mTransaction.getTransferAmount().getAmountMajor().abs());
            updateExchangeRates();
            isProcessingLinkedAmountInputs = false;
          }
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
        if (mIsResumed) setupListeners();
        break;
      case LAST_EXCHANGE_CURSOR:
        if (data.moveToFirst()) {
          final Currency currency1 = getCurrentAccount().currency;
          final Currency currency2 = Account.getInstanceFromDb(mTransferAccountSpinner
              .getSelectedItemId()).currency;
          if (currency1.getCurrencyCode().equals(data.getString(0)) &&
              currency2.getCurrencyCode().equals(data.getString(1))) {
            BigDecimal amount = new Money(currency1, data.getLong(2)).getAmountMajor();
            BigDecimal transferAmount = new Money(currency2, data.getLong(3)).getAmountMajor();
            BigDecimal exchangeRate = amount.compareTo(nullValue) != 0 ?
                transferAmount.divide(amount, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) : nullValue;
            if (exchangeRate.compareTo(nullValue) != 0) {
              mExchangeRate1Text.setAmount(exchangeRate);
            }
          }
        }
    }
  }

  private int setTransferAccountFilterMap() {
    Account fromAccount = mAccounts[mAccountSpinner.getSelectedItemPosition()];
    ArrayList<Integer> list = new ArrayList<>();
    int position = 0, selectedPosition = 0;
    for (int i = 0; i < mAccounts.length; i++) {
      if (fromAccount.getId() != mAccounts[i].getId()) {
        list.add(i);
        if (mTransaction.transfer_account != null && mTransaction.transfer_account == mAccounts[i].getId()) {
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
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(ContentUris.withAppendedId(Events.CONTENT_URI, mPlan.getId()));
    //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan.dtstart);
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan.dtstart);
    if (Utils.isIntentAvailable(this, intent)) {
      startActivity(intent);
    } else {
      Toast.makeText(this, R.string.no_calendar_app_installed, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    //should not be necessary to empty the autocompletetextview
    int id = loader.getId();
    switch (id) {
      case METHODS_CURSOR:
        mMethodsCursor = null;
        if (mMethodsAdapter != null) {
          mMethodsAdapter.swapCursor(null);
        }
        break;
      case ACCOUNTS_CURSOR:
        if (mAccountsAdapter != null) {
          mAccountsAdapter.swapCursor(null);
        }
        break;
    }
  }

  public void onToggleClicked(View view) {
    ((Template) mTransaction).planExecutionAutomatic = ((ToggleButton) view).isChecked();
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (feature == ContribFeature.ATTACH_PICTURE) {
      startMediaChooserDo();
    } else if (feature == ContribFeature.SPLIT_TRANSACTION) {
      restartWithType(MyExpenses.TYPE_SPLIT);
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
    if (feature == ContribFeature.SPLIT_TRANSACTION) {
      resetOperationType();
    }
  }

  public void disableAccountSpinner() {
    mAccountSpinner.setEnabled(false);
  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.AUTO_FILL_COMMAND:
        startAutoFill(args.getLong(KEY_ROWID));
        MyApplication.PrefKey.AUTO_FILL.putBoolean(true);
        break;
      default:
        super.onPositive(args);
    }
  }

  private void startAutoFill(long id) {
    startTaskExecution(
        TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2,
        new Long[]{id},
        null,
        R.string.progress_dialog_loading);
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
    if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.AUTO_FILL_COMMAND) {
      MyApplication.PrefKey.AUTO_FILL.putBoolean(false);
    } else {
      super.onDismissOrCancel(args);
    }
  }

  @Override
  protected void onPause() {
    //try to prevent cursor leak
    mPayeeAdapter.changeCursor(null);
    mIsResumed = false;
    super.onPause();
  }

  protected SplitPartList findSplitPartList() {
    return (SplitPartList) getSupportFragmentManager().findFragmentByTag(SPLIT_PART_LIST);
  }

  @SuppressLint("NewApi")
  public void showPicturePopupMenu(final View v) {
    PopupMenu popup = new PopupMenu(this, v);
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        handlePicturePopupMenuClick(item.getItemId());
        return true;
      }
    });
    popup.inflate(R.menu.picture_popup);
    popup.show();
  }

  private void handlePicturePopupMenuClick(int command) {
    switch (command) {
      case R.id.DELETE_COMMAND:
        mPictureUri = null;
        mAttachPictureButton.setVisibility(View.VISIBLE);
        mPictureViewContainer.setVisibility(View.GONE);
        break;
      case R.id.VIEW_COMMAND:
        startActivity(Transaction.getViewIntent(mPictureUri));
        break;
      case R.id.CHANGE_COMMAND:
        startMediaChooserDo();
        break;
    }
  }

  public void startMediaChooser(View v) {
    contribFeatureRequested(ContribFeature.ATTACH_PICTURE, null);
  }

  public void startMediaChooserDo() {

    Uri outputMediaUri = getCameraUri();
    Intent gallIntent = new Intent(Utils.getContentIntentAction());
    gallIntent.setType("image/*");
    Intent chooserIntent = Intent.createChooser(gallIntent, null);

    //if external storage is not available, camera capture won't work
    if (outputMediaUri != null) {
      Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      camIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputMediaUri);

      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
          new Intent[]{camIntent});
    }
    Log.d(MyApplication.TAG, "starting chooser for PICTURE_REQUEST with EXTRA_OUTPUT = " + outputMediaUri);
    startActivityForResult(chooserIntent, ProtectedFragmentActivity.PICTURE_REQUEST_CODE);
  }

  private Uri getCameraUri() {
    if (mPictureUriTemp == null) {
      mPictureUriTemp = Utils.getOutputMediaUri(true);
    }
    return mPictureUriTemp;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case ProtectionDelegate.PERMISSIONS_REQUEST_WRITE_CALENDAR: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mPlanButton.setVisibility(View.VISIBLE);
          mPlanToggleButton.setVisibility(View.VISIBLE);
        } else {
          mReccurenceSpinner.setSelection(0);
          if (!ActivityCompat.shouldShowRequestPermissionRationale(
              this, Manifest.permission.WRITE_CALENDAR)) {
            setPlannerRowVisibility(View.GONE);
          }
        }
      }
    }
  }

  private void updateExchangeRates() {
    BigDecimal amount = validateAmountInput(mAmountText, false);
    BigDecimal transferAmount = validateAmountInput(mTransferAmountText, false);
    BigDecimal exchangeRate =
        (amount != null && transferAmount != null && amount.compareTo(nullValue) != 0) ?
            transferAmount.divide(amount, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) : nullValue;
    BigDecimal inverseExchangeRate = exchangeRate.compareTo(nullValue) != 0 ?
        new BigDecimal(1).divide(exchangeRate, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) :
        nullValue;
    mExchangeRate1Text.setAmount(exchangeRate);
    mExchangeRate2Text.setAmount(inverseExchangeRate);
  }

  private class MyTextWatcher implements TextWatcher {
    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
  }

  private class LinkedTransferAmountTextWatcher extends MyTextWatcher {
    boolean isMain;

    public LinkedTransferAmountTextWatcher(boolean isMain) {
      this.isMain = isMain;
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (isProcessingLinkedAmountInputs) return;
      isProcessingLinkedAmountInputs = true;
      if (mTransaction instanceof Template) {
        (isMain ? mTransferAmountText : mAmountText).setText("");
      } else {
        if (mType == (isMain ? EXPENSE : INCOME)) {
          BigDecimal input = validateAmountInput(isMain ? mAmountText : mTransferAmountText, false);
          BigDecimal exchangeRate = validateAmountInput(isMain ? mExchangeRate1Text : mExchangeRate2Text, false);
          BigDecimal result = exchangeRate != null && input != null ? input.multiply(exchangeRate) : new BigDecimal(0);
          (isMain ? mTransferAmountText : mAmountText).setAmount(result);
        } else {
          updateExchangeRates();
        }
      }
      isProcessingLinkedAmountInputs = false;
    }
  }

  private class LinkedExchangeRateTextWatchter extends MyTextWatcher {
    boolean isMain;

    public LinkedExchangeRateTextWatchter(boolean isMain) {
      this.isMain = isMain;
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (isProcessingLinkedAmountInputs) return;
      isProcessingLinkedAmountInputs = true;
      BigDecimal input = validateAmountInput(isMain ? mAmountText : mTransferAmountText, false);
      BigDecimal inputRate = validateAmountInput(
          isMain ? mExchangeRate1Text : mExchangeRate2Text, false);
      if (inputRate == null) inputRate = nullValue;
      BigDecimal inverseInputRate = inputRate.compareTo(nullValue) != 0 ?
          new BigDecimal(1).divide(inputRate, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) :
          nullValue;

      (isMain ? mExchangeRate2Text : mExchangeRate1Text).setAmount(inverseInputRate);

      (isMain ? mTransferAmountText : mAmountText).setAmount(input != null ?
          input.multiply(inputRate) : nullValue);
      isProcessingLinkedAmountInputs = false;
    }
  }

  @Override
  public void showCalculator(View view) {
    if (view.getId() == R.id.CalculatorTransfer)
      showCalculatorInternal(mTransferAmountText);
    else
      super.showCalculator(view);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    isProcessingLinkedAmountInputs = true;
    super.onRestoreInstanceState(savedInstanceState);
    isProcessingLinkedAmountInputs = false;
  }
}
