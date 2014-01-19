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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
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
import android.support.v7.app.ActionBar.OnNavigationListener;
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
    EditTextDialogListener, OnNavigationListener,
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
  private SimpleCursorAdapter mDrawerListAdapter;
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
  private SimpleCursorAdapter mNavigationAdapter;
  private ListView mDrawerList;
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  
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
    mDrawerList = (ListView) findViewById(R.id.left_drawer);
    // set a custom shadow that overlays the main content when the drawer opens
    theme.resolveAttribute(R.attr.drawerShadow, value, true);
    mDrawerLayout.setDrawerShadow(value.resourceId, GravityCompat.START);
    String[] from = new String[]{"description","label","opening_balance","sum_income","sum_expenses","sum_transfer","current_balance"};
    // and an array of the fields we want to bind those fields to
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.sum_transfer,R.id.current_balance};
    mDrawerListAdapter = new SimpleCursorAdapter(this, R.layout.account_row, null, from, to,0) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        Cursor c = getCursor();
        c.moveToPosition(position);
        int col = c.getColumnIndex("currency");
        Currency currency = Utils.getSaveInstance(c.getString(col));
        View v = row.findViewById(R.id.color1);
        if (c.getLong(c.getColumnIndex(KEY_ROWID))<0) {
          row.findViewById(R.id.TransferRow).setVisibility(View.GONE);
        } else {
          setConvertedAmount((TextView)row.findViewById(R.id.sum_transfer), currency);
        }
        v.setBackgroundColor(c.getInt(c.getColumnIndex("color")));
        setConvertedAmount((TextView)row.findViewById(R.id.opening_balance), currency);
        setConvertedAmount((TextView)row.findViewById(R.id.sum_income), currency);
        setConvertedAmount((TextView)row.findViewById(R.id.sum_expenses), currency);
        setConvertedAmount((TextView)row.findViewById(R.id.current_balance), currency);
        col = c.getColumnIndex("description");
        String description = c.getString(col);
        if (description.equals(""))
          row.findViewById(R.id.description).setVisibility(View.GONE);
        return row;
      }
    };
    mDrawerList.setAdapter(mDrawerListAdapter);

    getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
        | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
    getSupportActionBar().setCustomView(R.layout.custom_title);
    theme.resolveAttribute(R.attr.drawerImage, value, true);
    mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
        value.resourceId, R.string.drawer_open, R.string.drawer_close) {

    /** Called when a drawer has settled in a completely closed state. */
    public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
    }

    /** Called when a drawer has settled in a completely open state. */
    public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
    }
};

  // Set the drawer toggle as the DrawerListener
  mDrawerLayout.setDrawerListener(mDrawerToggle);

    //setupActionBar();
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
      long idFromNotification = extras.getLong("transaction_id",0);
      if (idFromNotification != 0) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag("TRANSACTION_DETAIL") == null) {
          TransactionDetailFragment.newInstance(idFromNotification)
              .show(getSupportFragmentManager(), "TRANSACTION_DETAIL");
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
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_REQUIRE_ACCOUNT,null, null), "ASYNC_TASK")
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
    myPager.setCurrentItem(position,false);
  }
  private void setupActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    mNavigationAdapter = new SimpleCursorAdapter(
        actionBar.getThemedContext(),
        R.layout.account_navigation_spinner_item,
        null,
        new String[] {KEY_LABEL},
        new int[] {android.R.id.text1},
        0) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position,super.getView(position, convertView, parent));
      }
      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position,super.getDropDownView(position, convertView, parent));
      }
      private View getCustomView(int position, View row) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        row.findViewById(R.id.color1).setBackgroundColor(
            getItemId(position) < 0 ?
                colorAggregate :
                c.getInt(c.getColumnIndex(KEY_COLOR)));
        ((TextView) row.findViewById(R.id.end)).setText(
            Utils.formatCurrency(
                new Money(
                    Currency.getInstance(c.getString(c.getColumnIndex(KEY_CURRENCY))),
                    c.getLong(c.getColumnIndex("current_balance")))));
        return row;
      }
    };
    mNavigationAdapter.setDropDownViewResource(R.layout.account_navigation_spinner_dropdown_item);
    actionBar.setListNavigationCallbacks(mNavigationAdapter, this);
  }
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.EDIT_ACCOUNT_COMMAND).setVisible(mAccountId > 0);
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
    if (requestCode == ACTIVITY_EDIT && resultCode == RESULT_OK) {
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
        mAccountsCursor.moveToFirst();
        String currentCurrency = Account.getInstanceFromDb(mAccountId).currency.getCurrencyCode();
        int columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
        while (mAccountsCursor.isAfterLast() == false) {
          if (mAccountsCursor.getString(columnIndexCurrency).equals(currentCurrency)) {
            accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID));
            break;
          }
          mAccountsCursor.moveToNext();
        }
      } else {
        accountId = 0;
      }
    } else {
      accountId = mAccountId;
    }
    //since splits are immediately persisted they will not work without an account set
    if (accountId == 0 && type == TYPE_SPLIT)
      return;
    i.putExtra(KEY_ACCOUNTID,accountId);
    startActivityForResult(i, ACTIVITY_EDIT);
  }
