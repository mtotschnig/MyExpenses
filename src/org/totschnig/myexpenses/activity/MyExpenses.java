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

import java.io.Serializable;
import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SelectGroupingDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.WelcomeDialogFragment;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.util.Utils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.DialogFragment;
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
    EditTextDialogListener,
    ContribIFace {

  public static final int TYPE_TRANSACTION = 0;
  public static final int TYPE_TRANSFER = 1;
  public static final int TYPE_SPLIT = 2;
  public static final boolean ACCOUNT_BUTTON_CYCLE = false;
  public static final boolean ACCOUNT_BUTTON_TOGGLE = true;
  public static final String TRANSFER_EXPENSE = "=> ";
  public static final String TRANSFER_INCOME = "<= ";
  
  static final int TRESHOLD_REMIND_RATE = 47;
  static final int TRESHOLD_REMIND_CONTRIB = 113;

  public static final int ACCOUNTS_CURSOR=-1;
  public static final int ACCOUNTS_OTHER_CURSOR=2;
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
    mSettings = MyApplication.getInstance().getSettings();
    int prev_version = mSettings.getInt(MyApplication.PREFKEY_CURRENT_VERSION, -1);
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
    String[] from = new String[]{"description","label","opening_balance","sum_income","sum_expenses","sum_transfer","current_balance"};
    // and an array of the fields we want to bind those fields to
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.sum_transfer,R.id.current_balance};
    mDrawerListAdapter = new MyGroupedAdapter(this, R.layout.account_row, null, from, to,0);
    mDrawerList.setAdapter(mDrawerListAdapter);
    mDrawerList.setAreHeadersSticky(false);
    mDrawerList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        moveToPosition(position);
        mDrawerLayout.closeDrawer(mDrawerList);
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
      if (MyApplication.backupExists()) {
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
      } else {
        initialSetup();
      }
      return;
    }
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      mAccountId = extras.getLong(KEY_ROWID,0);
      idFromNotification = extras.getLong("transaction_id",0);
      //detail fragment from notification should only be shown upon first instantiation from notification
      if (idFromNotification != 0 && savedInstanceState == null) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag("TRANSACTION_DETAIL") == null) {
          TransactionDetailFragment.newInstance(idFromNotification)
              .show(fm, "TRANSACTION_DETAIL");
          getIntent().removeExtra("transaction_id");
        }
      }
    }
    if (mAccountId == 0)
      mAccountId = mSettings.getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
    setup();
  }
  private void initialSetup() {
    FragmentManager fm = getSupportFragmentManager();
    if (fm.findFragmentByTag("ASYNC_TASK") == null) {
      fm.beginTransaction()
        .add(WelcomeDialogFragment.newInstance(),"WELCOME")
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_REQUIRE_ACCOUNT,0L, null), "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_setup),"PROGRESS")
        .commit();
    }
  }
  private void setup() {
    newVersionCheck();
    SharedPreferencesCompat.apply(mSettings.edit().remove("restoreOnInstallAsked"));
    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin,margin, true);
    mViewPagerAdapter = new MyViewPagerAdapter(this,getSupportFragmentManager(),null);
    myPager = (ViewPager) this.findViewById(R.id.viewpager);
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
    menu.findItem(R.id.EDIT_ACCOUNT_COMMAND).setVisible(mAccountId > 0);
    menu.findItem(R.id.DELETE_ACCOUNT_COMMAND).setVisible(
        mAccountId > 0 && mAccountCount > 1);
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
      /*nextReminder = mSettings.getLong("nextReminderRate",TRESHOLD_REMIND_RATE);
      if (nextReminder != -1 && sequenceCount >= nextReminder) {
        new RemindRateDialogFragment().show(getSupportFragmentManager(),"REMIND_RATE");
        return;
      }*/
      if (!MyApplication.getInstance().isContribEnabled) {
        nextReminder = mSettings.getLong("nextReminderContrib",TRESHOLD_REMIND_CONTRIB);
        if (nextReminder != -1 && sequenceCount >= nextReminder) {
          CommonCommands.showContribInfoDialog(this,true);
          return;
        }
      }
    }
    if (requestCode == CREATE_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
      mAccountId = intent.getLongExtra(KEY_ROWID, 0);
    }
  }

  /**
   * start ExpenseEdit Activity for a new transaction/transfer/split
   * @param type either {@link #TYPE_TRANSACTION} or {@link #TYPE_TRANSFER} or {@link #TYPE_SPLIT}
   */
  private void createRow(int type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("operationType", type);
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
    switch (command) {
    case R.id.DISTRIBUTION_COMMAND:
      tl = getCurrentFragment();
      if (tl != null && tl.mappedCategories) {
        if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.DISTRIBUTION, null);
        }
        else {
          CommonCommands.showContribDialog(this,Feature.DISTRIBUTION, null);
        }
      } else {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            R.string.dialog_command_disabled_distribution,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.GROUPING_COMMAND:
      Account a = Account.getInstanceFromDb(mAccountId);
      if (a != null) {
        SelectGroupingDialogFragment.newInstance(
            R.id.GROUPING_COMMAND_DO,
            a.grouping.ordinal())
          .show(getSupportFragmentManager(), "SELECT_GROUPING");
      }
      return true;
    case R.id.GROUPING_COMMAND_DO:
      Grouping value = Account.Grouping.values()[(Integer)tag];
      if (mAccountId < 0) {
        AggregateAccount.getInstanceFromDB(mAccountId).persistGrouping(value);
        getContentResolver().notifyChange(TransactionProvider.ACCOUNTS_URI, null);
      } else {
        Account account = Account.getInstanceFromDb(mAccountId);
        account.grouping=value;
        account.save();
      }
      return true;
    case R.id.CREATE_TRANSACTION_COMMAND:
      createRow(TYPE_TRANSACTION);
      return true;
    case R.id.CREATE_TRANSFER_COMMAND:
      if (transferEnabled()) {
        createRow(TYPE_TRANSFER);
      } else {
        String currency = Account.getInstanceFromDb(mAccountId).currency.getCurrencyCode();
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            getString(R.string.dialog_command_disabled_insert_transfer,currency),
            new MessageDialogFragment.Button(R.string.menu_create_account, R.id.CREATE_ACCOUNT_COMMAND,currency),
            MessageDialogFragment.Button.okButton(),
            null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.CREATE_SPLIT_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.SPLIT_TRANSACTION, null);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.SPLIT_TRANSACTION, null);
      }
      return true;
    case R.id.RESET_COMMAND:
      tl = getCurrentFragment();
      if (tl != null && tl.hasItems) {
        if (Utils.isExternalStorageAvailable()) {
          if (mAccountId > 0 || MyApplication.getInstance().isContribEnabled) {
            contribFeatureCalled(Feature.RESET_ALL, null);
          } else {
            CommonCommands.showContribDialog(this,Feature.RESET_ALL, null);
          }
        } else {
          Toast.makeText(getBaseContext(),
              getString(R.string.external_storage_unavailable),
              Toast.LENGTH_LONG)
              .show();
        }
      } else {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            R.string.dialog_command_disabled_reset_account,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.EDIT_ACCOUNT_COMMAND:
      if (mAccountId >0) {
        i = new Intent(this, AccountEdit.class);
        i.putExtra(KEY_ROWID, mAccountId);
        startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
      }
      return true;
    case R.id.BACKUP_COMMAND:
      startActivity(new Intent("myexpenses.intent.backup"));
      return true;
    case R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND:
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
      return true;
    case R.id.REMIND_NO_COMMAND:
      SharedPreferencesCompat.apply(mSettings.edit().putLong("nextReminder" + (String) tag,-1));
      return true;
    case R.id.REMIND_LATER_COMMAND:
      String key = "nextReminder" + (String) tag;
      long treshold = ((String) tag).equals("Rate") ? TRESHOLD_REMIND_RATE : TRESHOLD_REMIND_CONTRIB;
      SharedPreferencesCompat.apply(mSettings.edit().putLong(key,sequenceCount+treshold));
      return true;
    case R.id.HELP_COMMAND:
      setHelpVariant();
      break;
    case R.id.MANAGE_PLANS_COMMAND:
      i = new Intent(this, ManageTemplates.class);
      i.putExtra("transferEnabled",transferEnabledGlobal());
      startActivity(i);
      return true;
    case R.id.DELETE_COMMAND_DO:
      finishActionMode();
      getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstance(
            TaskExecutionFragment.TASK_DELETE_TRANSACTION,
            (Long[])tag, null),
          "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_deleting),"PROGRESS")
        .commit();
      return true;
    case R.id.CREATE_ACCOUNT_COMMAND:
      //we need the accounts to be loaded in order to evaluate if the limit has been reached
      if (MyApplication.getInstance().isContribEnabled || (mAccountCount > 0 && mAccountCount < 5)) {
        i = new Intent(this, AccountEdit.class);
        if (tag != null)
          i.putExtra(KEY_CURRENCY,(String)tag);
        startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.ACCOUNTS_UNLIMITED, null);
      }
      return true;
      case R.id.DELETE_ACCOUNT_COMMAND:
        mAccountsCursor.moveToPosition(mCurrentPosition);
        long accountId = mAccountsCursor.getLong(columnIndexRowId); //we do not rely on mAccountId being in sync with mCurrentPosition
        if (accountId > 0) { //do nothing if accidentally we are positioned at an aggregate account
          MessageDialogFragment.newInstance(
              R.string.dialog_title_warning_delete_account,
              getString(R.string.warning_delete_account,mAccountsCursor.getString(columnIndexLabel)),
              new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                  accountId),
              null,
              MessageDialogFragment.Button.noButton())
            .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND_DO:
        //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
        mAccountId = 0;
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
           .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_ACCOUNT,(Long)tag, null), "ASYNC_TASK")
           .commit();
        return true;
      case R.id.SHARE_COMMAND:
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.tell_a_friend_message));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.menu_share)));
        return true;
      case R.id.CANCEL_CALLBACK_COMMAND:
        finishActionMode();
        return true;
    }
    return super.dispatchCommand(command, tag);
  }
  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    public MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    public String getFragmentName(int currentPosition) {
      //http://stackoverflow.com/questions/7379165/update-data-in-listfragment-as-part-of-viewpager
      //would call this function if it were visible
      //return makeFragmentName(R.id.viewpager,currentPosition);
      return "android:switcher:"+R.id.viewpager+":"+getItemId(currentPosition);
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
      feature.recordUsage();
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
      createRow(TYPE_SPLIT);
      break;
    case RESET_ALL:
      DialogUtils.showWarningResetDialog(this, mAccountId);
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
      builder.appendQueryParameter("mergeCurrencyAggregates", "1");
      return new CursorLoader(this,
          builder.build(), null, null, null, null);
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
    if (mAccountId != newAccountId)
      SharedPreferencesCompat.apply(
        mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccountId));
    mAccountId = newAccountId;
    setCustomTitle();
    mDrawerList.setItemChecked(position, true);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    mAccountCount = 0;
    mAccountsCursor = cursor;
    switch(loader.getId()) {
    case ACCOUNTS_CURSOR:
      if (!indexesCalculated) {
        columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID);
        columnIndexColor = mAccountsCursor.getColumnIndex(KEY_COLOR);
        columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
        columnIndexDescription = mAccountsCursor.getColumnIndex(KEY_DESCRIPTION);
        columnIndexLabel = mAccountsCursor.getColumnIndex(KEY_LABEL);
        indexesCalculated = true;
      }
      ((SimpleCursorAdapter) mDrawerListAdapter).swapCursor(mAccountsCursor);
      //swaping the cursor is altering the accountId, if the
      //sort order has changed, but we want to move to the same account as before
      long cacheAccountId = mAccountId;
      mViewPagerAdapter.swapCursor(cursor);
      mAccountId = cacheAccountId;
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
    String title = args.getString("result");
    if ((new Template(Transaction.getInstanceFromDb(args.getLong("transactionId")),title)).save() == null) {
      Toast.makeText(getBaseContext(),getString(R.string.template_title_exists,title), Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(getBaseContext(),getString(R.string.template_create_success,title), Toast.LENGTH_LONG).show();
    }
    finishActionMode();
  }
  @Override
  public void onCancelEditDialog() {
    finishActionMode();
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
  @Override
  public void onPostExecute(int taskId,Object o) {
    super.onPostExecute(taskId, o);
    switch(taskId) {
    case TaskExecutionFragment.TASK_CLONE:
      Integer successCount = (Integer) o;
      String msg = successCount == 0 ?  getString(R.string.clone_transaction_error) :
        getResources().getQuantityString(R.plurals.clone_transaction_success, successCount, successCount);
      Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
      break;
    case TaskExecutionFragment.TASK_REQUIRE_ACCOUNT:
      getSupportActionBar().show();
      setup();
      break;
    }
  }
  public void toggleCrStatus (View v) {
    getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_TOGGLE_CRSTATUS,(Long) v.getTag(), null), "ASYNC_TASK")
      .commit();
  }
  /**
   * @return true if for the current Account there is a second account
   * with the same currency we can transfer to
   */
  public boolean transferEnabled() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow("transfer_enabled")) > 0;
  }
  /**
   * @return true if for any Account there is a second account
   * with the same currency we can transfer to
   */
  public boolean transferEnabledGlobal() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null)
      return false;
    //we move to the last position in account cursor, and we check if it is an aggregate account
    //which means that there is at least one currency having multiple accounts
    mAccountsCursor.moveToLast();
    return mAccountsCursor.getLong(columnIndexRowId) < 0;
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
            Currency.getInstance(mAccountsCursor.getString(columnIndexCurrency)),
            mAccountsCursor.getLong(mAccountsCursor.getColumnIndex("current_balance")))));
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
      row.findViewById(R.id.TransferRow).setVisibility(
          c.getLong(columnIndexRowId)<0 ? View.GONE : View.VISIBLE);
      if (c.getLong(columnIndexRowId)>0) {
        setConvertedAmount((TextView)row.findViewById(R.id.sum_transfer), currency);
      }
      v.setBackgroundColor(c.getInt(columnIndexColor));
      setConvertedAmount((TextView)row.findViewById(R.id.opening_balance), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.sum_income), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.sum_expenses), currency);
      setConvertedAmount((TextView)row.findViewById(R.id.current_balance), currency);
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
    @SuppressLint("NewApi")
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.accounts_header, parent, false);
      }
      ((TextView) convertView.findViewById(R.id.sectionLabel)).setText(getHeaderId(position)==0?R.string.pref_manage_accounts_title:R.string.menu_aggregates);
      return convertView;
    }
    @Override
    public long getHeaderId(int position) {
      Cursor c = getCursor();
      c.moveToPosition(position);
      return c.getLong(columnIndexRowId)>0 ? 0 : 1;
    }
  }
  protected void onSaveInstanceState (Bundle outState) {
    super.onSaveInstanceState(outState);
    //detail fragment from notification should only be shown once
    if (idFromNotification !=0)
      outState.putLong("idFromNotification",0);
  }
}