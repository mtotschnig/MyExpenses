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
import java.util.ArrayList;
import java.util.HashMap;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment.SelectFromCursorDialogListener;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.dialog.WelcomeDialogFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.util.AllButOneCursorWrapper;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;  
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
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
    SelectFromCursorDialogListener, ContribIFace {
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_EDIT_ACCOUNT=4;
  public static final int ACTIVITY_EXPORT=5;

  public static final boolean TYPE_TRANSACTION = true;
  public static final boolean TYPE_TRANSFER = false;
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

  private Account mCurrentAccount;

  int currentPosition = -1;
  private void setCurrentAccount(Account newAccount) {
    long currentAccountId = mCurrentAccount != null? mCurrentAccount.id : 0;
    this.mCurrentAccount = newAccount;
    long newAccountId = newAccount.id;
    if (currentAccountId != newAccount.id)
      mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccountId)
      .putLong(MyApplication.PREFKEY_LAST_ACCOUNT, currentAccountId)
      .commit();
  }
  private SharedPreferences mSettings;
  private Cursor mAccountsCursor, mTemplatesCursor;
  private HashMap<String,Integer> currencyAccountCount;
  //private Cursor mExpensesCursor;
  private MyViewPagerAdapter myAdapter;
  private ViewPager myPager;
  private String fragmentCallbackTag = null;
  
  /* (non-Javadoc)
   * Called when the activity is first created.
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    //if we are launched from the contrib app, we refreshed the cached contrib status
    Bundle extras = getIntent().getExtras();
    if (extras != null && extras.getBoolean("refresh_contrib",false))
      MyApplication.getInstance().refreshContribEnabled();
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
      if (MyApplication.backupExists()) {
        if (!mSettings.getBoolean("inRestoreOnInstall", false)) {
          MessageDialogFragment.newInstance(R.string.dialog_title_restore_on_install,
              R.string.dialog_confirm_restore_on_install,
              R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(true),
              R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(false))
              .show(getSupportFragmentManager(),"RESTORE_ON_INSTALL");
          mSettings.edit().putBoolean("inRestoreOnInstall", true).commit();
        }
        return;
      }
    }
    setup();
  }
  private void setup() {
    Bundle extras = getIntent().getExtras();
    long account_id = 0;
    if (extras != null) {
      account_id = extras.getLong(KEY_ROWID,0);
    }
    if (account_id == 0)
      account_id = mSettings.getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
    if (account_id != 0) {
      try {
        setCurrentAccount(Account.getInstanceFromDb(account_id));
      } catch (DataObjectNotFoundException e) {
        //for any reason the account stored in pref no longer exists
        setCurrentAccount(Account.getInstanceFromDb(Account.firstId()));
      }
    } else {
      setCurrentAccount(requireAccount());
    }
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
  
  private void configButtons() {
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
    //I would prefer to use setEnabled, but the disabled state unfortunately
    //is not visually reflected in the actionbar
    menu.findItem(R.id.INSERT_TRANSFER_COMMAND)
      .setVisible(currentPosition > -1 && currencyAccountCount.get(mCurrentAccount.currency.getCurrencyCode()) > 1);
    menu.findItem(R.id.NEW_FROM_TEMPLATE_COMMAND)
      .setVisible(mTemplatesCursor != null && mTemplatesCursor.getCount() > 0);
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
  * upon return from CREATE or EDIT we call fillData to renew state of reset button
  * and to update current balance
  * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
  */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    configButtons();
    if (requestCode == ACTIVITY_EDIT && resultCode == RESULT_OK) {
      long nextReminder = mSettings.getLong("nextReminderRate",TRESHOLD_REMIND_RATE);
      long transactionCount = Transaction.getTransactionSequence();
      if (nextReminder != -1 && transactionCount >= nextReminder) {
        new RemindRateDialogFragment().show(getSupportFragmentManager(),"REMIND_RATE");
        return;
      }
      if (!MyApplication.getInstance().isContribEnabled) {
        nextReminder = mSettings.getLong("nextReminderContrib",TRESHOLD_REMIND_CONTRIB);
        if (nextReminder != -1 && transactionCount >= nextReminder) {
          showContribInfoDialog(true);
          return;
        }
      }
    }
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Transaction t;
    Bundle args;
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      Transaction.delete(info.id);
      configButtons();
      return true;
    case R.id.CLONE_TRANSACTION_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.CLONE_TRANSACTION, info.id);
      }
      else {
        CommonCommands.showContribDialog(this,Feature.CLONE_TRANSACTION, info.id);
      }
      return true;
    case R.id.SHOW_DETAIL_COMMAND:
      t = Transaction.getInstanceFromDb(info.id);
      String method = "";
      if (t.methodId != null) {
        method= PaymentMethod.getInstanceFromDb(t.methodId).getDisplayLabel();
      }
      String msg =  ((t.comment != null && t.comment.length() != 0) ?
          t.comment : "");
      if (t.payee != null && t.payee.length() != 0) {
        if (!msg.equals("")) {
          msg += "\n";
        }
        msg += getString(R.string.payee) + ": " + t.payee;
      }
      if (!method.equals("")) {
        if (!msg.equals("")) {
          msg += "\n";
        }
        msg += getString(R.string.method) + ": " + method;
      }
      Toast.makeText(getBaseContext(), msg != "" ? msg : getString(R.string.no_details), Toast.LENGTH_LONG).show();
      return true;
    case R.id.MOVE_TRANSACTION_COMMAND:
      args = new Bundle();
      args.putInt("id", R.id.MOVE_TRANSACTION_COMMAND);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_account));
      //args.putString("selection",KEY_ROWID + " != " + mCurrentAccount.id);
      args.putString("column", KEY_LABEL);
      args.putLong("contextTransactionId",info.id);
      args.putInt("cursorId", ACCOUNTS_OTHER_CURSOR);
      SelectFromCursorDialogFragment.newInstance(args)
        .show(getSupportFragmentManager(), "SELECT_ACCOUNT");
      return true;
    case R.id.CREATE_TEMPLATE_COMMAND:
      args = new Bundle();
      args.putLong("transactionId", info.id);
      args.putString("dialogTitle", getString(R.string.dialog_title_template_title));
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "TEMPLATE_TITLE");
      return true;
    }
    return super.onContextItemSelected(item);
  }
  /**
   * start ExpenseEdit Activity for a new transaction/transfer
   * @param type either {@link #TYPE_TRANSACTION} or {@link #TYPE_TRANSFER}
   */
  private void createRow(boolean type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("operationType", type);
    i.putExtra(KEY_ACCOUNTID,mCurrentAccount.id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }

  /**
   * create a new account, and return it
   */
  private Account requireAccount() {
    Account account = new Account(
          getString(R.string.app_name),
          0,
          getString(R.string.default_account_description)
      );
      account.save();
    return account;
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
      edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version).commit();
      WelcomeDialogFragment.newInstance()
        .show(getSupportFragmentManager(),"WELCOME");
    } else if (prev_version != current_version) {
      ArrayList<CharSequence> versionInfo = new ArrayList<CharSequence>();
      edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version).commit();
      if (prev_version < 19) {
        //renamed
        edit.putString(MyApplication.PREFKEY_SHARE_TARGET,mSettings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
      if (prev_version < 26) {
        versionInfo.add(getString(R.string.version_26_upgrade_info));
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
      if (prev_version < 32) {
        String target = mSettings.getString(MyApplication.PREFKEY_SHARE_TARGET,"");
        if (target.startsWith("ftp")) {
          Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (!Utils.isIntentAvailable(this,intent)) {
            versionInfo.add(getString(R.string.version_32_upgrade_info));
          }
        }
      }
      if (prev_version < 34) {
        versionInfo.add(getString(R.string.version_34_upgrade_info));
      }
      if (prev_version < 35) {
        versionInfo.add(getString(R.string.version_35_upgrade_info));
      }
      if (prev_version < 39) {
        versionInfo.add(Html.fromHtml(getString(R.string.version_39_upgrade_info,Utils.getContribFeatureLabelsAsFormattedList(this))));
      }
      if (prev_version < 40) {
        DbUtils.fixDateValues(getContentResolver());
        //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
        //if they are already above both tresholds, so we set some delay
        mSettings.edit().putLong("nextReminderContrib",Transaction.getTransactionSequence()+23).commit();
        versionInfo.add(getString(R.string.version_40_upgrade_info));
      }
      if (prev_version < 41) {
        versionInfo.add(getString(R.string.version_41_upgrade_info));
      }
      if (prev_version < 46) {
        versionInfo.add(getString(R.string.version_46_upgrade_info));
      }
      VersionDialogFragment.newInstance(versionInfo)
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
    case R.id.CONTRIB_COMMAND:
      showContribInfoDialog(false);
      break;
    case R.id.INSERT_TA_COMMAND:
      createRow(TYPE_TRANSACTION);
      break;
    case R.id.INSERT_TRANSFER_COMMAND:
      createRow(TYPE_TRANSFER);
      break;
    case R.id.RESET_ACCOUNT_COMMAND:
      if (Utils.isExternalStorageAvailable()) {
        DialogUtils.showWarningResetDialog(this,mCurrentAccount.id);
      } else {
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      break;
    case R.id.EDIT_ACCOUNT_COMMAND:
      i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID, mCurrentAccount.id);
      startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
      break;
    case android.R.id.home:
      i = new Intent(this, ManageAccounts.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(i);
      break;
    case R.id.BACKUP_COMMAND:
      startActivity(new Intent("myexpenses.intent.backup"));
      break;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      Bundle args = new Bundle();
      args.putInt("id", R.id.NEW_FROM_TEMPLATE_COMMAND);
      args.putInt("cursorId", TEMPLATES_CURSOR);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_template));
      args.putString("column", KEY_TITLE);
      SelectFromCursorDialogFragment.newInstance(args)
        .show(getSupportFragmentManager(), "SELECT_TEMPLATE");
      break;
    case R.id.RATE_COMMAND:
      mSettings.edit().putLong("nextReminderRate", -1).commit();
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("market://details?id=org.totschnig.myexpenses"));
      if (Utils.isIntentAvailable(this,i)) {
        startActivity(i);
      } else {
        Toast.makeText(getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
      }
      break;
    case R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND:
      mSettings.edit().remove("inRestoreOnInstall").commit();
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
      setup();
      break;
    case R.id.REMIND_NO_COMMAND:
      mSettings.edit().putLong("nextReminder" + (String) tag,-1).commit();
      break;
    case R.id.REMIND_LATER_COMMAND:
      String key = "nextReminder" + (String) tag;
      long treshold = ((String) tag).equals("Rate") ? TRESHOLD_REMIND_RATE : TRESHOLD_REMIND_CONTRIB;
      mSettings.edit().putLong(key,Transaction.getTransactionSequence()+treshold).commit();
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
      return TransactionList.newInstance(accountId);
    }

  }
  @Override
  public void onPageSelected(int position) {
    currentPosition = position;
    mAccountsCursor.moveToPosition(position);
    long accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID));
    setCurrentAccount(Account.getInstanceFromDb(accountId));
    getSupportActionBar().setSelectedNavigationItem(position);
  }
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    feature.recordUsage();
    Transaction.getInstanceFromDb((Long) tag).saveAsNew();
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
      mAccountsCursor.moveToFirst();
      currentPosition = 0;
      String currency;
      Integer count;
      currencyAccountCount = new HashMap<String,Integer>();
      while (mAccountsCursor.isAfterLast() == false) {
        if (mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID)) == mCurrentAccount.id) {
          currentPosition = mAccountsCursor.getPosition();
        }
        currency = mAccountsCursor.getString(mAccountsCursor.getColumnIndex(KEY_CURRENCY));
        count = currencyAccountCount.get(currency);
        if (count == null)
          count = 0;
        currencyAccountCount.put(currency, count+1);
        mAccountsCursor.moveToNext();
      }
      myAdapter.swapCursor(cursor);
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
      Transaction.move(
          args.getLong("contextTransactionId"),
          args.getLong("result"));
      break;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      Transaction.getInstanceFromTemplate(args.getLong("result")).save();
    }
    configButtons();
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
}