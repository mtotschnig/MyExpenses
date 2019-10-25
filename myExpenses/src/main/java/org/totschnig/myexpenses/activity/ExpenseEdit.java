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
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CrStatusAdapter;
import org.totschnig.myexpenses.adapter.NothingSelectedSpinnerAdapter;
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
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PreferenceUtils;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ButtonWithDialog;
import org.totschnig.myexpenses.ui.DateButton;
import org.totschnig.myexpenses.ui.DiscoveryHelper;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.ui.TimeButton;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.FilterCursorWrapper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.UiUtils.DateMode;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.ExpenseEditViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
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
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_ACCOUNT;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_AMOUNT;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_CATEGORY;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_COMMENT;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_HINT_SHOWN;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_METHOD;
import static org.totschnig.myexpenses.preference.PrefKey.LAST_ORIGINAL_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_PLAN_ENABLED;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_SPLIT_TEMPLATE_ENABLED;
import static org.totschnig.myexpenses.preference.PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.task.BuildTransactionTask.KEY_EXTRAS;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol;

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
public class ExpenseEdit extends AmountActivity implements
    OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>,
    ContribIFace, ConfirmationDialogListener, ButtonWithDialog.Host, ExchangeRateEdit.Host {

  private static final String SPLIT_PART_LIST = "SPLIT_PART_LIST";
  public static final String KEY_NEW_TEMPLATE = "newTemplate";
  public static final String KEY_CLONE = "clone";
  private static final String KEY_CACHED_DATA = "cachedData";
  private static final String KEY_CACHED_RECURRENCE = "cachedRecurrence";
  private static final String KEY_CACHED_PICTURE_URI = "cachedPictureUri";
  public static final String KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount";
  private static final String KEY_AUTOFILL_OVERRIDE_PREFERENCES = "autoFillOverridePreferences";
  private static int INPUT_EXCHANGE_RATE = 1;
  private static int INPUT_AMOUNT = 2;
  private static int INPUT_TRANSFER_AMOUNT = 3;
  private int[] lastExchangeRateRelevantInputs = {INPUT_EXCHANGE_RATE, INPUT_AMOUNT};
  @BindView(R.id.DateButton)
  DateButton dateEdit;
  @BindView(R.id.Date2Button)
  DateButton date2Edit;
  @BindView(R.id.TimeButton)
  TimeButton timeEdit;
  @BindView(R.id.Comment)
  EditText mCommentText;
  @BindView(R.id.Title)
  EditText mTitleText;
  @BindView(R.id.Number)
  EditText mReferenceNumberText;
  @BindView(R.id.TransferAmount)
  AmountInput transferInput;
  @BindView(R.id.OriginalAmount)
  AmountInput originalInput;
  @BindView(R.id.EquivalentAmount)
  AmountInput equivalentInput;
  @BindView(R.id.Category)
  Button mCategoryButton;
  @BindView(R.id.Plan)
  DateButton mPlanButton;
  @BindView(R.id.Payee)
  AutoCompleteTextView mPayeeText;
  @BindView(R.id.DateTimeLabel)
  TextView dateTimeLabel;
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
  @BindView(R.id.EquivalentAmountRow)
  ViewGroup equivalentAmountRow;
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
  @BindView(R.id.PlanRow)
  ViewGroup plannerRow;
  @BindView(R.id.AccountLabel)
  TextView accountLabel;
  @BindView(R.id.TransferAccountLabel)
  TextView transferAccountLabel;
  @BindView(R.id.TransferAmountLabel)
  TextView transferAmountLabel;
  @BindView(R.id.EquivalentAmountLabel)
  TextView equivalentAmountLabel;
  @BindView(R.id.ClearMethod)
  ImageView clearMethodButton;
  @BindView(R.id.ClearCategory)
  ImageView clearCategoryButton;
  @BindView(R.id.DateLink)
  ImageView datelink;

  private SpinnerHelper mMethodSpinner, mAccountSpinner, mTransferAccountSpinner, mStatusSpinner,
      mOperationTypeSpinner, mRecurrenceSpinner;
  private SimpleCursorAdapter mAccountsAdapter, mTransferAccountsAdapter, mPayeeAdapter;
  private ArrayAdapter<PaymentMethod> mMethodsAdapter;
  private OperationTypeAdapter mOperationTypeAdapter;
  private FilterCursorWrapper mTransferAccountCursor;

  @State
  Long mRowId = 0L;
  @State
  Long mTemplateId;
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
  String categoryIcon;
  @State
  Uri mPictureUri;
  @State
  Uri mPictureUriTemp;
  @State
  boolean originalAmountVisible;
  @State
  boolean equivalentAmountVisible;
  @State
  String originalCurrencyCode;

  private Account[] mAccounts;
  private Transaction mTransaction;
  private Plan mPlan;
  private long mPlanInstanceId, mPlanInstanceDate;
  /**
   * transaction, transfer or split
   */
  private int mOperationType;

  public static final int ACCOUNTS_CURSOR = 3;
  public static final int TRANSACTION_CURSOR = 5;
  public static final int SUM_CURSOR = 6;
  public static final int AUTOFILL_CURSOR = 8;

  private LoaderManager mManager;

  protected boolean mClone = false;
  protected boolean mCreateNew;
  protected boolean mIsMainTransactionOrTemplate, mIsMainTemplate, mIsMainTransaction;
  protected boolean mSavedInstance;
  protected boolean mRecordTemplateWidget;
  private boolean mIsResumed;
  boolean isProcessingLinkedAmountInputs = false;
  private ContentObserver pObserver;
  private boolean mPlanUpdateNeeded;
  private boolean didUserSetAccount;

  private ExpenseEditViewModel viewModel;
  private CurrencyViewModel currencyViewModel;

  @NonNull
  @Override
  public LocalDate getDate() {
    return date2Edit.date;
  }

  public enum HelpVariant {
    transaction, transfer, split, templateCategory, templateTransfer, templateSplit, splitPartCategory, splitPartTransfer
  }

  @Inject
  ImageViewIntentProvider imageViewIntentProvider;
  @Inject
  CurrencyFormatter currencyFormatter;
  @Inject
  DiscoveryHelper discoveryHelper;

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

    setupToolbar();
    mManager = LoaderManager.getInstance(this);
    viewModel = ViewModelProviders.of(this).get(ExpenseEditViewModel.class);
    viewModel.getMethods().observe(this, paymentMethods -> {
      if (mMethodsAdapter == null || paymentMethods == null || paymentMethods.isEmpty()) {
        methodRow.setVisibility(View.GONE);
        mMethodId = null;
      } else {
        methodRow.setVisibility(View.VISIBLE);
        mMethodsAdapter.clear();
        mMethodsAdapter.addAll(paymentMethods);
        setMethodSelection();
      }
    });
    ButterKnife.bind(this);
    currencyViewModel = ViewModelProviders.of(this).get(CurrencyViewModel.class);
    currencyViewModel.getCurrencies().observe(this, currencies -> {
      originalInput.setCurrencies(currencies, currencyContext);
      populateOriginalCurrency();
    });

    //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
    amountInput.setTypeEnabled(false);

    amountInput.addTextChangedListener(new MyTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        equivalentInput.setCompoundResultInput(amountInput.validate(false));
      }
    });

    originalInput.setCompoundResultOutListener(amount -> amountInput.setAmount(amount, false));

    mPayeeAdapter = new SimpleCursorAdapter(this, R.layout.support_simple_spinner_dropdown_item, null,
        new String[]{KEY_PAYEE_NAME},
        new int[]{android.R.id.text1},
        0);
    mPayeeText.setAdapter(mPayeeAdapter);
    mPayeeAdapter.setFilterQueryProvider(constraint -> {
      String selection = null;
      String[] selectArgs = new String[0];
      if (constraint != null) {
        String search = Utils.esacapeSqlLikeExpression(Utils.normalize(constraint.toString()));
        //we accept the string at the beginning of a word
        selection = KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
            KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
            KEY_PAYEE_NAME_NORMALIZED + " LIKE ?";
        selectArgs = new String[]{search + "%", "% " + search + "%", "%." + search + "%"};
      }
      return getContentResolver().query(
          TransactionProvider.PAYEES_URI,
          new String[]{KEY_ROWID, KEY_PAYEE_NAME},
          selection, selectArgs, null);
    });
    mPayeeAdapter.setStringConversionColumn(1);
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
          if (getPrefHandler().getBoolean(AUTO_FILL_HINT_SHOWN, false)) {
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
            b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
                getPrefHandler().getKey(AUTO_FILL_HINT_SHOWN));
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
    View operationTypeSpinner = findViewById(R.id.OperationType);
    currencyViewModel.loadCurrencies();

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
        return mTransaction != null && mTransaction.getCrStatus() != CrStatus.RECONCILED && position != CrStatus.RECONCILED.ordinal();
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
      if (!hasPendingTask(false)) {
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
          allowed = getPrefHandler().getBoolean(NEW_SPLIT_TEMPLATE_ENABLED, true);
        } else {
          contribFeature = ContribFeature.SPLIT_TRANSACTION;
          allowed = contribFeature.hasAccess() || contribFeature.usagesLeft(getPrefHandler()) > 0;
        }
        if (!allowed) {
          abortWithMessage(contribFeature.buildRequiresString(this));
          return;
        }
      }
      final Long parentId = getIntent().getLongExtra(KEY_PARENTID, 0);
      getSupportActionBar().setDisplayShowTitleEnabled(false);

      mOperationTypeSpinner = new SpinnerHelper(operationTypeSpinner);
      operationTypeSpinner.setVisibility(View.VISIBLE);
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
                accountId = getPrefHandler().getLong(TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, 0L);
              }
              mTransaction = Transaction.getNewInstance(accountId, parentId != 0 ? parentId : null);
              break;
            case TYPE_TRANSFER:
              Long transferAccountId = 0L;
              if (accountId == 0L) {
                accountId = getPrefHandler().getLong(TRANSFER_LAST_ACCOUNT_FROM_WIDGET, 0L);
                transferAccountId = getPrefHandler().getLong(TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, 0L);
              }
              mTransaction = Transfer.getNewInstance(accountId,
                  transferAccountId != 0 ? transferAccountId : null,
                  parentId != 0 ? parentId : null);
              break;
            case TYPE_SPLIT:
              if (accountId == 0L) {
                accountId = getPrefHandler().getLong(SPLIT_LAST_ACCOUNT_FROM_WIDGET, 0L);
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
          String errMsg = getString(R.string.warning_no_account);
          abortWithMessage(errMsg);
          return;
        }
        if (!mSavedInstance) {
          //processing data from user switching operation type
          Transaction cached = (Transaction) getIntent().getSerializableExtra(KEY_CACHED_DATA);
          if (cached != null) {
            mTransaction.setAccountId(cached.getAccountId());
            setLocalDateTime(cached);
            mPictureUri = getIntent().getParcelableExtra(KEY_CACHED_PICTURE_URI);
            setPicture();
            mMethodId = cached.getMethodId();
          }
        }
        setup();
      }
    }

    if (!discoveryHelper.discover(this, amountInput.findViewById(R.id.TaType),
        String.format("%s / %s", getString(R.string.expense), getString(R.string.income)),
        getString(R.string.discover_feature_expense_income_switch),
        1, DiscoveryHelper.Feature.EI_SWITCH, false)) {
      discoveryHelper.discover(this, operationTypeSpinner,
          String.format("%s / %s / %s", getString(R.string.transaction), getString(R.string.transfer), getString(R.string.split_transaction)),
          ExpenseEdit.this.getString(R.string.discover_feature_operation_type_select),
          2, DiscoveryHelper.Feature.OPERATION_TYPE_SELECT, true);
    }
  }

  private void abortWithMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
    Cursor oldCursor = mPayeeAdapter.getCursor();
    if (oldCursor != null && !oldCursor.isClosed()) {
      oldCursor.close();
    }
  }

  private void updateSplitBalance() {
    final SplitPartList splitPartList = findSplitPartList();
    if (splitPartList != null) {
      splitPartList.updateBalance();
    }
  }

  private void setup() {
    amountInput.setFractionDigits(mTransaction.getAmount().getCurrencyUnit().fractionDigits());
    linkInputsWithLabels();
    if (mOperationType == TYPE_SPLIT) {
      amountInput.addTextChangedListener(new MyTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
          updateSplitBalance();
        }
      });
    }

    if (mOperationType == TYPE_TRANSFER) {
      amountInput.addTextChangedListener(new LinkedTransferAmountTextWatcher(true));
      transferInput.addTextChangedListener(new LinkedTransferAmountTextWatcher(false));
      mExchangeRateEdit.setExchangeRateWatcher(new LinkedExchangeRateTextWatchter());
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
    mIsMainTransaction = mIsMainTransactionOrTemplate && !(mTransaction instanceof Template);
    mIsMainTemplate = mTransaction instanceof Template && !(mTransaction.isSplitpart());

    if (mIsMainTransactionOrTemplate) {

      // Spinner for methods
      mMethodsAdapter = new ArrayAdapter<PaymentMethod>(this, android.R.layout.simple_spinner_item) {
        @Override
        public long getItemId(int position) {
          return getItem(position).id();
        }
      };
      mMethodsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
      mMethodSpinner.setAdapter(new NothingSelectedSpinnerAdapter(
          mMethodsAdapter,
          android.R.layout.simple_spinner_item,
          // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
          this));
    } else {
      payeeRow.setVisibility(View.GONE);
      methodRow.setVisibility(View.GONE);
    }

    if (mOperationType == TYPE_TRANSFER) {
      amountInput.hideTypeButton();
      categoryRow.setVisibility(View.GONE);
      View accountContainer = findViewById(R.id.TransferAccountRow);
      if (accountContainer == null)
        accountContainer = findViewById(R.id.TransferAccount);
      accountContainer.setVisibility(View.VISIBLE);
      accountLabel.setText(R.string.transfer_from_account);

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
            mPlanButton.showDialog();
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
          setHelpVariant(HelpVariant.templateTransfer);
          break;
        case TYPE_SPLIT:
          setHelpVariant(HelpVariant.templateSplit);
          break;
        default:
          setHelpVariant(HelpVariant.templateCategory);
      }
    } else if (isSplitPart()) {
      if (mOperationType == TYPE_TRANSACTION) {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_split_part_category);
        }
        setHelpVariant(HelpVariant.splitPartCategory);
        mTransaction.status = STATUS_UNCOMMITTED;
      } else {
        //Transfer
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_split_part_transfer);
        }
        setHelpVariant(HelpVariant.splitPartTransfer);
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
          mPlanButton.setOnClickListener(view -> {
            final Account currentAccount = getCurrentAccount();
            if (currentAccount != null) {
              PlanMonthFragment.newInstance(
                  mTransaction.originTemplate.getTitle(),
                  mTransaction.originTemplate.getId(),
                  mTransaction.originTemplate.planId,
                  currentAccount.color, true, getThemeType()).show(getSupportFragmentManager(),
                  TemplatesList.CALDROID_DIALOG_FRAGMENT_TAG);
            }
          });
        }
      }
      if (mTransaction instanceof Transfer) {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_transfer);
        }
        setHelpVariant(HelpVariant.transfer);
      } else if (mTransaction instanceof SplitTransaction) {
        if (!mNewInstance) {
          setTitle(R.string.menu_edit_split);
        }
        setHelpVariant(HelpVariant.split);
      } else {
        if (mTransaction.getId() != 0) {
          setTitle(R.string.menu_edit_transaction);
        }
        setHelpVariant(HelpVariant.transaction);
      }
    }
    if (mOperationType == TYPE_SPLIT) {
      categoryRow.setVisibility(View.GONE);
      //add split list
      FragmentManager fm = getSupportFragmentManager();
      if (findSplitPartList() == null && !fm.isStateSaved()) {
        fm.beginTransaction()
            .add(R.id.edit_container, SplitPartList.newInstance(mTransaction), SPLIT_PART_LIST)
            .commit();
        fm.executePendingTransactions();
      }
    }
    if (mClone) {
      setTitle(R.string.menu_clone_transaction);
    }

    if (isNoMainTransaction()) {
      dateTimeRow.setVisibility(View.GONE);
    }

    //when we have a savedInstance, fields have already been populated
    if (!mSavedInstance) {
      populateFields();
      if (!(isSplitPart())) {
        setLocalDateTime(mTransaction);
      }
    }
    if (mTransaction.getId() != 0) {
      configureTransferDirection();
    }

    //after setLocalDateTime, so that the plan info can override the date
    configurePlan();

    setCategoryButton();
    if (mOperationType != TYPE_TRANSFER) {
      mCategoryButton.setOnClickListener(view -> startSelectCategory());
    }

    if (originalAmountVisible) {
      showOriginalAmount();
    }
    if (equivalentAmountVisible) {
      showEquivalentAmount();
    }
    if (mIsMainTransaction) {
      final CurrencyUnit homeCurrency = Utils.getHomeCurrency();
      addCurrencyToInput(equivalentAmountLabel, equivalentInput, homeCurrency.symbol(), R.string.menu_equivalent_amount);
      equivalentInput.setFractionDigits(homeCurrency.fractionDigits());
    }
  }

  public void hideKeyBoardAndShowDialog(int id) {
    hideKeyboard();
    showDialog(id);
  }

  @Override
  public void onValueSet(View view) {
    setDirty();
    if (view instanceof DateButton) {
      LocalDate date = ((DateButton) view).date;
      if (areDatesLinked()) {
        DateButton other = view.getId() == R.id.Date2Button ? dateEdit : date2Edit;
        other.setDate(date);
      }
    }
  }

  public void toggleDateLink(View view) {
    boolean isLinked = !areDatesLinked();
    ((ImageView) view).setImageResource(isLinked ? R.drawable.ic_hchain :
        R.drawable.ic_hchain_broken);
    view.setTag(String.valueOf(isLinked));
    view.setContentDescription(getString(isLinked ? R.string.content_description_dates_are_linked :
        R.string.content_description_dates_are_not_linked));
  }

  private boolean areDatesLinked() {
    return Boolean.parseBoolean((String) datelink.getTag());
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
    linkInputWithLabel(dateEdit, dateTimeLabel);
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
    linkInputWithLabel(transferInput, transferAmountLabel);
    final View originalAmountLabel = findViewById(R.id.OriginalAmountLabel);
    linkInputWithLabel(originalInput, originalAmountLabel);
    final View equivalentAmountLabel = findViewById(R.id.EquivalentAmountLabel);
    linkInputWithLabel(equivalentInput, equivalentAmountLabel);
  }

  private void linkAccountLabels() {
    linkInputWithLabel(mAccountSpinner.getSpinner(),
        isIncome() ? transferAccountLabel : accountLabel);
    linkInputWithLabel(mTransferAccountSpinner.getSpinner(),
        isIncome() ? accountLabel : transferAccountLabel);
  }

  @Override
  protected void onTypeChanged(boolean isClicked) {
    super.onTypeChanged(isClicked);
    if (mTransaction != null && mIsMainTransactionOrTemplate) {
      mMethodId = null;
      loadMethods(getCurrentAccount());
    }
    discoveryHelper.markDiscovered(DiscoveryHelper.Feature.EI_SWITCH);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem oaMenuItem = menu.findItem(R.id.ORIGINAL_AMOUNT_COMMAND);
    if (oaMenuItem != null) {
      oaMenuItem.setChecked(originalAmountVisible);
    }
    final Account currentAccount = getCurrentAccount();
    final MenuItem eaMenuItem = menu.findItem(R.id.EQUIVALENT_AMOUNT_COMMAND);
    if (eaMenuItem != null) {
      Utils.menuItemSetEnabledAndVisible(eaMenuItem, !(currentAccount == null || hasHomeCurrency(currentAccount)));
      eaMenuItem.setChecked(equivalentAmountVisible);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  protected boolean hasHomeCurrency(@NonNull Account account) {
    return account.getCurrencyUnit().equals(Utils.getHomeCurrency());
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
    } else if (mIsMainTransaction) {
      menu.add(Menu.NONE, R.id.ORIGINAL_AMOUNT_COMMAND, 0, R.string.menu_original_amount)
          .setCheckable(true)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
      menu.add(Menu.NONE, R.id.EQUIVALENT_AMOUNT_COMMAND, 0, R.string.menu_equivalent_amount)
          .setCheckable(true)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }
    return true;
  }

  @Override
  protected void doSave(boolean andNew) {
    if (mOperationType == TYPE_SPLIT &&
        !requireSplitPartList().splitComplete()) {
      showSnackbar(getString(R.string.unsplit_amount_greater_than_zero), Snackbar.LENGTH_SHORT);
    } else {
      if (andNew) {
        mCreateNew = true;
      }
      super.doSave(andNew);
    }
  }

  @Override
  protected void doHome() {
    cleanup();
    finish();
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    switch (command) {
      case R.id.CREATE_COMMAND: {
        createRow();
        return true;
      }
      case R.id.INVERT_TRANSFER_COMMAND: {
        amountInput.toggle();
        switchAccountViews();
        return true;
      }
      case R.id.ORIGINAL_AMOUNT_COMMAND: {
        originalAmountVisible = !originalAmountVisible;
        supportInvalidateOptionsMenu();
        showOriginalAmount();
        if (originalAmountVisible) {
          originalInput.requestFocus();
        } else {
          originalInput.clear();
        }
        return true;
      }
      case R.id.EQUIVALENT_AMOUNT_COMMAND: {
        equivalentAmountVisible = !equivalentAmountVisible;
        supportInvalidateOptionsMenu();
        showEquivalentAmount();
        if (equivalentAmountVisible) {
          final Account currentAccount = getCurrentAccount();
          if (validateAmountInput(equivalentInput, false) == null && currentAccount != null) {
            final BigDecimal rate = new BigDecimal(currentAccount.getExchangeRate());
            equivalentInput.setExchangeRate(rate);
          }
          equivalentInput.requestFocus();
        } else {
          equivalentInput.clear();
        }
        return true;
      }
    }
    return false;
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
    i.setAction(ManageCategories.ACTION_SELECT_MAPPING);
    forwardDataEntryFromWidget(i);
    //we pass the currently selected category in to prevent
    //it from being deleted, which can theoretically lead
    //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
    i.putExtra(KEY_ROWID, mCatId);
    startActivityForResult(i, SELECT_CATEGORY_REQUEST);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    hideKeyboard();
    try {
      return ((ButtonWithDialog) findViewById(id)).onCreateDialog();
    } catch (ClassCastException e) {
      Timber.e(e);
      return null;
    }
  }

  /**
   * populates the input fields with a transaction from the database or a new one
   */
  private void populateFields() {
    //processing data from user switching operation type
    Transaction cached = (Transaction) getIntent().getSerializableExtra(KEY_CACHED_DATA);
    Transaction cachedOrSelf = cached != null ? cached : mTransaction;

    isProcessingLinkedAmountInputs = true;
    mStatusSpinner.setSelection(cachedOrSelf.getCrStatus().ordinal(), false);
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
      originalAmountVisible = true;
      showOriginalAmount();
      originalInput.setAmount(cachedOrSelf.getOriginalAmount().getAmountMajor());
      originalCurrencyCode = cachedOrSelf.getOriginalAmount().getCurrencyUnit().code();
    } else {
      originalCurrencyCode = getPrefHandler().getString(LAST_ORIGINAL_CURRENCY, null);
    }
    populateOriginalCurrency();
    if (cachedOrSelf.getEquivalentAmount() != null) {
      equivalentAmountVisible = true;
      equivalentInput.setAmount(cachedOrSelf.getEquivalentAmount().getAmountMajor().abs());
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

  private void showEquivalentAmount() {
    setVisibility(equivalentAmountRow, equivalentAmountVisible);
    equivalentInput.setCompoundResultInput(equivalentAmountVisible ? amountInput.validate(false) : null);
  }

  private void showOriginalAmount() {
    setVisibility(originalAmountRow, originalAmountVisible);
  }

  private void populateOriginalCurrency() {
    if (originalCurrencyCode != null) {
      originalInput.setSelectedCurrency(originalCurrencyCode);
    }
  }

  protected void fillAmount(BigDecimal amount) {
    if (amount.signum() != 0) {
      amountInput.setAmount(amount);
    }
    amountInput.requestFocus();
    amountInput.selectAll();
  }

  protected void saveState() {
    if (syncStateAndValidate(true)) {
      mIsSaving = true;
      startDbWriteTask(true);
      if (getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
        switch (mOperationType) {
          case TYPE_TRANSACTION:
            getPrefHandler().putLong(TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
            break;
          case TYPE_TRANSFER:
            getPrefHandler().putLong(TRANSFER_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
            getPrefHandler().putLong(TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, mTransaction.getTransferAccountId());
            break;
          case TYPE_SPLIT:
            getPrefHandler().putLong(SPLIT_LAST_ACCOUNT_FROM_WIDGET, mTransaction.getAccountId());
            break;
        }
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
    }
    mTransaction.setAccountId(account.getId());

    mTransaction.setComment(mCommentText.getText().toString());

    if (!isNoMainTransaction()) {
      final ZonedDateTime transactionDate = readZonedDateTime(dateEdit);
      mTransaction.setDate(transactionDate);
      if (date2Edit.getVisibility() == View.VISIBLE) {
        mTransaction.setValueDate(date2Edit.getVisibility() == View.VISIBLE ?
            readZonedDateTime(date2Edit) : transactionDate);
      }
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
      final Account transferAccount = getTransferAccount();
      if (transferAccount == null) {
        return false;
      }
      boolean isSame = account.getCurrencyUnit().equals(transferAccount.getCurrencyUnit());
      if (mTransaction instanceof Template) {
        if (amount != null) {
          mTransaction.setAmount(new Money(account.getCurrencyUnit(), amount));
        } else if (!isSame) {
          BigDecimal transferAmount = validateAmountInput(transferInput, forSave);
          if (transferAmount != null) {
            mTransaction.setAccountId(transferAccount.getId());
            mTransaction.setTransferAccountId(account.getId());
            if (isIncome()) {
              transferAmount = transferAmount.negate();
            }
            mTransaction.setAmount(new Money(transferAccount.getCurrencyUnit(), transferAmount));
            amountInput.setError(null);
            validP = true; //we only need either amount or transfer amount
          }
        }
      } else {
        BigDecimal transferAmount = null;
        if (isSame) {
          if (amount != null) transferAmount = amount.negate();
        } else {
          transferAmount = validateAmountInput(transferInput, forSave);

          if (transferAmount == null) {
            //Snackbar is shown in validateAmountInput
            validP = false;
          } else {
            if (isIncome()) {
              transferAmount = transferAmount.negate();
            }
          }
        }
        if (validP) {
          ((Transfer) mTransaction).setAmountAndTransferAmount(
              new Money(account.getCurrencyUnit(), amount),
              new Money(transferAccount.getCurrencyUnit(), transferAmount != null ?
                  transferAmount : mTransaction.getTransferAmount().getAmountMajor()));
        }
      }
    } else {
      if (validP) {
        mTransaction.setAmount(new Money(account.getCurrencyUnit(), amount));
      }
      if (mIsMainTransaction) {
        BigDecimal originalAmount = validateAmountInput(originalInput, false);
        final Currency selectedItem = originalInput.getSelectedCurrency();
        if (selectedItem != null && originalAmount != null) {
          final String currency = selectedItem.code();
          LAST_ORIGINAL_CURRENCY.putString(currency);
          mTransaction.setOriginalAmount(new Money(currencyContext.get(currency), originalAmount));
        } else {
          mTransaction.setOriginalAmount(null);
        }
        BigDecimal equivalentAmount = validateAmountInput(equivalentInput, false);
        mTransaction.setEquivalentAmount(equivalentAmount == null ? null :
            new Money(Utils.getHomeCurrency(), isIncome() ? equivalentAmount : equivalentAmount.negate()));
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
              mPlanButton.date,
              ((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem()),
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
      if (forSave && !isSplitPart()) {
        if (mRecurrenceSpinner.getSelectedItemPosition() > 0) {
          mTransaction.setInitialPlan(Pair.create((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem(), dateEdit.date));
        }
      }
    }

    mTransaction.setCrStatus((CrStatus) mStatusSpinner.getSelectedItem());

    mTransaction.setPictureUri(mPictureUri);
    return validP;
  }

  @NonNull
  private ZonedDateTime readZonedDateTime(DateButton dateEdit) {
    return ZonedDateTime.of(dateEdit.date,
        timeEdit.getVisibility() == View.VISIBLE ? timeEdit.getTime() : LocalTime.now(),
        ZoneId.systemDefault());
  }

  private void setLocalDateTime(Transaction transaction) {
    final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(transaction.getDate()), ZoneId.systemDefault());
    final LocalDate localDate = zonedDateTime.toLocalDate();
    if (mTransaction instanceof Template) {
      mPlanButton.setDate(localDate);
    } else {
      dateEdit.setDate(localDate);
      date2Edit.setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.getValueDate()),
          ZoneId.systemDefault()).toLocalDate());
      timeEdit.setTime(zonedDateTime.toLocalTime());
    }
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
      categoryIcon = intent.getStringExtra(KEY_ICON);
      setCategoryButton();
      setDirty();
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
          setDirty();
        } else {
          requestStoragePermission();
        }
        return;
      } else {
        errorMsg = "Error while retrieving image: No data found.";
      }
      CrashHandler.report(errorMsg);
      showSnackbar(errorMsg, Snackbar.LENGTH_LONG);
    }
    if (requestCode == PLAN_REQUEST) {
      finish();
    }
  }

  protected void setPicture() {
    if (mPictureUri != null) {
      mPictureViewContainer.setVisibility(View.VISIBLE);
      Picasso.get().load(mPictureUri).fit().into((ImageView) mPictureViewContainer.findViewById(R.id.picture));
      mAttachPictureButton.setVisibility(View.GONE);
    }
  }

  @Override
  public void onBackPressed() {
    cleanup();
    super.onBackPressed();
  }

  protected void cleanup() {
    if (mTransaction != null) {
      mTransaction.cleanupCanceledEdit();
    }
  }

  /**
   * updates interface based on type (EXPENSE or INCOME)
   */
  protected void configureType() {
    if (mPayeeLabel != null) {
      mPayeeLabel.setText(amountInput.getType() ? R.string.payer : R.string.payee);
    }
    if (mOperationType == TYPE_SPLIT) {
      updateSplitBalance();
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
      CrashHandler.report("Received onChange on ContentOberver for plan, but mPlan is null");
    }
  }

  private void configureStatusSpinner() {
    Account a = getCurrentAccount();
    setVisibility(mStatusSpinner.getSpinner(),
        !isNoMainTransaction() && a != null && !a.getType().equals(AccountType.CASH));
  }

  private void setVisibility(View view, boolean visible) {
    view.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  /**
   * set label on category button
   */
  private void setCategoryButton() {
    if (mLabel != null && mLabel.length() != 0) {
      mCategoryButton.setText(mLabel);
      clearCategoryButton.setVisibility(View.VISIBLE);
      UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(mCategoryButton,
          categoryIcon != null ? UiUtils.resolveIcon(this, categoryIcon) : 0, 0, 0, 0);
    } else {
      mCategoryButton.setText(R.string.select);
      clearCategoryButton.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    long methodId = mMethodSpinner.getSelectedItemId();
    if (methodId > 0) {
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
    final Currency originalInputSelectedCurrency = originalInput.getSelectedCurrency();
    if (originalInputSelectedCurrency != null) {
      originalCurrencyCode = originalInputSelectedCurrency.code();
    }
    Icepick.saveInstanceState(this, outState);
  }

  private void switchAccountViews() {
    Spinner accountSpinner = mAccountSpinner.getSpinner();
    Spinner transferAccountSpinner = mTransferAccountSpinner.getSpinner();
    TableLayout table = findViewById(R.id.Table);
    table.removeView(amountRow);
    table.removeView(transferAmountRow);
    if (isIncome()) {
      if (accountSpinner.getParent() == accountRow && transferAccountSpinner.getParent() == transferAccountRow) {
        accountRow.removeView(accountSpinner);
        transferAccountRow.removeView(transferAccountSpinner);
        accountRow.addView(transferAccountSpinner);
        transferAccountRow.addView(accountSpinner);
      }
      table.addView(transferAmountRow, 2);
      table.addView(amountRow, 4);
    } else {
      if (accountSpinner.getParent() == transferAccountRow && transferAccountSpinner.getParent() == accountRow) {
        accountRow.removeView(transferAccountSpinner);
        transferAccountRow.removeView(accountSpinner);
        accountRow.addView(accountSpinner);
        transferAccountRow.addView(transferAccountSpinner);
      }
      table.addView(amountRow, 2);
      table.addView(transferAmountRow, 4);
    }
    linkAccountLabels();
  }

  public Money getAmount() {
    Account a = getCurrentAccount();
    if (a == null)
      return null;
    BigDecimal amount = validateAmountInput(false);
    return amount == null ? new Money(a.getCurrencyUnit(), 0L) :
        new Money(a.getCurrencyUnit(), amount);
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
        if (mTransaction.isSealed()) {
          abortWithMessage("This transaction refers to a closed account and can no longer be edited");
        }
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
              unsetPicture();
              showSnackbar(R.string.image_deleted, Snackbar.LENGTH_SHORT);
            }
          }
        }
        //if catId has already been set by onRestoreInstanceState, the value might have been edited by the user and has precedence
        if (mCatId == null) {
          mCatId = mTransaction.getCatId();
          mLabel = mTransaction.getLabel();
          categoryIcon = mTransaction.getCategoryIcon();
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
          mTransaction.setCrStatus(CrStatus.UNRECONCILED);
          mTransaction.status = STATUS_NONE;
          mTransaction.setDate(ZonedDateTime.now());
          mTransaction.uuid = Model.generateUuid();
          mClone = true;
        }
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

  private void unsetPicture() {
    mPictureUri = null;
    mAttachPictureButton.setVisibility(View.VISIBLE);
    mPictureViewContainer.setVisibility(View.GONE);
  }

  private boolean isFileAndNotExists(Uri uri) {
    if (uri.getScheme().equals("file") && !new File(uri.getPath()).exists()) {
      return true;
    }
    return false;
  }

  @Nullable
  @VisibleForTesting
  public Account getCurrentAccount() {
    return getAccountFromSpinner(mAccountSpinner);
  }

  @Nullable
  private Account getTransferAccount() {
    return getAccountFromSpinner(mTransferAccountSpinner);
  }

  @Nullable
  private Account getAccountFromSpinner(SpinnerHelper spinner) {
    if (mAccounts == null) {
      return null;
    }
    int selected = spinner.getSelectedItemPosition();
    if (selected == android.widget.AdapterView.INVALID_POSITION) {
      return null;
    }
    long selectedID = spinner.getSelectedItemId();
    for (Account account : mAccounts) {
      if (account.getId() == selectedID) {
        return account;
      }
    }
    return null;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
                             long id) {
    if (parent.getId() != R.id.OperationType) {
      setDirty();
    }
    switch (parent.getId()) {
      case R.id.Recurrence:
        int visibility = View.GONE;
        if (id > 0) {
          if (CALENDAR.hasPermission(this)) {
            boolean newSplitTemplateEnabled = getPrefHandler().getBoolean(NEW_SPLIT_TEMPLATE_ENABLED, true);
            boolean newPlanEnabled = getPrefHandler().getBoolean(NEW_PLAN_ENABLED, true);
            if (newPlanEnabled && (newSplitTemplateEnabled || mOperationType != TYPE_SPLIT || mTransaction instanceof Template)) {
              visibility = View.VISIBLE;
              showCustomRecurrenceInfo();
            } else {
              mRecurrenceSpinner.setSelection(0);
              ContribFeature contribFeature = mOperationType != TYPE_SPLIT || newSplitTemplateEnabled ?
                  ContribFeature.PLANS_UNLIMITED : ContribFeature.SPLIT_TEMPLATE;
              showContribDialog(contribFeature, null);
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
        final boolean hasSelection = position > 0;
        if (hasSelection) {
          mMethodId = parent.getSelectedItemId();
          if (mMethodId <= 0) {
            mMethodId = null;
          }
        } else {
          mMethodId = null;
        }
        setVisibility(clearMethodButton, hasSelection);
        setReferenceNumberVisibility();
        break;
      case R.id.Account:
        final Account account = mAccounts[position];
        if (mOperationType == TYPE_SPLIT) {
          final SplitPartList splitPartList = findSplitPartList();
          if (splitPartList != null && splitPartList.getSplitCount() > 0) {
            //call background task for moving parts to new account
            startTaskExecution(
                TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS,
                new Long[]{mTransaction.getId()},
                account.getId(),
                R.string.progress_dialog_updating_split_parts);
            break;
          }
        }
        updateAccount(account);
        break;
      case R.id.OperationType:
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.OPERATION_TYPE_SELECT);
        int newType = ((Integer) mOperationTypeSpinner.getItemAtPosition(position));
        if (newType != mOperationType && isValidType(newType)) {
          if (newType == TYPE_TRANSFER && !checkTransferEnabled(getCurrentAccount())) {
            //reset to previous
            resetOperationType();
          } else if (newType == TYPE_SPLIT) {
            resetOperationType();
            if (mTransaction instanceof Template) {
              if (NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true)) {
                restartWithType(TYPE_SPLIT);
              } else {
                showContribDialog(ContribFeature.SPLIT_TEMPLATE, null);
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

  private void configureAccountDependent(Account account) {
    final CurrencyUnit currencyUnit = account.getCurrencyUnit();
    addCurrencyToInput(mAmountLabel, amountInput, currencyUnit.symbol(), R.string.amount);
    originalInput.configureExchange(currencyUnit);
    if (hasHomeCurrency(account)) {
      equivalentAmountRow.setVisibility(View.GONE);
      equivalentAmountVisible = false;
    } else {
      equivalentInput.configureExchange(currencyUnit, Utils.getHomeCurrency());
    }
    configureDateInput(account);
  }

  private void loadMethods(@Nullable Account account) {
    if (account != null) {
      viewModel.loadMethods(isIncome(), account.getType());
    }
  }

  private void updateAccount(Account account) {
    mAccountId = account.getId();
    didUserSetAccount = true;
    mTransaction.setAccountId(account.getId());
    configureAccountDependent(account);
    if (mOperationType == TYPE_TRANSFER) {
      mTransferAccountSpinner.setSelection(setTransferAccountFilterMap());
      mTransaction.setTransferAccountId(mTransferAccountSpinner.getSelectedItemId());
      configureTransferInput();
    } else {
      if (!mTransaction.isSplitpart()) {
        loadMethods(account);
      }
      if (mOperationType == TYPE_SPLIT) {
        final SplitPartList splitPartList = findSplitPartList();
        if (splitPartList != null) {
          splitPartList.updateAccount(account);
        }
      }
    }
    configureStatusSpinner();
    amountInput.setFractionDigits(account.getCurrencyUnit().fractionDigits());
  }

  private void configureDateInput(Account account) {
    DateMode dateMode = UiUtils.getDateMode(account, getPrefHandler());
    setVisibility(timeEdit, dateMode == DateMode.DATE_TIME);
    setVisibility(date2Edit, dateMode == DateMode.BOOKING_VALUE);
    setVisibility(datelink, dateMode == DateMode.BOOKING_VALUE);
    String dateLabel;
    if (dateMode == DateMode.BOOKING_VALUE) {
      dateLabel = getString(R.string.booking_date) + "/" + getString(R.string.value_date);
    } else {
      dateLabel = getString(R.string.date);
      if (dateMode == DateMode.DATE_TIME) {
        dateLabel += " / " + getString(R.string.time);
      }
    }
    dateTimeLabel.setText(dateLabel);
  }

  private void configureTransferInput() {
    final Account transferAccount = getTransferAccount();
    final Account currentAccount = getCurrentAccount();
    if (transferAccount == null || currentAccount == null) {
      return;
    }
    final CurrencyUnit currency = currentAccount.getCurrencyUnit();
    final CurrencyUnit transferAccountCurrencyUnit = transferAccount.getCurrencyUnit();
    final boolean isSame = currency.equals(transferAccountCurrencyUnit);
    setVisibility(transferAmountRow, !isSame);
    setVisibility(exchangeRateRow, !isSame && !(mTransaction instanceof Template));
    //noinspection SetTextI18n
    addCurrencyToInput(transferAmountLabel, transferInput, transferAccountCurrencyUnit.symbol(), R.string.amount);
    transferInput.setFractionDigits(transferAccountCurrencyUnit.fractionDigits());
    mExchangeRateEdit.setCurrencies(currency, transferAccountCurrencyUnit);

    Bundle bundle = new Bundle(2);
    bundle.putStringArray(KEY_CURRENCY, new String[]{currency.code(), transferAccountCurrencyUnit.code()});
  }

  private void addCurrencyToInput(TextView label, AmountInput amountInput, String symbol, int textResId) {
    final String text = appendCurrencySymbol(this, textResId, symbol);
    label.setText(text);
    amountInput.setContentDescription(text);
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
    restartIntent.putExtra(KEY_CACHED_RECURRENCE, ((Plan.Recurrence) mRecurrenceSpinner.getSelectedItem()));
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
        if (mTransaction instanceof SplitTransaction) {
          recordUsage(ContribFeature.SPLIT_TRANSACTION);
        }
        if (mPictureUri != null) {
          recordUsage(ContribFeature.ATTACH_PICTURE);
        }
        if (mCreateNew) {
          mCreateNew = false;
          if (mOperationType == TYPE_SPLIT) {
            mTransaction = SplitTransaction.getNewInstance(mTransaction.getAccountId());
            mRowId = mTransaction.getId();
            final SplitPartList splitPartList = findSplitPartList();
            if (splitPartList != null) {
              splitPartList.updateParent(mRowId);
            }
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
          amountInput.clear();
          transferInput.clear();
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

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case ACCOUNTS_CURSOR:
        return new CursorLoader(this, TransactionProvider.ACCOUNTS_BASE_URI,
            null, KEY_SEALED + " = 0", null, null);
      case AUTOFILL_CURSOR:
        List<String> dataToLoad = new ArrayList<>();
        String autoFillAccountFromPreference = getPrefHandler().getString(AUTO_FILL_ACCOUNT, "never");
        boolean autoFillAccountFromExtra = getIntent().getBooleanExtra(KEY_AUTOFILL_MAY_SET_ACCOUNT, false);
        boolean overridePreferences = args.getBoolean(KEY_AUTOFILL_OVERRIDE_PREFERENCES);
        boolean mayLoadAccount = overridePreferences && autoFillAccountFromExtra ||
            autoFillAccountFromPreference.equals("always") ||
            (autoFillAccountFromPreference.equals("aggregate") && autoFillAccountFromExtra);
        if (overridePreferences || getPrefHandler().getBoolean(AUTO_FILL_AMOUNT, false)) {
          dataToLoad.add(KEY_CURRENCY);
          dataToLoad.add(KEY_AMOUNT);
        }
        if (overridePreferences || getPrefHandler().getBoolean(AUTO_FILL_CATEGORY, false)) {
          dataToLoad.add(KEY_CATID);
          dataToLoad.add(CAT_AS_LABEL);
        }
        if (overridePreferences || getPrefHandler().getBoolean(AUTO_FILL_COMMENT, false)) {
          dataToLoad.add(KEY_COMMENT);
        }
        if (overridePreferences || getPrefHandler().getBoolean(AUTO_FILL_METHOD, false)) {
          dataToLoad.add(KEY_METHODID);
        }
        if (mayLoadAccount) {
          dataToLoad.add(KEY_ACCOUNTID);
        }
        return new CursorLoader(this,
            ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, args.getLong(KEY_ROWID)),
            dataToLoad.toArray(new String[dataToLoad.size()]), null, null, null);
    }
    throw new IllegalStateException();
  }

  private void setReferenceNumberVisibility() {
    if (mTransaction instanceof Template) {
      return;
    }
    //ignore first row "select" merged in
    int position = mMethodSpinner.getSelectedItemPosition();
    if (position > 0) {
      PaymentMethod pm = mMethodsAdapter.getItem(position - 1);
      mReferenceNumberText.setVisibility(pm != null && pm.isNumbered() ? View.VISIBLE : View.INVISIBLE);
    } else {
      mReferenceNumberText.setVisibility(View.GONE);
    }
  }

  private void setMethodSelection() {
    if (mMethodId != null) {
      boolean found = false;
      for (int i = 0; i < mMethodsAdapter.getCount(); i++) {
        PaymentMethod pm = mMethodsAdapter.getItem(i);
        if (pm != null) {
          if (pm.id() == mMethodId) {
            mMethodSpinner.setSelection(i + 1);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        mMethodId = null;
        mMethodSpinner.setSelection(0);
      }
    } else {
      mMethodSpinner.setSelection(0);
    }
    setVisibility(clearMethodButton, mMethodId != null);
    setReferenceNumberVisibility();
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
    if (data == null || isFinishing()) {
      return;
    }
    int id = loader.getId();
    switch (id) {
      case ACCOUNTS_CURSOR:
        if (data.getCount() == 0) {
          abortWithMessage(getString(R.string.warning_no_account));
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
          if (mOperationType == TYPE_TRANSFER) {
            mTransaction.setTransferAccountId(mTransferAccountId);
          }
        }
        data.moveToFirst();
        boolean selectionSet = false;
        String currencyExtra = didUserSetAccount ? null : getIntent().getStringExtra(KEY_CURRENCY);
        while (!data.isAfterLast()) {
          int position = data.getPosition();
          Account a = Account.fromCursor(data);
          mAccounts[position] = a;
          if (!selectionSet &&
              (a.getCurrencyUnit().code().equals(currencyExtra) ||
                  (currencyExtra == null && a.getId().equals(mTransaction.getAccountId())))) {
            mAccountSpinner.setSelection(position);
            configureAccountDependent(a);
            selectionSet = true;
          }
          data.moveToNext();
        }
        //if the accountId we have been passed does not exist, we select the first entry
        if (mAccountSpinner.getSelectedItemPosition() == android.widget.AdapterView.INVALID_POSITION) {
          mAccountSpinner.setSelection(0);
          mTransaction.setAccountId(mAccounts[0].getId());
          configureAccountDependent(mAccounts[0]);
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
            transferInput.setAmount(mTransaction.getTransferAmount().getAmountMajor().abs());
            updateExchangeRates(transferInput);
            isProcessingLinkedAmountInputs = false;
          }
        } else {
          //the methods cursor is based on the current account,
          //hence it is loaded only after the accounts cursor is loaded
          if (!mTransaction.isSplitpart()) {
            loadMethods(getCurrentAccount());
          }
        }
        amountInput.setTypeEnabled(true);
        configureType();
        configureStatusSpinner();
        if (mIsResumed) setupListeners();
        break;
      case AUTOFILL_CURSOR:
        if (data.moveToFirst()) {
          boolean typeHasChanged = false;
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
          if (validateAmountInput(amountInput, false) == null && columnIndexAmount != -1 && columnIndexCurrency != -1) {
            boolean beforeType = isIncome();
            fillAmount(new Money(currencyContext.get(data.getString(columnIndexCurrency)), data.getLong(columnIndexAmount)).getAmountMajor());
            configureType();
            typeHasChanged = beforeType != isIncome();
          }
          int columnIndexMethodId = data.getColumnIndex(KEY_METHODID);
          if (mMethodId == null && columnIndexMethodId != -1) {
            mMethodId = DbUtils.getLongOrNull(data, columnIndexMethodId);
            if (!typeHasChanged) {//if type has changed, we need to wait for methods to be reloaded, method is then selected in onLoadFinished
              setMethodSelection();
            }
          }
          int columnIndexAccountId = data.getColumnIndex(KEY_ACCOUNTID);
          if (!didUserSetAccount && mAccounts != null && columnIndexAccountId != -1) {
            long accountId = data.getLong(columnIndexAccountId);
            for (int i = 0; i < mAccounts.length; i++) {
              if (mAccounts[i].getId().equals(accountId)) {
                mAccountSpinner.setSelection(i);
                updateAccount(mAccounts[i]);
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
    Utils.requireLoader(mManager, AUTOFILL_CURSOR, extras, this);
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
    mIsResumed = false;
    super.onPause();
  }

  @Nullable
  private SplitPartList findSplitPartList() {
    return (SplitPartList) getSupportFragmentManager().findFragmentByTag(SPLIT_PART_LIST);
  }

  @NonNull
  private SplitPartList requireSplitPartList() {
    SplitPartList splitPartList = findSplitPartList();
    if (splitPartList == null) throw new IllegalStateException("Split part list not found");
    return splitPartList;
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
        unsetPicture();
        break;
      case R.id.VIEW_COMMAND:
        imageViewIntentProvider.startViewIntent(this, mPictureUri);
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
          unsetPicture();
        }
      }
    }
  }

  private void updateExchangeRates(AmountInput other) {
    BigDecimal amount = validateAmountInput(amountInput, false);
    BigDecimal transferAmount = validateAmountInput(other, false);
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
      isProcessingLinkedAmountInputs = true;
      if (mTransaction instanceof Template) {
        (isMain ? transferInput : amountInput).clear();
      } else if (exchangeRateRow.getVisibility() == View.VISIBLE) {
        int currentFocus = isMain ? INPUT_AMOUNT : INPUT_TRANSFER_AMOUNT;
        if (lastExchangeRateRelevantInputs[0] != currentFocus) {
          lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0];
          lastExchangeRateRelevantInputs[0] = currentFocus;
        }
        if (lastExchangeRateRelevantInputs[1] == INPUT_EXCHANGE_RATE) {
          applyExchangRate(isMain ? amountInput : transferInput,
              isMain ? transferInput : amountInput,
              mExchangeRateEdit.getRate(!isMain));
        } else {
          updateExchangeRates(transferInput);
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

      AmountInput constant, variable;
      BigDecimal exchangeFactor;

      if (lastExchangeRateRelevantInputs[0] != INPUT_EXCHANGE_RATE) {
        lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0];
        lastExchangeRateRelevantInputs[0] = INPUT_EXCHANGE_RATE;
      }

      if (lastExchangeRateRelevantInputs[1] == INPUT_AMOUNT) {
        constant = amountInput;
        variable = transferInput;
        exchangeFactor = rate;
      } else {
        constant = transferInput;
        variable = amountInput;
        exchangeFactor = inverse;
      }

      applyExchangRate(constant, variable, exchangeFactor);
      isProcessingLinkedAmountInputs = false;
    }
  }

  private void applyExchangRate(AmountInput from, AmountInput to, BigDecimal rate) {
    BigDecimal input = validateAmountInput(from, false);
    to.setAmount(rate != null && input != null ? input.multiply(rate) : new BigDecimal(0), false);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    isProcessingLinkedAmountInputs = true;
    mExchangeRateEdit.setBlockWatcher(true);
    super.onRestoreInstanceState(savedInstanceState);
    mExchangeRateEdit.setBlockWatcher(false);
    isProcessingLinkedAmountInputs = false;
    if (mRowId == 0L && mTemplateId == 0L) {
      configureTransferDirection();
    }
  }

  private void configureTransferDirection() {
    if (isIncome() && mOperationType == TYPE_TRANSFER) {
      switchAccountViews();
    }
  }

  public void clearMethodSelection(View view) {
    mMethodId = null;
    setMethodSelection();
  }


  public void clearCategorySelection(View view) {
    mCatId = null;
    mLabel = null;
    setCategoryButton();
  }
}
