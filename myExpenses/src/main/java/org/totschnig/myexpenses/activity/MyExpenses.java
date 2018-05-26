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
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
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
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
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
import org.totschnig.myexpenses.ui.ProtectedCursorLoader;
import org.totschnig.myexpenses.ui.SnackbarAction;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;

import javax.inject.Inject;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.KEY_LONG_IDS;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_EXPORT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_PRINT;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 */
public class MyExpenses extends LaunchActivity implements
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConfirmationDialogFragment.ConfirmationDialogCheckedListener,
    ConfirmationDialogListener, ContribIFace, SimpleDialog.OnDialogResultListener {

  public static final long TRESHOLD_REMIND_RATE = 47L;

  public static final int ACCOUNTS_CURSOR = -1;
  public static final String KEY_SEQUENCE_COUNT = "sequenceCount";

  private LoaderManager mManager;

  int mCurrentPosition = -1;
  private Cursor mAccountsCursor;

  private MyViewPagerAdapter mViewPagerAdapter;
  private MyGroupedAdapter mDrawerListAdapter;
  private ViewPager myPager;
  private long mAccountId = 0;
  private int mAccountCount = 0;

  private AdHandler adHandler;
  private Toolbar mToolbar;
  private String mCurrentBalance;
  private SubMenu sortMenu;

  public enum HelpVariant {
    crStatus
  }

  private void setHelpVariant() {
    Account account = Account.getInstanceFromDb(mAccountId);
    helpVariant = account == null || account.getType().equals(AccountType.CASH) ?
        null : HelpVariant.crStatus;
  }

  /**
   * stores the number of transactions that have been
   * created in the db, updated after each creation of
   * a new transaction
   */
  private long sequenceCount = 0;
  private StickyListHeadersListView mDrawerList;
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;

  private int columnIndexRowId, columnIndexColor, columnIndexCurrency, columnIndexLabel;
  boolean indexesCalculated = false;
  private long idFromNotification = 0;
  private String mExportFormat = null;

  @Inject
  CurrencyFormatter currencyFormatter;

  private RoadmapViewModel roadmapViewModel;

  @Override
  protected void injectDependencies() {
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    adHandler = adHandlerFactory.create(findViewById(R.id.adContainer), prefHandler);
    adHandler.init();
    adHandler.maybeRequestNewInterstitial();

    mDrawerLayout = findViewById(R.id.drawer_layout);
    mDrawerList = findViewById(R.id.left_drawer);
    mToolbar = setupToolbar(false);
    mToolbar.addView(getLayoutInflater().inflate(R.layout.custom_title, mToolbar, false));
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
          //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
        }

        /**
         * Called when a drawer has settled in a completely open state.
         */
        public void onDrawerOpened(View drawerView) {
          super.onDrawerOpened(drawerView);
          TransactionList tl = getCurrentFragment();
          if (tl != null)
            tl.onDrawerOpened();
          //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
          super.onDrawerSlide(drawerView, 0); // this disables the animation
        }
      };

      // Set the drawer toggle as the DrawerListener
      mDrawerLayout.addDrawerListener(mDrawerToggle);
    }
    mDrawerListAdapter = new MyGroupedAdapter(this, R.layout.account_row, null, currencyFormatter);

    Toolbar accountsMenu = findViewById(R.id.accounts_menu);
    accountsMenu.setTitle(R.string.pref_manage_accounts_title);
    accountsMenu.inflateMenu(R.menu.accounts);
    accountsMenu.inflateMenu(R.menu.sort);

    Menu menu = accountsMenu.getMenu();

    //Sort submenu
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    sortMenu = menuItem.getSubMenu();
    sortMenu.findItem(R.id.SORT_CUSTOM_COMMAND).setVisible(true);

    //Grouping submenu
    SubMenu groupingMenu = menu.findItem(R.id.GROUPING_ACCOUNTS_COMMAND)
        .getSubMenu();
    AccountGrouping accountGrouping;
    try {
      accountGrouping = AccountGrouping.valueOf(
          PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
    } catch (IllegalArgumentException e) {
      accountGrouping = AccountGrouping.TYPE;
    }
    MenuItem activeItem;
    switch (accountGrouping) {
      case CURRENCY:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND);
        break;
      case NONE:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_NONE_COMMAND);
        break;
      default:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_TYPE_COMMAND);
        break;
    }
    activeItem.setChecked(true);

    accountsMenu.setOnMenuItemClickListener(item -> handleSortOption(item) || handleAccountsGrouping(item) ||
        dispatchCommand(item.getItemId(), null));

    mDrawerList.setAdapter(mDrawerListAdapter);
    mDrawerList.setAreHeadersSticky(false);
    mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
      if (mAccountId != id) {
        moveToPosition(position);
        closeDrawer();
      }
    });

    requireFloatingActionButtonWithContentDescription(Utils.concatResStrings(this, ". ",
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
    setup();

    roadmapViewModel = ViewModelProviders.of(this).get(RoadmapViewModel.class);
    roadmapViewModel.getVoteReminder().observe(this, reminderResId -> {
      if (reminderResId != null) {
        PrefKey.VOTE_REMINDER_SHOWN.putBoolean(true);
        showSnackbar(getString(reminderResId), Snackbar.LENGTH_INDEFINITE, new SnackbarAction(R.string.roadmap_vote, v -> {
          Intent intent = new Intent(this, RoadmapVoteActivity.class);
          startActivity(intent);
        }));
      }
    });
    roadmapViewModel.loadVoteReminder();
  }

  private void setup() {
    newVersionCheck();
    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin, margin, true);
    mViewPagerAdapter = new MyViewPagerAdapter(this, getSupportFragmentManager(), null);
    myPager = findViewById(R.id.viewpager);
    myPager.setAdapter(this.mViewPagerAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin((int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
    myPager.setPageMarginDrawable(margin.resourceId);
    mManager = getSupportLoaderManager();
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }

  private void moveToPosition(int position) {
    if (myPager.getCurrentItem() == position)
      setCurrentAccount(position);
    else
      myPager.setCurrentItem(position, false);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem balanceItem = menu.findItem(R.id.BALANCE_COMMAND);
    if (balanceItem != null) {
      boolean showBalanceCommand = false;
      if (mAccountId > 0 && mAccountsCursor != null && !mAccountsCursor.isClosed() &&
          mAccountsCursor.moveToPosition(mCurrentPosition)) {
        try {
          if (AccountType.valueOf(mAccountsCursor.getString(mAccountsCursor.getColumnIndexOrThrow(KEY_TYPE)))
              != AccountType.CASH) {
            showBalanceCommand = true;
          }
        } catch (IllegalArgumentException ex) {/*aggregate*/}
      }
      Utils.menuItemSetEnabledAndVisible(balanceItem, showBalanceCommand);
    }

    Account account = Account.getInstanceFromDb(mAccountId);

    MenuItem groupingItem = menu.findItem(R.id.GROUPING_COMMAND);
    if (groupingItem != null) {
      SubMenu groupingMenu = groupingItem.getSubMenu();
      if (account != null) {
        Utils.configureGroupingMenu(groupingMenu, account.getGrouping());
      }
    }

    MenuItem sortDirectionItem = menu.findItem(R.id.SORT_DIRECTION_COMMAND);
    if (sortDirectionItem != null) {
      SubMenu sortDirectionMenu = sortDirectionItem.getSubMenu();
      if (account != null) {
        Utils.configureSortDirectionMenu(sortDirectionMenu, account.getSortDirection());
      }
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
    inflater.inflate(R.menu.grouping, menu);
    super.onCreateOptionsMenu(menu);
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
    if (requestCode == EDIT_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
      long nextReminder;
      sequenceCount = intent.getLongExtra(KEY_SEQUENCE_COUNT, 0);
      if (!DistribHelper.isGithub()) {
        nextReminder =
            PrefKey.NEXT_REMINDER_RATE.getLong(TRESHOLD_REMIND_RATE);
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
      mAccountId = intent.getLongExtra(KEY_ROWID, 0);
    }
  }

  public void addFilterCriteria(Integer id, Criteria c) {
    TransactionList tl = getCurrentFragment();
    if (tl != null) {
      tl.addFilterCriteria(id, c);
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
    if (mAccountId < 0 && mAccountsCursor != null && mAccountsCursor.moveToPosition(mCurrentPosition)) {
      i.putExtra(KEY_CURRENCY, mAccountsCursor.getString(columnIndexCurrency));
      i.putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true);
    } else {
      //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
      i.putExtra(KEY_ACCOUNTID, mAccountId);
    }
    startActivityForResult(i, EDIT_TRANSACTION_REQUEST);
  }

  /**
   * @param command
   * @param tag
   * @return true if command has been handled
   */
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    TransactionList tl;
    switch (command) {

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
        createRow();
        return true;
      case R.id.BALANCE_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && hasCleared()) {
          mAccountsCursor.moveToPosition(mCurrentPosition);
          Currency currency = Utils.getSaveInstance(mAccountsCursor.getString(columnIndexCurrency));
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
      case R.id.BACKUP_COMMAND:
        startActivity(new Intent("myexpenses.intent.backup"));
        return true;
      case R.id.REMIND_NO_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(-1);
        return true;
      case R.id.REMIND_LATER_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(sequenceCount + TRESHOLD_REMIND_RATE);
        return true;
      case R.id.HELP_COMMAND_DRAWER:
        i = new Intent(this, Help.class);
        i.putExtra(Help.KEY_CONTEXT, "NavigationDrawer");
        //for result is needed since it allows us to inspect the calling activity
        startActivity(i);
        return true;
      case R.id.HELP_COMMAND:
        setHelpVariant();
        break;
      case R.id.MANAGE_PLANS_COMMAND:
        i = new Intent(this, ManageTemplates.class);
        startActivity(i);
        return true;
      case R.id.CREATE_ACCOUNT_COMMAND:
        if (mAccountCount == 0) {
          showSnackbar(R.string.account_list_not_yet_loaded, Snackbar.LENGTH_LONG);
        }
        //we need the accounts to be loaded in order to evaluate if the limit has been reached
        else if (ContribFeature.ACCOUNTS_UNLIMITED.hasAccess() || mAccountCount < 5) {
          closeDrawer();
          i = new Intent(this, AccountEdit.class);
          if (tag != null)
            i.putExtra(KEY_CURRENCY, (String) tag);
          startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
        } else {
          CommonCommands.showContribDialog(this, ContribFeature.ACCOUNTS_UNLIMITED, null);
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND_DO:
        //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
        mAccountId = 0;
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_ACCOUNT,
            new Long[]{(Long) tag},
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
      case R.id.QUIT_COMMAND:
        finish();
        return true;
      case R.id.EDIT_ACCOUNT_COMMAND:
        closeDrawer();
        long accountId = (Long) tag;
        if (accountId > 0) { //do nothing if accidentally we are positioned at an aggregate account
          i = new Intent(this, AccountEdit.class);
          i.putExtra(KEY_ROWID, accountId);
          startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND:
        closeDrawer();
        accountId = (Long) tag;
        //do nothing if accidentally we are positioned at an aggregate account or try to delete the last account
        if (mAccountsCursor.getCount() > 1 && accountId > 0) {
          final Account account = Account.getInstanceFromDb(accountId);
          if (account != null) {
            MessageDialogFragment.newInstance(
                R.string.dialog_title_warning_delete_account,
                getString(R.string.warning_delete_account, account.getLabel()),
                new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                    accountId),
                null,
                MessageDialogFragment.Button.noButton())
                .show(getSupportFragmentManager(), "DELETE_ACCOUNT");
          }
        }
        return true;
    }
    return super.dispatchCommand(command, tag);
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
      long accountId = cursor.getLong(columnIndexRowId);
      //calling the constructors, puts the objects into the cache from where the fragment can
      //retrieve it, without needing to create a new cursor
      Account.fromCacheOrFromCursor(cursor);
      return TransactionList.newInstance(accountId);
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
      ContextualActionBarFragment f =
          (ContextualActionBarFragment) getSupportFragmentManager().findFragmentByTag(
              mViewPagerAdapter.getFragmentName(mCurrentPosition));
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
        Intent i = new Intent(this, ManageCategories.class);
        i.setAction("myexpenses.intent.distribution");
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
        Account a = Account.getInstanceFromDb(mAccountId);
        if (a != null) {
          recordUsage(feature);
          Intent i = new Intent(this, HistoryActivity.class);
          i.putExtra(KEY_ACCOUNTID, mAccountId);
          startActivity(i);
          break;
        }
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
          args.putSparseParcelableArray(TransactionList.KEY_FILTER, tl.getFilterCriteria());
          args.putLong(KEY_ROWID, mAccountId);
          getSupportFragmentManager().beginTransaction()
              .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_PRINT), ASYNC_TAG)
              .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_printing), PROGRESS_TAG)
              .commit();
        }
        break;
      }
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch (id) {
      case ACCOUNTS_CURSOR:
        Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
        return new ProtectedCursorLoader(this, builder.build());
    }
    return null;
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
      int color700 = UiUtils.get700Tint(color);
      window.setStatusBarColor(color700);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //noinspection InlinedApi
        getWindow().getDecorView().setSystemUiVisibility(
            UiUtils.isBrightColor(color700) ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
      }
    }
    UiUtils.setBackgroundTintListOnFab(floatingActionButton, color);
    mAccountId = newAccountId;
    setBalance();
    mDrawerList.setItemChecked(position, true);
    supportInvalidateOptionsMenu();
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    switch (loader.getId()) {
      case ACCOUNTS_CURSOR:
        //we postpone this until cursor is loaded, because prefkey is updated in migration to db schema 56
        Utils.configureSortMenu(sortMenu, PrefKey.SORT_ORDER_ACCOUNTS.getString("USAGES"));
        mAccountCount = 0;
        mAccountsCursor = cursor;
        if (mAccountsCursor == null) {
          return;
        }
        //when account grouping is changed in setting, cursor is reloaded,
        //and we need to refresh the value here
        AccountGrouping grouping;
        try {
          grouping = AccountGrouping.valueOf(
              PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
        } catch (IllegalArgumentException e) {
          grouping = AccountGrouping.TYPE;
        }

        mDrawerListAdapter.setGrouping(grouping);
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
          //should be triggered through onPageSelected
          //setCurrentAccount(mCurrentPosition);
        }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    if (arg0.getId() == ACCOUNTS_CURSOR) {
      mViewPagerAdapter.swapCursor(null);
      mDrawerListAdapter.swapCursor(null);
      mCurrentPosition = -1;
      mAccountsCursor = null;
    }
  }

  @Override
  public void onPageScrollStateChanged(int arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (TransactionList.NEW_TEMPLATE_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      String label = extras.getString(SimpleInputDialog.TEXT);
      Uri uri = new Template(Transaction.getInstanceFromDb(extras.getLong(KEY_ROWID)), label).save();
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
    if (TransactionList.FILTER_COMMENT_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      addFilterCriteria(R.id.FILTER_COMMENT_COMMAND,
          new CommentCriteria(extras.getString(SimpleInputDialog.TEXT)));
      return true;
    }
    return false;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    String msg;
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TaskExecutionFragment.TASK_SPLIT:
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
    long balance = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex
        (KEY_CURRENT_BALANCE));
    mCurrentBalance = currencyFormatter.formatCurrency(new Money(Utils.getSaveInstance(mAccountsCursor
        .getString(columnIndexCurrency)), balance));
    TextView balanceTextView = mToolbar.findViewById(R.id.end);
    balanceTextView.setTextColor(balance < 0 ? colorExpense : colorIncome);
    balanceTextView.setText(mCurrentBalance);
  }

  public void setTitle(String title) {
    ((TextView) mToolbar.findViewById(R.id.action_bar_title)).setText(title);
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
        args.putSparseParcelableArray(TransactionList.KEY_FILTER,
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

  public void copyToClipBoard(View view) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setText(mCurrentBalance);
    showSnackbar(R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
  }

  protected boolean handleSortOption(MenuItem item) {
    String newSortOrder = Utils.getSortOrderFromMenuItemId(item.getItemId());
    if (newSortOrder != null) {
      if (!item.isChecked()) {
        PrefKey.SORT_ORDER_ACCOUNTS.putString(newSortOrder);
        item.setChecked(true);

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset()) {
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        } else {
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
        }
        if (item.getItemId() == R.id.SORT_CUSTOM_COMMAND) {
          showMessage(R.string.dialog_title_information,
              getString(R.string.dialog_info_custom_sort));
        }
      }
      return true;
    }
    return false;
  }

  protected boolean handleAccountsGrouping(MenuItem item) {
    AccountGrouping newGrouping = null;
    switch (item.getItemId()) {
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
    if (newGrouping != null) {
      if (!item.isChecked()) {
        PrefKey.ACCOUNT_GROUPING.putString(newGrouping.name());
        item.setChecked(true);

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset())
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        else
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
      }
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

}