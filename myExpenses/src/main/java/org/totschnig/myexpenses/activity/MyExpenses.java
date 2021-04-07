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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;

import com.annimon.stream.Stream;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.MyGroupedAdapter;
import org.totschnig.myexpenses.databinding.ActivityMainBinding;
import org.totschnig.myexpenses.dialog.BalanceDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.ExportDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.select.SelectFilterDialog;
import org.totschnig.myexpenses.dialog.select.SelectHiddenAccountDialogFragment;
import org.totschnig.myexpenses.feature.Feature;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Sort;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.preference.PreferenceUtilsKt;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.ui.SnackbarAction;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import eltos.simpledialogfragment.list.MenuDialog;
import kotlin.Unit;
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
import static eltos.simpledialogfragment.list.CustomListDialog.SELECTED_SINGLE_ID;
import static org.totschnig.myexpenses.activity.ConstantsKt.CREATE_ACCOUNT_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.EDIT_ACCOUNT_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.EDIT_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.OCR_REQUEST;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.preference.PrefKey.OCR;
import static org.totschnig.myexpenses.preference.PreferenceUtilsKt.requireString;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_CLEARED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.KEY_LONG_IDS;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_BALANCE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_EXPORT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_PRINT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_REVOKE_SPLIT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SET_ACCOUNT_HIDDEN;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SET_ACCOUNT_SEALED;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SPLIT;
import static org.totschnig.myexpenses.viewmodel.MyExpensesViewModelKt.ERROR_INIT_DOWNGRADE;

/**
 * This is the main activity where all expenses are listed
 * From the menu sub activities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 */