/*  public boolean dispatchLongCommand(int command, Object tag) {
    Intent i;
    switch (command) {
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra("template_id", (Long) tag);
      i.putExtra("instantiate", true);
      startActivityForResult(i, ACTIVITY_EDIT);
      return true;
    }
    return false;
  }*/
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
      tl = (TransactionList) getSupportFragmentManager().findFragmentByTag(
          mViewPagerAdapter.getFragmentName(mCurrentPosition));
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
        AggregateAccount.getCachedInstance(mAccountId).persistGrouping(value);
        getContentResolver().notifyChange(TransactionProvider.ACCOUNTS_URI, null);
      } else {
        Account account = Account.getInstanceFromDb(mAccountId);
        account.grouping=value;
        account.save();
      }
      return true;
    case R.id.INSERT_TA_COMMAND:
      createRow(TYPE_TRANSACTION);
      return true;
    case R.id.INSERT_TRANSFER_COMMAND:
      if (transferEnabled()) {
        createRow(TYPE_TRANSFER);
      } else {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            getString(R.string.dialog_command_disabled_insert_transfer,Account.getInstanceFromDb(mAccountId).currency.getCurrencyCode())
              + " " + getString(R.string.dialog_command_disabled_insert_transfer_1),
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    case R.id.INSERT_SPLIT_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.SPLIT_TRANSACTION, null);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.SPLIT_TRANSACTION, null);
      }
      return true;
    case R.id.RESET_ACCOUNT_COMMAND:
      tl = (TransactionList) getSupportFragmentManager().findFragmentByTag(
          mViewPagerAdapter.getFragmentName(mCurrentPosition));
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
      i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID, mAccountId);
      startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
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
      getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_TRANSACTION,(Long)tag, null), "ASYNC_TASK")
      .commit();
      return true;
    case R.id.CREATE_COMMAND:
      //we need the accounts to be loaded in order to evaluate if the limit has been reached
      if (MyApplication.getInstance().isContribEnabled || (mAccountCount > 0 && mAccountCount < 5)) {
        i = new Intent(this, AccountEdit.class);
        startActivityForResult(i, 0);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.ACCOUNTS_UNLIMITED, null);
      }
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
      Account account;
      long accountId = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
      if (accountId < 0) {
        account = AggregateAccount.getCachedInstance(accountId);
        if (account == null)
          account = new AggregateAccount(cursor);
      } else {
      if (Account.isInstanceCached(accountId))
        account = Account.getInstanceFromDb(accountId);
        else
        account = new Account(cursor);
      }
      return TransactionList.newInstance(account);
    }

  }
  @Override
  public void onPageSelected(int position) {
    mCurrentPosition = position;
    mAccountsCursor.moveToPosition(position);
    setCurrentAccount();
    //getSupportActionBar().setSelectedNavigationItem(position);
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    switch(feature){
    case DISTRIBUTION:
      feature.recordUsage();
      Intent i = new Intent(this, ManageCategories.class);
      i.setAction("myexpenses.intent.distribution");
      i.putExtra(KEY_ACCOUNTID, mAccountId);
      if (tag != null) {
        int year = (int) ((Long)tag/1000);
        int groupingSecond = (int) ((Long)tag % 1000);
        i.putExtra("grouping", Account.getInstanceFromDb(mAccountId).grouping);
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
   * set the Current account to the current position of mAccountsCursor
   * @param newAccountId
   */
  private void setCurrentAccount() {
    long newAccountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID));
    if (mAccountId != newAccountId)
      SharedPreferencesCompat.apply(
        mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccountId));
    mAccountId = newAccountId;
    setCustomTitle();
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    int id = loader.getId();
    switch(id) {
    case ACCOUNTS_CURSOR:
      mAccountCount = 0;
      mAccountsCursor = cursor;
      //swaping the cursor is altering the accountId, if the
      //sort order has changed, but we want to move to the same account as before
      long cacheAccountId = mAccountId;
      mViewPagerAdapter.swapCursor(cursor);
      mAccountId = cacheAccountId;
      mAccountsCursor.moveToFirst();
      mCurrentPosition = -1;
      int columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID);
      while (mAccountsCursor.isAfterLast() == false) {
        long accountId = mAccountsCursor.getLong(columnIndexRowId);
        if (accountId == mAccountId) {
          mCurrentPosition = mAccountsCursor.getPosition();
        }
        if (accountId > 0) {
          mAccountCount++;
        }
        mAccountsCursor.moveToNext();
      }
      //the current account was deleted, we set it to the first
      if (mCurrentPosition == -1) {
        mCurrentPosition = 0;
        mAccountsCursor.moveToFirst();
        setCurrentAccount();
      } else {
        mAccountsCursor.moveToPosition(mCurrentPosition);
        setCustomTitle();
      }
      //mNavigationAdapter.swapCursor(mAccountsCursor);
      //getSupportActionBar().setSelectedNavigationItem(currentPosition);
      mDrawerListAdapter.swapCursor(mAccountsCursor);
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
  }
  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    mCurrentPosition = itemPosition;
    moveToPosition(itemPosition);
    return true;
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
    if (taskId == TaskExecutionFragment.TASK_REQUIRE_ACCOUNT) {
      //setCurrentAccount(((Account) o).id);
      getSupportActionBar().show();
      setup();
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
    //we move to the last position in account cursor, and we check if it an aggregate account
    //which means that there is at least one currency having multiple accounts
    mAccountsCursor.moveToLast();
    return mAccountsCursor.getLong(mAccountsCursor.getColumnIndexOrThrow(KEY_ROWID)) < 0;
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
        mAccountsCursor.getString(mAccountsCursor.getColumnIndex(KEY_LABEL)));
    ((TextView) titleBar.findViewById(R.id.end)).setText(Utils.formatCurrency(
        new Money(
            Currency.getInstance(mAccountsCursor.getString(mAccountsCursor.getColumnIndex(KEY_CURRENCY))),
            mAccountsCursor.getLong(mAccountsCursor.getColumnIndex("current_balance")))));
    titleBar.findViewById(R.id.color1).setBackgroundColor(
        mAccountId < 0 ?
            colorAggregate :
              mAccountsCursor.getInt(mAccountsCursor.getColumnIndex(KEY_COLOR)));
  }
}