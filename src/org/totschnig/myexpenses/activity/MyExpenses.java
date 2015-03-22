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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.dialog.BalanceDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SelectGroupingDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.WelcomeDialogFragment;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CommentCriteria;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 * @author Michael Totschnig
 *
 */
public class MyExpenses extends LaunchActivity implements
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    EditTextDialogListener, ConfirmationDialogListener,
    ContribIFace {

  private static final int VIEWPAGER = R.id.viewpager;
  public static final int TYPE_TRANSACTION = 0;
  public static final int TYPE_TRANSFER = 1;
  public static final int TYPE_SPLIT = 2;
  public static final boolean ACCOUNT_BUTTON_CYCLE = false;
  public static final boolean ACCOUNT_BUTTON_TOGGLE = true;
  public static final String TRANSFER_EXPENSE = "=> ";
  public static final String TRANSFER_INCOME = "<= ";
  
  static final long TRESHOLD_REMIND_RATE = 47L;
  static final long TRESHOLD_REMIND_CONTRIB = 113L;

  public static final int ACCOUNTS_CURSOR=-1;
  public static final int SPLIT_PART_CURSOR=3;
  private LoaderManager mManager;

  int mCurrentPosition = -1;
  private Cursor mAccountsCursor;

  private MyViewPagerAdapter mViewPagerAdapter;
  private StickyListHeadersAdapter mDrawerListAdapter;
  private ViewPager myPager;
  private long mAccountId = 0;
  int mAccountCount = 0;
  public enum HelpVariant {
    crStatus
  }
  private void setHelpVariant() {
    Account account = Account.getInstanceFromDb(mAccountId);
    helpVariant = account == null || account.type.equals(Type.CASH) ?
        null : HelpVariant.crStatus;
  }
  /**
   * stores the number of transactions that have been 
   * created in the db, updated after each creation of
   * a new transaction
   */
  private long sequenceCount = 0;
  private int colorAggregate;
  private StickyListHeadersListView mDrawerList;
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  
  private int columnIndexRowId, columnIndexColor, columnIndexCurrency, columnIndexDescription, columnIndexLabel;
  boolean indexesCalculated = false;
  private long idFromNotification = 0;
  private String mExportFormat = null;
  public boolean setupComplete;
  private Account.AccountGrouping mAccountGrouping;

  /* (non-Javadoc)
   * Called when the activity is first created.
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    //if we are launched from the contrib app, we refresh the cached contrib status
    setTheme(MyApplication.getThemeId());
    Resources.Theme theme = getTheme();
    TypedValue value = new TypedValue();
    theme.resolveAttribute(R.attr.colorAggregate, value, true);
    colorAggregate = value.data;
    int prev_version = MyApplication.PrefKey.CURRENT_VERSION.getInt(-1);
    if (prev_version == -1) {
      //prevent preference change listener from firing when preference file is created
      PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mDrawerList = (StickyListHeadersListView) findViewById(R.id.left_drawer);
    // set a custom shadow that overlays the main content when the drawer opens
    theme.resolveAttribute(R.attr.drawerShadow, value, true);
    mDrawerLayout.setDrawerShadow(value.resourceId, GravityCompat.START);
    String[] from = new String[]{
        KEY_DESCRIPTION,
        KEY_LABEL,
        KEY_OPENING_BALANCE,
        KEY_SUM_INCOME,
        KEY_SUM_EXPENSES,
        KEY_SUM_TRANSFERS,
        KEY_CURRENT_BALANCE,
        KEY_TOTAL,
        KEY_CLEARED_TOTAL,
        KEY_RECONCILED_TOTAL
    };
    // and an array of the fields we want to bind those fields to
    int[] to = new int[]{
        R.id.description,
        R.id.label,
        R.id.opening_balance,
        R.id.sum_income,
        R.id.sum_expenses,
        R.id.sum_transfer,
        R.id.current_balance,
        R.id.total,
        R.id.cleared_total,
        R.id.reconciled_total
    };
    mDrawerListAdapter = new MyGroupedAdapter(this, R.layout.account_row, null, from, to,0);
    LinearLayout footer = new LinearLayout(this);
    footer.setLayoutParams(new AbsListView.LayoutParams(
        AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
    footer.setGravity(Gravity.CENTER_HORIZONTAL);
    Button createAccount = new Button(this);
    createAccount.setText(R.string.menu_create_account);
    createAccount.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_add, 0, 0, 0);
    createAccount.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    createAccount.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mDrawerLayout.closeDrawers();
        dispatchCommand(R.id.CREATE_ACCOUNT_COMMAND, null);
      }
    });
    footer.addView(createAccount);
    mDrawerList.addFooterView(footer);
    mDrawerList.setAdapter(mDrawerListAdapter);
    mDrawerList.setAreHeadersSticky(false);
    mDrawerList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        if (mAccountId!=id) {
          moveToPosition(position);
          mDrawerLayout.closeDrawers();
        }
      }
    });

    getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
        | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
        |  ActionBar.DISPLAY_USE_LOGO);
    getSupportActionBar().setCustomView(R.layout.custom_title);
    theme.resolveAttribute(R.attr.drawerImage, value, true);
    mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
        value.resourceId, R.string.drawer_open, R.string.drawer_close) {

    /** Called when a drawer has settled in a completely closed state. */
    public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        TransactionList tl = getCurrentFragment();
        if (tl != null)
          tl.onDrawerClosed();
        //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
    }

    /** Called when a drawer has settled in a completely open state. */
    public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        TransactionList tl = getCurrentFragment();
        if (tl != null)
          tl.onDrawerOpened();
        //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
    }
};

  // Set the drawer toggle as the DrawerListener
  mDrawerLayout.setDrawerListener(mDrawerToggle);

  if (prev_version == -1) {
    getSupportActionBar().hide();
    /*if (MyApplication.backupExists()) {
      if (!mSettings.getBoolean("restoreOnInstallAsked", false)) {
        DialogFragment df = MessageDialogFragment.newInstance(
            R.string.dialog_title_restore_on_install,
            R.string.dialog_confirm_restore_on_install,
            new MessageDialogFragment.Button(
                android.R.string.yes,
                R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,
                Boolean.valueOf(true)),
            null,
            new MessageDialogFragment.Button(
                android.R.string.no,
                R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,
                Boolean.valueOf(false)));
        df.setCancelable(false);
        df.show(getSupportFragmentManager(),"RESTORE_ON_INSTALL");
        SharedPreferencesCompat.apply(
            mSettings.edit().putBoolean("restoreOnInstallAsked", true));
      }
    } else {*/
      initialSetup();
     /* }*/

      return;
    }
    if (savedInstanceState != null) {
      mExportFormat = savedInstanceState.getString("exportFormat");
    }
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      mAccountId = extras.getLong(KEY_ROWID,0);
      idFromNotification = extras.getLong(KEY_TRANSACTIONID,0);
      //detail fragment from notification should only be shown upon first instantiation from notification
      if (idFromNotification != 0 && savedInstanceState == null) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TransactionDetailFragment.class.getName()) == null) {
          TransactionDetailFragment.newInstance(idFromNotification)
              .show(fm, TransactionDetailFragment.class.getName());
          getIntent().removeExtra(KEY_TRANSACTIONID);
        }
      }
    }
    if (mAccountId == 0)
      mAccountId = MyApplication.PrefKey.CURRENT_ACCOUNT.getLong(0L);
    setup();
  }
  private void initialSetup() {
    FragmentManager fm = getSupportFragmentManager();
    if (fm.findFragmentByTag("ASYNC_TASK") == null) {
      fm.beginTransaction()
        .add(WelcomeDialogFragment.newInstance(),"WELCOME")
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_REQUIRE_ACCOUNT,new Long[]{0L}, null), "ASYNC_TASK")
        .commit();
      setupComplete = false;
    }
  }
  private void setup() {
    newVersionCheck();
    //SharedPreferencesCompat.apply(mSettings.edit().remove("restoreOnInstallAsked"));
    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin,margin, true);
    mViewPagerAdapter = new MyViewPagerAdapter(this,getSupportFragmentManager(),null);
    myPager = (ViewPager) this.findViewById(VIEWPAGER);
    myPager.setAdapter(this.mViewPagerAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin((int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
    myPager.setPageMarginDrawable(margin.resourceId);
    mManager= getSupportLoaderManager();
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }

  private void moveToPosition(int position) {
    if (myPager.getCurrentItem()==position)
      setCurrentAccount(position);
    else
      myPager.setCurrentItem(position,false);
  }
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean showBalanceCommand = false;
    if (mAccountId > 0 && mAccountsCursor != null && mAccountsCursor.moveToPosition(mCurrentPosition)) {
      try {
        if (Type.valueOf(mAccountsCursor.getString(mAccountsCursor.getColumnIndexOrThrow(KEY_TYPE)))
            != Type.CASH) {
          showBalanceCommand = true;
        }
      } catch (IllegalArgumentException ex) {/*aggregate*/}
    }
    Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BALANCE_COMMAND),
        showBalanceCommand);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
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
      sequenceCount = intent.getLongExtra("sequence_count", 0);