public class MyExpenses extends BaseMyExpenses implements
    ViewPager.OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConfirmationDialogFragment.ConfirmationDialogCheckedListener,
    ConfirmationDialogListener, SortUtilityDialogFragment.OnConfirmListener, SelectFilterDialog.Host {

  public static final int ACCOUNTS_CURSOR = -1;
  private static final String DIALOG_TAG_GROUPING = "GROUPING";
  private static final String DIALOG_TAG_SORTING = "SORTING";
  private static final String MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN";

  private LoaderManager mManager;
  private ActivityMainBinding binding;

  private MyViewPagerAdapter mViewPagerAdapter;
  private MyGroupedAdapter mDrawerListAdapter;
  private int mAccountCount = 0;

  private AdHandler adHandler;
  private AccountGrouping accountGrouping;
  private Sort accountSort;

  public void toggleScanMode() {
    final boolean oldMode = prefHandler.getBoolean(OCR, false);
    final boolean newMode = !oldMode;
    if (newMode) {
      contribFeatureRequested(ContribFeature.OCR, false);
    } else {
      prefHandler.putBoolean(OCR, newMode);
      updateFab();
      invalidateOptionsMenu();
    }
  }

  ExpandableStickyListHeadersListView accountList() {
    return binding.accountPanel.accountList;
  }

  ViewPager viewPager() {
    return binding.viewPagerMain.viewPager;
  }

  NavigationView navigationView() {
    return binding.accountPanel.expansionContent;
  }
  private ActionBarDrawerToggle mDrawerToggle;

  boolean indexesCalculated = false;

  private RoadmapViewModel roadmapViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    final ViewGroup adContainer = findViewById(R.id.adContainer);
    accountGrouping = readAccountGroupingFromPref();
    accountSort = readAccountSortFromPref();
    adHandler = adHandlerFactory.create(adContainer, this);
    adContainer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {

          @Override
          public void onGlobalLayout() {
            adContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            adHandler.startBanner();
          }
        });

    try {
      adHandler.maybeRequestNewInterstitial();
    } catch (Exception e) {
      CrashHandler.report(e);
    }

    toolbar = setupToolbar(false);
    toolbar.setVisibility(View.INVISIBLE);
    setupToolbarPopupMenu();
    if (binding.drawer != null) {
      mDrawerToggle = new ActionBarDrawerToggle(this, binding.drawer,
          toolbar, R.string.drawer_open, R.string.drawer_close) {

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
      binding.drawer.addDrawerListener(mDrawerToggle);
    }
    mDrawerListAdapter = new MyGroupedAdapter(this, null, currencyFormatter, prefHandler, currencyContext);

    navigationView().setNavigationItemSelectedListener(item -> dispatchCommand(item.getItemId(), null));
    View navigationMenuView = navigationView().getChildAt(0);
    if (navigationMenuView != null) {
      navigationMenuView.setVerticalScrollBarEnabled(false);
    }

    accountList().setAdapter(mDrawerListAdapter);
    accountList().setAreHeadersSticky(false);
    accountList().setOnHeaderClickListener(new StickyListHeadersListView.OnHeaderClickListener() {
      @Override
      public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
        if (accountList().isHeaderCollapsed(headerId)) {
          accountList().expand(headerId);
        } else {
          accountList().collapse(headerId);
        }
        persistCollapsedHeaderIds();
      }

      @Override
      public boolean onHeaderLongClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
        return false;
      }
    });
    accountList().setOnItemClickListener((parent, view, position, id) -> {
      if (accountId != id) {
        moveToPosition(position);
        closeDrawer();
      }
    });
    registerForContextMenu(accountList());
    accountList().setFastScrollEnabled(prefHandler.getBoolean(PrefKey.ACCOUNT_LIST_FAST_SCROLL, false));

    updateFab();
    setupFabSubMenu();
    if (!isScanMode()) {
      floatingActionButton.setVisibility(View.INVISIBLE);
    }
    if (savedInstanceState == null) {
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        accountId = Utils.getFromExtra(extras, KEY_ROWID, 0);
        showTransactionFromIntent(extras);
      }
    }
    if (accountId == 0) {
      accountId = prefHandler.getLong(PrefKey.CURRENT_ACCOUNT, 0L);
    }
    roadmapViewModel = new ViewModelProvider(this).get(RoadmapViewModel.class);
    viewModel.getHasHiddenAccounts().observe(this,
        result -> navigationView().getMenu().findItem(R.id.HIDDEN_ACCOUNTS_COMMAND).setVisible(result != null && result));
    if (savedInstanceState != null) {
      setup(false);
    } else {
      newVersionCheck();
      viewModel.initialize().observe(this, result -> {
        if (result == 0) {
          setup(true);
        } else {
          showMessage(result == ERROR_INIT_DOWNGRADE ? "Database cannot be downgraded from a newer version. Please either uninstall MyExpenses, before reinstalling, or upgrade to a new version." :
              "Database upgrade failed. Please contact support@myexpenses.mobi !", new MessageDialogFragment.Button(android.R.string.ok, R.id.QUIT_COMMAND, null),
              null,
              null, false);
        }
      });
    }
    if (savedInstanceState == null) {
      //voteReminderCheck();
      voteReminderCheck2();
    }
    reviewManager.init(this);
  }

  public void showTransactionFromIntent(Bundle extras) {
    long idFromNotification = extras.getLong(KEY_TRANSACTIONID, 0);
    //detail fragment from notification should only be shown upon first instantiation from notification
    if (idFromNotification != 0) {
      FragmentManager fm = getSupportFragmentManager();
      TransactionDetailFragment.newInstance(idFromNotification)
          .show(fm, TransactionDetailFragment.class.getName());
      getIntent().removeExtra(KEY_TRANSACTIONID);
    }
  }

  public void persistCollapsedHeaderIds() {
    PreferenceUtilsKt.putLongList(prefHandler, collapsedHeaderIdsPrefKey(), accountList().getCollapsedHeaderIds());
  }

  private String collapsedHeaderIdsPrefKey() {
    return "collapsedHeadersDrawer_" + accountGrouping.name();
  }

  private void setup(boolean firstCreate) {
    viewModel.loadHiddenAccountCount();
    mManager = LoaderManager.getInstance(this);
    if (firstCreate) {
      mManager.initLoader(ACCOUNTS_CURSOR, null, this);
    }

  }

  private void voteReminderCheck() {
    final String prefKey = "vote_reminder_shown_" + RoadmapViewModel.EXPECTED_MINIMAL_VERSION;
    if (Utils.getDaysSinceUpdate(this) > 1 &&
        !prefHandler.getBoolean(prefKey, false)) {
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
    }
  }

  private void voteReminderCheck2() {
    roadmapViewModel.getShouldShowVoteReminder().observe(this, shouldShow -> {
      if (shouldShow) {
        prefHandler.putLong(PrefKey.VOTE_REMINDER_LAST_CHECK, System.currentTimeMillis());
        showSnackbar(getString(R.string.reminder_vote_update), Snackbar.LENGTH_INDEFINITE, new SnackbarAction(R.string.roadmap_vote, v -> {
          Intent intent = new Intent(this, RoadmapVoteActivity.class);
          startActivity(intent);
        }));
      }
    });
  }

  private void moveToPosition(int position) {
    if (viewPager().getCurrentItem() == position)
      setCurrentAccount(position);
    else
      viewPager().setCurrentItem(position, false);
  }

  private AccountGrouping readAccountGroupingFromPref() {
    try {
      return AccountGrouping.valueOf(
          prefHandler.getString(PrefKey.ACCOUNT_GROUPING, AccountGrouping.TYPE.name()));
    } catch (IllegalArgumentException e) {
      return AccountGrouping.TYPE;
    }
  }

  private Sort readAccountSortFromPref() {
    try {
      return Sort.valueOf(
          prefHandler.getString(PrefKey.SORT_ORDER_ACCOUNTS, Sort.USAGES.name()));
    } catch (IllegalArgumentException e) {
      return Sort.USAGES;
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    if (((AdapterView.AdapterContextMenuInfo) menuInfo).id > 0) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.accounts_context, menu);
      getAccountsCursor().moveToPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
      final boolean isSealed = getAccountsCursor().getInt(getAccountsCursor().getColumnIndex(KEY_SEALED)) == 1;
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
    if (requestCode == EDIT_REQUEST) {
      floatingActionButton.show();
      if (resultCode == RESULT_OK) {
        if (!adHandler.onEditTransactionResult()) {
          reviewManager.onEditTransactionResult(this);
        }
      }
    }
    if (requestCode == CREATE_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
      //navigating to the new account currently does not work, due to the way LoaderManager behaves
      //since its implementation is based on MutableLiveData
      accountId = intent.getLongExtra(KEY_ROWID, 0);
    }
    if (requestCode == CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        ocrViewModel.startOcrFeature(scanFile, getSupportFragmentManager());
      } else  {
        processImageCaptureError(resultCode, CropImage.getActivityResult(intent));
      }
    }
    if (requestCode == OCR_REQUEST) {
      ocrViewModel.handleOcrData(intent, getSupportFragmentManager());
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
   * @return true if command has been handled
   */
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    Intent i;
    TransactionList tl;
    if (command == R.id.BUDGET_COMMAND) {
      contribFeatureRequested(ContribFeature.BUDGET, null);
      return true;
    } else if (command == R.id.DISTRIBUTION_COMMAND) {
      tl = getCurrentFragment();
      if (tl != null && tl.hasMappedCategories()) {
        contribFeatureRequested(ContribFeature.DISTRIBUTION, null);
      } else {
        showMessage(R.string.dialog_command_disabled_distribution);
      }
      return true;
    } else if (command == R.id.HISTORY_COMMAND) {
      tl = getCurrentFragment();
      if (tl != null && tl.hasItems()) {
        contribFeatureRequested(ContribFeature.HISTORY, null);
      } else {
        showMessage(R.string.no_expenses);
      }
      return true;
    } else if (command == R.id.CREATE_COMMAND) {
      if (mAccountCount == 0) {
        showSnackbar(R.string.warning_no_account);
      } else {
        if (isScanMode()) {
          contribFeatureRequested(ContribFeature.OCR, true);
        } else {
          createRowDo(TYPE_TRANSACTION, false);
        }
      }
      return true;
    } else if (command == R.id.BALANCE_COMMAND) {
      tl = getCurrentFragment();
      if (tl != null && hasCleared()) {
        getAccountsCursor().moveToPosition(getCurrentPosition());
        CurrencyUnit currency = getCurrentCurrencyUnit();
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_ROWID,
            getAccountsCursor().getLong(getColumnIndexRowId()));
        bundle.putString(KEY_LABEL,
            getAccountsCursor().getString(getColumnIndexLabel()));
        bundle.putString(KEY_RECONCILED_TOTAL,
            currencyFormatter.formatCurrency(
                new Money(currency,
                    getAccountsCursor().getLong(getAccountsCursor().getColumnIndex(KEY_RECONCILED_TOTAL)))));
        bundle.putString(KEY_CLEARED_TOTAL, currencyFormatter.formatCurrency(
            new Money(currency,
                getAccountsCursor().getLong(getAccountsCursor().getColumnIndex(KEY_CLEARED_TOTAL)))));
        BalanceDialogFragment.newInstance(bundle)
            .show(getSupportFragmentManager(), "BALANCE_ACCOUNT");
      } else {
        showMessage(R.string.dialog_command_disabled_balance);
      }
      return true;
    } else if (command == R.id.RESET_COMMAND) {
      tl = getCurrentFragment();
      if (tl != null && tl.hasItems()) {
        Result appDirStatus = AppDirHelper.checkAppDir(this);
        if (appDirStatus.isSuccess()) {
          ExportDialogFragment.newInstance(accountId, tl.isFiltered())
              .show(this.getSupportFragmentManager(), "WARNING_RESET");
        } else {
          showSnackbar(appDirStatus.print(this));
        }
      } else {
        showExportDisabledCommand();
      }
      return true;
    } else if (command == R.id.HELP_COMMAND_DRAWER) {
      i = new Intent(this, Help.class);
      i.putExtra(Help.KEY_CONTEXT, "NavigationDrawer");
      //for result is needed since it allows us to inspect the calling activity
      startActivity(i);
      return true;
    } else if (command == R.id.MANAGE_TEMPLATES_COMMAND) {
      i = new Intent(this, ManageTemplates.class);
      startActivity(i);
      return true;
    } else if (command == R.id.CREATE_ACCOUNT_COMMAND) {
      if (getAccountsCursor() == null) {
        complainAccountsNotLoaded();
      }
      //we need the accounts to be loaded in order to evaluate if the limit has been reached
      else if (licenceHandler.hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED) || mAccountCount < ContribFeature.FREE_ACCOUNTS) {
        closeDrawer();
        i = new Intent(this, AccountEdit.class);
        if (tag != null)
          i.putExtra(KEY_CURRENCY, (String) tag);
        startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
      } else {
        showContribDialog(ContribFeature.ACCOUNTS_UNLIMITED, null);
      }
      return true;
    } else if (command == R.id.DELETE_ACCOUNT_COMMAND_DO) {//reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
      final Long[] accountIds = (Long[]) tag;
      if (Stream.of(accountIds).anyMatch(id -> id == accountId)) {
        accountId = 0;
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
    } else if (command == R.id.SHARE_COMMAND) {
      i = new Intent();
      i.setAction(Intent.ACTION_SEND);
      i.putExtra(Intent.EXTRA_TEXT, Utils.getTellAFriendMessage(this).toString());
      i.setType("text/plain");
      startActivity(Intent.createChooser(i, getResources().getText(R.string.menu_share)));
      return true;
    } else if (command == R.id.CANCEL_CALLBACK_COMMAND) {
      finishActionMode();
      return true;
    } else if (command == R.id.OPEN_PDF_COMMAND) {
      i = new Intent();
      i.setAction(Intent.ACTION_VIEW);
      Uri data = AppDirHelper.ensureContentUri(Uri.parse((String) tag));
      i.setDataAndType(data, "application/pdf");
      i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(i, R.string.no_app_handling_pdf_available, null);
      return true;
    } else if (command == R.id.SHARE_PDF_COMMAND) {
      Result shareResult = ShareUtils.share(this,
          Collections.singletonList(AppDirHelper.ensureContentUri(Uri.parse((String) tag))),
          getShareTarget(),
          "application/pdf");
      if (!shareResult.isSuccess()) {
        showSnackbar(shareResult.print(this));
      }
      return true;
    } else if (command == R.id.EDIT_ACCOUNT_COMMAND) {
      closeDrawer();
      long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
      if (accountId > 0) { //do nothing if accidentally we are positioned at an aggregate account
        i = new Intent(this, AccountEdit.class);
        i.putExtra(KEY_ROWID, accountId);
        startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
      }
      return true;
    } else if (command == R.id.DELETE_ACCOUNT_COMMAND) {
      closeDrawer();
      long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
      //do nothing if accidentally we are positioned at an aggregate account
      if (accountId > 0) {
        getAccountsCursor().moveToPosition(((AdapterView.AdapterContextMenuInfo) tag).position);
        String label = getAccountsCursor().getString(getColumnIndexLabel());
        MessageDialogFragment.newInstance(
            getResources().getQuantityString(R.plurals.dialog_title_warning_delete_account, 1, 1),
            getString(R.string.warning_delete_account, label) + " " + getString(R.string.continue_confirmation),
            new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                new Long[]{accountId}),
            null,
            MessageDialogFragment.noButton(), 0)
            .show(getSupportFragmentManager(), "DELETE_ACCOUNT");
      }
      return true;
    } else if (command == R.id.GROUPING_ACCOUNTS_COMMAND) {
      MenuDialog.build()
          .menu(this, R.menu.accounts_grouping)
          .choiceIdPreset(accountGrouping.commandId)
          .title(R.string.menu_grouping)
          .show(this, DIALOG_TAG_GROUPING);
      return true;
    } else if (command == R.id.SORT_COMMAND) {
      MenuDialog.build()
          .menu(this, R.menu.accounts_sort)
          .choiceIdPreset(accountSort.getCommandId())
          .title(R.string.menu_sort)
          .show(this, DIALOG_TAG_SORTING);
      return true;
    } else if (command == R.id.CLEAR_FILTER_COMMAND) {
      getCurrentFragment().clearFilter();
      return true;
    } else if (command == R.id.ROADMAP_COMMAND) {
      Intent intent = new Intent(this, RoadmapVoteActivity.class);
      startActivity(intent);
      return true;
    } else if (command == R.id.CLOSE_ACCOUNT_COMMAND) {
      long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
      //do nothing if accidentally we are positioned at an aggregate account
      if (accountId > 0) {
        getAccountsCursor().moveToPosition(((AdapterView.AdapterContextMenuInfo) tag).position);
        if (getAccountsCursor().getString(getAccountsCursor().getColumnIndex(KEY_SYNC_ACCOUNT_NAME)) == null) {
          startTaskExecution(
              TASK_SET_ACCOUNT_SEALED,
              new Long[]{accountId},
              true, 0);
        } else {
          showSnackbar(getString(R.string.warning_synced_account_cannot_be_closed),
              Snackbar.LENGTH_LONG, null, null, accountList());
        }
      }
      return true;
    } else if (command == R.id.REOPEN_ACCOUNT_COMMAND) {
      long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
      //do nothing if accidentally we are positioned at an aggregate account
      if (accountId > 0) {
        startTaskExecution(
            TASK_SET_ACCOUNT_SEALED,
            new Long[]{accountId},
            false, 0);
      }
      return true;
    } else if (command == R.id.HIDE_ACCOUNT_COMMAND) {
      long accountId = ((AdapterView.AdapterContextMenuInfo) tag).id;
      //do nothing if accidentally we are positioned at an aggregate account
      if (accountId > 0) {
        startTaskExecution(
            TASK_SET_ACCOUNT_HIDDEN,
            new Long[]{accountId},
            true, 0);
      }
      return true;
    } else if (command == R.id.HIDDEN_ACCOUNTS_COMMAND) {
      SelectHiddenAccountDialogFragment.newInstance().show(getSupportFragmentManager(),
          MANAGE_HIDDEN_FRAGMENT_TAG);
      return true;
    } else if (command == R.id.OCR_FAQ_COMMAND) {
      startActionView("https://github.com/mtotschnig/MyExpenses/wiki/FAQ:-OCR");
      return true;
    }
    return false;
  }

  public String getShareTarget() {
    return requireString(prefHandler, PrefKey.SHARE_TARGET, "").trim();
  }

  private void complainAccountsNotLoaded() {
    showSnackbar(R.string.account_list_not_yet_loaded);
  }

  public void showExportDisabledCommand() {
    showMessage(R.string.dialog_command_disabled_reset_account);
  }

  private void closeDrawer() {
    if (binding.drawer != null) binding.drawer.closeDrawers();
  }

  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    String getFragmentName(int currentPosition) {
      return FragmentPagerAdapter.makeFragmentName(viewPager().getId(), getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
      return TransactionList.newInstance(cursor.getLong(getColumnIndexRowId()));
    }
  }

  @Override
  public void onPageSelected(int position) {
    finishActionMode();
    setCurrentPosition(position);
    setCurrentAccount(position);
  }

  public void finishActionMode() {
    if (getCurrentPosition() != -1) {
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
        getAccountsCursor().moveToPosition(getCurrentPosition());
        recordUsage(feature);
        Intent i = new Intent(this, Distribution.class);
        i.putExtra(KEY_ACCOUNTID, accountId);
        i.putExtra(KEY_GROUPING, getAccountsCursor().getString(getColumnIndexGrouping()));
        if (tag != null) {
          int year = (int) ((Long) tag / 1000);
          int groupingSecond = (int) ((Long) tag % 1000);
          i.putExtra(KEY_YEAR, year);
          i.putExtra(KEY_SECOND_GROUP, groupingSecond);
        }
        startActivity(i);
        break;
      }
      case HISTORY: {
        recordUsage(feature);
        getAccountsCursor().moveToPosition(getCurrentPosition());
        Intent i = new Intent(this, HistoryActivity.class);
        i.putExtra(KEY_ACCOUNTID, accountId);
        i.putExtra(KEY_GROUPING, getAccountsCursor().getString(getColumnIndexGrouping()));
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
        } else {
          createRowDo(TYPE_SPLIT, false);
        }
        break;
      }
      case PRINT: {
        TransactionList tl = getCurrentFragment();
        if (tl != null) {
          Bundle args = new Bundle();
          args.putParcelableArrayList(TransactionList.KEY_FILTER, tl.getFilterCriteria());
          args.putLong(KEY_ROWID, accountId);
          if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager().beginTransaction()
                .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_PRINT), ASYNC_TAG)
                .add(ProgressDialogFragment.newInstance(getString(R.string.progress_dialog_printing)), PROGRESS_TAG)
                .commit();
          }
        }
        break;
      }
      case BUDGET: {
        if (accountId != 0 && getCurrentCurrency() != null) {
          recordUsage(feature);
          Intent i = new Intent(this, ManageBudgets.class);
          startActivity(i);
        }
        break;
      }
      case OCR: {
        if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
          if ((Boolean) tag) {
        /*scanFile = new File("/sdcard/OCR_bg.jpg");
        ocrViewModel.startOcrFeature(scanFile, getSupportFragmentManager());*/
            ocrViewModel.getScanFiles(pair -> {
              scanFile = pair.getSecond();
              CropImage.activity()
                  .setCameraOnly(true)
                  .setAllowFlipping(false)
                  .setOutputUri(Uri.fromFile(scanFile))
                  .setCaptureImageOutputUri(ocrViewModel.getScanUri(pair.getFirst()))
                  .setGuidelines(CropImageView.Guidelines.ON)
                  .start(this);
              return Unit.INSTANCE;
            });
          } else {
            activateOcrMode();
          }
        } else {
          featureViewModel.requestFeature(this, Feature.OCR);
        }
      }
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
    if (!DistributionHelper.isGithub() && feature == ContribFeature.AD_FREE) {
      finish();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    if (id == ACCOUNTS_CURSOR) {
      Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
      builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
      return new CursorLoader(this, builder.build(), null, KEY_HIDDEN + " = 0", null, null);
    }
    throw new IllegalStateException("Unknown loader id " + id);
  }

  /**
   * set the Current account to the one in the requested position of mAccountsCursor
   */
  private void setCurrentAccount(int position) {
    getAccountsCursor().moveToPosition(position);
    long newAccountId = getAccountsCursor().getLong(getColumnIndexRowId());
    if (accountId != newAccountId) {
      prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, newAccountId);
    }
    tintSystemUiAndFab(newAccountId < 0 ? getResources().getColor(R.color.colorAggregate) : getAccountsCursor().getInt(getColumnIndexColor()));

    accountId = newAccountId;
    setCurrentCurrency(getAccountsCursor().getString(getColumnIndexCurrency()));
    setBalance();
    if (getAccountsCursor().getInt(getAccountsCursor().getColumnIndex(KEY_SEALED)) == 1) {
      floatingActionButton.hide();
    } else {
      floatingActionButton.show();
    }
    accountList().setItemChecked(position, true);
    supportInvalidateOptionsMenu();
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    if (loader.getId() == ACCOUNTS_CURSOR) {
      mAccountCount = 0;
      setAccountsCursor(cursor);
      if (getAccountsCursor() == null) {
        return;
      }

      mDrawerListAdapter.setGrouping(accountGrouping);
      accountList().setCollapsedHeaderIds(PreferenceUtilsKt.getLongList(prefHandler, collapsedHeaderIdsPrefKey()));
      mDrawerListAdapter.swapCursor(cursor);
      //swapping the cursor is altering the accountId, if the
      //sort order has changed, but we want to move to the same account as before
      long cacheAccountId = accountId;
      setupViewPager(cursor);
      accountId = cacheAccountId;
      if (!indexesCalculated) {
        setColumnIndexRowId(cursor.getColumnIndex(KEY_ROWID));
        setColumnIndexColor(cursor.getColumnIndex(KEY_COLOR));
        setColumnIndexCurrency(cursor.getColumnIndex(KEY_CURRENCY));
        setColumnIndexLabel(cursor.getColumnIndex(KEY_LABEL));
        setColumnIndexGrouping(cursor.getColumnIndex(KEY_GROUPING));
        setColumnIndexType(cursor.getColumnIndex(KEY_TYPE));
        indexesCalculated = true;
      }
      moveToAccount();
      toolbar.setVisibility(View.VISIBLE);
    }
  }

  public void setupViewPager(Cursor cursor) {
    if (mViewPagerAdapter == null) {
      mViewPagerAdapter = new MyViewPagerAdapter(this, getSupportFragmentManager(), cursor);
      viewPager().setAdapter(mViewPagerAdapter);
      viewPager().addOnPageChangeListener(this);
      viewPager().setPageMargin(UiUtils.dp2Px(10, getResources()));
      viewPager().setPageMarginDrawable(new ColorDrawable(UiUtils.getColor(this, R.attr.colorOnSurface)));
    } else {
      mViewPagerAdapter.swapCursor(cursor);
    }
  }



  public void moveToAccount() {
    Cursor cursor = getAccountsCursor();
    if (cursor != null && cursor.moveToFirst()) {
      int position = 0;
      while (!cursor.isAfterLast()) {
        long accountId = cursor.getLong(getColumnIndexRowId());
        if (accountId == this.accountId) {
          position = cursor.getPosition();
        }
        if (accountId > 0) {
          mAccountCount++;
        }
        cursor.moveToNext();
      }
      setCurrentPosition(position);
      moveToPosition(getCurrentPosition());
    } else {
      onNoData();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Bundle extras = intent.getExtras();
    if (extras != null) {
      long accountId = extras.getLong(KEY_ROWID);
      if (accountId != this.accountId) {
        this.accountId = accountId;
        moveToAccount();
      }
      showTransactionFromIntent(extras);
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    if (loader.getId() == ACCOUNTS_CURSOR) {
      mViewPagerAdapter.swapCursor(null);
      mDrawerListAdapter.swapCursor(null);
      onNoData();
      setAccountsCursor(null);
    }
  }

  public void onNoData() {
    setTitle(R.string.app_name);
    toolbar.setSubtitle(null);
    setCurrentPosition(-1);
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
    if (DIALOG_TAG_SORTING.equals(dialogTag)) {
      return handleSortOption((int) extras.getLong(SELECTED_SINGLE_ID));
    }
    if (DIALOG_TAG_GROUPING.equals(dialogTag)) {
      return handleAccountsGrouping((int) extras.getLong(SELECTED_SINGLE_ID));
    }
    return super.onResult(dialogTag, which, extras);
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TASK_BALANCE: {
        Result result = (Result) o;
        if (!result.isSuccess()) {
          showSnackbar(result.print(this));
        }
        break;
      }
      case TASK_SPLIT: {
        Result result = (Result) o;
        if (((Result) o).isSuccess()) {
          recordUsage(ContribFeature.SPLIT_TRANSACTION);
        }
        showSnackbar(result.print(this));
        break;
      }
      case TASK_REVOKE_SPLIT: {
        Result result = (Result) o;
        showSnackbar(result.print(this));
        break;
      }
      case TASK_EXPORT: {
        Pair<ExportFormat, List<Uri>> result = (Pair<ExportFormat, List<Uri>>) o;
        if (result != null && !result.second.isEmpty()) {
          Result shareResult = ShareUtils.share(this, result.second,
              getShareTarget(),
              "text/" + result.first.name().toLowerCase(Locale.US));
          if (!shareResult.isSuccess()) {
            showSnackbar(shareResult.print(this));
          }
        }
        break;
      }
      case TASK_PRINT: {
        Result<Uri> result = (Result<Uri>) o;
        if (result.isSuccess()) {
          recordUsage(ContribFeature.PRINT);
          showMessage(result.print(this),
              new MessageDialogFragment.Button(R.string.menu_open, R.id.OPEN_PDF_COMMAND, result.getExtra().toString(), true),
              MessageDialogFragment.nullButton(R.string.button_label_close),
              new MessageDialogFragment.Button(R.string.button_label_share_file, R.id.SHARE_PDF_COMMAND, result.getExtra().toString(), true),
              false);
        } else {
          showSnackbar(result.print(this));
        }
        break;
      }
    }
  }

  private boolean hasCleared() {
    Cursor cursor = getAccountsCursor();
    //in case we are called before the accounts cursor is loaded, we return false
    if (cursor == null || cursor.getCount() == 0)
      return false;
    cursor.moveToPosition(getCurrentPosition());
    return cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_CLEARED)) > 0;
  }

  @Override
  protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    if (mDrawerToggle != null) mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
    if (item.getItemId() == R.id.SCAN_MODE_COMMAND) {
      toggleScanMode();
      return true;
    }

    return handleGrouping(item) || handleSortDirection(item) || super.onOptionsItemSelected(item);
  }

  public TransactionList getCurrentFragment() {
    if (mViewPagerAdapter == null)
      return null;
    return (TransactionList) getSupportFragmentManager().findFragmentByTag(
        mViewPagerAdapter.getFragmentName(getCurrentPosition()));
  }

  @Override
  public void onPositive(Bundle args) {
    super.onPositive(args);
    int anInt = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    if (anInt == R.id.START_EXPORT_COMMAND) {
      args.putParcelableArrayList(TransactionList.KEY_FILTER,
          getCurrentFragment().getFilterCriteria());
      getSupportFragmentManager().beginTransaction()
          .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_EXPORT),
              ASYNC_TAG)
          .add(ProgressDialogFragment.newInstance(
              getString(R.string.pref_category_title_export), null, ProgressDialog.STYLE_SPINNER, true), PROGRESS_TAG)
          .commit();
    } else if (anInt == R.id.DELETE_COMMAND_DO) {//Confirmation dialog was shown without Checkbox, because it was called with only void transactions
      onPositive(args, false);
    } else if (anInt == R.id.SPLIT_TRANSACTION_COMMAND) {
      finishActionMode();
      startTaskExecution(TASK_SPLIT, args, R.string.progress_dialog_saving);
    } else if (anInt == R.id.UNGROUP_SPLIT_COMMAND) {
      finishActionMode();
      startTaskExecution(TASK_REVOKE_SPLIT, args, R.string.progress_dialog_saving);
    } else  if (anInt == R.id.LINK_TRANSFER_COMMAND) {
      finishActionMode();
      viewModel.linkTransfer(args.getLongArray(KEY_LONG_IDS));
    }
  }

  @Override
  public void onPositive(Bundle args, boolean checked) {
    int anInt = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    if (anInt == R.id.DELETE_COMMAND_DO) {
      finishActionMode();
      startTaskExecution(
          TaskExecutionFragment.TASK_DELETE_TRANSACTION,
          ArrayUtils.toObject(args.getLongArray(TaskExecutionFragment.KEY_OBJECT_IDS)),
          checked,
          R.string.progress_dialog_deleting);
    } else if (anInt == R.id.BALANCE_COMMAND_DO) {
      startTaskExecution(TASK_BALANCE,
          new Long[]{args.getLong(KEY_ROWID)},
          checked, 0);
    } else if (anInt == R.id.REMAP_COMMAND) {
      getCurrentFragment().remap(args, checked);
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
    if (binding.drawer != null && binding.drawer.isDrawerOpen(GravityCompat.START)) {
      binding.drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  protected boolean handleSortOption(int itemId) {
    Sort newSort = Sort.fromCommandId(itemId);
    boolean result = false;
    if (newSort != null) {
      if (!newSort.equals(accountSort)) {
        accountSort = newSort;
        prefHandler.putString(PrefKey.SORT_ORDER_ACCOUNTS, newSort.name());

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset()) {
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        } else {
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
        }
      }
      result = true;
      if (itemId == R.id.SORT_CUSTOM_COMMAND) {
        Cursor cursor = getAccountsCursor();
        if (cursor == null) {
          complainAccountsNotLoaded();
        } else {
          ArrayList<AbstractMap.SimpleEntry<Long, String>> accounts = new ArrayList<>();
          if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
              final long id = cursor.getLong(getColumnIndexRowId());
              if (id > 0) {
                accounts.add(new AbstractMap.SimpleEntry<>(id, cursor.getString(getColumnIndexLabel())));
              }
              cursor.moveToNext();
            }
          }
          SortUtilityDialogFragment.newInstance(accounts).show(getSupportFragmentManager(), "SORT_ACCOUNTS");
        }
      }
    }
    return result;
  }

  protected boolean handleAccountsGrouping(int itemId) {
    AccountGrouping newGrouping = null;

    if (itemId == R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND) {
      newGrouping = AccountGrouping.CURRENCY;
    } else if (itemId == R.id.GROUPING_ACCOUNTS_TYPE_COMMAND) {
      newGrouping = AccountGrouping.TYPE;
    } else if (itemId == R.id.GROUPING_ACCOUNTS_NONE_COMMAND) {
      newGrouping = AccountGrouping.NONE;
    }
    if (newGrouping != null && !newGrouping.equals(accountGrouping)) {
      accountGrouping = newGrouping;
      prefHandler.putString(PrefKey.ACCOUNT_GROUPING, newGrouping.name());

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
        viewModel.persistGrouping(accountId, newGrouping);
      }
      return true;
    }
    return false;
  }

  protected boolean handleSortDirection(MenuItem item) {
    SortDirection newSortDirection = Utils.getSortDirectionFromMenuItemId(item.getItemId());
    if (newSortDirection != null) {
      if (!item.isChecked()) {
        if (accountId == Account.HOME_AGGREGATE_ID) {
          viewModel.persistSortDirectionHomeAggregate(newSortDirection);
        } else if (accountId < 0) {
          viewModel.persistSortDirectionAggregate(getCurrentCurrency(), newSortDirection);
        } else {
          viewModel.persistSortDirection(accountId, newSortDirection);
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

  public int getAccountCount() {
    return mAccountCount;
  }
}