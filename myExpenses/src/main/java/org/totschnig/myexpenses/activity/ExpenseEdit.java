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
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import android.widget.ToggleButton;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;
import com.squareup.picasso.Picasso;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CrStatusAdapter;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.adapter.OperationTypeAdapter;
import org.totschnig.myexpenses.adapter.RecurrenceAdapter;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.PlanMonthFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.preference.PreferenceUtils;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.FilterCursorWrapper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.MyExpenses.KEY_SEQUENCE_COUNT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.task.BuildTransactionTask.KEY_EXTRAS;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

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
  private static final String KEY_CACHED_DATA = "cachedData";
  private static final String KEY_CACHED_RECURRENCE = "cachedRecurrence";
  private static final String KEY_CACHED_PICTURE_URI = "cachedPictureUri";
  public static final String KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount";
  private static final String PREFKEY_TRANSACTION_LAST_ACCOUNT_FROM_WIDGET = "transactionLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_ACCOUNT_FROM_WIDGET = "transferLastAccountFromWidget";
  private static final String PREFKEY_TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET = "transferLastTransferAccountFromWidget";
  private static final String PREFKEY_SPLIT_LAST_ACCOUNT_FROM_WIDGET = "splitLastAccountFromWidget";
  private static final String KEY_AUTOFILL_OVERRIDE_PREFERENCES = "autoFillOverridePreferences";
  private static int INPUT_EXCHANGE_RATE = 1;
  private static int INPUT_AMOUNT = 2;
  private static int INPUT_TRANSFER_AMOUNT = 3;
  private int[] lastExchangeRateRelevantInputs = {INPUT_EXCHANGE_RATE, INPUT_AMOUNT};
  @BindView(R.id.DateButton)
  Button mDateButton;
  @BindView(R.id.TimeButton)
  Button mTimeButton;
  @BindView(R.id.Comment)
  EditText mCommentText;
  @BindView(R.id.Title)
  EditText mTitleText;
  @BindView(R.id.Number)
  EditText mReferenceNumberText;
  @BindView(R.id.ExchangeRate)
  ExchangeRateEdit mExchangeRateEdit;
  @BindView(R.id.TranferAmount)
  AmountEditText mTransferAmountText;
  @BindView(R.id.OriginalAmount)
  AmountEditText originalAmountText;
  @BindView(R.id.Category)
  Button mCategoryButton;
  @BindView(R.id.Plan)
  Button mPlanButton;
  @BindView(R.id.Payee)
  AutoCompleteTextView mPayeeText;
  @BindView(R.id.PayeeLabel)
  TextView mPayeeLabel;
  @BindView(R.id.PlanExecutionAutomatic)
  ToggleButton mPlanToggleButton;
  @BindView(R.id.AttachImage)
  ImageView mAttachPictureButton;
  @BindView(R.id.picture_container)
  FrameLayout mPictureViewContainer;
  @BindView(R.id.TitleRow)
  ViewGroup titleRow;
  @BindView(R.id.AccountRow)
  ViewGroup accountRow;
  @BindView(R.id.OriginalAmountRow)
  ViewGroup originalAmountRow;
  @BindView(R.id.TransferAmountRow)
  ViewGroup transferAmountRow;
  @BindView(R.id.TransferAccountRow)
  ViewGroup transferAccountRow;
  @BindView(R.id.DateTimeRow)
  ViewGroup dateTimeRow;
  @BindView(R.id.PayeeRow)
  ViewGroup payeeRow;
  @BindView(R.id.CategoryRow)
  ViewGroup categoryRow;
  @BindView(R.id.MethodRow)
  ViewGroup methodRow;
  @BindView(R.id.PlannerRow)
  ViewGroup plannerRow;

  private SpinnerHelper mMethodSpinner, mAccountSpinner, mTransferAccountSpinner, mStatusSpinner,
      mOperationTypeSpinner, mRecurrenceSpinner, mCurrencySpinner;
  private SimpleCursorAdapter mMethodsAdapter, mAccountsAdapter, mTransferAccountsAdapter, mPayeeAdapter;
  private OperationTypeAdapter mOperationTypeAdapter;
  private FilterCursorWrapper mTransferAccountCursor;

  @State
  Long mRowId = 0L;
  @State
  Long mTemplateId;
  @State
  Calendar mCalendar = Calendar.getInstance();
  @State
  Long mCatId = null;
  @State
  Long mMethodId = null;
  @State
  Long mAccountId = null;
  @State
  Long mTransferAccountId;
  @State
  String mLabel;
  @State
  Uri mPictureUri;
  @State
  Uri mPictureUriTemp;

  private DateFormat mDateFormat, mTimeFormat;
  private Account[] mAccounts;
  private Transaction mTransaction;
  private Cursor mMethodsCursor;
  private Plan mPlan;
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
  public static final int AUTOFILL_CURSOR = 8;
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
  private CurrencyAdapter currencyAdapter;

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
    ButterKnife.bind(this);
    //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
    mTypeButton.setEnabled(false);
    mExchangeRateEdit.setExchangeRateWatcher(new LinkedExchangeRateTextWatchter());

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
          new String[]{KEY_ROWID, KEY_PAYEE_NAME},
          selection, selectArgs, null);
    });

    mPayeeAdapter.setCursorToStringConverter(cur -> cur.getString(1));
    FragmentManager supportFragmentManager = getSupportFragmentManager();
    mPayeeText.setOnItemClickListener((parent, view, position, id) -> {
      Cursor c = (Cursor) mPayeeAdapter.getItem(position);
      if (c.moveToPosition(position)) {
        long payeeId = c.getLong(0);
        mTransaction.updatePayeeWithId(c.getString(1), payeeId);
        if (mNewInstance && mTransaction != null &&
            !(mTransaction instanceof Template || mTransaction instanceof SplitTransaction)) {
          //moveToPosition should not be necessary,
          //but has been reported to not be positioned correctly on samsung GT-I8190N
          if (PrefKey.AUTO_FILL_HINT_SHOWN.getBoolean(false)) {
            if (PreferenceUtils.shouldStartAutoFill()) {
              startAutoFill(payeeId, false);
            }
          } else {
            Bundle b = new Bundle();
            b.putLong(KEY_ROWID, payeeId);
            b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information);
            b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string
                .hint_auto_fill));
            b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND);
            b.putString(ConfirmationDialogFragment.KEY_PREFKEY, PrefKey
                .AUTO_FILL_HINT_SHOWN.getKey());
            b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes);
            b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no);
            ConfirmationDialogFragment.newInstance(b).show(supportFragmentManager,
                "AUTO_FILL_HINT");
          }
        }
      }
    });

    mMethodSpinner = new SpinnerHelper(findViewById(R.id.Method));
    mAccountSpinner = new SpinnerHelper(findViewById(R.id.Account));
    mTransferAccountSpinner = new SpinnerHelper(findViewById(R.id.TransferAccount));
    mTransferAccountSpinner.setOnItemSelectedListener(this);
    mStatusSpinner = new SpinnerHelper(findViewById(R.id.Status));
    mRecurrenceSpinner = new SpinnerHelper(findViewById(R.id.Recurrence));
    mCurrencySpinner = new SpinnerHelper(findViewById(R.id.OriginalCurrency));
    currencyAdapter = new CurrencyAdapter(this) {
      @NonNull
      @Override
      public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ((TextView) view).setText(getItem(position).name());
        return view;
      }
    };
    mCurrencySpinner.setAdapter(currencyAdapter);
    TextPaint paint = mPlanToggleButton.getPaint();
    int automatic = (int) paint.measureText(getString(R.string.plan_automatic));
    int manual = (int) paint.measureText(getString(R.string.plan_manual));
    mPlanToggleButton.setWidth(
        (automatic > manual ? automatic : manual) +
            +mPlanToggleButton.getPaddingLeft()
            + mPlanToggleButton.getPaddingRight());

    Bundle extras = getIntent().getExtras();
    mRowId = Utils.getFromExtra(extras, KEY_ROWID, 0);
    mTemplateId = getIntent().getLongExtra(KEY_TEMPLATEID, 0);

    //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
    if (savedInstanceState != null) {
      mSavedInstance = true;
      Icepick.restoreInstanceState(this, savedInstanceState);
      setPicture();
      if (mAccountId != null) {
        didUserSetAccount = true;
      }
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
      if (supportFragmentManager.findFragmentByTag(ASYNC_TAG) == null) {
        startTaskExecution(
            taskId,
            new Long[]{objectId},
            extra,
            R.string.progress_dialog_loading);
      }
    } else {
      mOperationType = getIntent().getIntExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      if (!isValidType(mOperationType)) {
        mOperationType = TYPE_TRANSACTION;
      }
      final boolean isNewTemplate = getIntent().getBooleanExtra(KEY_NEW_TEMPLATE, false);
      if (mOperationType == TYPE_SPLIT) {
        boolean allowed;
        ContribFeature contribFeature;
        if (isNewTemplate) {
          contribFeature = ContribFeature.SPLIT_TEMPLATE;
          allowed = PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true);
        } else {
          contribFeature = ContribFeature.SPLIT_TRANSACTION;
          allowed = contribFeature.hasAccess() || contribFeature.usagesLeft() > 0;
        }
        if (!allowed) {
          abortWithMessage(contribFeature.buildRequiresString(this));
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
      if (!mSavedInstance && Intent.ACTION_INSERT.equals(getIntent().getAction()) && extras != null) {
        Bundle args = new Bundle(1);
        args.putBundle(KEY_EXTRAS, extras);
        startTaskExecution(TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS, args,
            R.string.progress_dialog_loading);
      } else {
        if (isNewTemplate) {
          mTransaction = Template.getTypedNewInstance(mOperationType, accountId, true, parentId != 0 ? parentId : null);
          if (mOperationType == TYPE_SPLIT && mTransaction != null) {
            mTemplateId = mTransaction.getId();
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
          IllegalStateException e = new IllegalStateException(errMsg);
          if (extras != null) {
            AcraHelper.report(e, "Extras", extras.toString());
          } else {
            AcraHelper.report(e);
          }
          abortWithMessage(errMsg);
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
            mMethodId = cached.getMethodId();
          }
        }
        setup();
      }
    }
  }

  private void abortWithMessage(String message) {
    showSnackbar(message, Snackbar.LENGTH_LONG);
    finish();
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
      payeeRow.setVisibility(View.GONE);
      methodRow.setVisibility(View.GONE);
    }

    TextView accountLabelTv = findViewById(R.id.AccountLabel);
    if (mOperationType == TYPE_TRANSFER) {
      mTypeButton.setVisibility(View.GONE);
      categoryRow.setVisibility(View.GONE);
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
      titleRow.setVisibility(View.VISIBLE);
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
      categoryRow.setVisibility(View.GONE);
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
      dateTimeRow.setVisibility(View.GONE);
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
    plannerRow.setVisibility(visibility);
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
    final View dateTimeLabel = findViewById(R.id.DateTimeLabel);
    linkInputWithLabel(mDateButton, dateTimeLabel);
    linkInputWithLabel(mTimeButton, dateTimeLabel);
    linkInputWithLabel(mPayeeText, mPayeeLabel);
    final View commentLabel = findViewById(R.id.CommentLabel);
    linkInputWithLabel(mStatusSpinner.getSpinner(), commentLabel);
    linkInputWithLabel(mAttachPictureButton, commentLabel);
    linkInputWithLabel(mPictureViewContainer, commentLabel);
    linkInputWithLabel(mCommentText, commentLabel);
    linkInputWithLabel(mCategoryButton, findViewById(R.id.CategoryLabel));
    final View methodLabel = findViewById(R.id.MethodLabel);
    linkInputWithLabel(mMethodSpinner.getSpinner(), methodLabel);
    linkInputWithLabel(mReferenceNumberText, methodLabel);
    final View planLabel = findViewById(R.id.PlanLabel);
    linkInputWithLabel(mPlanButton, planLabel);
    linkInputWithLabel(mRecurrenceSpinner.getSpinner(), planLabel);
    linkInputWithLabel(mPlanToggleButton, planLabel);
    final View transferAmountLabel = findViewById(R.id.TransferAmountLabel);
    linkInputWithLabel(mTransferAmountText, transferAmountLabel);
    linkInputWithLabel(findViewById(R.id.CalculatorTransfer), transferAmountLabel);
    final View exchangeRateAmountLabel = findViewById(R.id.ExchangeRateLabel);
    linkInputWithLabel(findViewById(R.id.ExchangeRate_1), exchangeRateAmountLabel);
    linkInputWithLabel(findViewById(R.id.ExchangeRate_2), exchangeRateAmountLabel);
    final View originalAmountLabel = findViewById(R.id.OriginalAmountLabel);
    linkInputWithLabel(originalAmountText, originalAmountLabel);
    linkInputWithLabel(mCurrencySpinner.getSpinner(), originalAmountLabel);
    linkInputWithLabel(findViewById(R.id.CalculatorOriginal), originalAmountLabel);
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
      menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
          .setIcon(R.drawable.ic_action_save_new)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }
    if (mOperationType == TYPE_TRANSFER) {
      menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
          .setIcon(R.drawable.ic_menu_move)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    } else {
      menu.add(Menu.NONE, R.id.ORIGINAL_AMOUNT_COMMAND, 0, R.string.original_amount)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case android.R.id.home: {
        cleanup();
        finish();
        return true;
      }
      case R.id.SAVE_COMMAND:
      case R.id.SAVE_AND_NEW_COMMAND: {
        if (mOperationType == TYPE_SPLIT &&
            !findSplitPartList().splitComplete()) {
          showSnackbar(getString(R.string.unsplit_amount_greater_than_zero), Snackbar.LENGTH_SHORT);
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
      }
      case R.id.CREATE_COMMAND: {
        createRow();
        return true;
      }
      case R.id.INVERT_TRANSFER_COMMAND: {
        mType = !mType;
        switchAccountViews();
        return true;
      }
      case R.id.ORIGINAL_AMOUNT_COMMAND: {
        originalAmountRow.setVisibility(View.VISIBLE);

        return true;
      }
    }
    return super.dispatchCommand(command, tag);
  }

  private boolean checkTransferEnabled(Account account) {
    if (account == null)
      return false;
    if (!(mAccounts.length > 1)) {
      showMessage(R.string.dialog_command_disabled_insert_transfer);
      return false;
    }
    return true;
  }

  private void createRow() {
    Account account = getCurrentAccount();
    if (account == null) {
      showSnackbar(R.string.account_list_not_yet_loaded, Snackbar.LENGTH_LONG);
      return;
    }
    Intent i = new Intent(this, ExpenseEdit.class);
    forwardDataEntryFromWidget(i);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
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
              setFirstDayOfWeek(datePickerDialog, startOfWeek);
            } catch (UnsupportedOperationException e) {/*Nothing left to do*/}
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

    if (cachedOrSelf.getOriginalAmount() != null) {
      originalAmountRow.setVisibility(View.VISIBLE);
      originalAmountText.setAmount(cachedOrSelf.getOriginalAmount().getAmountMajor());
      mCurrencySpinner.setSelection(currencyAdapter.getPosition(
          CurrencyEnum.valueOf(cachedOrSelf.getOriginalAmount().getCurrency().getCurrencyCode())));
    }

    if (mNewInstance) {
      if (mIsMainTemplate) {
        mTitleText.requestFocus();
      } else if (mIsMainTransactionOrTemplate && PreferenceUtils.shouldStartAutoFill()) {
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
   * @return false if any data is not valid, also informs user through snackbar
   */
  protected boolean syncStateAndValidate(boolean forSave) {
    boolean validP = true;
    String title;

    Account account = getCurrentAccount();
    if (account == null)
      return false;

    BigDecimal amount = validateAmountInput(forSave);

    if (amount == null) {
      //Snackbar is shown in validateAmountInput
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
      mTransaction.setMethodId(mMethodId);
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
          if (amount != null) {
            mTransaction.getTransferAmount().setAmountMajor(amount.negate());
          }
        } else {
          BigDecimal transferAmount = validateAmountInput(mTransferAmountText, forSave);

          if (transferAmount == null) {
            //Snackbar is shown in validateAmountInput
            validP = false;
          } else {
            if (mType == INCOME) {
              transferAmount = transferAmount.negate();
            }
            mTransaction.getTransferAmount().setAmountMajor(transferAmount);
          }
        }
      }
    } else {
      BigDecimal originalAmount = validateAmountInput(originalAmountText, false);
      if (originalAmount != null) {
        String currency = ((CurrencyEnum) mCurrencySpinner.getSelectedItem()).name();
        mTransaction.setOriginalAmount(new Money(Utils.getSaveInstance(currency), originalAmount));
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
      String description = mTransaction.compileDescription(ExpenseEdit.this, currencyFormatter);
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
      showSnackbar(errorMsg, Snackbar.LENGTH_LONG);
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
        a == null || a.getType().equals(AccountType.CASH)) ? View.GONE : View.VISIBLE);
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
    long methodId = mMethodSpinner.getSelectedItemId();
    if (methodId != android.widget.AdapterView.INVALID_ROW_ID) {
      mMethodId = methodId;
    }
    if (didUserSetAccount) {
      long accountId = mAccountSpinner.getSelectedItemId();
      if (accountId != android.widget.AdapterView.INVALID_ROW_ID) {
         mAccountId = accountId;
      }
    }
    if (mOperationType == TYPE_TRANSFER) {
      long transferAccountId = mTransferAccountSpinner.getSelectedItemId();
      if (transferAccountId != android.widget.AdapterView.INVALID_ROW_ID) {
        mTransferAccountId = transferAccountId;
      }
    }
    Icepick.saveInstanceState(this, outState);
  }

  private void switchAccountViews() {
    Spinner accountSpinner = mAccountSpinner.getSpinner();
    Spinner transferAccountSpinner = mTransferAccountSpinner.getSpinner();
    TableLayout table = findViewById(R.id.Table);
    table.removeView(amountRow);
    table.removeView(transferAmountRow);
    if (mType == INCOME) {
      accountRow.removeView(accountSpinner);
      transferAccountRow.removeView(transferAccountSpinner);
      accountRow.addView(transferAccountSpinner);
      transferAccountRow.addView(accountSpinner);
      table.addView(transferAmountRow, 2);
      table.addView(amountRow, 4);
    } else {
      accountRow.removeView(transferAccountSpinner);
      transferAccountRow.removeView(accountSpinner);
      accountRow.addView(accountSpinner);
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
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
        if (o == null) {
          abortWithMessage(getString(R.string.save_transaction_template_deleted));
          return;
        }
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
      case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
      case TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS:
        if (o == null) {
          abortWithMessage(taskId == TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS ?
              "Unable to build transaction from extras" : "Object has been deleted from db");
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
              showSnackbar(R.string.image_deleted, Snackbar.LENGTH_SHORT);
            }
          }
        }
        //if catId has already been set by onRestoreInstanceState, the value might have been edited by the user and has precedence
        if (mCatId == null) {
          mCatId = mTransaction.getCatId();
          mLabel = mTransaction.getLabel();
        }
        if (mMethodId == null) {
          mMethodId = mTransaction.getMethodId();
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
          showSnackbar(getString(R.string.warning_cannot_move_split_transaction, account.getLabel()),
              Snackbar.LENGTH_LONG);
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
          if (CALENDAR.hasPermission(this)) {
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
            requestPermission(CALENDAR);
          }
        }
        if (mTransaction instanceof Template) {
          mPlanButton.setVisibility(visibility);
          mPlanToggleButton.setVisibility(visibility);
        }
        break;
      case R.id.Method:
        if (position > 0) {
          mMethodId = parent.getSelectedItemId();
        } else {
          mMethodId = null;
        }
        setReferenceNumberVisibility();
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
      showSnackbar(R.string.plan_custom_recurrence_info, Snackbar.LENGTH_LONG);
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
    transferAmountRow.setVisibility(isSame ? View.GONE : View.VISIBLE);
    exchangeRateRow.setVisibility(
        isSame || (mTransaction instanceof Template) ? View.GONE : View.VISIBLE);
    final String symbol2 = Money.getSymbol(transferAccount.currency);
    //noinspection SetTextI18n
    addCurrencyToLabel(findViewById(R.id.TransferAmountLabel), symbol2);
    mTransferAmountText.setFractionDigits(Money.getFractionDigits(transferAccount.currency));
    final String symbol1 = Money.getSymbol(currency);
    mExchangeRateEdit.setSymbols(symbol1, symbol2);

    Bundle bundle = new Bundle(2);
    bundle.putStringArray(KEY_CURRENCY, new String[]{currency.getCurrencyCode(), transferAccount
        .currency.getCurrencyCode()});
    if (!isSame && !mSavedInstance && (mNewInstance || mPlanInstanceId == -1) && !(mTransaction instanceof Template)) {
      mManager.restartLoader(LAST_EXCHANGE_CURSOR, bundle, this);
    }
  }

  private void setAccountLabel(Account account) {
    addCurrencyToLabel(mAmountLabel, Money.getSymbol(account.currency));
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
    restartIntent.putExtra(OPERATION_TYPE, newType);
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
      showSnackbar("Unknown error while saving transaction", Snackbar.LENGTH_SHORT);
    } else {
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
        showSnackbar(errorMsg, Snackbar.LENGTH_LONG);
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
          showSnackbar(getString(R.string.save_transaction_and_new_success), Snackbar.LENGTH_SHORT);
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
                .appendPath(a.getType().name()).build(),
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
      case AUTOFILL_CURSOR:
        List<String> dataToLoad = new ArrayList<>();
        String autoFillAccountFromPreference = PrefKey.AUTO_FILL_ACCOUNT.getString("never");
        boolean autoFillAccountFromExtra = getIntent().getBooleanExtra(KEY_AUTOFILL_MAY_SET_ACCOUNT, false);
        boolean overridePreferences = args.getBoolean(KEY_AUTOFILL_OVERRIDE_PREFERENCES);
        boolean mayLoadAccount = overridePreferences && autoFillAccountFromExtra ||
            autoFillAccountFromPreference.equals("always") ||
            (autoFillAccountFromPreference.equals("aggregate") && autoFillAccountFromExtra);
        if (overridePreferences || PrefKey.AUTO_FILL_AMOUNT.getBoolean(false)) {
          dataToLoad.add(KEY_CURRENCY);
          dataToLoad.add(KEY_AMOUNT);
        }
        if (overridePreferences || PrefKey.AUTO_FILL_CATEGORY.getBoolean(false)) {
          dataToLoad.add(KEY_CATID);
          dataToLoad.add(CAT_AS_LABEL);
        }
        if (overridePreferences || PrefKey.AUTO_FILL_COMMENT.getBoolean(false)) {
          dataToLoad.add(KEY_COMMENT);
        }
        if (overridePreferences || PrefKey.AUTO_FILL_METHOD.getBoolean(false)) {
          dataToLoad.add(KEY_METHODID);
        }
        if (mayLoadAccount) {
          dataToLoad.add(KEY_ACCOUNTID);
        }
        return new CursorLoader(this,
            ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, args.getLong(KEY_ROWID)),
            dataToLoad.toArray(new String[dataToLoad.size()]), null, null, null);
    }
    return null;
  }

  private void setReferenceNumberVisibility() {
    if (mTransaction instanceof Template) {
      return;
    }
    //ignore first row "no method" merged in
    int position = mMethodSpinner.getSelectedItemPosition();
    if (position > 0) {
      mMethodsCursor.moveToPosition(position - 1);
      mReferenceNumberText.setVisibility(mMethodsCursor.getInt(mMethodsCursor.getColumnIndexOrThrow(KEY_IS_NUMBERED)) > 0 ?
          View.VISIBLE : View.INVISIBLE);
    } else {
      mReferenceNumberText.setVisibility(View.GONE);
    }
  }

  private void setMethodSelection() {
    mMethodsCursor.moveToFirst();
    if (mMethodId != null) {
      boolean found = false;
      while (!mMethodsCursor.isAfterLast()) {
        if (mMethodsCursor.getLong(mMethodsCursor.getColumnIndex(KEY_ROWID)) == mMethodId) {
          mMethodSpinner.setSelection(mMethodsCursor.getPosition() + 1); //first row is ---
          found = true;
          break;
        }
        mMethodsCursor.moveToNext();
      }
      if (!found) {
        mMethodId = null;
      }
    }
    if (mMethodId == null) {
      mMethodSpinner.setSelection(0);
    }
    setReferenceNumberVisibility();
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
        if (mMethodsAdapter == null || !data.moveToFirst()) {
          methodRow.setVisibility(View.GONE);
        } else {
          methodRow.setVisibility(View.VISIBLE);
          MatrixCursor extras = new MatrixCursor(new String[]{KEY_ROWID, KEY_LABEL, KEY_IS_NUMBERED});
          extras.addRow(new String[]{"0", "- - - -", "0"});
          mMethodsAdapter.swapCursor(new MergeCursor(new Cursor[]{extras, data}));
          setMethodSelection();
        }
        break;
      case ACCOUNTS_CURSOR:
        if (data.getCount() == 0) {
          abortWithMessage("No accounts found");
          return;
        }
        if (data.getCount() == 1 && mOperationType == TYPE_TRANSFER) {
          abortWithMessage(getString(R.string.dialog_command_disabled_insert_transfer));
          return;
        }
        mAccountsAdapter.swapCursor(data);
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
            mExchangeRateEdit.calculateAndSetRate(amount, transferAmount);
          }
        }
        break;
      case AUTOFILL_CURSOR:
        if (data.moveToFirst()) {
          int columnIndexCatId = data.getColumnIndex(KEY_CATID);
          int columnIndexLabel = data.getColumnIndex(KEY_LABEL);
          if (mCatId == null && columnIndexCatId != -1 && columnIndexLabel != -1) {
            mCatId = DbUtils.getLongOrNull(data, columnIndexCatId);
            mLabel = data.getString(columnIndexLabel);
            setCategoryButton();
          }
          int columnIndexComment = data.getColumnIndex(KEY_COMMENT);
          if (TextUtils.isEmpty(mCommentText.getText().toString()) && columnIndexComment != -1) {
            mCommentText.setText(data.getString(columnIndexComment));
          }
          int columnIndexAmount = data.getColumnIndex(KEY_AMOUNT);
          int columnIndexCurrency = data.getColumnIndex(KEY_CURRENCY);
          if (TextUtils.isEmpty(mAmountText.getText().toString()) && columnIndexAmount != -1 && columnIndexCurrency != -1) {
            fillAmount(new Money(Currency.getInstance(data.getString(columnIndexCurrency)), data.getLong(columnIndexAmount)).getAmountMajor());
            configureType();
          }
          int columnIndexMethodId = data.getColumnIndex(KEY_METHODID);
          if (mMethodId == null && mMethodsCursor != null && columnIndexMethodId != -1) {
            mMethodId = DbUtils.getLongOrNull(data, columnIndexMethodId);
            setMethodSelection();
          }
          int columnIndexAccountId = data.getColumnIndex(KEY_ACCOUNTID);
          if (!didUserSetAccount && mAccounts != null && columnIndexAccountId != -1) {
            long accountId = data.getLong(columnIndexAccountId);
            for (int i = 0; i < mAccounts.length; i++) {
              if (mAccounts[i].getId().equals(accountId)) {
                mAccountSpinner.setSelection(i);
                break;
              }
            }
          }
        }
        break;
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
      showSnackbar(R.string.no_calendar_app_installed, Snackbar.LENGTH_SHORT);
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
        startAutoFill(args.getLong(KEY_ROWID), true);
        PreferenceUtils.enableAutoFill();
        break;
      default:
        super.onPositive(args);
    }
  }

  /**
   * @param id                  id of Payee/Payer for whom data should be loaded
   * @param overridePreferences if true data is loaded irrespective of what is set in preferences
   */
  private void startAutoFill(long id, boolean overridePreferences) {
    Bundle extras = new Bundle(2);
    extras.putLong(KEY_ROWID, id);
    extras.putBoolean(KEY_AUTOFILL_OVERRIDE_PREFERENCES, overridePreferences);
    mManager.restartLoader(AUTOFILL_CURSOR, extras, this);
  }

  @Override
  public void onNegative(Bundle args) {
    if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.AUTO_FILL_COMMAND) {
      PreferenceUtils.disableAutoFill();
    } else {
      super.onNegative(args);
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
    popup.setOnMenuItemClickListener(item -> {
      handlePicturePopupMenuClick(item.getItemId());
      return true;
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
    boolean granted = PermissionHelper.allGranted(grantResults);
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
          if (!CALENDAR.shouldShowRequestPermissionRationale(this)) {
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
    mExchangeRateEdit.calculateAndSetRate(amount, transferAmount);
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
          BigDecimal exchangeRate = mExchangeRateEdit.getRate(!isMain);
          BigDecimal result = exchangeRate != null && input != null ? input.multiply(exchangeRate) : new BigDecimal(0);
          (isMain ? mTransferAmountText : mAmountText).setAmount(result);
        } else {
          updateExchangeRates();
        }
      }
      isProcessingLinkedAmountInputs = false;
    }
  }

  private class LinkedExchangeRateTextWatchter implements ExchangeRateEdit.ExchangeRateWatcher {

    @Override
    public void afterExchangeRateChanged(BigDecimal rate, BigDecimal inverse) {
      if (isProcessingLinkedAmountInputs) return;
      isProcessingLinkedAmountInputs = true;
      if (lastExchangeRateRelevantInputs[0] != INPUT_EXCHANGE_RATE) {
        lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0];
        lastExchangeRateRelevantInputs[0] = INPUT_EXCHANGE_RATE;
      }


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
        variable.setAmount(input.multiply(mainProvided ? rate : inverse));
      }
      isProcessingLinkedAmountInputs = false;
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    isProcessingLinkedAmountInputs = true;
    super.onRestoreInstanceState(savedInstanceState);
    isProcessingLinkedAmountInputs = false;
  }

  @Override
  @IdRes
  protected int getSnackbarContainerId() {
    return R.id.OneExpense;
  }
}
