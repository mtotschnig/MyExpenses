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

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.annimon.stream.Stream;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.MyGroupedAdapter;
import org.totschnig.myexpenses.dialog.BalanceDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.ExportDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFilterDialog;
import org.totschnig.myexpenses.dialog.SelectHiddenAccountDialogFragment;
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Sort;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CommentCriteria;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import eltos.simpledialogfragment.list.MenuDialog;
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import timber.log.Timber;

import static eltos.simpledialogfragment.list.CustomListDialog.SELECTED_SINGLE_ID;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_CLEARED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_EXPORTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_AGGREGATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.KEY_LONG_IDS;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_EXPORT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_PRINT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SET_ACCOUNT_HIDDEN;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SET_ACCOUNT_SEALED;

/**
 * This is the main activity where all expenses are listed
 * From the menu sub activities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 */
public class MyExpenses extends LaunchActivity implements
    ViewPager.OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConfirmationDialogFragment.ConfirmationDialogCheckedListener,
    ConfirmationDialogListener, ContribIFace, SimpleDialog.OnDialogResultListener,
    SortUtilityDialogFragment.OnConfirmListener, SelectFilterDialog.Host {

  public static final long THRESHOLD_REMIND_RATE = 47L;

  public static final int ACCOUNTS_CURSOR = -1;
  public static final String KEY_SEQUENCE_COUNT = "sequenceCount";
  private static final String DIALOG_TAG_GROUPING = "GROUPING";
  private static final String DIALOG_TAG_SORTING = "SORTING";
  private static final String MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN";

  private LoaderManager mManager;

  private int mCurrentPosition = -1;
  private Cursor mAccountsCursor;

  private MyViewPagerAdapter mViewPagerAdapter;
  private MyGroupedAdapter mDrawerListAdapter;
  private long mAccountId = 0;
  private String currentCurrency;
  private int mAccountCount = 0;

  private AdHandler adHandler;
  private Toolbar mToolbar;
  private String mCurrentBalance;

  public enum HelpVariant {
    crStatus
  }

  private void setHelpVariant() {
    Account account = Account.getInstanceFromDb(mAccountId);
    setHelpVariant(account == null || account.getType().equals(AccountType.CASH) ?
        null : HelpVariant.crStatus);
  }

  /**
   * stores the number of transactions that have been
   * created in the db, updated after each creation of
   * a new transaction
   */
  private long sequenceCount = 0;
  @BindView(R.id.left_drawer)
  ExpandableStickyListHeadersListView mDrawerList;
  @Nullable
  @BindView(R.id.drawer_layout)
  DrawerLayout mDrawerLayout;
  @BindView(R.id.viewpager)
  ViewPager myPager;
  @BindView(R.id.expansionContent)
  NavigationView navigationView;
  private ActionBarDrawerToggle mDrawerToggle;

  private int columnIndexRowId, columnIndexColor, columnIndexCurrency, columnIndexLabel;
  boolean indexesCalculated = false;
  private long idFromNotification = 0;
  private String mExportFormat = null;

  @Inject
  CurrencyFormatter currencyFormatter;

  private RoadmapViewModel roadmapViewModel;
  private MyExpensesViewModel viewModel;

  @Override
  protected void injectDependencies() {
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeId());

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final ViewGroup adContainer = findViewById(R.id.adContainer);
    adHandler = adHandlerFactory.create(adContainer);
    adContainer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {

          @Override
          public void onGlobalLayout() {
            adContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            adHandler.startBanner();
          }
        });

    adHandler.maybeRequestNewInterstitial();

    ButterKnife.bind(this);

    mToolbar = setupToolbar(false);
    mToolbar.setOnClickListener(v -> copyToClipBoard());
    if (mDrawerLayout != null) {
      mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
          mToolbar, R.string.drawer_open, R.string.drawer_close) {

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        public void onDrawerClosed(View view) {
          super.onDrawerClosed(view);
          TransactionList tl = getCurrentFragment();
          if (tl != null)
            tl.onDrawerClosed();
        }

        /**
         * Called when a drawer has settled in a completely open state.
         */
        public void onDrawerOpened(View drawerView) {
          super.onDrawerOpened(drawerView);
          TransactionList tl = getCurrentFragment();
          if (tl != null)
            tl.onDrawerOpened();
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
          super.onDrawerSlide(drawerView, 0); // this disables the animation
        }
      };

      // Set the drawer toggle as the DrawerListener
      mDrawerLayout.addDrawerListener(mDrawerToggle);
    }
    mDrawerListAdapter = new MyGroupedAdapter(this, null, currencyFormatter, getPrefHandler(), currencyContext);

    navigationView.setNavigationItemSelectedListener(item -> dispatchCommand(item.getItemId(), null));
    View navigationMenuView = navigationView.getChildAt(0);
    if (navigationMenuView != null) {
      navigationMenuView.setVerticalScrollBarEnabled(false);
    }

    mDrawerList.setAdapter(mDrawerListAdapter);
    mDrawerList.setAreHeadersSticky(false);
    mDrawerList.setOnHeaderClickListener(new StickyListHeadersListView.OnHeaderClickListener() {
      @Override
      public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
        if (mDrawerList.isHeaderCollapsed(headerId)) {
          mDrawerList.expand(headerId);
        } else {
          mDrawerList.collapse(headerId);
        }
      }

      @Override
      public boolean onHeaderLongClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
        return false;
      }
    });
    mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
      if (mAccountId != id) {
        moveToPosition(position);
        closeDrawer();
      }
    });
    registerForContextMenu(mDrawerList);
    mDrawerList.setFastScrollEnabled(getPrefHandler().getBoolean(PrefKey.ACCOUNT_LIST_FAST_SCROLL, false));

    requireFloatingActionButtonWithContentDescription(TextUtils.concatResStrings(this, ". ",
        R.string.menu_create_transaction, R.string.menu_create_transfer, R.string.menu_create_split));
    if (savedInstanceState != null) {
      mExportFormat = savedInstanceState.getString("exportFormat");
      mAccountId = savedInstanceState.getLong(KEY_ACCOUNTID, 0L);
    } else {
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        mAccountId = Utils.getFromExtra(extras, KEY_ROWID, 0);
        idFromNotification = extras.getLong(KEY_TRANSACTIONID, 0);
        //detail fragment from notification should only be shown upon first instantiation from notification
        if (idFromNotification != 0) {
          FragmentManager fm = getSupportFragmentManager();
          if (fm.findFragmentByTag(TransactionDetailFragment.class.getName()) == null) {
            TransactionDetailFragment.newInstance(idFromNotification)
                .show(fm, TransactionDetailFragment.class.getName());
            getIntent().removeExtra(KEY_TRANSACTIONID);
          }
        }
      }
    }
    if (mAccountId == 0) {
      mAccountId = PrefKey.CURRENT_ACCOUNT.getLong(0L);
    }
    roadmapViewModel = ViewModelProviders.of(this).get(RoadmapViewModel.class);
    viewModel = ViewModelProviders.of(this).get(MyExpensesViewModel.class);
    viewModel.getHasHiddenAccounts().observe(this,
        result -> navigationView.getMenu().findItem(R.id.HIDDEN_ACCOUNTS_COMMAND).setVisible(result != null && result));
    viewModel.loadHiddenAccountCount();
    setup();
    /*if (savedInstanceState == null) {
      voteReminderCheck();
    }*/
  }

  private void setup() {
    newVersionCheck();
    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin, margin, true);
    mViewPagerAdapter = new MyViewPagerAdapter(this, getSupportFragmentManager(), null);
    myPager.setAdapter(this.mViewPagerAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin(UiUtils.dp2Px(10, getResources()));
    myPager.setPageMarginDrawable(margin.resourceId);
    mManager =  LoaderManager.getInstance(this);
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }

  private void voteReminderCheck() {
    final String prefKey = "vote_reminder_shown_" + RoadmapViewModel.EXPECTED_MINIMAL_VERSION;
    if (Utils.getDaysSinceUpdate(this) > 1 &&
        !getPrefHandler().getBoolean(prefKey, false)) {
      roadmapViewModel.getLastVote().observe(this, vote -> {
        boolean hasNotVoted = vote == null;
        if (hasNotVoted || vote.getVersion() < RoadmapViewModel.EXPECTED_MINIMAL_VERSION) {
          Bundle bundle = new Bundle();
          bundle.putCharSequence(
              ConfirmationDialogFragment.KEY_MESSAGE, hasNotVoted ? getString(R.string.roadmap_intro) :
                  TextUtils.concatResStrings(MyExpenses.this, " ",
                      R.string.roadmap_intro, R.string.roadmap_intro_update));
          bundle.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.ROADMAP_COMMAND);
          bundle.putString(ConfirmationDialogFragment.KEY_PREFKEY, prefKey);
          bundle.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.roadmap_vote);
          ConfirmationDialogFragment.newInstance(bundle).show(getSupportFragmentManager(),
              "ROAD_MAP_VOTE_REMINDER");
        }
      });
      roadmapViewModel.loadLastVote();
    }
  }

  private void moveToPosition(int position) {
    if (myPager.getCurrentItem() == position)
      setCurrentAccount(position);
    else
      myPager.setCurrentItem(position, false);
  }

  private AccountGrouping currentAccountGrouping() {
    try {
      return AccountGrouping.valueOf(
          PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
    } catch (IllegalArgumentException e) {
      return AccountGrouping.TYPE;
    }
  }

  private Sort currentSort() {
    try {
      return Sort.valueOf(
          PrefKey.SORT_ORDER_ACCOUNTS.getString("USAGES"));
    } catch (IllegalArgumentException e) {
      return Sort.USAGES;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
    inflater.inflate(R.menu.grouping, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    if (((AdapterView.AdapterContextMenuInfo) menuInfo).id > 0) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.accounts_context, menu);
      mAccountsCursor.moveToPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
      final boolean isSealed = mAccountsCursor.getInt(mAccountsCursor.getColumnIndex(KEY_SEALED)) == 1;
      menu.findItem(R.id.CLOSE_ACCOUNT_COMMAND).setVisible(!isSealed);
      menu.findItem(R.id.REOPEN_ACCOUNT_COMMAND).setVisible(isSealed);
      menu.findItem(R.id.EDIT_ACCOUNT_COMMAND).setVisible(!isSealed);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    dispatchCommand(item.getItemId(), item.getMenuInfo());
    return true;
  }

  /* (non-Javadoc)
   * check if we should show one of the reminderDialogs
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == EDIT_REQUEST && resultCode == RESULT_OK) {
      long nextReminder;
      sequenceCount = intent.getLongExtra(KEY_SEQUENCE_COUNT, 0);
      if (!DistribHelper.isGithub()) {
        nextReminder =
            PrefKey.NEXT_REMINDER_RATE.getLong(THRESHOLD_REMIND_RATE);
        if (nextReminder != -1 && sequenceCount >= nextReminder) {
          RemindRateDialogFragment f = new RemindRateDialogFragment();
          f.setCancelable(false);
          f.show(getSupportFragmentManager(), "REMIND_RATE");
          return;
        }
      }
      adHandler.onEditTransactionResult();
    }
    if (requestCode == CREATE_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
      //navigating to the new account currently does not work, due to the way LoaderManager behaves
      //since its implementation is based on MutableLiveData
      mAccountId = intent.getLongExtra(KEY_ROWID, 0);
    }
  }

  @Override
  public void addFilterCriteria(@NotNull Criteria c) {
    TransactionList tl = getCurrentFragment();
    if (tl != null) {
      tl.addFilterCriteria(c);
    }
  }

  /**
   * start ExpenseEdit Activity for a new transaction/transfer/split
   * Originally the form for transaction is rendered, user can change from spinner in toolbar
   */
  private void createRow() {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    //if we are called from an aggregate cursor, we also hand over the currency
    if (mAccountId < 0) {
      i.putExtra(KEY_CURRENCY, currentCurrency);
      i.putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true);
    } else {
      //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
      i.putExtra(KEY_ACCOUNTID, mAccountId);
    }
    startActivityForResult(i, EDIT_REQUEST);
  }

  @Override
  protected void doHelp(String variant) {
    setHelpVariant();
    super.doHelp(variant);
  }

  /**
   * @param command
   * @param tag
   * @return true if command has been handled
   */
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    Intent i;
    TransactionList tl;
    switch (command) {
      case R.id.BUDGET_COMMAND:
        contribFeatureRequested(ContribFeature.BUDGET, null);
        return true;
      case R.id.DISTRIBUTION_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && tl.hasMappedCategories()) {
          contribFeatureRequested(ContribFeature.DISTRIBUTION, null);
        } else {
          showMessage(R.string.dialog_command_disabled_distribution);
        }
        return true;
      case R.id.HISTORY_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && tl.hasItems()) {
          contribFeatureRequested(ContribFeature.HISTORY, null);
        } else {
          showMessage(R.string.no_expenses);
        }
        return true;

      case R.id.CREATE_COMMAND:
        if (mAccountCount == 0) {
          showSnackbar(R.string.warning_no_account, Snackbar.LENGTH_LONG);
        } else {
          createRow();
        }
        return true;
      case R.id.BALANCE_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && hasCleared()) {
          mAccountsCursor.moveToPosition(mCurrentPosition);
          CurrencyUnit currency = currencyContext.get(currentCurrency);
          Bundle bundle = new Bundle();
          bundle.putLong(KEY_ROWID,
              mAccountsCursor.getLong(columnIndexRowId));
          bundle.putString(KEY_LABEL,
              mAccountsCursor.getString(columnIndexLabel));
          bundle.putString(KEY_RECONCILED_TOTAL,
              currencyFormatter.formatCurrency(
                  new Money(currency,
                      mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_RECONCILED_TOTAL)))));
          bundle.putString(KEY_CLEARED_TOTAL, currencyFormatter.formatCurrency(
              new Money(currency,
                  mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_CLEARED_TOTAL)))));
          BalanceDialogFragment.newInstance(bundle)
              .show(getSupportFragmentManager(), "BALANCE_ACCOUNT");
        } else {
          showMessage(R.string.dialog_command_disabled_balance);
        }
        return true;
      case R.id.RESET_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && tl.hasItems()) {
          Result appDirStatus = AppDirHelper.checkAppDir(this);
          if (appDirStatus.isSuccess()) {
            ExportDialogFragment.newInstance(mAccountId, tl.isFiltered())
                .show(this.getSupportFragmentManager(), "WARNING_RESET");
          } else {
            showSnackbar(appDirStatus.print(this), Snackbar.LENGTH_LONG);
          }
        } else {
          showExportDisabledCommand();
        }
        return true;
      case R.id.REMIND_NO_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(-1);
        return true;
      case R.id.REMIND_LATER_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(sequenceCount + THRESHOLD_REMIND_RATE);
        return true;
      case R.id.HELP_COMMAND_DRAWER:
        i = new Intent(this, Help.class);
        i.putExtra(Help.KEY_CONTEXT, "NavigationDrawer");
        //for result is needed since it allows us to inspect the calling activity
        startActivity(i);
        return true;
      case R.id.MANAGE_PLANS_COMMAND:
        i = new Intent(this, ManageTemplates.class);
        startActivity(i);
        return true;
      case R.id.CREATE_ACCOUNT_COMMAND:
        if (mAccountsCursor == null) {
          complainAccountsNotLoaded();
        }
        //we need the accounts to be loaded in order to evaluate if the limit has been reached
        else if (ContribFeature.ACCOUNTS_UNLIMITED.hasAccess() || mAccountCount < ContribFeature.FREE_ACCOUNTS) {
          closeDrawer();
          i = new Intent(this, AccountEdit.class);
          if (tag != null)
            i.putExtra(KEY_CURRENCY, (String) tag);
          startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
        } else {
          showContribDialog(ContribFeature.ACCOUNTS_UNLIMITED, null);
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND_DO:
        //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
        final Long[] accountIds = (Long[]) tag;
        if (Stream.of(accountIds).anyMatch(id -> id == mAccountId)) {
          mAccountId = 0;
        }
        final Fragment manageHiddenFragment = getSupportFragmentManager().findFragmentByTag(MANAGE_HIDDEN_FRAGMENT_TAG);
        if (manageHiddenFragment != null) {
          getSupportFragmentManager().beginTransaction().remove(manageHiddenFragment).commit();
        }
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_ACCOUNT,
            accountIds,
            null,
            R.string.progress_dialog_deleting);
        return true;
      case R.id.SHARE_COMMAND:
        i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, Utils.getTellAFriendMessage(this).toString());
        i.setType("text/plain");
        startActivity(Intent.createChooser(i, getResources().getText(R.string.menu_share)));
        return true;
      case R.id.CANCEL_CALLBACK_COMMAND:
        finishActionMode();
        return true;
      case R.id.OPEN_PDF_COMMAND: {
        i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        Uri data = AppDirHelper.ensureContentUri(Uri.parse((String) tag));
        i.setDataAndType(data, "application/pdf");
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!Utils.isIntentAvailable(this, i)) {
          showSnackbar(R.string.no_app_handling_pdf_available, Snackbar.LENGTH_LONG);
        } else {
          startActivity(i);
        }
        return true;
      }
      case R.id.SHARE_PDF_COMMAND: {
        Result shareResult = ShareUtils.share(this,
            Collections.singletonList(AppDirHelper.ensureContentUri(Uri.parse((String) tag))),
            PrefKey.SHARE_TARGET.getString("").trim(),
            "application/pdf");
        if (!shareResult.isSuccess()) {
          showSnackbar(shareResult.print(this), Snackbar.LENGTH_LONG);
        }
        return true;
      }
      case R.id.EDIT_ACCOUNT_COMMAND: {
        closeDrawer();
        long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
        if (accountId > 0) { //do nothing if accidentally we are positioned at an aggregate account
          i = new Intent(this, AccountEdit.class);
          i.putExtra(KEY_ROWID, accountId);
          startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
        }
        return true;
      }
      case R.id.DELETE_ACCOUNT_COMMAND: {
        closeDrawer();
        long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
        //do nothing if accidentally we are positioned at an aggregate account
        if (accountId > 0) {
          final Account account = Account.getInstanceFromDb(accountId);
          if (account != null) {
            MessageDialogFragment.newInstance(
                getResources().getQuantityString(R.plurals.dialog_title_warning_delete_account, 1, 1),
                getString(R.string.warning_delete_account, account.getLabel()) + " " + getString(R.string.continue_confirmation),
                new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                    new Long[]{accountId}),
                null,
                MessageDialogFragment.Button.noButton(), 0)
                .show(getSupportFragmentManager(), "DELETE_ACCOUNT");
          }
        }
        return true;
      }
      case R.id.GROUPING_ACCOUNTS_COMMAND: {
        MenuDialog.build()
            .menu(this, R.menu.accounts_grouping)
            .choiceIdPreset(currentAccountGrouping().commandId)
            .title(R.string.menu_grouping)
            .show(this, DIALOG_TAG_GROUPING);
        return true;
      }
      case R.id.SORT_COMMAND: {
        MenuDialog.build()
            .menu(this, R.menu.accounts_sort)
            .choiceIdPreset(currentSort().commandId)
            .title(R.string.menu_sort)
            .show(this, DIALOG_TAG_SORTING);
        return true;
      }
      case R.id.CLEAR_FILTER_COMMAND: {
        getCurrentFragment().clearFilter();
        return true;
      }
      case R.id.ROADMAP_COMMAND : {
        Intent intent = new Intent(this, RoadmapVoteActivity.class);
        startActivity(intent);
        return true;
      }
      case R.id.CLOSE_ACCOUNT_COMMAND: {
        long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
        //do nothing if accidentally we are positioned at an aggregate account
        if (accountId > 0) {
          mAccountsCursor.moveToPosition(((AdapterView.AdapterContextMenuInfo) tag).position);
          if (mAccountsCursor.getString(mAccountsCursor.getColumnIndex(KEY_SYNC_ACCOUNT_NAME)) == null ) {
            startTaskExecution(
                TASK_SET_ACCOUNT_SEALED,
                new Long[]{accountId},
                true, 0);
          } else {
            showSnackbar(getString(R.string.warning_synced_account_cannot_be_closed),
                Snackbar.LENGTH_LONG, null, null, mDrawerList);
          }
        }
        return true;
      }
      case R.id.REOPEN_ACCOUNT_COMMAND: {
        long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
        //do nothing if accidentally we are positioned at an aggregate account
        if (accountId > 0) {
          startTaskExecution(
              TASK_SET_ACCOUNT_SEALED,
              new Long[]{accountId},
              false, 0);
        }
        return true;
      }
      case R.id.HIDE_ACCOUNT_COMMAND: {
        long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
        //do nothing if accidentally we are positioned at an aggregate account
        if (accountId > 0) {
          startTaskExecution(
              TASK_SET_ACCOUNT_HIDDEN,
              new Long[]{accountId},
              true, 0);
        }
        return true;
      }
      case R.id.HIDDEN_ACCOUNTS_COMMAND: {
        SelectHiddenAccountDialogFragment.newInstance().show(getSupportFragmentManager(),
            MANAGE_HIDDEN_FRAGMENT_TAG);
      }
    }
    return false;
  }

  private void complainAccountsNotLoaded() {
    showSnackbar(R.string.account_list_not_yet_loaded, Snackbar.LENGTH_LONG);
  }

  public void showExportDisabledCommand() {
    showMessage(R.string.dialog_command_disabled_reset_account);
  }

  private void closeDrawer() {
    if (mDrawerLayout != null) mDrawerLayout.closeDrawers();
  }

  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    public MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    public String getFragmentName(int currentPosition) {
      return FragmentPagerAdapter.makeFragmentName(R.id.viewpager, getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
      return TransactionList.newInstance(cursor.getLong(columnIndexRowId));
    }
  }

  @Override
  public void onPageSelected(int position) {
    finishActionMode();
    mCurrentPosition = position;
    setCurrentAccount(position);
  }

  public void finishActionMode() {
    if (mCurrentPosition != -1) {
      ContextualActionBarFragment f = getCurrentFragment();
      if (f != null)
        f.finishActionMode();
    }
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    switch (feature) {
      case DISTRIBUTION: {
        Account a = Account.getInstanceFromDb(mAccountId);
        recordUsage(feature);
        Intent i = new Intent(this, Distribution.class);
        i.putExtra(KEY_ACCOUNTID, mAccountId);
        if (tag != null) {
          int year = (int) ((Long) tag / 1000);
          int groupingSecond = (int) ((Long) tag % 1000);
          i.putExtra(KEY_GROUPING, a != null ? a.getGrouping() : Grouping.NONE);
          i.putExtra(KEY_YEAR, year);
          i.putExtra(KEY_SECOND_GROUP, groupingSecond);
        }
        startActivity(i);
        break;
      }
      case HISTORY: {
        recordUsage(feature);
        Intent i = new Intent(this, HistoryActivity.class);
        i.putExtra(KEY_ACCOUNTID, mAccountId);
        startActivity(i);
        break;
      }
      case SPLIT_TRANSACTION: {
        if (tag != null) {
          Bundle b = new Bundle();
          b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.warning_split_transactions));
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SPLIT_TRANSACTION_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE, R.id.CANCEL_CALLBACK_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_split_transaction);
          b.putLongArray(KEY_LONG_IDS, (long[]) tag);
          ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "SPLIT_TRANSACTION");
        }
        break;
      }
      case PRINT: {
        TransactionList tl = getCurrentFragment();
        if (tl != null) {
          Bundle args = new Bundle();
          args.putParcelableArrayList(TransactionList.KEY_FILTER, tl.getFilterCriteria());
          args.putLong(KEY_ROWID, mAccountId);
          if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager().beginTransaction()
                .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_PRINT), ASYNC_TAG)
                .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_printing), PROGRESS_TAG)
                .commit();
          }
        }
        break;
      }
      case BUDGET: {
        if (mAccountId != 0 && currentCurrency != null) {
          recordUsage(feature);
          Intent i = new Intent(this, ManageBudgets.class);
          startActivity(i);
        }
        break;
      }
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
    if (!DistribHelper.isGithub() && feature == ContribFeature.AD_FREE) {
      finish();
    }
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch (id) {
      case ACCOUNTS_CURSOR:
        Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
        return new CursorLoader(this, builder.build(), null, KEY_HIDDEN + " = 0", null, null);
    }
    throw new IllegalStateException("Unknown loader id " + id);
  }

  /**
   * set the Current account to the one in the requested position of mAccountsCursor
   *
   * @param position
   */
  private void setCurrentAccount(int position) {
    mAccountsCursor.moveToPosition(position);
    long newAccountId = mAccountsCursor.getLong(columnIndexRowId);
    if (mAccountId != newAccountId) {
      PrefKey.CURRENT_ACCOUNT.putLong(newAccountId);
    }
    int color = newAccountId < 0 ? colorAggregate : mAccountsCursor.getInt(columnIndexColor);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      //noinspection InlinedApi
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      //noinspection InlinedApi
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      int color700 = ColorUtils.get700Tint(color);
      window.setStatusBarColor(color700);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //noinspection InlinedApi
        getWindow().getDecorView().setSystemUiVisibility(
            ColorUtils.isBrightColor(color700) ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
      }
    }
    UiUtils.setBackgroundTintListOnFab(floatingActionButton, color);
    mAccountId = newAccountId;
    currentCurrency = mAccountsCursor.getString(columnIndexCurrency);
    setBalance();
    if (mAccountsCursor.getInt(mAccountsCursor.getColumnIndex(KEY_SEALED)) == 1) {
      floatingActionButton.hide();
    } else {
      floatingActionButton.show();
    }
    mDrawerList.setItemChecked(position, true);
    supportInvalidateOptionsMenu();
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    switch (loader.getId()) {
      case ACCOUNTS_CURSOR: {
        mAccountCount = 0;
        mAccountsCursor = cursor;
        if (mAccountsCursor == null) {
          return;
        }

        mDrawerListAdapter.setGrouping(currentAccountGrouping());
        mDrawerListAdapter.swapCursor(mAccountsCursor);
        //swaping the cursor is altering the accountId, if the
        //sort order has changed, but we want to move to the same account as before
        long cacheAccountId = mAccountId;
        mViewPagerAdapter.swapCursor(cursor);
        mAccountId = cacheAccountId;
        if (!indexesCalculated) {
          columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID);
          columnIndexColor = mAccountsCursor.getColumnIndex(KEY_COLOR);
          columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
          columnIndexLabel = mAccountsCursor.getColumnIndex(KEY_LABEL);
          indexesCalculated = true;
        }
        if (mAccountsCursor.moveToFirst()) {
          int position = 0;
          while (!mAccountsCursor.isAfterLast()) {
            long accountId = mAccountsCursor.getLong(columnIndexRowId);
            if (accountId == mAccountId) {
              position = mAccountsCursor.getPosition();
            }
            if (accountId > 0) {
              mAccountCount++;
            }
            mAccountsCursor.moveToNext();
          }
          mCurrentPosition = position;
          moveToPosition(mCurrentPosition);
        }
        break;
      }
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    if (loader.getId() == ACCOUNTS_CURSOR) {
      mViewPagerAdapter.swapCursor(null);
      mDrawerListAdapter.swapCursor(null);
      mCurrentPosition = -1;
      mAccountsCursor = null;
    }
  }

  @Override
  public void onPageScrollStateChanged(int arg0) {
    // noop
  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
    // noop
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (which != BUTTON_POSITIVE) return false;
    if (TransactionList.NEW_TEMPLATE_DIALOG.equals(dialogTag)) {
      String label = extras.getString(SimpleInputDialog.TEXT);
      final Transaction transaction = Transaction.getInstanceFromDb(extras.getLong(KEY_ROWID));
      Uri uri = transaction == null ? null : new Template(transaction, label).save();
      if (uri == null) {
        showSnackbar(R.string.template_create_error, Snackbar.LENGTH_LONG);
      } else {
        // show template edit activity
        Intent i = new Intent(this, ExpenseEdit.class);
        i.putExtra(DatabaseConstants.KEY_TEMPLATEID, ContentUris.parseId(uri));
        startActivity(i);
      }

      finishActionMode();
      return true;
    }
    if (TransactionList.FILTER_COMMENT_DIALOG.equals(dialogTag)) {
      final String textResult = extras.getString(SimpleInputDialog.TEXT);
      if (textResult != null) {
        addFilterCriteria(
            new CommentCriteria(textResult.trim()));
      }
      return true;
    }
    if (DIALOG_TAG_SORTING.equals(dialogTag)) {
      return handleSortOption((int) extras.getLong(SELECTED_SINGLE_ID));
    }
    if (DIALOG_TAG_GROUPING.equals(dialogTag)) {
      return handleAccountsGrouping((int) extras.getLong(SELECTED_SINGLE_ID));
    }
    return false;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TaskExecutionFragment.TASK_SPLIT: {
        Result result = (Result) o;
        if (((Result) o).isSuccess()) {
          recordUsage(ContribFeature.SPLIT_TRANSACTION);
        }
        showSnackbar(result.print(this), Snackbar.LENGTH_LONG);
        break;
      }
      case TaskExecutionFragment.TASK_REVOKE_SPLIT: {
        Result result = (Result) o;
        showSnackbar(result.print(this), Snackbar.LENGTH_LONG);
        break;
      }
      case TaskExecutionFragment.TASK_EXPORT: {
        ArrayList<Uri> files = (ArrayList<Uri>) o;
        if (files != null && !files.isEmpty()) {
          Result shareResult = ShareUtils.share(this, files,
              PrefKey.SHARE_TARGET.getString("").trim(),
              "text/" + mExportFormat.toLowerCase(Locale.US));
          if (!shareResult.isSuccess()) {
            showSnackbar(shareResult.print(this), Snackbar.LENGTH_LONG);
          }
        }
        break;
      }
      case TaskExecutionFragment.TASK_PRINT: {
        Result<Uri> result = (Result<Uri>) o;
        if (result.isSuccess()) {
          recordUsage(ContribFeature.PRINT);
          MessageDialogFragment f = MessageDialogFragment.newInstance(
              0,
              result.print(this),
              new MessageDialogFragment.Button(R.string.menu_open, R.id.OPEN_PDF_COMMAND, result.getExtra().toString(), true),
              MessageDialogFragment.Button.nullButton(R.string.button_label_close),
              new MessageDialogFragment.Button(R.string.button_label_share_file, R.id.SHARE_PDF_COMMAND, result.getExtra().toString(), true));
          f.setCancelable(false);
          f.show(getSupportFragmentManager(), "PRINT_RESULT");
        } else {
          showSnackbar(result.print(this), Snackbar.LENGTH_LONG);
        }
        break;
      }
    }
  }

  public boolean hasExported() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow(KEY_HAS_EXPORTED)) > 0;
  }

  private boolean hasCleared() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow(KEY_HAS_CLEARED)) > 0;
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    if (mDrawerToggle != null) mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    return handleGrouping(item) || handleSortDirection(item) || super.onOptionsItemSelected(item);

  }

  private void setBalance() {
    long balance = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_CURRENT_BALANCE));
    boolean isHome  = mAccountsCursor.getInt(mAccountsCursor.getColumnIndex(KEY_IS_AGGREGATE)) == AggregateAccount.AGGREGATE_HOME;
    mCurrentBalance = String.format(Locale.getDefault(), "%s%s", isHome ? " ≈ " : "",
        currencyFormatter.formatCurrency(new Money(currencyContext.get(currentCurrency), balance)));
    mToolbar.setSubtitle(mCurrentBalance);
    mToolbar.setSubtitleTextColor(balance < 0 ? colorExpense : colorIncome);
  }

  public TransactionList getCurrentFragment() {
    if (mViewPagerAdapter == null)
      return null;
    return (TransactionList) getSupportFragmentManager().findFragmentByTag(
        mViewPagerAdapter.getFragmentName(mCurrentPosition));
  }

  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    //detail fragment from notification should only be shown once
    if (idFromNotification != 0) {
      outState.putLong("idFromNotification", 0);
    }
    outState.putString("exportFormat", mExportFormat);
    outState.putLong(KEY_ACCOUNTID, mAccountId);
  }

  @Override
  public void onPositive(Bundle args) {
    super.onPositive(args);
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.START_EXPORT_COMMAND:
        mExportFormat = args.getString("format");
        args.putParcelableArrayList(TransactionList.KEY_FILTER,
            getCurrentFragment().getFilterCriteria());
        getSupportFragmentManager().beginTransaction()
            .add(TaskExecutionFragment.newInstanceWithBundle(args, TaskExecutionFragment.TASK_EXPORT),
                ASYNC_TAG)
            .add(ProgressDialogFragment.newInstance(
                R.string.pref_category_title_export, 0, ProgressDialog.STYLE_SPINNER, true), PROGRESS_TAG)
            .commit();
        break;
      case R.id.DELETE_COMMAND_DO:
        //Confirmation dialog was shown without Checkbox, because it was called with only void transactions
        onPositive(args, false);
        break;
      case R.id.SPLIT_TRANSACTION_COMMAND: {
        startTaskExecution(TaskExecutionFragment.TASK_SPLIT, args, R.string.progress_dialog_saving);
        break;
      }
      case R.id.UNGROUP_SPLIT_COMMAND: {
        startTaskExecution(TaskExecutionFragment.TASK_REVOKE_SPLIT, args, R.string.progress_dialog_saving);
        break;
      }
    }
  }

  @Override
  public void onPositive(Bundle args, boolean checked) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.DELETE_COMMAND_DO: {
        finishActionMode();
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_TRANSACTION,
            ArrayUtils.toObject(args.getLongArray(TaskExecutionFragment.KEY_OBJECT_IDS)),
            checked,
            R.string.progress_dialog_deleting);
        break;
      }
      case R.id.BALANCE_COMMAND_DO: {
        startTaskExecution(TaskExecutionFragment.TASK_BALANCE,
            new Long[]{args.getLong(KEY_ROWID)},
            checked, 0);
        break;
      }
    }
  }

  @Override
  public void onNegative(Bundle args) {
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE);
    if (command != 0) {
      dispatchCommand(command, null);
    }
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  @Override
  protected void onResume() {
    super.onResume();
    adHandler.onResume();
  }

  @Override
  public void onDestroy() {
    adHandler.onDestroy();
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    adHandler.onPause();
    super.onPause();
  }

  public void onBackPressed() {
    if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
      mDrawerLayout.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  public void copyToClipBoard() {
    try {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      clipboard.setText(mCurrentBalance);
      showSnackbar(R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
    } catch (RuntimeException e) {
      Timber.e(e);
    }
  }

  protected boolean handleSortOption(int itemId) {
    Sort oldSort = currentSort();
    Sort newSort = Sort.fromCommandId(itemId);
    boolean result = false;
    if (newSort != null) {
      if (!newSort.equals(oldSort)) {
        PrefKey.SORT_ORDER_ACCOUNTS.putString(newSort.name());

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset()) {
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        } else {
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
        }
      }
      result = true;
      if (itemId == R.id.SORT_CUSTOM_COMMAND) {
        if (mAccountsCursor == null) {
          complainAccountsNotLoaded();
        } else {
          ArrayList<AbstractMap.SimpleEntry<Long, String>> accounts = new ArrayList<>();
          if (mAccountsCursor.moveToFirst()) {
            final int columnIndexId = mAccountsCursor.getColumnIndex(KEY_ROWID);
            final int columnIndexLabel = mAccountsCursor.getColumnIndex(KEY_LABEL);
            while (!mAccountsCursor.isAfterLast()) {
              final long id = mAccountsCursor.getLong(columnIndexId);
              if (id > 0) {
                accounts.add(new AbstractMap.SimpleEntry<>(id, mAccountsCursor.getString(columnIndexLabel)));
              }
              mAccountsCursor.moveToNext();
            }
          }
          SortUtilityDialogFragment.newInstance(accounts).show(getSupportFragmentManager(), "SORT_ACCOUNTS");
        }
      }
    }
    return result;
  }

  protected boolean handleAccountsGrouping(int itemId) {
    AccountGrouping oldGrouping = currentAccountGrouping();
    AccountGrouping newGrouping = null;

    switch (itemId) {
      case R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND:
        newGrouping = AccountGrouping.CURRENCY;
        break;
      case R.id.GROUPING_ACCOUNTS_TYPE_COMMAND:
        newGrouping = AccountGrouping.TYPE;
        break;
      case R.id.GROUPING_ACCOUNTS_NONE_COMMAND:
        newGrouping = AccountGrouping.NONE;
        break;
    }
    if (newGrouping != null && !newGrouping.equals(oldGrouping)) {
      PrefKey.ACCOUNT_GROUPING.putString(newGrouping.name());

      if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset())
        mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
      else
        mManager.initLoader(ACCOUNTS_CURSOR, null, this);
      return true;
    }
    return false;
  }

  protected boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        final Account account = Account.getInstanceFromDb(mAccountId);
        if (account != null) {
          item.setChecked(true);
          account.persistGrouping(newGrouping);
        }
      }
      return true;
    }
    return false;
  }

  protected boolean handleSortDirection(MenuItem item) {
    SortDirection newSortDirection = Utils.getSortDirectionFromMenuItemId(item.getItemId());
    if (newSortDirection != null) {
      if (!item.isChecked()) {
        final Account account = Account.getInstanceFromDb(mAccountId);
        if (account != null) {
          item.setChecked(true);
          account.persistSortDirection(newSortDirection);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldKeepProgress(int taskId) {
    return taskId == TASK_EXPORT;
  }

  @Override
  public void onSortOrderConfirmed(long[] sortedIds) {
    Bundle extras = new Bundle(1);
    extras.putLongArray(KEY_SORT_KEY, sortedIds);
    startTaskExecution(TaskExecutionFragment.TASK_ACCOUNT_SORT, extras, R.string.progress_dialog_saving);
  }

  public void clearFilter(View view) {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.clear_all_filters));
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.CLEAR_FILTER_COMMAND);
    ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "CLEAR_FILTER");
  }
}