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

package org.totschnig.myexpenses.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.IntStream;
import com.annimon.stream.Stream;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.TransactionAdapter;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DateFilterDialog;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectPayerDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectSingleAccountDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectSingleMethodDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectSinglePayeeDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.CheckSealedHandler;
import org.totschnig.myexpenses.provider.CheckTransferAccountOfSplitPartsHandler;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CategoryCriteria;
import org.totschnig.myexpenses.provider.filter.CommentCriteria;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.provider.filter.FilterPersistence;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel;
import org.totschnig.myexpenses.viewmodel.data.EventObserver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import kotlin.Unit;
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.SectionIndexingStickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView.OnHeaderClickListener;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.MAP_ACCOUNT_RQEUST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.MAP_CATEGORY_RQEUST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.MAP_METHOD_RQEUST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.MAP_PAYEE_RQEUST;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_SPLIT_TEMPLATE_ENABLED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.HAS_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_PARENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_MONTH_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.MAPPED_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.MAPPED_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.MAPPED_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.KEY_LONG_IDS;
import static org.totschnig.myexpenses.util.ColorUtils.getContrastColor;
import static org.totschnig.myexpenses.util.MoreUiUtilsKt.addChipsBulk;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

public class TransactionList extends ContextualActionBarFragment implements
    LoaderManager.LoaderCallbacks<Cursor>, OnHeaderClickListener, SimpleDialog.OnDialogResultListener {

  public static final String NEW_TEMPLATE_DIALOG = "dialogNewTempl";
  public static final String FILTER_COMMENT_DIALOG = "dialogFilterCom";
  public static final String REMAP_DIALOG = "dialogRemap";
  public static final String KEY_COLUMN = "column";

  protected int getMenuResource() {
    return R.menu.transactionlist_context;
  }

  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  private static final int GROUPING_CURSOR = 2;
  private static final int SECTION_CURSOR = 3;

  public static final String KEY_FILTER = "filter";
  public static final String CATEGORY_SEPARATOR = " : ",
      COMMENT_SEPARATOR = " / ";
  private MyGroupedAdapter mAdapter;
  private boolean hasItems;
  private boolean mappedCategories;
  private boolean mappedPayees;
  private boolean mappedMethods;
  private boolean hasTransfers;
  private boolean firstLoadCompleted;
  private Cursor mTransactionsCursor;
  private Parcelable listState;

  @BindView(R.id.list)
  ExpandableStickyListHeadersListView mListView;
  @BindView(R.id.empty)
  View emptyView;
  @BindView(R.id.filter)
  ChipGroup filterView;
  @BindView(R.id.filterCard)
  ViewGroup filterCard;
  private LoaderManager mManager;

  /**
   * maps header to an array that holds an array of following sums:
   * [0] incomeSum
   * [1] expenseSum
   * [2] transferSum
   * [3] previousBalance
   * [4] delta (incomSum - expenseSum + transferSum)
   * [5] interimBalance
   * [6] mappedCategories
   */
  private LongSparseArray<Long[]> headerData = new LongSparseArray<>();
  private String[] sections;
  private int[] sectionIds;
  /**
   * maps section to index to the position of first item in section
   */
  private SparseIntArray mSectionCache;

  /**
   * used to restore list selection when drawer is reopened
   */
  private SparseBooleanArray mCheckedListItems;

  private int columnIndexYear, columnIndexYearOfWeekStart, columnIndexMonth,
      columnIndexWeek, columnIndexDay, columnIndexLabelSub,
      columnIndexPayee, columnIndexCrStatus, columnIndexYearOfMonthStart,
      columnIndexLabelMain;
  private boolean indexesCalculated = false;
  private Account mAccount;
  private Money budget = null;
  private TransactionListViewModel viewModel;
  private ContentObserver budgetsObserver;

  @Inject
  CurrencyFormatter currencyFormatter;
  @Inject
  PrefHandler prefHandler;
  @Inject
  CurrencyContext currencyContext;
  FilterPersistence filterPersistence;

  public static Fragment newInstance(long accountId) {
    TransactionList pageFragment = new TransactionList();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ACCOUNTID, accountId);
    pageFragment.setArguments(bundle);
    return pageFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    viewModel = new ViewModelProvider(this).get(TransactionListViewModel.class);
    viewModel.account(getArguments().getLong(KEY_ACCOUNTID)).observe(this, account -> {
      mAccount = account;
      mAdapter.setAccount(mAccount);
      setGrouping();
      Utils.requireLoader(mManager, TRANSACTION_CURSOR, null, TransactionList.this);
      Utils.requireLoader(mManager, SUM_CURSOR, null, TransactionList.this);
      Utils.requireLoader(mManager, SECTION_CURSOR, null, TransactionList.this);
    });
    viewModel.getBudgetAmount().observe(this, budget -> {
      if (this.budget != budget) {
        this.budget = budget;
        refresh(false);
      }
    });
    viewModel.getUpdateComplete().observe(this, new EventObserver<>(result -> {
          switch (result.getFirst()) {
            case TransactionListViewModel.TOKEN_REMAP_CATEGORY: {
              final String message = result.getSecond() > 0 ? getString(R.string.remapping_result) : "No transactions were mapped";
              ((ProtectedFragmentActivity) TransactionList.this.getActivity()).showSnackbar(message, Snackbar.LENGTH_LONG);
            }
          }
          return Unit.INSTANCE;
        })
    );
    MyApplication.getInstance().getAppComponent().inject(this);
    firstLoadCompleted = (savedInstanceState != null);
    budgetsObserver = new BudgetObserver();
    getContext().getContentResolver().registerContentObserver(
        TransactionProvider.BUDGETS_URI,
        true, budgetsObserver);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (budgetsObserver != null) {
      try {
        ContentResolver cr = getContext().getContentResolver();
        cr.unregisterContentObserver(budgetsObserver);
      } catch (IllegalStateException ise) {
        // Do Nothing.  Observer has already been unregistered.
      }
    }
  }

  private class BudgetObserver extends ContentObserver {
    public BudgetObserver() {
      super(new Handler());
    }

    @Override
    public void onChange(boolean selfChange) {
      if (mAccount != null) {
        viewModel.loadBudget(mAccount);
      }
    }
  }

  private void setGrouping() {
    mAdapter.refreshDateFormat();
    restartGroupingLoader();
  }

  private void restartGroupingLoader() {
    Utils.requireLoader(mManager, GROUPING_CURSOR, null, this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mManager = LoaderManager.getInstance(this);
    filterPersistence = new FilterPersistence(prefHandler, prefNameForCriteria(), savedInstanceState, true, true);
    View v = inflater.inflate(R.layout.expenses_list, container, false);
    ButterKnife.bind(this, v);
    if (mAdapter == null) {
      mAdapter = new MyGroupedAdapter(getActivity(), R.layout.expense_row, null, 0);
    }
    configureListView();
    registerForContextualActionBar(mListView.getWrappedList());
    return v;
  }

  @Override
  protected boolean shouldStartActionMode() {
    return mAccount != null && (mAccount.isAggregate() || !mAccount.isSealed());
  }

  private void configureListView() {
    mListView.setOnHeaderClickListener(this);
    mListView.setDrawingListUnderStickyHeader(false);

    mListView.setEmptyView(emptyView);
    mListView.setOnItemClickListener((a, v1, position, id) -> {
      FragmentManager fm = getActivity().getSupportFragmentManager();
      DialogFragment f = (DialogFragment) fm.findFragmentByTag(TransactionDetailFragment.class.getName());
      if (f == null) {
        FragmentTransaction ft = fm.beginTransaction();
        TransactionDetailFragment.newInstance(id).show(ft, TransactionDetailFragment.class.getName());
      }
    });
    mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
      private int currentState = 0;

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE && currentState != scrollState && view.isFastScrollEnabled()) {
          view.postDelayed(new Runnable() {
            @Override
            public void run() {
              if (currentState == SCROLL_STATE_IDLE) view.setFastScrollEnabled(false);
            }
          }, 1000);
        }
        currentState = scrollState;
      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (currentState == SCROLL_STATE_TOUCH_SCROLL) {

          if (!view.isFastScrollEnabled())
            view.setFastScrollEnabled(true);
        }
      }
    });
    mListView.addFooterView(LayoutInflater.from(getActivity()).inflate(R.layout.group_divider, mListView.getWrappedList(), false), null, false);
    mListView.setAdapter(mAdapter);
  }

  protected void refresh(boolean invalidateMenu) {
    if (mAccount != null) { //if we are refreshed from onActivityResult, it might happen, that mAccount is not yet set (report 5c1754c8f8b88c29631ef140)
      mManager.restartLoader(TRANSACTION_CURSOR, null, this);
      mManager.restartLoader(GROUPING_CURSOR, null, this);
    }
    if (invalidateMenu) {
      getActivity().invalidateOptionsMenu();
    }
  }

  @Override
  public void onDestroyView() {
    if (mListView != null) {
      listState = mListView.getWrappedList().onSaveInstanceState();
    }
    super.onDestroyView();
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (ctx == null) return false;
    FragmentManager fm = getFragmentManager();
    switch (command) {
      case R.id.DELETE_COMMAND: {
        boolean hasReconciled = false, hasNotVoid = false;
        for (int i = 0; i < positions.size(); i++) {
          if (positions.valueAt(i)) {
            mTransactionsCursor.moveToPosition(positions.keyAt(i));
            CrStatus status;
            try {
              status = CrStatus.valueOf(mTransactionsCursor.getString(columnIndexCrStatus));
            } catch (IllegalArgumentException ex) {
              status = CrStatus.UNRECONCILED;
            }
            if (status == CrStatus.RECONCILED) {
              hasReconciled = true;
            }
            if (status != CrStatus.VOID) {
              hasNotVoid = true;
            }
            if (hasNotVoid && hasReconciled) break;
          }
        }
        boolean finalHasReconciled = hasReconciled;
        boolean finalHasNotVoid = hasNotVoid;
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          String message = getResources().getQuantityString(R.plurals.warning_delete_transaction, itemIds.length, itemIds.length);
          if (finalHasReconciled) {
            message += " " + getString(R.string.warning_delete_reconciled);
          }
          Bundle b = new Bundle();
          b.putInt(ConfirmationDialogFragment.KEY_TITLE,
              R.string.dialog_title_warning_delete_transaction);
          b.putString(
              ConfirmationDialogFragment.KEY_MESSAGE, message);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
              R.id.DELETE_COMMAND_DO);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
              R.id.CANCEL_CALLBACK_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_delete);
          if (finalHasNotVoid) {
            b.putInt(ConfirmationDialogFragment.KEY_CHECKBOX_LABEL,
                R.string.mark_void_instead_of_delete);
          }
          b.putLongArray(TaskExecutionFragment.KEY_OBJECT_IDS, ArrayUtils.toPrimitive(itemIds));
          ConfirmationDialogFragment.newInstance(b).show(fm, "DELETE_TRANSACTION");
        });
        return true;
      }
      case R.id.SPLIT_TRANSACTION_COMMAND:
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> ctx.contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, ArrayUtils.toPrimitive(itemIds)));
        break;
      case R.id.UNGROUP_SPLIT_COMMAND: {
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          Bundle b = new Bundle();
          b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.warning_ungroup_split_transactions));
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.UNGROUP_SPLIT_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE, R.id.CANCEL_CALLBACK_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_ungroup_split_transaction);
          b.putLongArray(KEY_LONG_IDS, ArrayUtils.toPrimitive(itemIds));
          ConfirmationDialogFragment.newInstance(b).show(fm, "UNSPLIT_TRANSACTION");
        });
        return true;
      }
      case R.id.UNDELETE_COMMAND:
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> ctx.startTaskExecution(
            TaskExecutionFragment.TASK_UNDELETE_TRANSACTION,
            itemIds,
            null,
            0));
        break;

      case R.id.REMAP_CATEGORY_COMMAND: {
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          Intent i = new Intent(getActivity(), ManageCategories.class);
          i.setAction(ManageCategories.ACTION_SELECT_MAPPING);
          startActivityForResult(i, MAP_CATEGORY_RQEUST);
        });
        return true;
      }
      case R.id.REMAP_PAYEE_COMMAND: {
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          final SelectSinglePayeeDialogFragment dialogFragment = SelectSinglePayeeDialogFragment.newInstance(R.string.menu_remap, R.string.no_parties);
          dialogFragment.setTargetFragment(this, MAP_PAYEE_RQEUST);
          dialogFragment.show(getActivity().getSupportFragmentManager(), "REMAP_PAYEE");
        });
        return true;
      }

      case R.id.REMAP_METHOD_COMMAND: {
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          boolean hasExpense = false, hasIncome = false;
          Set<String> accountTypes = new HashSet<>();
          for (int i = 0; i < positions.size(); i++) {
            if (positions.valueAt(i)) {
              mTransactionsCursor.moveToPosition(positions.keyAt(i));
              long amount = mTransactionsCursor.getLong(mTransactionsCursor.getColumnIndex(KEY_AMOUNT));
              if (amount > 0) hasIncome = true;
              if (amount < 0) hasExpense = true;
              accountTypes.add(mTransactionsCursor.getString(mTransactionsCursor.getColumnIndex(KEY_ACCOUNT_TYPE)));
            }
          }
          int type = 0;
          if (hasExpense && !hasIncome) type = -1;
          else if (hasIncome && !hasExpense) type = 1;
          final SelectSingleMethodDialogFragment dialogFragment = SelectSingleMethodDialogFragment.newInstance(
              R.string.menu_remap, R.string.remap_empty_list, accountTypes.toArray(new String[0]), type);
          dialogFragment.setTargetFragment(this, MAP_METHOD_RQEUST);
          dialogFragment.show(getActivity().getSupportFragmentManager(), "REMAP_METHOD");
        });
        return true;
      }
      case R.id.REMAP_ACCOUNT_COMMAND: {
        checkSealed(ArrayUtils.toPrimitive(itemIds), () -> {
          List<Long> excludedIds = new ArrayList<>();
          List<Long> splitIds = new ArrayList<>();
          if (!mAccount.isAggregate()) {
            excludedIds.add(mAccount.getId());
          }
          for (int i = 0; i < positions.size(); i++) {
            if (positions.valueAt(i)) {
              mTransactionsCursor.moveToPosition(positions.keyAt(i));
              long transferaccount = DbUtils.getLongOr0L(mTransactionsCursor, KEY_TRANSFER_ACCOUNT);
              if (transferaccount != 0) {
                excludedIds.add(transferaccount);
              }
              if (SPLIT_CATID.equals(DbUtils.getLongOrNull(mTransactionsCursor, KEY_CATID))) {
                splitIds.add(DbUtils.getLongOr0L(mTransactionsCursor, KEY_ROWID));
              }
            }
          }
          new CheckTransferAccountOfSplitPartsHandler(getActivity().getContentResolver()).check(splitIds, result -> {
            excludedIds.addAll(result);
            final SelectSingleAccountDialogFragment dialogFragment = SelectSingleAccountDialogFragment.newInstance(
                R.string.menu_remap, R.string.remap_empty_list, excludedIds);
            dialogFragment.setTargetFragment(this, MAP_ACCOUNT_RQEUST);
            dialogFragment.show(getActivity().getSupportFragmentManager(), "REMAP_ACCOUNT");
          });
        });
        return true;
      }
      //super is handling deactivation of mActionMode
    }
    return super.dispatchCommandMultiple(command, positions, itemIds);
  }

  private void checkSealed(long[] itemIds, Runnable onChecked) {
    Bundle extras = new Bundle();
    extras.putLongArray(KEY_LONG_IDS, itemIds);
    new CheckSealedHandler(getActivity().getContentResolver()).check(itemIds, result -> {
      if (result) {
        onChecked.run();
      } else {
        warnSealedAccount();
      }
    });
  }

  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) info;
    MyExpenses ctx = (MyExpenses) getActivity();
    mTransactionsCursor.moveToPosition(acmi.position);
    switch (command) {
      case R.id.EDIT_COMMAND:
      case R.id.CLONE_TRANSACTION_COMMAND:
        final boolean isTransferPartPeer = DbUtils.getLongOrNull(mTransactionsCursor, KEY_TRANSFER_PEER_PARENT) != null;
        checkSealed(new long[]{acmi.id}, () -> {
          if (isTransferPartPeer) {
            ctx.showSnackbar(R.string.warning_splitpartcategory_context, Snackbar.LENGTH_LONG);
          } else {
            Intent i = new Intent(ctx, ExpenseEdit.class);
            i.putExtra(KEY_ROWID, acmi.id);
            if (command == R.id.CLONE_TRANSACTION_COMMAND) {
              i.putExtra(ExpenseEdit.KEY_CLONE, true);
            }
            ctx.startActivityForResult(i, MyExpenses.EDIT_REQUEST);
          }
        });
        //super is handling deactivation of mActionMode
        break;
      case R.id.CREATE_TEMPLATE_COMMAND:
        final boolean splitAtPosition = isSplitAtPosition(acmi.position);
        String label = mTransactionsCursor.getString(columnIndexPayee);
        if (TextUtils.isEmpty(label))
          label = mTransactionsCursor.getString(columnIndexLabelSub);
        if (TextUtils.isEmpty(label))
          label = mTransactionsCursor.getString(columnIndexLabelMain);
        String finalLabel = label;
        checkSealed(new long[]{acmi.id}, () -> {
          if (splitAtPosition && !prefHandler.getBoolean(NEW_SPLIT_TEMPLATE_ENABLED, true)) {
            ctx.showContribDialog(ContribFeature.SPLIT_TEMPLATE, null);
          } else {
            Bundle args = new Bundle();
            args.putLong(KEY_ROWID, acmi.id);
            SimpleInputDialog.build()
                .title(R.string.dialog_title_template_title)
                .cancelable(false)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                .hint(R.string.label)
                .text(finalLabel)
                .extra(args)
                .pos(R.string.dialog_button_add)
                .neut()
                .show(this, NEW_TEMPLATE_DIALOG);
          }
        });
        return true;
    }
    return super.dispatchCommandSingle(command, info);
  }

  private void warnSealedAccount() {
    ((ProtectedFragmentActivity) getActivity()).showSnackbar(
        concatResStrings(getContext(), " ", R.string.warning_account_for_transaction_is_closed, R.string.object_sealed),
        Snackbar.LENGTH_LONG);
  }

  private WhereFilter getFilter() {
    return filterPersistence.getWhereFilter();
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    CursorLoader cursorLoader = null;
    String selection;
    String[] selectionArgs;
    if (mAccount.isHomeAggregate()) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_EXCLUDE_FROM_TOTALS + " = 0)";
      selectionArgs = null;
    } else if (mAccount.isAggregate()) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + " = 0)";
      selectionArgs = new String[]{mAccount.getCurrencyUnit().code()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[]{String.valueOf(mAccount.getId())};
    }
    switch (id) {
      case TRANSACTION_CURSOR:
        if (!getFilter().isEmpty()) {
          String selectionForParents = getFilter().getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
          if (!selectionForParents.equals("")) {
            if (!TextUtils.isEmpty(selection)) {
              selection += " AND ";
            }
            selection += selectionForParents;
            selectionArgs = Utils.joinArrays(selectionArgs, getFilter().getSelectionArgs(false));
          }
        }
        if (!TextUtils.isEmpty(selection)) {
          selection += " AND ";
        }
        selection += KEY_PARENTID + " is null";
        cursorLoader = new CursorLoader(getActivity(),
            mAccount.getExtendedUriForTransactionList(false),
            mAccount.getExtendedProjectionForTransactionList(),
            selection,
            selectionArgs, KEY_DATE + " " + mAccount.getSortDirection().name());
        break;
      //TODO: probably we can get rid of SUM_CURSOR, if we also aggregate unmapped transactions
      case SUM_CURSOR:
        cursorLoader = new CursorLoader(getActivity(),
            TransactionProvider.TRANSACTIONS_URI,
            new String[]{MAPPED_CATEGORIES, MAPPED_METHODS, MAPPED_PAYEES, HAS_TRANSFERS},
            selection,
            selectionArgs, null);
        break;
      case GROUPING_CURSOR:
      case SECTION_CURSOR:
        selection = null;
        selectionArgs = null;
        Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
        if (!getFilter().isEmpty()) {
          selection = getFilter().getSelectionForParts(DatabaseConstants.VIEW_EXTENDED);//GROUP query uses extended view
          if (!selection.equals("")) {
            selectionArgs = getFilter().getSelectionArgs(true);
          }
        }
        builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
            .appendPath(id == GROUPING_CURSOR ? mAccount.getGrouping().name() : Grouping.MONTH.name());
        if (!mAccount.isHomeAggregate()) {
          if (mAccount.isAggregate()) {
            builder.appendQueryParameter(KEY_CURRENCY, mAccount.getCurrencyUnit().code());
          } else {
            builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(mAccount.getId()));
          }
        }
        String sortOrder = null;
        if (id == SECTION_CURSOR) {
          builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_SECTIONS, "1");
          sortOrder = String.format("%1$s %3$s,%2$s %3$s",
              KEY_YEAR, KEY_SECOND_GROUP, mAccount.getSortDirection().name());
        }
        cursorLoader = new CursorLoader(getActivity(),
            builder.build(),
            null, selection, selectionArgs, sortOrder);
        break;
    }
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
    final int count = c.getCount();
    switch (arg0.getId()) {
      case TRANSACTION_CURSOR:
        mTransactionsCursor = c;
        hasItems = count > 0;
        if (!indexesCalculated) {
          columnIndexYear = c.getColumnIndex(KEY_YEAR);
          columnIndexYearOfWeekStart = c.getColumnIndex(KEY_YEAR_OF_WEEK_START);
          columnIndexYearOfMonthStart = c.getColumnIndex(KEY_YEAR_OF_MONTH_START);
          columnIndexMonth = c.getColumnIndex(KEY_MONTH);
          columnIndexWeek = c.getColumnIndex(KEY_WEEK);
          columnIndexDay = c.getColumnIndex(KEY_DAY);
          columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
          columnIndexLabelMain = c.getColumnIndex(KEY_LABEL_MAIN);
          columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
          columnIndexCrStatus = c.getColumnIndex(KEY_CR_STATUS);
          indexesCalculated = true;
        }
        mAdapter.swapCursor(c);
        if (count > 0) {
          if (firstLoadCompleted) {
            mListView.post(() -> {
              if (listState != null) {
                mListView.getWrappedList().onRestoreInstanceState(listState);
                listState = null;
              }
            });
          } else {
            firstLoadCompleted = true;
            if (prefHandler.getBoolean(PrefKey.SCROLL_TO_CURRENT_DATE, false)) {
              final int currentPosition = findCurrentPosition(c);
              mListView.post(() -> {
                mListView.setSelection(currentPosition);
              });
            }
          }
        }
        invalidateCAB();
        break;
      case SUM_CURSOR:
        c.moveToFirst();
        mappedCategories = c.getInt(c.getColumnIndex(KEY_MAPPED_CATEGORIES)) > 0;
        mappedPayees = c.getInt(c.getColumnIndex(KEY_MAPPED_PAYEES)) > 0;
        mappedMethods = c.getInt(c.getColumnIndex(KEY_MAPPED_METHODS)) > 0;
        hasTransfers = c.getInt(c.getColumnIndex(KEY_HAS_TRANSFERS)) > 0;
        getActivity().invalidateOptionsMenu();
        break;
      case GROUPING_CURSOR:
        int columnIndexGroupYear = c.getColumnIndex(KEY_YEAR);
        int columnIndexGroupSecond = c.getColumnIndex(KEY_SECOND_GROUP);
        int columnIndexGroupSumIncome = c.getColumnIndex(KEY_SUM_INCOME);
        int columnIndexGroupSumExpense = c.getColumnIndex(KEY_SUM_EXPENSES);
        int columnIndexGroupSumTransfer = c.getColumnIndex(KEY_SUM_TRANSFERS);
        int columnIndexGroupMappedCategories = c.getColumnIndex(KEY_MAPPED_CATEGORIES);
        headerData.clear();
        if (c.moveToFirst()) {
          long previousBalance = mAccount.openingBalance.getAmountMinor();
          do {
            long sumIncome = c.getLong(columnIndexGroupSumIncome);
            long sumExpense = c.getLong(columnIndexGroupSumExpense);
            long sumTransfer = c.getLong(columnIndexGroupSumTransfer);
            long delta = sumIncome + sumExpense + sumTransfer;
            long interimBalance = previousBalance + delta;
            long mappedCategories = c.getLong(columnIndexGroupMappedCategories);
            headerData.put(calculateHeaderId(c.getInt(columnIndexGroupYear), c.getInt(columnIndexGroupSecond)),
                new Long[]{sumIncome, sumExpense, sumTransfer, previousBalance, delta, interimBalance, mappedCategories});
            previousBalance = interimBalance;
          } while (c.moveToNext());
        }
        //if the transactionscursor has been loaded before the grouping cursor, we need to refresh
        //in order to have accurate grouping values
        if (mTransactionsCursor != null)
          mAdapter.notifyDataSetChanged();
        break;
      case SECTION_CURSOR:
        sections = new String[count];
        sectionIds = new int[count];
        mSectionCache = new SparseIntArray(count);
        if (c.moveToFirst()) {
          final Calendar cal = Calendar.getInstance();
          final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yy", MyApplication.getUserPreferedLocale());
          do {
            final int year = c.getInt(c.getColumnIndex(KEY_YEAR));
            final int month = c.getInt(c.getColumnIndex(KEY_SECOND_GROUP));
            cal.set(year, month, 1);
            final int position = c.getPosition();
            sections[position] = dateFormat.format(cal.getTime());
            sectionIds[position] = calculateHeaderId(year, month, Grouping.MONTH);
          } while (c.moveToNext());
        }
    }
  }

  private int findCurrentPosition(Cursor c) {
    int dateColumn = c.getColumnIndex(KEY_DATE);
    switch (mAccount.getSortDirection()) {
      case ASC:
        long startOfToday = ZonedDateTime.of(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS), ZoneId.systemDefault()).toEpochSecond();
        if (c.moveToLast()) {
          do {
            if (c.getLong(dateColumn) <= startOfToday) {
              return c.isLast() ? c.getPosition() : c.getPosition() + 1;
            }
          } while (c.moveToPrevious());
        }
        break;
      case DESC:
        long endOfDay = ZonedDateTime.of(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1), ZoneId.systemDefault()).toEpochSecond();
        if (c.moveToFirst()) {
          do {
            if (c.getLong(dateColumn) < endOfDay) {
              return c.getPosition();
            }
          } while (c.moveToNext());
        }
    }
    return 0;
  }


  private int calculateHeaderId(int year, int second, Grouping grouping) {
    if (grouping.equals(Grouping.NONE)) {
      return 1;
    }
    return year * 1000 + second;
  }

  private int calculateHeaderId(int year, int second) {
    return calculateHeaderId(year, second, mAccount.getGrouping());
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
    switch (arg0.getId()) {
      case TRANSACTION_CURSOR:
        mTransactionsCursor = null;
        mAdapter.swapCursor(null);
        hasItems = false;
        break;
      case SUM_CURSOR:
        mappedCategories = false;
        mappedPayees = false;
        mappedMethods = false;
        break;
    }
  }

  public boolean isFiltered() {
    return !getFilter().isEmpty();
  }

  public boolean hasItems() {
    return hasItems;
  }

  public boolean hasMappedCategories() {
    return mappedCategories;
  }

  private class MyGroupedAdapter extends TransactionAdapter implements SectionIndexingStickyListHeadersAdapter {
    private LayoutInflater inflater;

    private MyGroupedAdapter(Context context, int layout, Cursor c, int flags) {
      super(context, layout, c, flags, currencyFormatter, prefHandler, currencyContext);
      inflater = LayoutInflater.from(getActivity());
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      HeaderViewHolder holder = null;
      final boolean withBudget = TransactionList.this.getFilter().isEmpty() &&
          budget != null;

      if (convertView != null) {
        holder = (HeaderViewHolder) convertView.getTag();
        boolean holderHasBudget = holder.budgetProgress != null;
        if (withBudget != holderHasBudget) holder = null;
      }
      if (holder == null) {
        final int headerLayout = withBudget ? R.layout.header_with_budget : R.layout.header;
        convertView = inflater.inflate(headerLayout, parent, false);
        holder = new HeaderViewHolder(convertView);
        convertView.setTag(holder);
      }

      Cursor c = getCursor();
      if (c != null) {
        c.moveToPosition(position);
        fillSums(holder, getHeaderId(position));
        holder.text.setText(mAccount.getGrouping().getDisplayTitle(getActivity(), c.getInt(getColumnIndexForYear()), getSecond(c), c));
      }
      return convertView;
    }

    @SuppressLint("SetTextI18n")
    private void fillSums(HeaderViewHolder holder, long headerId) {
      Long[] data = headerData != null ? headerData.get(headerId) : null;
      if (data != null) {
        holder.sumIncome.setText("+ " + currencyFormatter.convAmount(data[0], mAccount.getCurrencyUnit()));
        final Long expensesSum = data[1];
        holder.sumExpense.setText(currencyFormatter.convAmount(expensesSum, mAccount.getCurrencyUnit()));
        holder.sumTransfer.setText(Transfer.BI_ARROW + " " + currencyFormatter.convAmount(
            data[2], mAccount.getCurrencyUnit()));
        String formattedDelta = String.format("%s %s", Long.signum(data[4]) > -1 ? "+" : "-",
            currencyFormatter.convAmount(Math.abs(data[4]), mAccount.getCurrencyUnit()));
        currencyFormatter.convAmount(Math.abs(data[4]), mAccount.getCurrencyUnit());
        holder.interimBalance.setText(
            TransactionList.this.getFilter().isEmpty() && !mAccount.isHomeAggregate() ? String.format("%s %s = %s",
                currencyFormatter.convAmount(data[3], mAccount.getCurrencyUnit()), formattedDelta,
                currencyFormatter.convAmount(data[5], mAccount.getCurrencyUnit())) :
                formattedDelta);
        if (holder.budgetProgress != null && budget != null) {
          long budgetAmountMinor = budget.getAmountMinor();
          int progress = budgetAmountMinor == 0 ? 100 : Math.round(-expensesSum * 100F / budgetAmountMinor);
          UiUtils.configureProgress(holder.budgetProgress, progress);
          holder.budgetProgress.setFinishedStrokeColor(mAccount.color);
          holder.budgetProgress.setUnfinishedStrokeColor(getContrastColor(mAccount.color));
        }
      }
    }

    @Override
    public long getHeaderId(int position) {
      return getHeaderIdInt(position);
    }

    private int getHeaderIdInt(int position) {
      Cursor c = getCursor();
      if (c == null) return 0;
      c.moveToPosition(position);
      return calculateHeaderId(c.getInt(getColumnIndexForYear()), getSecond(c));
    }


    private int getSectioningId(int position) {
      Cursor c = getCursor();
      return c.moveToPosition(position) ? calculateHeaderId(c.getInt(columnIndexYear), c.getInt(columnIndexMonth), Grouping.MONTH) : 0;
    }

    private int getSecond(Cursor c) {
      switch (mAccount.getGrouping()) {
        case DAY:
          return c.getInt(columnIndexDay);
        case WEEK:
          return c.getInt(columnIndexWeek);
        case MONTH:
          return c.getInt(columnIndexMonth);
        default:
          return 0;
      }
    }

    private int getColumnIndexForYear() {
      switch (mAccount.getGrouping()) {
        case WEEK:
          return columnIndexYearOfWeekStart;
        case MONTH:
          return columnIndexYearOfMonthStart;
        default:
          return columnIndexYear;
      }
    }

    @Override
    public Object[] getSections() {
      return sections;
    }

    /**
     * inspired by {@link android.widget.AlphabetIndexer}<p>
     * {@inheritDoc}
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
      // Check bounds
      if (sectionIds == null) {
        return 0;
      }
      if (sectionIndex >= sections.length) {
        sectionIndex = sections.length - 1;
        if (sectionIndex <= 0) {
          return 0;
        }
      }

      int count = mTransactionsCursor.getCount();
      int start = 0;
      int end = count;
      int pos;
      int targetHeaderId = sectionIds[sectionIndex];

      // Check map
      if (Integer.MIN_VALUE != (pos = mSectionCache.get(targetHeaderId, Integer.MIN_VALUE))) {
        // Is it approximate? Using negative value to indicate that it's
        // an approximation and positive value when it is the accurate
        // position.
        if (pos < 0) {
          pos = -pos;
          end = pos;
        } else {
          // Not approximate, this is the confirmed start of section, return it
          Timber.d("getPositionForSection from cache %d: %d", sectionIndex, pos);
          return pos;
        }
      }

      // Do we have the position of the previous section?
      if (sectionIndex > 0) {
        int prevLetterPos = mSectionCache.get(sectionIds[sectionIndex - 1], Integer.MIN_VALUE);
        if (prevLetterPos != Integer.MIN_VALUE) {
          start = Math.abs(prevLetterPos);
        }
      }

      // Now that we have a possibly optimized start and end, let's binary search

      pos = (end + start) / 2;

      while (pos < end) {
        // Get letter at pos
        int curHeaderId = getSectioningId(pos);

        int diff = Utils.compare(curHeaderId, targetHeaderId);
        if (diff != 0) {
          if (mAccount.getSortDirection().equals(SortDirection.DESC)) diff = -diff;
          // Enter approximation in hash if a better solution doesn't exist
          int curPos = mSectionCache.get(curHeaderId, Integer.MIN_VALUE);
          if (curPos == Integer.MIN_VALUE || Math.abs(curPos) > pos) {
            //     Negative pos indicates that it is an approximation
            mSectionCache.put(curHeaderId, -pos);
          }
          if (diff < 0) {
            start = pos + 1;
            if (start >= count) {
              pos = count;
              break;
            }
          } else {
            end = pos;
          }
        } else {
          // They're the same, but that doesn't mean it's the start
          if (start == pos) {
            // This is it
            break;
          } else {
            // Need to go further lower to find the starting row
            end = pos;
          }
        }
        pos = (start + end) / 2;
      }
      mSectionCache.put(targetHeaderId, pos);
      Timber.d("getPositionForSection %d: %d", sectionIndex, pos);
      return pos;
    }

    @Override
    public int getSectionForPosition(int position) {
      if (sectionIds == null) return 0;
      final int indexOfKey = IntStream.range(0, sectionIds.length)
          .filter(i -> sectionIds[i] == getSectioningId(position))
          .findFirst().orElse(0);
      Timber.d("getSectionForPosition %d: %d", position, indexOfKey);
      return indexOfKey;
    }
  }

  class HeaderViewHolder {
    @BindView(R.id.interim_balance)
    TextView interimBalance;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.sum_income)
    TextView sumIncome;
    @BindView(R.id.sum_expense)
    TextView sumExpense;
    @BindView(R.id.sum_transfer)
    TextView sumTransfer;
    @Nullable
    @BindView(R.id.budgetProgress)
    DonutProgress budgetProgress;
    @BindView(R.id.divider_bottom)
    View dividerBottom;

    HeaderViewHolder(View convertView) {
      ButterKnife.bind(this, convertView);
    }
  }

  @Override
  public void onHeaderClick(StickyListHeadersListView l, View header,
                            int itemPosition, long headerId, boolean currentlySticky) {
    final HeaderViewHolder viewHolder = (HeaderViewHolder) header.getTag();
    if (mListView.isHeaderCollapsed(headerId)) {
      mListView.expand(headerId);
      viewHolder.dividerBottom.setVisibility(View.VISIBLE);
    } else {
      mListView.collapse(headerId);
      viewHolder.dividerBottom.setVisibility(View.GONE);
    }
  }

  @Override
  public boolean onHeaderLongClick(StickyListHeadersListView l, View header,
                                   int itemPosition, long headerId, boolean currentlySticky) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (headerData != null && headerData.get(headerId)[6] > 0) {
      ctx.contribFeatureRequested(ContribFeature.DISTRIBUTION, headerId);
    } else {
      ctx.showSnackbar(R.string.no_mapped_transactions, Snackbar.LENGTH_LONG);
    }
    return true;
  }

  @Override
  protected void configureMenuLegacy(Menu menu, ContextMenuInfo menuInfo, int listId) {
    super.configureMenuLegacy(menu, menuInfo, listId);
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    final boolean hasSplit = isSplitAtPosition(info.position);
    configureMenuInternal(menu, hasSplit, isVoidAtPosition(info.position), !hasSplit, isTransferAtPosition(info.position), 1);
  }

  @Override
  protected void configureMenu11(Menu menu, int count, AbsListView lv) {
    super.configureMenu11(menu, count, lv);
    SparseBooleanArray checkedItemPositions = lv.getCheckedItemPositions();
    boolean hasSplit = false, hasVoid = false, hasNotSplit = false, hasTransfer = false;
    for (int i = 0; i < checkedItemPositions.size(); i++) {
      if (checkedItemPositions.valueAt(i)) {
        if (isSplitAtPosition(checkedItemPositions.keyAt(i))) {
          hasSplit = true;
        } else {
          hasNotSplit = true;
        }
        if (hasSplit && hasNotSplit) {
          break;
        }
      }
    }
    for (int i = 0; i < checkedItemPositions.size(); i++) {
      if (checkedItemPositions.valueAt(i)) {
        if (isVoidAtPosition(checkedItemPositions.keyAt(i))) {
          hasVoid = true;
          break;
        }
      }
    }
    for (int i = 0; i < checkedItemPositions.size(); i++) {
      if (checkedItemPositions.valueAt(i)) {
        if (isTransferAtPosition(checkedItemPositions.keyAt(i))) {
          hasTransfer = true;
          break;
        }
      }
    }
    configureMenuInternal(menu, hasSplit, hasVoid, hasNotSplit, hasTransfer, count);
  }

  private boolean isTransferAtPosition(int position) {
    if (mTransactionsCursor != null) {
      return mTransactionsCursor.moveToPosition(position) &&
          DbUtils.getLongOr0L(mTransactionsCursor, KEY_TRANSFER_ACCOUNT) != 0L;
    }
    return false;
  }

  private boolean isSplitAtPosition(int position) {
    if (mTransactionsCursor != null) {
      return mTransactionsCursor.moveToPosition(position) &&
          SPLIT_CATID.equals(DbUtils.getLongOrNull(mTransactionsCursor, KEY_CATID));
    }
    return false;
  }

  private boolean isVoidAtPosition(int position) {
    if (mTransactionsCursor != null) {
      if (mTransactionsCursor.moveToPosition(position)) {
        CrStatus status;
        try {
          status = CrStatus.valueOf(mTransactionsCursor.getString(columnIndexCrStatus));
        } catch (IllegalArgumentException ex) {
          status = CrStatus.UNRECONCILED;
        }
        if (status.equals(CrStatus.VOID)) {
          return true;
        }
      }
    }
    return false;
  }

  private void configureMenuInternal(Menu menu, boolean hasSplit, boolean hasVoid, boolean hasNotSplit, boolean hasTransfer, int count) {
    menu.findItem(R.id.CREATE_TEMPLATE_COMMAND).setVisible(count == 1);
    menu.findItem(R.id.SPLIT_TRANSACTION_COMMAND).setVisible(!hasSplit && !hasVoid);
    menu.findItem(R.id.UNGROUP_SPLIT_COMMAND).setVisible(!hasNotSplit && !hasVoid);
    menu.findItem(R.id.UNDELETE_COMMAND).setVisible(hasVoid);
    menu.findItem(R.id.EDIT_COMMAND).setVisible(count == 1 && !hasVoid);
    menu.findItem(R.id.REMAP_ACCOUNT_COMMAND).setVisible(((MyExpenses) getActivity()).getAccountCount() > 1);
    menu.findItem(R.id.REMAP_PAYEE_COMMAND).setVisible(!hasTransfer);
    menu.findItem(R.id.REMAP_CATEGORY_COMMAND).setVisible(!hasTransfer && !hasSplit);
    menu.findItem(R.id.REMAP_METHOD_COMMAND).setVisible(!hasTransfer);
  }

  @SuppressLint("NewApi")
  public void onDrawerOpened() {
    if (mActionMode != null) {
      mCheckedListItems = mListView.getWrappedList().getCheckedItemPositions().clone();
      mActionMode.finish();
    }
  }

  public void onDrawerClosed() {
    if (mCheckedListItems != null) {
      for (int i = 0; i < mCheckedListItems.size(); i++) {
        if (mCheckedListItems.valueAt(i)) {
          mListView.getWrappedList().setItemChecked(mCheckedListItems.keyAt(i), true);
        }
      }
    }
    mCheckedListItems = null;
  }

  public void addFilterCriteria(Criteria c) {
    filterPersistence.addCriteria(c);
    refreshAfterFilterChange();
  }

  protected void refreshAfterFilterChange() {
    refresh(true);
  }

  /**
   * Removes a given filter
   *
   * @param id
   * @return true if the filter was set and succesfully removed, false otherwise
   */
  private boolean removeFilter(int id) {
    boolean isFiltered = filterPersistence.removeFilter(id);
    if (isFiltered) {
      refreshAfterFilterChange();
    }
    return isFiltered;
  }

  private String prefNameForCriteria() {
    return String.format(Locale.ROOT, "%s_%%s_%d", KEY_FILTER, getArguments().getLong(KEY_ACCOUNTID));
  }

  public void clearFilter() {
    filterPersistence.clearFilter();
    refreshAfterFilterChange();
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    inflater.inflate(R.menu.expenses, menu);
    inflater.inflate(R.menu.grouping, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mAccount == null || getActivity() == null) {
      //mAccount seen in report 3331195c529454ca6b25a4c5d403beda
      //getActivity seen in report 68a501c984bdfcc95b40050af4f815bf
      return;
    }
    MenuItem searchMenu = menu.findItem(R.id.SEARCH_COMMAND);
    if (searchMenu != null) {
      Drawable searchMenuIcon = searchMenu.getIcon();
      if (searchMenuIcon == null) {
        CrashHandler.report("Search menu icon not found");
      }
      filterCard.setVisibility(getFilter().isEmpty() ? View.GONE : View.VISIBLE);
      searchMenu.setChecked(!getFilter().isEmpty());
      if (searchMenuIcon != null) {
        DrawableCompat.setTintList(searchMenuIcon, getFilter().isEmpty() ? null : ColorStateList.valueOf(Color.GREEN));
      }
      if (!getFilter().isEmpty()) {
        addChipsBulk(filterView, Stream.of(getFilter().getCriteria()).map(criterion -> criterion.prettyPrint(getContext())).collect(Collectors.toList()));
      }
      SubMenu filterMenu = searchMenu.getSubMenu();
      for (int i = 0; i < filterMenu.size(); i++) {
        MenuItem filterItem = filterMenu.getItem(i);
        boolean enabled = true;
        switch (filterItem.getItemId()) {
          case R.id.FILTER_CATEGORY_COMMAND:
            enabled = mappedCategories;
            break;
          case R.id.FILTER_STATUS_COMMAND:
            enabled = !mAccount.getType().equals(AccountType.CASH);
            break;
          case R.id.FILTER_PAYEE_COMMAND:
            enabled = mappedPayees;
            break;
          case R.id.FILTER_METHOD_COMMAND:
            enabled = mappedMethods;
            break;
          case R.id.FILTER_TRANSFER_COMMAND:
            enabled = hasTransfers;
            break;
        }
        Criteria c = getFilter().get(filterItem.getItemId());
        Utils.menuItemSetEnabledAndVisible(filterItem, enabled || c != null);
        if (c != null) {
          filterItem.setChecked(true);
          filterItem.setTitle(c.prettyPrint(getContext()));
        }
      }
    } else {
      CrashHandler.report("Search menu not found");
    }

    MenuItem groupingItem = menu.findItem(R.id.GROUPING_COMMAND);
    if (groupingItem != null) {
      SubMenu groupingMenu = groupingItem.getSubMenu();
      Utils.configureGroupingMenu(groupingMenu, mAccount.getGrouping());
    }

    MenuItem sortDirectionItem = menu.findItem(R.id.SORT_DIRECTION_COMMAND);
    if (sortDirectionItem != null) {
      SubMenu sortDirectionMenu = sortDirectionItem.getSubMenu();
      Utils.configureSortDirectionMenu(sortDirectionMenu, mAccount.getSortDirection());
    }

    MenuItem balanceItem = menu.findItem(R.id.BALANCE_COMMAND);
    if (balanceItem != null) {
      Utils.menuItemSetEnabledAndVisible(balanceItem, mAccount.getType() != AccountType.CASH && !mAccount.isSealed());
    }

    MenuItem syncItem = menu.findItem(R.id.SYNC_COMMAND);
    if (syncItem != null) {
      Utils.menuItemSetEnabledAndVisible(syncItem, mAccount.getSyncAccountName() != null);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (filterPersistence != null) {
      filterPersistence.onSaveInstanceState(outState);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int command = item.getItemId();
    switch (command) {
      case R.id.FILTER_CATEGORY_COMMAND:
        if (!removeFilter(command)) {
          Intent i = new Intent(getActivity(), ManageCategories.class);
          i.setAction(ManageCategories.ACTION_SELECT_FILTER);
          startActivityForResult(i, ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST);
        }
        return true;
      case R.id.FILTER_AMOUNT_COMMAND:
        if (!removeFilter(command)) {
          AmountFilterDialog.newInstance(mAccount.getCurrencyUnit())
              .show(getActivity().getSupportFragmentManager(), "AMOUNT_FILTER");
        }
        return true;
      case R.id.FILTER_DATE_COMMAND:
        if (!removeFilter(command)) {
          DateFilterDialog.newInstance()
              .show(getActivity().getSupportFragmentManager(), "AMOUNT_FILTER");
        }
        return true;
      case R.id.FILTER_COMMENT_COMMAND:
        if (!removeFilter(command)) {
          SimpleInputDialog.build()
              .title(R.string.search_comment)
              .pos(R.string.menu_search)
              .neut()
              .show(this, FILTER_COMMENT_DIALOG);
        }
        return true;
      case R.id.FILTER_STATUS_COMMAND:
        if (!removeFilter(command)) {
          SelectCrStatusDialogFragment.newInstance()
              .show(getActivity().getSupportFragmentManager(), "STATUS_FILTER");
        }
        return true;
      case R.id.FILTER_PAYEE_COMMAND:
        if (!removeFilter(command)) {
          SelectPayerDialogFragment.newInstance(mAccount.getId())
              .show(getActivity().getSupportFragmentManager(), "PAYER_FILTER");
        }
        return true;
      case R.id.FILTER_METHOD_COMMAND:
        if (!removeFilter(command)) {
          SelectMethodDialogFragment.newInstance(mAccount.getId())
              .show(getActivity().getSupportFragmentManager(), "METHOD_FILTER");
        }
        return true;
      case R.id.FILTER_TRANSFER_COMMAND:
        if (!removeFilter(command)) {
          SelectTransferAccountDialogFragment.newInstance(mAccount.getId())
              .show(getActivity().getSupportFragmentManager(), "TRANSFER_FILTER");
        }
        return true;
      case R.id.PRINT_COMMAND:
        MyExpenses ctx = (MyExpenses) getActivity();
        Result appDirStatus = AppDirHelper.checkAppDir(ctx);
        if (hasItems) {
          if (appDirStatus.isSuccess()) {
            ctx.contribFeatureRequested(ContribFeature.PRINT, null);
          } else {
            ctx.showSnackbar(appDirStatus.print(ctx), Snackbar.LENGTH_LONG);
          }
        } else {
          ctx.showExportDisabledCommand();
        }
        return true;

      case R.id.SYNC_COMMAND: {
        mAccount.requestSync();
        return true;
      }
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public ArrayList<Criteria> getFilterCriteria() {
    return getFilter().getCriteria();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (resultCode == Activity.RESULT_CANCELED) {
      return;
    }
    if (requestCode == ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST) {
      String label = intent.getStringExtra(KEY_LABEL);
      if (resultCode == Activity.RESULT_OK) {
        long catId = intent.getLongExtra(KEY_CATID, 0);
        addCategoryFilter(label, catId);
      }
      if (resultCode == Activity.RESULT_FIRST_USER) {
        long[] catIds = intent.getLongArrayExtra(KEY_CATID);
        addCategoryFilter(label, catIds);
      }
    }
    if (requestCode == MAP_CATEGORY_RQEUST || requestCode == MAP_PAYEE_RQEUST || requestCode == MAP_METHOD_RQEUST || requestCode == MAP_ACCOUNT_RQEUST) {
      Bundle b = new Bundle();
      int columnStringResId, confirmationStringResId;
      String column;
      String intentKey = KEY_ROWID;
      switch (requestCode) {
        case MAP_CATEGORY_RQEUST: {
          column = intentKey = KEY_CATID;
          columnStringResId = R.string.category;
          confirmationStringResId = R.string.remap_category;
          break;
        }
        case MAP_PAYEE_RQEUST: {
          column = KEY_PAYEEID;
          columnStringResId = R.string.payer_or_payee;
          confirmationStringResId = R.string.remap_payee;
          break;
        }
        case MAP_METHOD_RQEUST: {
          column = KEY_METHODID;
          columnStringResId = R.string.method;
          confirmationStringResId = R.string.remap_method;
          break;
        }
        case MAP_ACCOUNT_RQEUST: {
          column = KEY_ACCOUNTID;
          columnStringResId = R.string.account;
          confirmationStringResId = R.string.remap_account;
          break;
        }
        default:
          throw new IllegalStateException("Unexpected value: " + requestCode);
      }
      b.putString(KEY_COLUMN, column);
      b.putLong(KEY_ROWID, intent.getLongExtra(intentKey, 0));

      SimpleDialog.build()
          .title(getString(R.string.dialog_title_confirm_remap, getString(columnStringResId)))
          .pos(R.string.menu_remap)
          .neg(android.R.string.cancel)
          .msg(getString(confirmationStringResId, intent.getStringExtra(KEY_LABEL)) + " " + getString(R.string.continue_confirmation))
          .extra(b)
          .show(this, REMAP_DIALOG);
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (which == BUTTON_POSITIVE) {
      if (dialogTag.equals(REMAP_DIALOG)) {
        viewModel.remap(mListView.getCheckedItemIds(), extras.getString(KEY_COLUMN), extras.getLong(KEY_ROWID));
      }
      if (NEW_TEMPLATE_DIALOG.equals(dialogTag)) {
        MyExpenses ctx = (MyExpenses) getActivity();
        String label = extras.getString(SimpleInputDialog.TEXT);
        final Transaction transaction = Transaction.getInstanceFromDb(extras.getLong(KEY_ROWID));
        Uri uri = transaction == null ? null : new Template(transaction, label).save();
        if (uri == null) {
          ctx.showSnackbar(R.string.template_create_error, Snackbar.LENGTH_LONG);
        } else {
          // show template edit activity
          Intent i = new Intent(ctx, ExpenseEdit.class);
          i.putExtra(DatabaseConstants.KEY_TEMPLATEID, ContentUris.parseId(uri));
          startActivity(i);
        }
        finishActionMode();
      }
      if (TransactionList.FILTER_COMMENT_DIALOG.equals(dialogTag)) {
        final String textResult = extras.getString(SimpleInputDialog.TEXT);
        if (textResult != null) {
          addFilterCriteria(
              new CommentCriteria(textResult.trim()));
        }
        return true;
      }
      return true;
    }
    return false;
  }

  private void addCategoryFilter(String label, long... catIds) {
    addFilterCriteria(catIds.length == 1 && catIds[0] == -1 ?
        new CategoryCriteria() : new CategoryCriteria(label, catIds));
  }

}
