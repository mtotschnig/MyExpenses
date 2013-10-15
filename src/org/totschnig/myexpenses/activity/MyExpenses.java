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
import java.util.HashMap;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SelectGroupingDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment.SelectFromCursorDialogListener;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.dialog.WelcomeDialogFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.util.AllButOneCursorWrapper;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.view.View;
import android.widget.Toast;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;  
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.TypedValue;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 * @author Michael Totschnig
 *
 */
public class MyExpenses extends ProtectedFragmentActivity implements
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    EditTextDialogListener, OnNavigationListener,
    SelectFromCursorDialogListener, ContribIFace, TaskExecutionFragment.TaskCallbacks  {
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_EDIT_ACCOUNT=4;
  public static final int ACTIVITY_EXPORT=5;

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
  public static final int TEMPLATES_CURSOR=1;
  public static final int ACCOUNTS_OTHER_CURSOR=2;
  private LoaderManager mManager;

  //private ExpensesDbAdapter mDbHelper;

  int currentPosition = -1;
  private void setCurrentAccount(long newAccountId) {
    if (mAccountId != newAccountId)
      SharedPreferencesCompat.apply(
        mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccountId));
    mAccountId = newAccountId;
  }
  private SharedPreferences mSettings;
  private Cursor mAccountsCursor, mTemplatesCursor;
  private HashMap<String,Integer> currencyAccountCount;
  //private Cursor mExpensesCursor;
  private MyViewPagerAdapter myAdapter;
  private ViewPager myPager;
  private String fragmentCallbackTag = null;
  public boolean mTransferEnabled = false;
  private long mAccountId = 0;
  public enum HelpVariant {
    crStatus
  }
  private void setHelpVariant() {
    helpVariant = Account.getInstanceFromDb(mAccountId).type.equals(Type.CASH) ?
        null : HelpVariant.crStatus;
  }
  /**
   * stores the number of transactions that have been 
   * created in the db, updated after each creation of
   * a new transaction
   */
  private long sequenceCount = 0;
  
  /* (non-Javadoc)
   * Called when the activity is first created.
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    //if we are launched from the contrib app, we refresh the cached contrib status
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      if (extras.getBoolean("refresh_contrib",false))
        MyApplication.getInstance().refreshContribEnabled();
      String instrumentLanguage = extras.getString("instrument_language");
      if (instrumentLanguage != null) {
        Locale locale = new Locale(instrumentLanguage,extras.getString("instrument_country"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config,
            getResources().getDisplayMetrics());
      }
    }
    setTheme(MyApplication.getThemeId());
    mSettings = MyApplication.getInstance().getSettings();
    int prev_version = mSettings.getInt(MyApplication.PREFKEY_CURRENT_VERSION, -1);
    if (prev_version == -1) {
      //prevent preference change listener from firing when preference file is created
      PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.viewpager);
    if (prev_version == -1) {
      getSupportActionBar().hide();
      if (MyApplication.backupExists()) {
        if (!mSettings.getBoolean("inRestoreOnInstall", false)) {
          DialogFragment df = MessageDialogFragment.newInstance(R.string.dialog_title_restore_on_install,
              R.string.dialog_confirm_restore_on_install,
              R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(true),
              R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(false));
          df.setCancelable(false);
          df.show(getSupportFragmentManager(),"RESTORE_ON_INSTALL");
          SharedPreferencesCompat.apply(
              mSettings.edit().putBoolean("inRestoreOnInstall", true));
        }
      } else if (!mSettings.getBoolean("inInitialSetup", false)) {
        initialSetup();
      }
      return;
    }
    if (extras != null) {
      mAccountId = extras.getLong(KEY_ROWID,0);
    }
    if (mAccountId == 0)
      mAccountId = mSettings.getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
    setup();
  }
  private void initialSetup() {
    SharedPreferencesCompat.apply(
      mSettings.edit().putBoolean("inInitialSetup", true));
    FragmentManager fm = getSupportFragmentManager();
    fm.beginTransaction()
      .add(WelcomeDialogFragment.newInstance(),"WELCOME")
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_REQUIRE_ACCOUNT,null, null), "REQUIRE_ACCOUNT_TASK")
      .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_setup),"PROGRESS")
      .commit();
  }
  private void setup() {
    newVersionCheck();

    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin,margin, true);
    myAdapter = new MyViewPagerAdapter(this,getSupportFragmentManager(),null);
    myPager = (ViewPager) this.findViewById(R.id.viewpager);
    myPager.setAdapter(this.myAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin(10);
    myPager.setPageMarginDrawable(margin.resourceId);
    mManager= getSupportLoaderManager();
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }
  private void moveToPosition(int position) {
    myPager.setCurrentItem(position,false);
    if (mManager.getLoader(position) != null && !mManager.getLoader(position).isReset()) {
        mManager.restartLoader(position, null,this);
    } else {
      mManager.initLoader(position, null, this);
    }
    configButtons();
  }
  private void fillNavigation() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        actionBar.getThemedContext(),
        R.layout.custom_spinner_item, mAccountsCursor, new String[] {KEY_LABEL}, new int[] {android.R.id.text1}) {
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
        row.findViewById(R.id.color1).setBackgroundColor(c.getInt(c.getColumnIndex(KEY_COLOR)));
        return row;
      }
    };
    adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
    actionBar.setListNavigationCallbacks(adapter, this);
  }
  
  public void configButtons() {
    supportInvalidateOptionsMenu();
  }
  
  /* (non-Javadoc)
* here we check if we have other accounts with the same category,
* only under this condition do we make the Insert Transfer Activity
* available
* @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
*/
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mAccountId== 0)
      return true;
    //I would prefer to use setEnabled, but the disabled state unfortunately
    //is not visually reflected in the actionbar
    if (currentPosition > -1) {
      Integer sameCurrencyCount = currencyAccountCount.get(
          Account.getInstanceFromDb(mAccountId).currency.getCurrencyCode());
      if (sameCurrencyCount != null && sameCurrencyCount >1)
        mTransferEnabled = true;
    }
    Utils.menuItemSetEnabled(menu,R.id.INSERT_TRANSFER_COMMAND,mTransferEnabled);
    Utils.menuItemSetEnabled(menu,R.id.NEW_FROM_TEMPLATE_COMMAND,
        mTemplatesCursor != null && mTemplatesCursor.getCount() > 0);
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  /* (non-Javadoc)
  * upon return from CREATE or EDIT we call configButtons
  * and check if we should show one of the reminderDialogs
  * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
  */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    //configButtons();
    if (requestCode == ACTIVITY_EDIT && resultCode == RESULT_OK) {
      long nextReminder = mSettings.getLong("nextReminderRate",TRESHOLD_REMIND_RATE);
      //TODO move getTransactionSequence out of UI thread, probably cache in Application class
      sequenceCount = intent.getLongExtra("sequence_count", 0);
      if (nextReminder != -1 && sequenceCount >= nextReminder) {
        new RemindRateDialogFragment().show(getSupportFragmentManager(),"REMIND_RATE");
        return;
      }
      if (!MyApplication.getInstance().isContribEnabled) {
        nextReminder = mSettings.getLong("nextReminderContrib",TRESHOLD_REMIND_CONTRIB);
        if (nextReminder != -1 && sequenceCount >= nextReminder) {
          showContribInfoDialog(true);
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
    i.putExtra("transferEnabled",mTransferEnabled);
    i.putExtra(KEY_ACCOUNTID,mAccountId);
    startActivityForResult(i, ACTIVITY_EDIT);
  }
  /**
   * check if this is the first invocation of a new version
   * in which case help dialog is presented
   * also is used for hooking version specific upgrade procedures
   */
  public void newVersionCheck() {
    Editor edit = mSettings.edit();
    int prev_version = mSettings.getInt(MyApplication.PREFKEY_CURRENT_VERSION, -1);
    int current_version = CommonCommands.getVersionNumber(this);
    if (prev_version == current_version)
      return;
    if (prev_version == -1) {
      //edit.putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, mCurrentAccount.id).commit();
      SharedPreferencesCompat.apply(edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version));
    } else if (prev_version != current_version) {
      SharedPreferencesCompat.apply(edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version));
      if (prev_version < 19) {
        //renamed
        edit.putString(MyApplication.PREFKEY_SHARE_TARGET,mSettings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
      if (prev_version < 28) {
        Log.i("MyExpenses",String.format("Upgrading to version 28: Purging %d transactions from datbase",
            getContentResolver().delete(TransactionProvider.TRANSACTIONS_URI,
                KEY_ACCOUNTID + " not in (SELECT _id FROM accounts)", null)));
      }
      if (prev_version < 30) {
        if (mSettings.getString(MyApplication.PREFKEY_SHARE_TARGET,"") != "") {
          edit.putBoolean(MyApplication.PREFKEY_PERFORM_SHARE,true).commit();
        }
      }
      if (prev_version < 40) {
        DbUtils.fixDateValues(getContentResolver());
        //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
        //if they are already above both tresholds, so we set some delay
        mSettings.edit().putLong("nextReminderContrib",Transaction.getSequenceCount()+23).commit();
      }
      VersionDialogFragment.newInstance(prev_version)
        .show(getSupportFragmentManager(),"VERSION_INFO");
    }
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
    switch (command) {
    case R.id.DISTRIBUTION_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
      contribFeatureCalled(Feature.DISTRIBUTION, null);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.DISTRIBUTION, null);
      }
      return true;
    case R.id.GROUPING_COMMAND:
      SelectGroupingDialogFragment.newInstance(
          R.id.GROUPING_COMMAND_DO,Account.getInstanceFromDb(mAccountId).grouping.ordinal())
        .show(getSupportFragmentManager(), "SELECT_GROUPING");
      return true;
    case R.id.GROUPING_COMMAND_DO:
      Account account = Account.getInstanceFromDb(mAccountId);
      account.grouping=Account.Grouping.values()[(Integer)tag];
      account.save();
      return true;
    case R.id.CONTRIB_COMMAND:
      showContribInfoDialog(false);
      return true;
    case R.id.INSERT_TA_COMMAND:
      createRow(TYPE_TRANSACTION);
      return true;
    case R.id.INSERT_TRANSFER_COMMAND:
      createRow(TYPE_TRANSFER);
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
      if (Utils.isExternalStorageAvailable()) {
        DialogUtils.showWarningResetDialog(this,mAccountId);
      } else {
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      return true;
    case R.id.EDIT_ACCOUNT_COMMAND:
      i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID, mAccountId);
      startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
      return true;
    case android.R.id.home:
      i = new Intent(this, ManageAccounts.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(i);
      return true;
    case R.id.BACKUP_COMMAND:
      startActivity(new Intent("myexpenses.intent.backup"));
      return true;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      Bundle args = new Bundle();
      args.putInt("id", R.id.NEW_FROM_TEMPLATE_COMMAND);
      args.putInt("cursorId", TEMPLATES_CURSOR);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_template));
      args.putString("column", KEY_TITLE);
      SelectFromCursorDialogFragment.newInstance(args)
        .show(getSupportFragmentManager(), "SELECT_TEMPLATE");
      return true;
    case R.id.RATE_COMMAND:
      SharedPreferencesCompat.apply(mSettings.edit().putLong("nextReminderRate", -1));
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("market://details?id=org.totschnig.myexpenses"));
      if (Utils.isIntentAvailable(this,i)) {
        startActivity(i);
      } else {
        Toast.makeText(getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
      }
      return true;
    case R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND:
      SharedPreferencesCompat.apply(mSettings.edit().remove("inRestoreOnInstall"));
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
    }
    return super.dispatchCommand(command, tag);
  }
  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    public MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
      long accountId = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
      //we want to make sure that the fragment does not need to create a new cursor
      //when getting the account object, so we
      //set up the account object that the fragment can retrieve with getInstanceFromDb
      //since it is cached by the Account class
      //we only need to do this, if the account has not been cached yet
      if (!Account.isInstanceCached(accountId))
        new Account(accountId,cursor);
      return TransactionList.newInstance(accountId);
    }

  }
  @Override
  public void onPageSelected(int position) {
    currentPosition = position;
    mAccountsCursor.moveToPosition(position);
    long accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID));
    setCurrentAccount(accountId);
    getSupportActionBar().setSelectedNavigationItem(position);
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    switch(feature){
    case DISTRIBUTION:
      feature.recordUsage();
      Intent i = new Intent(this, ManageCategories.class);
      i.putExtra(KEY_ACCOUNTID, mAccountId);
      i.putExtra("grouping",Grouping.NONE);
      if (tag != null) {
        int year = (int) ((Long)tag/1000);
        int groupingSecond = (int) ((Long)tag % 1000);
        i.putExtra("grouping", Account.getInstanceFromDb(mAccountId).grouping);
        i.putExtra("groupingYear",year);
        i.putExtra("groupingSecond", groupingSecond);
      }
      startActivity(i);
      break;
    case SPLIT_TRANSACTION:
      createRow(TYPE_SPLIT);
      break;
    }
  }
  @Override
  public void contribFeatureNotCalled() {
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    String[] projection;
    switch(id) {
    case ACCOUNTS_CURSOR:
      // TODO specify columns
      projection = null;
        return new CursorLoader(this,
          TransactionProvider.ACCOUNTS_URI, projection, null, null, null);
    }
    projection = new String[] {KEY_ROWID,KEY_TITLE};
    String selection = KEY_ACCOUNTID + " = ?";
    mAccountsCursor.moveToPosition(currentPosition);
    String[] selectionArgs = new String[] {
        String.valueOf(mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID))) };
    return new CursorLoader(this,TransactionProvider.TEMPLATES_URI,
          projection,selection,selectionArgs,null);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    int id = loader.getId();
    switch(id) {
    case ACCOUNTS_CURSOR:
      mAccountsCursor = cursor;
      //swaping the cursor is altering the accountId, if the
      //sort order has changed, but we want to move to the same account as before
      long cacheAccountId = mAccountId;
      myAdapter.swapCursor(cursor);
      mAccountId = cacheAccountId;
      mAccountsCursor.moveToFirst();
      currentPosition = -1;
      int columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID),
          columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
      String currency;
      Integer count;
      currencyAccountCount = new HashMap<String,Integer>();
      while (mAccountsCursor.isAfterLast() == false) {
        if (mAccountsCursor.getLong(columnIndexRowId) == mAccountId) {
          currentPosition = mAccountsCursor.getPosition();
        }
        currency = mAccountsCursor.getString(columnIndexCurrency);
        count = currencyAccountCount.get(currency);
        if (count == null)
          count = 0;
        currencyAccountCount.put(currency, count+1);
        mAccountsCursor.moveToNext();
      }
      //the current account was deleted, we set it to the first
      if (currentPosition == -1) {
        currentPosition = 0;
        mAccountsCursor.moveToFirst();
        mAccountId = mAccountsCursor.getLong(columnIndexRowId);
      }
      fillNavigation();
      getSupportActionBar().setSelectedNavigationItem(currentPosition);
      if ("SELECT_ACCOUNT".equals(fragmentCallbackTag)) {
       ((SelectFromCursorDialogFragment) getSupportFragmentManager().findFragmentByTag("SELECT_ACCOUNT"))
          .setCursor(new AllButOneCursorWrapper(mAccountsCursor,currentPosition));
       fragmentCallbackTag = null;
      }
      return;
    }
    //templates cursor that are loaded are not necessarily for the current account
    if (id==currentPosition) {
      mTemplatesCursor = cursor;
      configButtons();
      if ("SELECT_TEMPLATE".equals(fragmentCallbackTag)) {
        ((SelectFromCursorDialogFragment) getSupportFragmentManager().findFragmentByTag("SELECT_TEMPLATE"))
          .setCursor(mTemplatesCursor);
        fragmentCallbackTag = null;
      }
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    if (arg0.getId() == ACCOUNTS_CURSOR) {
      myAdapter.swapCursor(null);
      currentPosition = -1;
      mAccountsCursor = null;
    } else {
      mTemplatesCursor = null;
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
  public void showContribInfoDialog(boolean reminderP) {
    ContribInfoDialogFragment.newInstance(reminderP).show(getSupportFragmentManager(),"CONTRIB_INFO");
  }
  @Override
  public void onFinishEditDialog(Bundle args) {
    String title = args.getString("result");
    if ((new Template(Transaction.getInstanceFromDb(args.getLong("transactionId")),title)).save() == null) {
      Toast.makeText(getBaseContext(),getString(R.string.template_title_exists,title), Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(getBaseContext(),getString(R.string.template_create_success,title), Toast.LENGTH_LONG).show();
      configButtons();
    }
  }
  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    currentPosition = itemPosition;
    moveToPosition(itemPosition);
    return true;
  }
  @Override
  public void onItemSelected(Bundle args) {
    switch(args.getInt("id")) {
    case R.id.MOVE_TRANSACTION_COMMAND:
      getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_MOVE,
          args.getLong("contextTransactionId"), args.getLong("result")), "TOGGLE_TASK")
      .commit();
      break;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE,
          args.getLong("result"), null), "TOGGLE_TASK")
      .commit();
    }
  }
  @Override
  public Cursor getCursor(int cursorId,String fragmentCallbackTag) {
    Cursor c = null;
    switch(cursorId) {
    case ACCOUNTS_CURSOR:
      c = mAccountsCursor;
      break;
    case ACCOUNTS_OTHER_CURSOR:
      c = mAccountsCursor == null ? null : new AllButOneCursorWrapper(mAccountsCursor,currentPosition);
      break;
    case TEMPLATES_CURSOR:
      c = mTemplatesCursor;
    }
    if (c==null)
      this.fragmentCallbackTag = fragmentCallbackTag;
    return c;
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
    if (taskId == TaskExecutionFragment.TASK_REQUIRE_ACCOUNT) {
      setCurrentAccount(((Account) o).id);
      getSupportActionBar().show();
      SharedPreferencesCompat.apply(mSettings.edit().remove("inInitialSetup"));
      setup();
    }
    super.onPostExecute(taskId, o);
  }
  public void toggleCrStatus (View v) {
    getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_TOGGLE_CRSTATUS,(Long) v.getTag(), null), "TOGGLE_TASK")
      .commit();
  }
}