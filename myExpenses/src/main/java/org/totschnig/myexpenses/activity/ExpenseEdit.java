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
import android.annotation.TargetApi;
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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
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
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.PlanMonthFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.FilterCursorWrapper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.tracking.Tracker;
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
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

import static org.totschnig.myexpenses.activity.MyExpenses.KEY_SEQUENCE_COUNT;
import static org.totschnig.myexpenses.model.Transaction.TYPE_SPLIT;
import static org.totschnig.myexpenses.model.Transaction.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.model.Transaction.TYPE_TRANSFER;
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
  private static final String KEY_CACHED_DATA = "cachedData";
  private static final String KEY_CACHED_RECURRENCE = "cachedRecurrence";
  private static final String KEY_CACHED_PICTURE_URI = "cachedPictureUri";
  public static final String KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount";
  private static final String PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET = "transactionLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET = "transferLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET = "transferLastTransferAccountFromWidget";
  private static final String PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET = "splitLastAccountFromWidget";
  public static final int EXCHANGE_RATE_FRACTION_DIGITS = 5;
  private static int INPUT_EXCHANGE_RATE = 1;
  private static int INPUT_AMOUNT = 2;
  private static int INPUT_TRANSFER_AMOUNT = 3;
  private int[] lastExchangeRateRelevantInputs = {INPUT_EXCHANGE_RATE, INPUT_AMOUNT};
  private static final BigDecimal nullValue = new BigDecimal(0);
  private Button mDateButton;
  private Button mTimeButton;
  private EditText mCommentText, mTitleText, mReferenceNumberText;
  private AmountEditText mTransferAmountText, mExchangeRate1Text, mExchangeRate2Text;

  private Button mCategoryButton, mPlanButton;
  private SpinnerHelper mMethodSpinner, mAccountSpinner, mTransferAccountSpinner, mStatusSpinner,
      mOperationTypeSpinner, mRecurrenceSpinner;
  private SimpleCursorAdapter mMethodsAdapter, mAccountsAdapter, mTransferAccountsAdapter, mPayeeAdapter;
  private OperationTypeAdapter mOperationTypeAdapter;
  private FilterCursorWrapper mTransferAccountCursor;
  private AutoCompleteTextView mPayeeText;
  protected TextView mPayeeLabel;
  private ToggleButton mPlanToggleButton;
  private ImageView mAttachPictureButton;
  private FrameLayout mPictureViewContainer;
  private Long mRowId = 0L;
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
  protected boolean mIsMainTransactionOrTemplate, mIsMainTemplate;
  protected boolean mSavedInstance;
  protected boolean mRecordTemplateWidget;
  private boolean mIsResumed;
  boolean isProcessingLinkedAmountInputs = false;
  private ContentObserver pObserver;
  private boolean mPlanUpdateNeeded;
  private boolean didUserSetAccount;

  public enum HelpVariant {
    transaction, transfer, split, templateCategory, templateTransfer, templateSplit, splitPartCategory, splitPartTransfer
  }

  @Inject
  ImageViewIntentProvider imageViewIntentProvider;
  @Inject
  CurrencyFormatter currencyFormatter;

  @Override
  int getDiscardNewMessage() {
    return mTransaction instanceof Template ? R.string.dialog_confirm_discard_new_template :
        R.string.dialog_confirm_discard_new_transaction;
  }

  @Override
  protected void injectDependencies() {
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.one_expense);

    mDateFormat = Utils.getDateFormatSafe(this);
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
    mPayeeAdapter.setFilterQueryProvider(str -> {
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
    });

    mPayeeAdapter.setCursorToStringConverter(cur -> cur.getString(1));
    mPayeeText.setOnItemClickListener((parent, view, position, id) -> {
      Cursor c = (Cursor) mPayeeAdapter.getItem(position);
      if (c.moveToPosition(position)) {
        mTransaction.updatePayeeWithId(c.getString(1), c.getLong(0));
        if (mNewInstance && mTransaction != null &&
            !(mTransaction instanceof Template || mTransaction instanceof SplitTransaction)) {
          //moveToPosition should not be necessary,
          //but has been reported to not be positioned correctly on samsung GT-I8190N
          if (!c.isNull(2)) {
            if (PrefKey.AUTO_FILL_HINT_SHOWN.getBoolean(false)) {
              if (PrefKey.AUTO_FILL.getBoolean(true)) {
                startAutoFill(c.getLong(2));
              }
            } else {
              Bundle b = new Bundle();
              b.putLong(KEY_ROWID, c.getLong(2));
              b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information);
              b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string
                  .hint_auto_fill));
              b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND);
              b.putString(ConfirmationDialogFragment.KEY_PREFKEY, PrefKey
                  .AUTO_FILL_HINT_SHOWN.getKey());
              b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes);
              b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no);
              ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(),
                  "AUTO_FILL_HINT");
            }
          }
        }
      }
    });

    mCategoryButton = findViewById(R.id.Category);
    mPlanButton = findViewById(R.id.Plan);
    mMethodSpinner = new SpinnerHelper(findViewById(R.id.Method));
    mAccountSpinner = new SpinnerHelper(findViewById(R.id.Account));
    mTransferAccountSpinner = new SpinnerHelper(findViewById(R.id.TransferAccount));
    mTransferAccountSpinner.setOnItemSelectedListener(this);
    mStatusSpinner = new SpinnerHelper(findViewById(R.id.Status));
    mRecurrenceSpinner = new SpinnerHelper(findViewById(R.id.Recurrence));
    mPlanToggleButton = findViewById(R.id.PlanExecutionAutomatic);
    TextPaint paint = mPlanToggleButton.getPaint();
    int automatic = (int) paint.measureText(getString(R.string.plan_automatic));
    int manual = (int) paint.measureText(getString(R.string.plan_manual));
    mPlanToggleButton.setWidth(
        (automatic > manual ? automatic : manual) +
            +mPlanToggleButton.getPaddingLeft()
            + mPlanToggleButton.getPaddingRight());

    mRowId = Utils.getFromExtra(getIntent().getExtras(), KEY_ROWID, 0);
    mTemplateId = getIntent().getLongExtra(KEY_TEMPLATEID, 0);

    //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
    if (savedInstanceState != null) {
      mSavedInstance = true;
      mRowId = savedInstanceState.getLong(KEY_ROWID);
      mTemplateId = savedInstanceState.getLong(KEY_TEMPLATEID);
      mPictureUri = savedInstanceState.getParcelable(KEY_PICTURE_URI);
      mPictureUriTemp = savedInstanceState.getParcelable(KEY_PICTURE_URI_TMP);
      setPicture();


      mCalendar = (Calendar) savedInstanceState.getSerializable(KEY_CALENDAR);
      mLabel = savedInstanceState.getString(KEY_LABEL);
      if ((mCatId = savedInstanceState.getLong(KEY_CATID)) == 0L) {
        mCatId = null;
      }
      if ((mMethodId = savedInstanceState.getLong(KEY_METHODID)) == 0L) {
        mMethodId = null;
      }
      if ((mAccountId = savedInstanceState.getLong(KEY_ACCOUNTID)) == 0L) {
        mAccountId = null;
      } else {
        didUserSetAccount = true;
      }
      if ((mTransferAccountId = savedInstanceState.getLong(KEY_TRANSFER_ACCOUNT)) == 0L)
        mTransferAccountId = null;
    }
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
        //are we editing the template or instantiating a new transaction from the template
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
      mOperationType = getIntent().getIntExtra(MyApplication.KEY_OPERATION_TYPE, TYPE_TRANSACTION);
      if (!isValidType(mOperationType)) {
        mOperationType = TYPE_TRANSACTION;
      }
      final boolean isNewTemplate = getIntent().getBooleanExtra(KEY_NEW_TEMPLATE, false);
      if (mOperationType == TYPE_SPLIT) {
        boolean allowed = true;
        ContribFeature contribFeature;
        if (isNewTemplate) {
          contribFeature = ContribFeature.SPLIT_TEMPLATE;
          allowed = PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true);
        }
        else {
          contribFeature = ContribFeature.SPLIT_TRANSACTION;
          allowed = contribFeature.hasAccess() || contribFeature.usagesLeft() > 0;
        }
        if (!allowed) {
          Toast.makeText(this, contribFeature.buildRequiresString(this),
              Toast.LENGTH_LONG).show();
          finish();
          return;
        }
      }
      final Long parentId = getIntent().getLongExtra(KEY_PARENTID, 0);
      getSupportActionBar().setDisplayShowTitleEnabled(false);
      View spinner = findViewById(R.id.OperationType);
      mOperationTypeSpinner = new SpinnerHelper(spinner);
      spinner.setVisibility(View.VISIBLE);
      List<Integer> allowedOperationTypes = new ArrayList<>();
      allowedOperationTypes.add(TYPE_TRANSACTION);
      allowedOperationTypes.add(TYPE_TRANSFER);
      if (parentId == 0) {
        allowedOperationTypes.add(TYPE_SPLIT);
      }
      mOperationTypeAdapter = new OperationTypeAdapter(this, allowedOperationTypes,
          isNewTemplate, parentId != 0);
      mOperationTypeSpinner.setAdapter(mOperationTypeAdapter);
      resetOperationType();
      mOperationTypeSpinner.setOnItemSelectedListener(this);
      Long accountId = getIntent().getLongExtra(KEY_ACCOUNTID, 0);
      if (isNewTemplate) {
        mTransaction = Template.getTypedNewInstance(mOperationType, accountId, true, parentId != 0 ? parentId : null);
        if (mOperationType == TYPE_SPLIT && mTransaction != null) {
          mRowId = mTransaction.getId();
        }
      } else {
        switch (mOperationType) {
          case TYPE_TRANSACTION:
            if (accountId == 0L) {
              accountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, 0L);
            }
            mTransaction = Transaction.getNewInstance(accountId, parentId != 0 ? parentId : null);
            break;
          case TYPE_TRANSFER:
            Long transferAccountId = 0L;
            if (accountId == 0L) {
              accountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET, 0L);
              transferAccountId = MyApplication.getInstance().getSettings()
                  .getLong(PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, 0L);
            }
            mTransaction = Transfer.getNewInstance(accountId,
                transferAccountId != 0 ? transferAccountId : null,
                parentId != 0 ? parentId : null);
            break;
          case TYPE_SPLIT:
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
        Bundle extras = getIntent().getExtras();
        IllegalStateException e = new IllegalStateException(errMsg);
        if (extras != null) {
          AcraHelper.report(e, "Extras", extras.toString());
        } else {
          AcraHelper.report(e);
        }
        Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      if (!mSavedInstance) {
        //processing data from user switching operation type
        Transaction cached = (Transaction) getIntent().getSerializableExtra(KEY_CACHED_DATA);
        if (cached != null) {
          mTransaction.setAccountId(cached.getAccountId());
          mCalendar.setTime(cached.getDate());
          mPictureUri = getIntent().getParcelableExtra(KEY_CACHED_PICTURE_URI);
          setPicture();
          mTransaction.setMethodId(cached.getMethodId());
        }
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
    if (mOperationType == TYPE_SPLIT) {
      mAmountText.addTextChangedListener(new MyTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
          findSplitPartList().updateBalance();
        }
      });
    }
    if (mOperationType == TYPE_TRANSFER) {
      mAmountText.addTextChangedListener(new LinkedTransferAmountTextWatcher(true));
      mTransferAmountText.addTextChangedListener(new LinkedTransferAmountTextWatcher(false));
    }
    // Spinner for account and transfer account
    mAccountsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
        new String[]{KEY_LABEL}, new int[]{android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    if (isSplitPart()) {
      disableAccountSpinner();
    }
    mIsMainTransactionOrTemplate = mOperationType != TYPE_TRANSFER && !(mTransaction.isSplitpart());
    mIsMainTemplate = mTransaction instanceof Template && !mTransaction.isSplitpart();

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
    if (mOperationType == TYPE_TRANSFER) {
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

    if (mIsMainTemplate) {
      findViewById(R.id.TitleRow).setVisibility(View.VISIBLE);
      if (!isCalendarPermissionPermanentlyDeclined()) {
        //if user has denied access and checked that he does not want to be asked again, we do not
        //bother him with a button that is not working
        setPlannerRowVisibility(View.VISIBLE);
        RecurrenceAdapter recurrenceAdapter = new RecurrenceAdapter(this,
            DistribHelper.shouldUseAndroidPlatformCalendar() ? null : Plan.Recurrence.CUSTOM);
        mRecurrenceSpinner.setAdapter(recurrenceAdapter);
        mRecurrenceSpinner.setOnItemSelectedListener(this);
        mPlanButton.setOnClickListener(view -> {
          if (mPlan == null) {
            hideKeyBoardAndShowDialog(DATE_DIALOG_ID);
          } else if (DistribHelper.shouldUseAndroidPlatformCalendar()) {
            launchPlanView(false);
          }
        });
      }
      mAttachPictureButton.setVisibility(View.GONE);
      if (mTransaction.getId() != 0) {
        int typeResId;
        switch (mOperationType) {
          case TYPE_TRANSFER:
            typeResId = R.string.transfer;
            break;
          case TYPE_SPLIT:
            typeResId = R.string.split_transaction;
            break;
          default:
            typeResId = R.string.transaction;
        }
        setTitle(getString(R.string.menu_edit_template) + " (" + getString(typeResId) + ")");
      }
      switch (mOperationType) {
        case TYPE_TRANSFER:
          helpVariant = HelpVariant.templateTransfer;
          break;
        case TYPE_SPLIT:
          helpVariant = HelpVariant.templateSplit;
          break;
        default:
          helpVariant = HelpVariant.templateCategory;
      }
    } else if (isSplitPart()) {
      if (mOperationType == TYPE_TRANSACTION) {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_split_part_category);
        }
        helpVariant = HelpVariant.splitPartCategory;
        mTransaction.status = STATUS_UNCOMMITTED;
      } else {
        //Transfer
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_split_part_transfer);
        }
        helpVariant = HelpVariant.splitPartTransfer;
        mTransaction.status = STATUS_UNCOMMITTED;
      }
    } else {
      //Transfer or Transaction, we can suggest to create a plan
      if (!isCalendarPermissionPermanentlyDeclined()) {
        //we set adapter even if spinner is not immediately visible, since it might become visible
        //after SAVE_AND_NEW action
        RecurrenceAdapter recurrenceAdapter = new RecurrenceAdapter(this,
            Plan.Recurrence.ONETIME, Plan.Recurrence.CUSTOM);
        mRecurrenceSpinner.setAdapter(recurrenceAdapter);
        Plan.Recurrence cachedRecurrence = (Plan.Recurrence) getIntent().getSerializableExtra(KEY_CACHED_RECURRENCE);
        if (cachedRecurrence != null) {
          mRecurrenceSpinner.setSelection(
              ((ArrayAdapter) mRecurrenceSpinner.getAdapter()).getPosition(cachedRecurrence));
        }
        mRecurrenceSpinner.setOnItemSelectedListener(this);
        setPlannerRowVisibility(View.VISIBLE);
        if (mTransaction.originTemplate != null && mTransaction.originTemplate.getPlan() != null) {
          mRecurrenceSpinner.getSpinner().setVisibility(View.GONE);
          mPlanButton.setVisibility(View.VISIBLE);
          mPlanButton.setText(Plan.prettyTimeInfo(this,
              mTransaction.originTemplate.getPlan().rrule, mTransaction.originTemplate.getPlan().dtstart));
          mPlanButton.setOnClickListener(view -> PlanMonthFragment.newInstance(
              mTransaction.originTemplate.getTitle(),
              mTransaction.originTemplate.getId(),
              mTransaction.originTemplate.planId,
              getCurrentAccount().color, true).show(getSupportFragmentManager(),
              TemplatesList.CALDROID_DIALOG_FRAGMENT_TAG));
        }
      }
      if (mTransaction instanceof Transfer) {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_transfer);
        }
        helpVariant = HelpVariant.transfer;
      } else if (mTransaction instanceof Transaction) {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_transaction);
        }
        helpVariant = HelpVariant.transaction;
      } else if (mTransaction instanceof SplitTransaction) {
        if (!mNewInstance) {
          setTitle(R.string.menu_edit_split);
        }
        helpVariant = HelpVariant.split;
      }
    }
    if (mOperationType == TYPE_SPLIT) {
      categoryContainer.setVisibility(View.GONE);
      //add split list
      if (findSplitPartList() == null) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
            .add(R.id.OneExpense, SplitPartList.newInstance(mTransaction), SPLIT_PART_LIST)
            .commit();
        fm.executePendingTransactions();
      }
    }
    if (mClone) {
      setTitle(R.string.menu_clone_transaction);
    }

    if (isNoMainTransaction()) {
      findViewById(R.id.DateTimeRow).setVisibility(View.GONE);
    } else {
      //noinspection SetTextI18n
      ((TextView) findViewById(R.id.DateTimeLabel)).setText(getString(
          R.string.date) + " / " + getString(R.string.time));
      mDateButton.setOnClickListener(v -> hideKeyBoardAndShowDialog(DATE_DIALOG_ID));

      mTimeButton.setOnClickListener(v -> hideKeyBoardAndShowDialog(TIME_DIALOG_ID));
    }

    //when we have a savedInstance, fields have already been populated
    if (!mSavedInstance) {
      populateFields();
    }

    if (!(isSplitPart())) {
      setDateTime();
    }
    //after setdatetime, so that the plan info can override the date
    configurePlan();


    if (mType == INCOME && mOperationType == TYPE_TRANSFER) {
      switchAccountViews();
    }

    setCategoryButton();
    if (mOperationType != TYPE_TRANSFER) {
      mCategoryButton.setOnClickListener(view -> startSelectCategory());
    }
  }

  public void hideKeyBoardAndShowDialog(int id) {
    hideKeyboard();
    showDialog(id);
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
    linkInputWithLabel(mMethodSpinner.getSpinner(), methodLabel);
    linkInputWithLabel(mReferenceNumberText, methodLabel);
    View planLabel = findViewById(R.id.PlanLabel);
    linkInputWithLabel(mPlanButton, planLabel);
    linkInputWithLabel(mRecurrenceSpinner.getSpinner(), planLabel);
    linkInputWithLabel(mPlanToggleButton, planLabel);
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
      mTransaction.setMethodId(null);
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
    if (mTransaction != null && !(isNoMainTransaction() ||
        (mTransaction instanceof SplitTransaction &&
            !MyApplication.getInstance().getLicenceHandler().isContribEnabled()))) {
      MenuItemCompat.setShowAsAction(
          menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
              .setIcon(R.drawable.ic_action_save_new),
          MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    if (mOperationType == TYPE_TRANSFER) {
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
        if (mOperationType == TYPE_SPLIT &&
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
    i.putExtra(MyApplication.KEY_OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, account.getId());
    i.putExtra(KEY_PARENTID, mTransaction.getId());
    i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, mTransaction instanceof Template);
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
      (view, year, monthOfYear, dayOfMonth) -> {
        if (mCalendar.get(Calendar.YEAR) != year ||
            mCalendar.get(Calendar.MONTH) != monthOfYear ||
            mCalendar.get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
          mCalendar.set(year, monthOfYear, dayOfMonth);
          setDate();
          setDirty(true);
        }
      };

  /**
   * listens on changes in the time dialog and sets the time on the button
   */
  private TimePickerDialog.OnTimeSetListener mTimeSetListener =
      (view, hourOfDay, minute) -> {
        if (mCalendar.get(Calendar.HOUR_OF_DAY) != hourOfDay ||
            mCalendar.get(Calendar.MINUTE) != minute) {
          mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
          mCalendar.set(Calendar.MINUTE, minute);
          setTime();
          setDirty(true);
        }
      };

  @Override
  protected Dialog onCreateDialog(int id) {
    hideKeyboard();
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
        if (PrefKey.GROUP_WEEK_STARTS.isSet()) {
          int startOfWeek = Utils.getFirstDayOfWeekFromPreferenceWithFallbackToLocale(Locale.getDefault());
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            datePickerDialog.getDatePicker().setFirstDayOfWeek(startOfWeek);
          } else {
            try {
              if (Utils.hasApiLevel(Build.VERSION_CODES.HONEYCOMB_MR1)) {
                setFirstDayOfWeek(datePickerDialog, startOfWeek);
              }
            } catch (UnsupportedOperationException e) {/*Nothing left tod do*/}
          }
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

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private void setFirstDayOfWeek(DatePickerDialog datePickerDialog, int startOfWeek) {
    CalendarView calendarView = datePickerDialog.getDatePicker().getCalendarView();
    calendarView.setFirstDayOfWeek(startOfWeek);
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
    //processing data from user switching operation type
    Transaction cached = (Transaction) getIntent().getSerializableExtra(KEY_CACHED_DATA);
    Transaction cachedOrSelf = cached != null ? cached : mTransaction;

    isProcessingLinkedAmountInputs = true;
    mStatusSpinner.setSelection(cachedOrSelf.crStatus.ordinal(), false);
    mCommentText.setText(cachedOrSelf.getComment());
    if (mIsMainTransactionOrTemplate) {
      mPayeeText.setText(cachedOrSelf.getPayee());
    }
    if (mIsMainTemplate) {
      mTitleText.setText(((Template) cachedOrSelf).getTitle());
      mPlanToggleButton.setChecked(((Template) mTransaction).isPlanExecutionAutomatic());
    } else {
      mReferenceNumberText.setText(cachedOrSelf.getReferenceNumber());
    }

    fillAmount(cachedOrSelf.getAmount().getAmountMajor());

    if (mNewInstance) {
      if (mIsMainTemplate) {
        mTitleText.requestFocus();
      } else if (mIsMainTransactionOrTemplate && PrefKey.AUTO_FILL.getBoolean(false)) {
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
    if (syncStateAndValidate(true)) {
      mIsSaving = true;
      startDbWriteTask(true);
      if (getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
        SharedPreferences.Editor editor = MyApplication.getInstance().getSettings().edit();
        switch (mOperationType) {
          case TYPE_TRANSACTION:
            editor.putLong(PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
            break;
          case TYPE_TRANSFER:
            editor.putLong(PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
            editor.putLong(PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, mTransaction.getTransferAccountId());
            break;
          case TYPE_SPLIT:
            editor.putLong(PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
        }
        editor.apply();
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
  protected boolean syncStateAndValidate(boolean forSave) {
    boolean validP = true;
    String title;

    Account account = getCurrentAccount();
    if (account == null)
      return false;

    BigDecimal amount = validateAmountInput(forSave);

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
    mTransaction.setAccountId(account.getId());

    mTransaction.setComment(mCommentText.getText().toString());

    if (!isSplitPart()) {
      mTransaction.setDate(mCalendar.getTime());
    }

    if (mOperationType == TYPE_TRANSACTION) {
      mTransaction.setCatId(mCatId);
    }
    if (mIsMainTransactionOrTemplate) {
      mTransaction.setPayee(mPayeeText.getText().toString());
      long selected = mMethodSpinner.getSelectedItemId();
      mTransaction.setMethodId((selected != AdapterView.INVALID_ROW_ID && selected > 0) ?
          selected : null);
    }
    if (mOperationType == TYPE_TRANSFER) {
      mTransaction.setTransferAccountId(mTransferAccountSpinner.getSelectedItemId());
      final Account transferAccount = Account.getInstanceFromDb(mTransferAccountSpinner
          .getSelectedItemId());
      boolean isSame = account.currency.equals(transferAccount.currency);
      if (mTransaction instanceof Template) {
        if (!isSame && amount == null) {
          BigDecimal transferAmount = validateAmountInput(mTransferAmountText, forSave);
          if (transferAmount != null) {
            mTransaction.setAccountId(transferAccount.getId());
            mTransaction.setTransferAccountId(account.getId());
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
          BigDecimal transferAmount = validateAmountInput(mTransferAmountText, forSave);

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
    if (mIsMainTemplate) {
      title = mTitleText.getText().toString();
      if (title.equals("")) {
        if (forSave) {
          mTitleText.setError(getString(R.string.no_title_given));
        }
        validP = false;
      }
      ((Template) mTransaction).setTitle(title);
      String description = ((Template) mTransaction).compileDescription(ExpenseEdit.this, currencyFormatter);
      if (mPlan == null) {
        if (mRecurrenceSpinner.getSelectedItemPosition() > 0) {
          mPlan = new Plan(
              mCalendar,
              ((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem()).toRrule(mCalendar),
              ((Template) mTransaction).getTitle(),
              description);
          ((Template) mTransaction).setPlan(mPlan);
        }
      } else {
        mPlan.description = description;
        mPlan.title = title;
        ((Template) mTransaction).setPlan(mPlan);
      }
    } else {
      mTransaction.setReferenceNumber(mReferenceNumberText.getText().toString());
      if (forSave && !(isSplitPart())) {
        if (mRecurrenceSpinner.getSelectedItemPosition() > 0) {
          title = TextUtils.isEmpty(mTransaction.getPayee()) ?
            (mOperationType == TYPE_SPLIT || TextUtils.isEmpty(mLabel) ?
                (TextUtils.isEmpty(mTransaction.getComment()) ?
                    getString(R.string.menu_create_template) : mTransaction.getComment()) : mLabel) : mTransaction.getPayee();
          String description = mTransaction.compileDescription(ExpenseEdit.this, currencyFormatter);
          mTransaction.setInitialPlan(new Plan(
              mCalendar,
              ((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem()).toRrule(mCalendar),
              title,
              description));
        }
      }
    }

    mTransaction.crStatus = (Transaction.CrStatus) mStatusSpinner.getSelectedItem();

    mTransaction.setPictureUri(mPictureUri);
    return validP;
  }

  private boolean isSplitPart() {
    return mTransaction.isSplitpart();
  }

  private boolean isNoMainTransaction() {
    return isSplitPart() || mTransaction instanceof Template;
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SELECT_CATEGORY_REQUEST && intent != null) {
      mCatId = intent.getLongExtra(KEY_CATID, 0);
      mLabel = intent.getStringExtra(KEY_LABEL);
      mCategoryButton.setText(mLabel);
      setDirty(true);
    }
    if (requestCode == PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
      Uri uri;
      String errorMsg;
      if (intent == null) {
        uri = mPictureUriTemp;
        Timber.d("got result for PICTURE request, intent null, relying on stored output uri %s", mPictureUriTemp);
      } else if (intent.getData() != null) {
        uri = intent.getData();
        Timber.d("got result for PICTURE request, found uri in intent data %s", uri.toString());
      } else {
        Timber.d("got result for PICTURE request, intent != null, getData() null, relying on stored output uri %s", mPictureUriTemp);
        uri = mPictureUriTemp;
      }
      if (uri != null) {
        mPictureUri = uri;
        if (PermissionHelper.canReadUri(uri, this)) {
          setPicture();
          setDirty(true);
        } else {
          requestStoragePermission();
        }
        return;
      } else {
        errorMsg = "Error while retrieving image: No data found.";
      }
      AcraHelper.report(new Exception(errorMsg));
      Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
    if (requestCode == PLAN_REQUEST) {
      finish();
    }
  }

  protected void setPicture() {
    if (mPictureUri != null) {
      mPictureViewContainer.setVisibility(View.VISIBLE);
      Picasso.with(this).load(mPictureUri).fit().into((ImageView) mPictureViewContainer.findViewById(R.id.picture));
      mAttachPictureButton.setVisibility(View.GONE);
    }
  }

  @Override
  public void onBackPressed() {
    cleanup();
    super.onBackPressed();
  }

  protected void cleanup() {
    mTransaction.cleanupCanceledEdit();
  }

  /**
   * updates interface based on type (EXPENSE or INCOME)
   */
  protected void configureType() {
    super.configureType();
    if (mPayeeLabel != null) {
      mPayeeLabel.setText(mType ? R.string.payer : R.string.payee);
    }
    if (mOperationType == TYPE_SPLIT) {
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
      mRecurrenceSpinner.getSpinner().setVisibility(View.GONE);
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
      } else {
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
    if (mPlan != null) {
      startTaskExecution(TaskExecutionFragment.TASK_INSTANTIATE_PLAN,
          new Long[]{mPlan.getId()}, null, 0);
    } else {
      //seen in report 96a04ce6a647555356751634fee9fc73, need to investigate how this can happen
      AcraHelper.report(new Exception("Received onChange on ContentOberver for plan, but mPlan is null"));
    }
  }

  private void configureStatusSpinner() {
    Account a = getCurrentAccount();
    mStatusSpinner.getSpinner().setVisibility((isNoMainTransaction() ||
        a == null || a.type.equals(AccountType.CASH)) ? View.GONE : View.VISIBLE);
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
      outState.putLong(mTransaction instanceof Template ? KEY_TEMPLATEID : KEY_ROWID, mRowId);
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
    if (didUserSetAccount) {
      long accountId = mAccountSpinner.getSelectedItemId();
      if (accountId == android.widget.AdapterView.INVALID_ROW_ID && mAccountId != null) {
        accountId = mAccountId;
      }
      if (accountId != android.widget.AdapterView.INVALID_ROW_ID) {
        outState.putLong(KEY_ACCOUNTID, accountId);
      }
    }
    if (mOperationType == TYPE_TRANSFER) {
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
            mLabel = t.getLabel();
            setCategoryButton();
          }
          if (TextUtils.isEmpty(mCommentText.getText().toString())) {
            mCommentText.setText(t.getComment());
          }
          if (TextUtils.isEmpty(mAmountText.getText().toString())) {
            fillAmount(t.getAmount().getAmountMajor());
            configureType();
          }
          if (!didUserSetAccount && getIntent().getBooleanExtra(KEY_AUTOFILL_MAY_SET_ACCOUNT, false)
              && mAccounts != null) {
            for (int i = 0; i < mAccounts.length; i++) {
              if (mAccounts[i].getId().equals(t.getAccountId())) {
                mAccountSpinner.setSelection(i);
                break;
              }
            }
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
        if (mTransaction instanceof Template) {
          mPlan = ((Template) mTransaction).getPlan();
        }
        mOperationType = mTransaction.operationType();
        if (mPictureUri == null) { // we might have received a picture in onActivityResult before
          // arriving here, in this case it takes precedence
          mPictureUri = mTransaction.getPictureUri();
          if (mPictureUri != null) {
            if (PictureDirHelper.doesPictureExist(mTransaction.getPictureUri())) {
              setPicture();
            } else {
              mPictureUri = null;
              Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
            }
          }
        }
        //if catId has already been set by onRestoreInstanceState, the value might have been edited by the user and has precedence
        if (mCatId == null) {
          mCatId = mTransaction.getCatId();
          mLabel = mTransaction.getLabel();
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
          mTransaction.uuid = Model.generateUuid();
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
            if (mAccounts[i].getId().equals(mTransaction.getAccountId())) {
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
        if (id > 0) {
          if (ContextCompat.checkSelfPermission(ExpenseEdit.this,
              Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            boolean newSplitTemplateEnabled = PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true);
            boolean newPlanEnabled = PrefKey.NEW_PLAN_ENABLED.getBoolean(true);
            if (newPlanEnabled && (newSplitTemplateEnabled || mOperationType != TYPE_SPLIT)) {
              visibility = View.VISIBLE;
              showCustomRecurrenceInfo();
            } else {
              mRecurrenceSpinner.setSelection(0);
              ContribFeature contribFeature = mOperationType != TYPE_SPLIT || newSplitTemplateEnabled ?
                  ContribFeature.PLANS_UNLIMITED : ContribFeature.SPLIT_TEMPLATE;
              CommonCommands.showContribDialog(this, contribFeature, null);
            }
          } else {
            requestPermission(PermissionHelper.PermissionGroup.CALENDAR);
          }
        }
        if (mTransaction instanceof Template) {
          mPlanButton.setVisibility(visibility);
          mPlanToggleButton.setVisibility(visibility);
        }
        break;
      case R.id.Method:
        if (id > 0) {
          //ignore first row "no method" merged in
          mMethodsCursor.moveToPosition(position - 1);
          if (!(mTransaction instanceof Template))
            mReferenceNumberText.setVisibility(mMethodsCursor.getInt(mMethodsCursor.getColumnIndexOrThrow(KEY_IS_NUMBERED)) > 0 ?
                View.VISIBLE : View.INVISIBLE);
        } else {
          mTransaction.setMethodId(null);
          mReferenceNumberText.setVisibility(View.GONE);
        }
        break;
      case R.id.Account:
        final Account account = mAccounts[position];
        if (mOperationType == TYPE_SPLIT && findSplitPartList().getSplitCount() > 0) {
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
          if (newType == TYPE_TRANSFER && !checkTransferEnabled(getCurrentAccount())) {
            //reset to previous
            resetOperationType();
          } else if (newType == TYPE_SPLIT) {
            resetOperationType();
            if (mTransaction instanceof Template) {
              if (PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true)) {
                restartWithType(TYPE_SPLIT);
              } else {
                CommonCommands.showContribDialog(this, ContribFeature.SPLIT_TEMPLATE, null);
              }
            } else {
              contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null);
            }
          } else {
            restartWithType(newType);
          }
        }
        break;
      case R.id.TransferAccount:
        mTransaction.setTransferAccountId(mTransferAccountSpinner.getSelectedItemId());
        configureTransferInput();
        break;
    }
  }

  private void showCustomRecurrenceInfo() {
    if (mRecurrenceSpinner.getSelectedItem() == Plan.Recurrence.CUSTOM) {
      Snackbar snackbar = Snackbar.make(findViewById(R.id.OneExpense),
          R.string.plan_custom_recurrence_info, Snackbar.LENGTH_LONG);
      View snackbarView = snackbar.getView();
      TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
      textView.setMaxLines(3);
      UiUtils.configureSnackbarForDarkTheme(snackbar);
      snackbar.show();
    }
  }

  private boolean isValidType(int type) {
    return type == TYPE_SPLIT || type == TYPE_TRANSACTION ||
        type == TYPE_TRANSFER;
  }

  private void updateAccount(Account account) {
    didUserSetAccount = true;
    mTransaction.setAccountId(account.getId());
    setAccountLabel(account);
    if (mOperationType == TYPE_TRANSFER) {
      mTransferAccountSpinner.setSelection(setTransferAccountFilterMap());
      mTransaction.setTransferAccountId(mTransferAccountSpinner.getSelectedItemId());
      configureTransferInput();
    } else {
      if (!mTransaction.isSplitpart()) {
        if (mManager.getLoader(METHODS_CURSOR) != null && !mManager.getLoader(METHODS_CURSOR).isReset()) {
          mManager.restartLoader(METHODS_CURSOR, null, this);
        } else {
          mManager.initLoader(METHODS_CURSOR, null, this);
        }
      }
      if (mOperationType == TYPE_SPLIT) {
        final SplitPartList splitPartList = findSplitPartList();
        splitPartList.updateAccount(account);
      }
    }
    configureStatusSpinner();
    mAmountText.setFractionDigits(Money.getFractionDigits(account.currency));
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
    Bundle bundle = new Bundle();
    bundle.putInt(Tracker.EVENT_PARAM_OPERATION_TYPE, newType);
    logEvent(Tracker.EVENT_SELECT_OPERATION_TYPE, bundle);
    cleanup();
    Intent restartIntent = getIntent();
    restartIntent.putExtra(MyApplication.KEY_OPERATION_TYPE, newType);
    syncStateAndValidate(false);
    restartIntent.putExtra(KEY_CACHED_DATA, mTransaction);
    if (mOperationType != TYPE_SPLIT && newType != TYPE_SPLIT) {
      restartIntent.putExtra(KEY_CACHED_RECURRENCE, ((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem()));
    }
    if (mTransaction.getPictureUri() != null) {
      restartIntent.putExtra(KEY_CACHED_PICTURE_URI, mTransaction.getPictureUri());
    }
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
      String errorMsg;
      switch (sequenceCount.intValue()) {
        case DbWriteFragment.ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE:
          errorMsg = getString(R.string.external_storage_unavailable);
          break;
        case DbWriteFragment.ERROR_PICTURE_SAVE_UNKNOWN:
          errorMsg = "Error while saving picture";
          break;
        case DbWriteFragment.ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE:
          mRecurrenceSpinner.setSelection(0);
          mTransaction.originTemplate = null;
          errorMsg = "Recurring transactions are not available, because calendar integration is not functional on this device.";
          break;
        default:
          //possibly the selected category has been deleted
          mCatId = null;
          mCategoryButton.setText(R.string.select);

          errorMsg = "Error while saving transaction";
      }
      Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
      mCreateNew = false;
    } else {
      if (mRecordTemplateWidget) {
        recordUsage(ContribFeature.TEMPLATE_WIDGET);
        TemplateWidget.showContribMessage(this);
      }
      if (mCreateNew) {
        mCreateNew = false;
        if (mOperationType == TYPE_SPLIT) {
          mTransaction = SplitTransaction.getNewInstance(mTransaction.getAccountId());
          mRowId = mTransaction.getId();
          findSplitPartList().updateParent(mRowId);
        } else {
          mTransaction.setId(0L);
          mTransaction.uuid = Model.generateUuid();
          mRowId = 0L;
          mRecurrenceSpinner.getSpinner().setVisibility(View.VISIBLE);
          mRecurrenceSpinner.setSelection(0);
          mPlanButton.setVisibility(View.GONE);
        }
        //while saving the picture might have been moved from temp to permanent
        mPictureUri = mTransaction.getPictureUri();
        mNewInstance = true;
        mClone = false;
        isProcessingLinkedAmountInputs = true;
        mAmountText.setText("");
        mTransferAmountText.setText("");
        isProcessingLinkedAmountInputs = false;
        Toast.makeText(this, getString(R.string.save_transaction_and_new_success), Toast.LENGTH_SHORT).show();
      } else {
        if (mRecurrenceSpinner.getSelectedItem() == Plan.Recurrence.CUSTOM) {
          launchPlanView(true);
        } else {
          //make sure soft keyboard is closed
          hideKeyboard();
          Intent intent = new Intent();
          intent.putExtra(KEY_SEQUENCE_COUNT, sequenceCount);
          setResult(RESULT_OK, intent);
          finish();
          //no need to call super after finish
          return;
        }
      }
    }
    super.onPostExecute(result);
  }

  private void hideKeyboard() {
    InputMethodManager im = (InputMethodManager) this.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    im.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
            mTransaction.setMethodId(mMethodId);
          }
          if (mTransaction.getMethodId() != null) {
            while (!data.isAfterLast()) {
              if (data.getLong(data.getColumnIndex(KEY_ROWID)) == mTransaction.getMethodId()) {
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
        if (data.getCount() == 1 && mOperationType == TYPE_TRANSFER) {
          Toast.makeText(this, R.string.dialog_command_disabled_insert_transfer, Toast.LENGTH_LONG).show();
          finish();
          return;
        }
        mAccounts = new Account[data.getCount()];
        if (didUserSetAccount) {
          mTransaction.setAccountId(mAccountId);
          mTransaction.setTransferAccountId(mTransferAccountId);
        }
        data.moveToFirst();
        boolean selectionSet = false;
        String currencyExtra = didUserSetAccount ? null : getIntent().getStringExtra(KEY_CURRENCY);
        while (!data.isAfterLast()) {
          int position = data.getPosition();
          Account a = Account.fromCacheOrFromCursor(data);
          mAccounts[position] = a;
          if (!selectionSet &&
              (a.currency.getCurrencyCode().equals(currencyExtra) ||
                  (currencyExtra == null && a.getId().equals(mTransaction.getAccountId())))) {
            mAccountSpinner.setSelection(position);
            setAccountLabel(a);
            selectionSet = true;
          }
          data.moveToNext();
        }
        //if the accountId we have been passed does not exist, we select the first entry
        if (mAccountSpinner.getSelectedItemPosition() == android.widget.AdapterView.INVALID_POSITION) {
          mAccountSpinner.setSelection(0);
          mTransaction.setAccountId(mAccounts[0].getId());
          setAccountLabel(mAccounts[0]);
        }
        if (mOperationType == TYPE_TRANSFER) {
          mTransferAccountCursor = new FilterCursorWrapper(data);
          int selectedPosition = setTransferAccountFilterMap();
          mTransferAccountsAdapter.swapCursor(mTransferAccountCursor);
          mTransferAccountSpinner.setSelection(selectedPosition);
          mTransaction.setTransferAccountId(mTransferAccountSpinner.getSelectedItemId());
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
          if (!mTransaction.isSplitpart()) {
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
      if (!fromAccount.getId().equals(mAccounts[i].getId())) {
        list.add(i);
        if (mTransaction.getTransferAccountId() != null && mTransaction.getTransferAccountId().equals(mAccounts[i].getId())) {
          selectedPosition = position;
        }
        position++;
      }
    }
    mTransferAccountCursor.setFilterMap(list);
    mTransferAccountsAdapter.notifyDataSetChanged();
    return selectedPosition;
  }

  private void launchPlanView(boolean forResult) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(ContentUris.withAppendedId(Events.CONTENT_URI, mPlan.getId()));
    //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan.dtstart);
    intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan.dtstart);
    if (Utils.isIntentAvailable(this, intent)) {
      if (forResult) {
        startActivityForResult(intent, PLAN_REQUEST);
      } else {
        startActivity(intent);
      }
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
    ((Template) mTransaction).setPlanExecutionAutomatic(((ToggleButton) view).isChecked());
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (feature == ContribFeature.ATTACH_PICTURE) {
      startMediaChooserDo();
    } else if (feature == ContribFeature.SPLIT_TRANSACTION) {
      restartWithType(TYPE_SPLIT);
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
        PrefKey.AUTO_FILL.putBoolean(true);
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
      PrefKey.AUTO_FILL.putBoolean(false);
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
        startActivity(imageViewIntentProvider.getViewIntent(this, mPictureUri));
        break;
      case R.id.CHANGE_COMMAND:
        startMediaChooserDo();
        break;
    }
  }

  public void startMediaChooser(View v) {
    contribFeatureRequested(ContribFeature.ATTACH_PICTURE, null);
  }

  @Override
  public void contribFeatureRequested(@NonNull ContribFeature feature, Serializable tag) {
    hideKeyboard();
    super.contribFeatureRequested(feature, tag);
  }

  public void startMediaChooserDo() {

    Uri outputMediaUri = getCameraUri();
    Intent gallIntent = new Intent(PictureDirHelper.getContentIntentAction());
    gallIntent.setType("image/*");
    Intent chooserIntent = Intent.createChooser(gallIntent, null);

    //if external storage is not available, camera capture won't work
    if (outputMediaUri != null) {
      Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      camIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputMediaUri);

      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
          new Intent[]{camIntent});
    }
    Timber.d("starting chooser for PICTURE_REQUEST with EXTRA_OUTPUT %s ", outputMediaUri);
    startActivityForResult(chooserIntent, ProtectedFragmentActivity.PICTURE_REQUEST_CODE);
  }

  private Uri getCameraUri() {
    if (mPictureUriTemp == null) {
      mPictureUriTemp = PictureDirHelper.getOutputMediaUri(true);
    }
    return mPictureUriTemp;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[], @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    // If request is cancelled, the result arrays are empty.
    boolean granted = grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    switch (requestCode) {
      case PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR: {
        if (granted) {
          if (mTransaction instanceof Template) {
            mPlanButton.setVisibility(View.VISIBLE);
            mPlanToggleButton.setVisibility(View.VISIBLE);
            showCustomRecurrenceInfo();
          }
        } else {
          mRecurrenceSpinner.setSelection(0);
          if (!ActivityCompat.shouldShowRequestPermissionRationale(
              this, Manifest.permission.WRITE_CALENDAR)) {
            setPlannerRowVisibility(View.GONE);
          }
        }
      }
      case PermissionHelper.PERMISSIONS_REQUEST_STORAGE: {
        if (granted) {
          setPicture();
        } else {
          mPictureUri = null;
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
    BigDecimal inverseExchangeRate =
        (amount != null && transferAmount != null && transferAmount.compareTo(nullValue) != 0) ?
            amount.divide(transferAmount, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) : nullValue;
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
    /**
     * true if we are linked to from amount
     */
    boolean isMain;

    public LinkedTransferAmountTextWatcher(boolean isMain) {
      this.isMain = isMain;
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (isProcessingLinkedAmountInputs) return;
      int currentFocus = isMain ? INPUT_AMOUNT : INPUT_TRANSFER_AMOUNT;
      isProcessingLinkedAmountInputs = true;
      if (mTransaction instanceof Template) {
        (isMain ? mTransferAmountText : mAmountText).setText("");
      } else {
        if (lastExchangeRateRelevantInputs[0] != currentFocus) {
          lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0];
          lastExchangeRateRelevantInputs[0] = currentFocus;
        }
        if (lastExchangeRateRelevantInputs[1] == INPUT_EXCHANGE_RATE) {
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
    /**
     * true if we are linked to exchange rate where unit is from account currency
     */
    boolean isMain;

    public LinkedExchangeRateTextWatchter(boolean isMain) {
      this.isMain = isMain;
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (isProcessingLinkedAmountInputs) return;
      isProcessingLinkedAmountInputs = true;
      if (lastExchangeRateRelevantInputs[0] != INPUT_EXCHANGE_RATE) {
        lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0];
        lastExchangeRateRelevantInputs[0] = INPUT_EXCHANGE_RATE;
      }
      BigDecimal inputRate = validateAmountInput(
          isMain ? mExchangeRate1Text : mExchangeRate2Text, false);
      if (inputRate == null) inputRate = nullValue;
      BigDecimal inverseInputRate = calculateInverse(inputRate);
      (isMain ? mExchangeRate2Text : mExchangeRate1Text).setAmount(inverseInputRate);


      AmountEditText constant, variable;
      boolean mainProvided;
      if (lastExchangeRateRelevantInputs[1] == INPUT_AMOUNT) {
        constant = mAmountText;
        variable = mTransferAmountText;
        mainProvided = true;
      } else {
        constant = mTransferAmountText;
        variable = mAmountText;
        mainProvided = false;
      }
      BigDecimal input = validateAmountInput(constant, false);
      if (input != null) {
        variable.setAmount(input.multiply(
            mainProvided == isMain ? inputRate : inverseInputRate));
      }
      isProcessingLinkedAmountInputs = false;
    }
  }

  private BigDecimal calculateInverse(BigDecimal input) {
    return input.compareTo(nullValue) != 0 ?
        new BigDecimal(1).divide(input, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) :
        nullValue;
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