/*      nextReminder = 
          MyApplication.PrefKey.NEXT_REMINDER_RATE.getLong(TRESHOLD_REMIND_RATE);
      if (nextReminder != -1 && sequenceCount >= nextReminder) {
        RemindRateDialogFragment f = 
        new org.totschnig.myexpenses.dialog.RemindRateDialogFragment();
        f.setCancelable(false);
        f.show(getSupportFragmentManager(),"REMIND_RATE");
        return;
      }*/
      if (!MyApplication.getInstance(). isContribEnabled()) {
        nextReminder = 
            MyApplication.PrefKey.NEXT_REMINDER_CONTRIB.getLong(TRESHOLD_REMIND_CONTRIB);
        if (nextReminder != -1 && sequenceCount >= nextReminder) {
          CommonCommands.showContribInfoDialog(this,sequenceCount);
          return;
        }
      }
    }
    if (requestCode == CREATE_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
      mAccountId = intent.getLongExtra(KEY_ROWID, 0);
    }
  }
  public void addFilterCriteria(Integer id,Criteria c) {
    TransactionList tl = getCurrentFragment();
    if (tl != null) {
      tl.addFilterCriteria(id,c);
    }
  }
  /**
   * start ExpenseEdit Activity for a new transaction/transfer/split
   * @param type either {@link #TYPE_TRANSACTION} or {@link #TYPE_TRANSFER} or {@link #TYPE_SPLIT}
   */
  private void createRow(int type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(MyApplication.KEY_OPERATION_TYPE, type);
    //if we are called from an aggregate cursor, we look for the first account
    //with the same currency
    long accountId = 0;
    if (mAccountId < 0) {
      if (mAccountsCursor != null) {
        Account a = Account.getInstanceFromDb(mAccountId);
        if (a != null) {
          mAccountsCursor.moveToFirst();
          String currentCurrency = a.currency.getCurrencyCode();
          while (mAccountsCursor.isAfterLast() == false) {
            if (mAccountsCursor.getString(columnIndexCurrency).equals(currentCurrency)) {
              accountId = mAccountsCursor.getLong(columnIndexRowId);
              break;
            }
            mAccountsCursor.moveToNext();
          }
        }
      }
    } else {
      accountId = mAccountId;
    }
    //since splits are immediately persisted they will not work without an account set
    if (accountId == 0 && type == TYPE_SPLIT)
      return;
    //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
    i.putExtra(KEY_ACCOUNTID,accountId);
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
    Account a;
    switch (command) {
    case R.id.DISTRIBUTION_COMMAND:
      tl = getCurrentFragment();
      if (tl != null && tl.mappedCategories) {
        if (MyApplication.getInstance().isContribEnabled()) {
          contribFeatureCalled(Feature.DISTRIBUTION, null);
        }
        else {
          CommonCommands.showContribDialog(this,Feature.DISTRIBUTION, null);
        }
      } else {
        MessageDialogFragment.newInstance(
            0,
            R.string.dialog_command_disabled_distribution,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.GROUPING_COMMAND:
      a = Account.getInstanceFromDb(mAccountId);
      if (a != null) {
        SelectGroupingDialogFragment.newInstance(
            a.grouping.ordinal())
          .show(getSupportFragmentManager(), "SELECT_GROUPING");
      }
      return true;
    case R.id.GROUPING_COMMAND_DO:
      Grouping value = Account.Grouping.values()[(Integer)tag];
      if (mAccountId < 0) {
        AggregateAccount.getInstanceFromDb(mAccountId).persistGrouping(value);
      } else {
        Account.getInstanceFromDb(mAccountId).persistGrouping(value);
      }
      return true;
    case R.id.CREATE_TRANSACTION_COMMAND:
      createRow(TYPE_TRANSACTION);
      return true;
    case R.id.CREATE_TRANSFER_COMMAND:
      if (transferEnabled()) {
        createRow(TYPE_TRANSFER);
      } else {
        a = Account.getInstanceFromDb(mAccountId);
        if (a != null) {
          String currency = a.currency.getCurrencyCode();
          MessageDialogFragment.newInstance(
              0,
              getString(R.string.dialog_command_disabled_insert_transfer_1) +
              " " +
              getString(R.string.dialog_command_disabled_insert_transfer_2,
                  currency),
              new MessageDialogFragment.Button(R.string.menu_create_account, R.id.CREATE_ACCOUNT_COMMAND,currency),
              MessageDialogFragment.Button.okButton(),
              null)
           .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
        }
      }
      return true;
    case R.id.CREATE_SPLIT_COMMAND:
      if (MyApplication.getInstance().isContribEnabled()) {
        contribFeatureCalled(Feature.SPLIT_TRANSACTION, null);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.SPLIT_TRANSACTION, null);
      }
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
            Utils.formatCurrency(
                new Money(currency,
                    mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_RECONCILED_TOTAL)))));
        bundle.putString(KEY_CLEARED_TOTAL, Utils.formatCurrency(
            new Money(currency,
                mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_CLEARED_TOTAL)))));
        BalanceDialogFragment.newInstance(bundle)
            .show(getSupportFragmentManager(), "BALANCE_ACCOUNT");
      } else {
        MessageDialogFragment.newInstance(
            0,
            R.string.dialog_command_disabled_balance,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.RESET_COMMAND:
      tl = getCurrentFragment();
      if (tl != null && tl.hasItems) {
        Result appDirStatus = Utils.checkAppDir();
        if (appDirStatus.success) {
          DialogUtils.showWarningResetDialog(this, mAccountId);
        } else {
          Toast.makeText(getBaseContext(),
              appDirStatus.print(this),
              Toast.LENGTH_LONG)
              .show();
        }
      } else {
        MessageDialogFragment.newInstance(
            0,
            R.string.dialog_command_disabled_reset_account,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.BACKUP_COMMAND:
      startActivity(new Intent("myexpenses.intent.backup"));
      return true;
/*    case R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND:
      if ((Boolean) tag) {
        if (MyApplication.backupRestore()) {
          //if we have successfully restored, we relaunch in order to force password check if needed
          i = getBaseContext().getPackageManager()
              .getLaunchIntentForPackage( getBaseContext().getPackageName() );
          i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(i);
          break;
        }
      }
      initialSetup();
      return true;*/
    case R.id.REMIND_NO_RATE_COMMAND:
      PrefKey.NEXT_REMINDER_RATE.putLong(-1);
      return true;
    case R.id.REMIND_LATER_RATE_COMMAND:
      PrefKey.NEXT_REMINDER_RATE.putLong(sequenceCount+TRESHOLD_REMIND_RATE);
      return true;
    case R.id.HELP_COMMAND:
      setHelpVariant();
      break;
    case R.id.MANAGE_PLANS_COMMAND:
      i = new Intent(this, ManageTemplates.class);
      i.putExtra(DatabaseConstants.KEY_TRANSFER_ENABLED,transferEnabledGlobal());
      startActivity(i);
      return true;
    case R.id.DELETE_COMMAND_DO:
      finishActionMode();
      startTaskExecution(
          TaskExecutionFragment.TASK_DELETE_TRANSACTION,
          (Long[])tag,
          null,
          R.string.progress_dialog_deleting);
      return true;
    case R.id.CREATE_ACCOUNT_COMMAND:
      if (mAccountCount == 0) {
        Toast.makeText(this, "Account list not yet loaded. Please try again", Toast.LENGTH_LONG).show();
      }
      //we need the accounts to be loaded in order to evaluate if the limit has been reached
      else if (MyApplication.getInstance().isContribEnabled() || mAccountCount < 5) {
        i = new Intent(this, AccountEdit.class);
        if (tag != null)
          i.putExtra(KEY_CURRENCY,(String)tag);
        startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.ACCOUNTS_UNLIMITED, null);
      }
      return true;
      case R.id.DELETE_ACCOUNT_COMMAND_DO:
        //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
        mAccountId = 0;
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_ACCOUNT,
            new Long[] { (Long) tag },
            null,
            0);
        return true;
      case R.id.SHARE_COMMAND:
        i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.tell_a_friend_message));
        i.setType("text/plain");
        startActivity(Intent.createChooser(i, getResources().getText(R.string.menu_share)));
        return true;
      case R.id.CANCEL_CALLBACK_COMMAND:
        finishActionMode();
        return true;
      case R.id.OPEN_PDF_COMMAND:
        i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile((File) tag), "application/pdf");
        if (!Utils.isIntentAvailable(this,i)) {
          Toast.makeText(this,R.string.no_app_handling_pdf_available, Toast.LENGTH_LONG).show();
        } else {
          startActivity(i);
        }
        return true;
      case R.id.QUIT_COMMAND:
        finish();
        return true;
    }
    return super.dispatchCommand(command, tag);
  }
  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    public MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    public String getFragmentName(int currentPosition) {
      return FragmentPagerAdapter.makeFragmentName(VIEWPAGER,getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
      long accountId = cursor.getLong(columnIndexRowId);
      if (!Account.isInstanceCached(accountId)) {
        //calling the constructors, puts the objects into the cache from where the fragment can
        //retrieve it, without needing to create a new cursor
        if (accountId < 0)  {
          new AggregateAccount(cursor);
        } else {
          new Account(cursor);
        }
      }
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
    if (mCurrentPosition != -1 && Build.VERSION.SDK_INT >= 11) {
      ContextualActionBarFragment f = 
      (ContextualActionBarFragment) getSupportFragmentManager().findFragmentByTag(
          mViewPagerAdapter.getFragmentName(mCurrentPosition));
      if (f != null)
        f.finishActionMode();
    }
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    switch(feature){
    case DISTRIBUTION:
      Account a = Account.getInstanceFromDb(mAccountId);
      recordUsage(feature);
      Intent i = new Intent(this, ManageCategories.class);
      i.setAction("myexpenses.intent.distribution");
      i.putExtra(KEY_ACCOUNTID, mAccountId);
      if (tag != null) {
        int year = (int) ((Long)tag/1000);
        int groupingSecond = (int) ((Long)tag % 1000);
        i.putExtra("grouping", a!= null ? a.grouping : Grouping.NONE);
        i.putExtra("groupingYear",year);
        i.putExtra("groupingSecond", groupingSecond);
      } else {
        i.putExtra("grouping",Grouping.NONE);
      }
      startActivity(i);
      break;
    case SPLIT_TRANSACTION:
      if (tag==null) {
        createRow(TYPE_SPLIT);
      } else {
        startTaskExecution(
            TaskExecutionFragment.TASK_SPLIT,
            (Object[]) tag,
            null,
            0);
      }
      break;
    case PRINT:
      TransactionList tl = getCurrentFragment();
      if (tl != null)  {
        Bundle args = new Bundle();
        args.putSparseParcelableArray(TransactionList.KEY_FILTER, tl.getFilterCriteria());
        args.putLong(KEY_ROWID, mAccountId);
        getSupportFragmentManager().beginTransaction()
          .add(TaskExecutionFragment.newInstancePrint(args),
              "ASYNC_TASK")
          .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_printing),"PROGRESS")
          .commit();
      }
      break;
    }
  }
  @Override
  public void contribFeatureNotCalled() {
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch(id) {
    case ACCOUNTS_CURSOR:
      Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
      builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
      return new CursorLoader(this,
          builder.build(), null, null, null, null) {
        @Override
        public Cursor loadInBackground() {
          try {
            return super.loadInBackground();
          } catch (Exception e) {
            Utils.reportToAcra(e);
            String msg = e instanceof TransactionDatabase.SQLiteDowngradeFailedException ?
                ("Database cannot be downgraded from a newer version. Please either uninstall MyExpenses, " +
                    "before reinstalling, or upgrade to a new version.") :
                "Database upgrade failed. Please contact support@myexpenses.mobi !";
                MessageDialogFragment f = MessageDialogFragment.newInstance(
                    0,
                    msg,
                    new MessageDialogFragment.Button(android.R.string.ok,R.id.QUIT_COMMAND,null),
                    null,
                    null);
                f.setCancelable(false);
                f.show(getSupportFragmentManager(),"DOWNGRADE"); 
                return null;
          }
        }
      };
    }
    return null;
  }
  /**
   * set the Current account to the one in the requested position of mAccountsCursor
   * @param position
   */
  private void setCurrentAccount(int position) {
    mAccountsCursor.moveToPosition(position);
    long newAccountId = mAccountsCursor.getLong(columnIndexRowId);
    if (mAccountId != newAccountId) {
      MyApplication.PrefKey.CURRENT_ACCOUNT.putLong(newAccountId);
    }
    mAccountId = newAccountId;
    setCustomTitle();
    mDrawerList.setItemChecked(position, true);
    supportInvalidateOptionsMenu();
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    switch(loader.getId()) {
    case ACCOUNTS_CURSOR:
      mAccountCount = 0;
      mAccountsCursor = cursor;
      if (mAccountsCursor == null) {
        return;
      }
      //when account grouping is changed in setting, cursor is reloaded,
      //and we need to refresh the value here
      try {
        mAccountGrouping = Account.AccountGrouping.valueOf(
            MyApplication.PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
      } catch (IllegalArgumentException e) {
        mAccountGrouping = Account.AccountGrouping.TYPE;
      }
      ((SimpleCursorAdapter) mDrawerListAdapter).swapCursor(mAccountsCursor);
      //swaping the cursor is altering the accountId, if the
      //sort order has changed, but we want to move to the same account as before
      long cacheAccountId = mAccountId;
      mViewPagerAdapter.swapCursor(cursor);
      mAccountId = cacheAccountId;
      if (!indexesCalculated) {
        columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID);
        columnIndexColor = mAccountsCursor.getColumnIndex(KEY_COLOR);
        columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
        columnIndexDescription = mAccountsCursor.getColumnIndex(KEY_DESCRIPTION);
        columnIndexLabel = mAccountsCursor.getColumnIndex(KEY_LABEL);
        indexesCalculated = true;
      }
      if (mAccountsCursor.moveToFirst()) {
        int position = 0;
        while (mAccountsCursor.isAfterLast() == false) {
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
      ((SimpleCursorAdapter) mDrawerListAdapter).swapCursor(null);
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
  public void onFinishEditDialog(Bundle args) {
    String result = args.getString(EditTextDialog.KEY_RESULT);
    switch (args.getInt(EditTextDialog.KEY_REQUEST_CODE)) {
    case TEMPLATE_TITLE_REQUEST:
      if ((new Template(Transaction.getInstanceFromDb(args.getLong(KEY_ROWID)),result)).save() == null) {
        Toast.makeText(getBaseContext(),getString(R.string.template_title_exists,result), Toast.LENGTH_LONG).show();
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.template_create_success,result), Toast.LENGTH_LONG).show();
      }
      finishActionMode();
      break;
    case FILTER_COMMENT_REQUEST:
      addFilterCriteria(R.id.FILTER_COMMENT_COMMAND,new CommentCriteria(result));
      break;
    }
  }
  @Override
  public void onCancelEditDialog() {
    finishActionMode();
  }
  @Override
  public void onPostExecute(int taskId,Object o) {
    super.onPostExecute(taskId, o);
    switch(taskId) {
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
      TransactionDetailFragment tdf = (TransactionDetailFragment)
          getSupportFragmentManager().findFragmentByTag(TransactionDetailFragment.class.getName());
      if (tdf!= null) {
        tdf.fillData((Transaction) o);
      }
      break;
    case TaskExecutionFragment.TASK_CLONE:
      Integer successCount = (Integer) o;
      String msg = successCount == 0 ?  getString(R.string.clone_transaction_error) :
        getResources().getQuantityString(R.plurals.clone_transaction_success, successCount, successCount);
      Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
      break;
    case TaskExecutionFragment.TASK_SPLIT:
      successCount = (Integer) o;
      msg = successCount == 0 ?  getString(R.string.split_transaction_error) :
        getResources().getQuantityString(R.plurals.split_transaction_success, successCount, successCount);
      Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
      break;
    case TaskExecutionFragment.TASK_REQUIRE_ACCOUNT:
      setupComplete = true;
      getSupportActionBar().show();
      FragmentManager fm = getSupportFragmentManager();
      setup();
      WelcomeDialogFragment wdf =
          ((WelcomeDialogFragment) fm.findFragmentByTag("WELCOME"));
      if (wdf!=null) {
        wdf.setSetupComplete();
      }
      break;
    case TaskExecutionFragment.TASK_EXPORT:
      ArrayList<File> files = (ArrayList<File>) o;
      if (files != null && files.size() >0)
        Utils.share(this,files,
            MyApplication.PrefKey.SHARE_TARGET.getString("").trim(),
            "text/" + mExportFormat.toLowerCase(Locale.US));
      break;
    case TaskExecutionFragment.TASK_PRINT:
      Result result = (Result) o;
      if (result.success) {
        recordUsage(Feature.PRINT);
        MessageDialogFragment f = MessageDialogFragment.newInstance(
            0,
            result.print(this),
            new MessageDialogFragment.Button(R.string.menu_open,R.id.OPEN_PDF_COMMAND,(File) result.extra[0]),
            null,
            MessageDialogFragment.Button.nullButton(android.R.string.cancel));
        f.setCancelable(false);
        f.show(getSupportFragmentManager(),"PRINT_RESULT");
      } else {
        Toast.makeText(this,result.print(this),Toast.LENGTH_LONG).show();
      }
      break;
    }
  }
  public void deleteAccount (View v) {
    mDrawerLayout.closeDrawers();
    int position = (Integer) v.getTag();
    mAccountsCursor.moveToPosition(position);
    long accountId = mAccountsCursor.getLong(columnIndexRowId);
    //do nothing if accidentally we are positioned at an aggregate account or try to delete the last account
    if (mAccountsCursor.getCount()==1 || accountId > 0) {
      MessageDialogFragment.newInstance(
          R.string.dialog_title_warning_delete_account,
          getString(R.string.warning_delete_account,mAccountsCursor.getString(columnIndexLabel)),
          new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
              accountId),
          null,
          MessageDialogFragment.Button.noButton())
        .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
    }
  }
  public void editAccount(View v) {
    mDrawerLayout.closeDrawers();
    long id = (Long) v.getTag();
    if (id > 0) { //do nothing if accidentally we are positioned at an aggregate account
      Intent i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID,id);
      startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
    }
  }
  /**
   * @return true if for the current Account there is a second account
   * with the same currency we can transfer to
   */
  public boolean transferEnabled() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow(KEY_TRANSFER_ENABLED)) > 0;
  }
  /**
   * @return true if for any Account there is a second account
   * with the same currency we can transfer to
   */
  public boolean transferEnabledGlobal() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    //we move to the last position in account cursor, and we check if it is an aggregate account
    //which means that there is at least one currency having multiple accounts
    mAccountsCursor.moveToLast();
    return mAccountsCursor.getLong(columnIndexRowId) < 0;
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

  private void setConvertedAmount(TextView tv,Currency currency) {
    tv.setText(Utils.convAmount(tv.getText().toString(),currency));
  }
  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
      super.onPostCreate(savedInstanceState);
      // Sync the toggle state after onRestoreInstanceState has occurred.
      mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Pass the event to ActionBarDrawerToggle, if it returns
      // true, then it has handled the app icon touch event
      if (mDrawerToggle.onOptionsItemSelected(item)) {
        return true;
      }
      // Handle your other action bar items...

      return super.onOptionsItemSelected(item);
  }

  private void setCustomTitle() {
    View titleBar = getSupportActionBar().getCustomView();
    ((TextView) titleBar.findViewById(android.R.id.text1)).setText(
        mAccountsCursor.getString(columnIndexLabel));
    ((TextView) titleBar.findViewById(R.id.end)).setText(Utils.formatCurrency(
        new Money(
            Utils.getSaveInstance(mAccountsCursor.getString(columnIndexCurrency)),
            mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_CURRENT_BALANCE)))));
    titleBar.findViewById(R.id.color1).setBackgroundColor(
        mAccountId < 0 ?
            colorAggregate :
              mAccountsCursor.getInt(columnIndexColor));
  }
  public TransactionList getCurrentFragment() {
    if (mViewPagerAdapter == null)
      return null;
    return (TransactionList) getSupportFragmentManager().findFragmentByTag(
        mViewPagerAdapter.getFragmentName(mCurrentPosition));
  }
  public class MyAdapter extends SimpleCursorAdapter {
    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row=super.getView(position, convertView, parent);
      Cursor c = getCursor();
      c.moveToPosition(position);
      Currency currency = Utils.getSaveInstance(c.getString(columnIndexCurrency));
      View v = row.findViewById(R.id.color1);
      long rowId =  c.getLong(columnIndexRowId);
      long sum_transfer = c.getLong(c.getColumnIndex(KEY_SUM_TRANSFERS));
      boolean has_future = c.getInt(c.getColumnIndex(KEY_HAS_FUTURE)) > 0;
      boolean is_aggregate =rowId<0;
      boolean hide_cr;
      View deleteAccount = row.findViewById(R.id.DELETE_ACCOUNT_COMMAND);
      deleteAccount.setVisibility(is_aggregate || c.getCount()==1 ? View.GONE : View.VISIBLE);
      deleteAccount.setTag(position);
      View editAccount = row.findViewById(R.id.EDIT_ACCOUNT_COMMAND);
      editAccount.setVisibility(is_aggregate ? View.GONE : View.VISIBLE);
      editAccount.setTag(rowId);

      if (is_aggregate) {
        hide_cr = true;
        if (mAccountGrouping==Account.AccountGrouping.CURRENCY) {
          ((TextView) row.findViewById(R.id.label)).setText(R.string.menu_aggregates);
        }
      } else {
        //for deleting we need the position, because we need to find out the account's label
        try {
          hide_cr = Type.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE))).equals(Type.CASH);
        } catch (IllegalArgumentException ex) {
          hide_cr = true;
        }
      }
      row.findViewById(R.id.TransferRow).setVisibility(
          sum_transfer==0 ? View.GONE : View.VISIBLE);
      row.findViewById(R.id.TotalRow).setVisibility(
          has_future ? View.VISIBLE : View.GONE);
      row.findViewById(R.id.ClearedRow).setVisibility(
          hide_cr ? View.GONE : View.VISIBLE);
      row.findViewById(R.id.ReconciledRow).setVisibility(
          hide_cr ? View.GONE : View.VISIBLE);
      if (c.getLong(columnIndexRowId)>0) {
        setConvertedAmount((TextView)row.findViewById(R.id.sum_transfer), currency);
      }
      v.setBackgroundColor(c.getInt(columnIndexColor));
      setConvertedAmount((TextView)row.findViewById(R.id.opening_balance), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.sum_income), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.sum_expenses), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.current_balance), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.total), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.reconciled_total), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.cleared_total), currency);
      String description = c.getString(columnIndexDescription);
      if (description.equals(""))
        row.findViewById(R.id.description).setVisibility(View.GONE);
      return row;
    }
  }
  public class MyGroupedAdapter extends MyAdapter implements StickyListHeadersAdapter {
    LayoutInflater inflater;
    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(MyExpenses.this);
    }
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.accounts_header, parent, false);
      }
      Cursor c = getCursor();
      c.moveToPosition(position);
      long headerId = getHeaderId(position);
      TextView sectionLabelTV = (TextView) convertView.findViewById(R.id.sectionLabel);
      switch(mAccountGrouping) {
      case CURRENCY:
        sectionLabelTV.setText(Account.CurrencyEnum.valueOf(c.getString(columnIndexCurrency)).toString());
        break;
      case NONE:
        sectionLabelTV.setText(headerId==0?R.string.pref_manage_accounts_title:R.string.menu_aggregates);
        break;
      case TYPE:
        int headerRes;
        if (headerId == Type.values().length) {
          headerRes = R.string.menu_aggregates;
        } else {
          headerRes = Type.values()[(int) headerId].toStringResPlural();
        }
        sectionLabelTV.setText(headerRes);
      default:
        break;
      
      }
      return convertView;
    }
    @Override
    public long getHeaderId(int position) {
      Cursor c = getCursor();
      c.moveToPosition(position);
      switch(mAccountGrouping) {
      case CURRENCY:
        return Account.CurrencyEnum.valueOf(c.getString(columnIndexCurrency)).ordinal();
      case NONE:
        return c.getLong(columnIndexRowId)>0 ? 0 : 1;
      case TYPE:
        Type type;
        try {
          type = Type.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE)));
          return type.ordinal();
        } catch (IllegalArgumentException ex) {
          return Type.values().length;
        }
      }
      return 0;
    }
  }
  protected void onSaveInstanceState (Bundle outState) {
    super.onSaveInstanceState(outState);
    //detail fragment from notification should only be shown once
    if (idFromNotification !=0) {
      outState.putLong("idFromNotification",0);
    }
    outState.putString("exportFormat", mExportFormat);
  }
  @Override
  public void onPositive(Bundle args) {
   switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
   case R.id.START_EXPORT_COMMAND:
     mExportFormat = args.getString("format");
     getSupportFragmentManager().beginTransaction()
       .add(TaskExecutionFragment.newInstanceExport(args),
           "ASYNC_TASK")
       .add(ProgressDialogFragment.newInstance(
           R.string.pref_category_title_export,0,ProgressDialog.STYLE_SPINNER,true),"PROGRESS")
       .commit();
     break;
   case R.id.BALANCE_COMMAND_DO:
     startTaskExecution(TaskExecutionFragment.TASK_BALANCE,
         new Long[]{args.getLong(KEY_ROWID)},
         args.getBoolean("deleteP"), 0);
     break;
   }
  }
  @Override
  public void onNegative(Bundle args) {
  }
  @Override
  public void onDismissOrCancel(Bundle args) {
  }
}