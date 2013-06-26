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

import java.util.ArrayList;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromUriDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromUriDialogFragment.SelectFromUriDialogListener;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
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
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;
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
    OnClickListener, OnSharedPreferenceChangeListener, 
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    EditTextDialogListener, OnNavigationListener,
    SelectFromUriDialogListener {
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_PREF=2;
  public static final int ACTIVITY_CREATE_ACCOUNT=3;
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

  private LoaderManager mManager;

  //private ExpensesDbAdapter mDbHelper;

  private Account mCurrentAccount;

  private void setCurrentAccount(Account newAccount) {
    long currentAccountId = mCurrentAccount != null? mCurrentAccount.id : 0;
    this.mCurrentAccount = newAccount;
    long newAccountId = newAccount.id;
    MyApplication.setCurrentAccountColor(newAccount.color);
    if (currentAccountId != newAccount.id)
      mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccountId)
      .putLong(MyApplication.PREFKEY_LAST_ACCOUNT, currentAccountId)
      .commit();
  }
  private SharedPreferences mSettings;
  private Cursor mAccountsCursor;
  //private Cursor mExpensesCursor;
  private MyViewPagerAdapter myAdapter;
  private ViewPager myPager;

  private boolean scheduledRestart = false;

  /**
   * TODO
   * several dialogs need an object on which they operate, and this object must survive
   * orientation change, and the call to the contrib dialog, currently there are three use cases for this:
   * 3) CLONE_TRANSACTION: the transaction to be cloned
   */
  private long mDialogContextId = 0L;

/*  private int monkey_state = 0;

  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event) {
    Intent i;
    if (keyCode == MyApplication.BACKDOOR_KEY) {
      switch (monkey_state) {
      case 0:
        dispatchCommand(R.id.CREATE_ACCOUNT_COMMAND,null);
        monkey_state = 1;
        return true;
      case 1:
        dispatchCommand(R.id.INSERT_TA_COMMAND,null);
        monkey_state = 2;
        return true;
      case 2:
        showDialogWrapper(RESET_DIALOG_ID);
        monkey_state = 3;
        return true;
      case 3:
        startActivityForResult(new Intent(MyExpenses.this, MyPreferenceActivity.class),ACTIVITY_PREF);
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }*/
  
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
    super.onCreate(savedInstanceState);
    setContentView(R.layout.viewpager);

    mSettings = MyApplication.getInstance().getSettings();
    if (mSettings.getInt("currentversion", -1) == -1) {
      if (MyApplication.backupExists()) {
        MessageDialogFragment.newInstance(R.string.dialog_title_restore_on_install,
            R.string.dialog_confirm_restore_on_install,
            R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(true),
            R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(false))
            .show(getSupportFragmentManager(),"RESTORE_ON_INSTALL");
        return;
      }
    }
    setup();
  }
  private void setup() {
    long account_id = 0;
    Bundle extras = getIntent().getExtras();
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
        setCurrentAccount(requireAccount());
      }
    } else {
      setCurrentAccount(requireAccount());
    }
    newVersionCheck();
    mSettings.registerOnSharedPreferenceChangeListener(this);

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
    mManager.initLoader(-1, null, this);
  }
  private void moveToPosition(int position) {
    myPager.setCurrentItem(position);
    configButtons();
  }
  private void fillNavigation() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(true);
    //actionBar.setDisplayUseLogoEnabled(true);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        getSupportActionBar().getThemedContext(),
        R.layout.sherlock_spinner_item, mAccountsCursor, new String[] {KEY_LABEL}, new int[] {android.R.id.text1}) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        TextView tv1 = (TextView)row.findViewById(android.R.id.text1);
        Cursor c = getCursor();
        c.moveToPosition(position);
        int color = c.getInt(c.getColumnIndex(KEY_COLOR));
        row.setBackgroundColor(color);
        tv1.setTextColor( Utils.getTextColorForBackground(color));
        return row;
      }
    };
    adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
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
    menu.findItem(R.id.RESET_ACCOUNT_COMMAND)
      .setVisible(mCurrentAccount.getSize() > 0);
    menu.findItem(R.id.INSERT_TRANSFER_COMMAND)
      .setVisible(Account.countPerCurrency(mCurrentAccount.currency) > 1);
    menu.findItem(R.id.NEW_FROM_TEMPLATE_COMMAND)
      .setVisible(Template.countPerAccount(mCurrentAccount.id) > 0);
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
    return true;
  }
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (dispatchCommand(item.getItemId(),null))
      return true;
    else
      return super.onMenuItemSelected(featureId, item);
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
    if (requestCode == ACTIVITY_CREATE_ACCOUNT && resultCode == RESULT_OK && intent != null) {
      //we cannot use moveToaccount yet, since the cursor has not yet been swapped yet
      setCurrentAccount(Account.getInstanceFromDb(intent.getLongExtra("account_id",0)));
    }
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
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    menu.add(0, R.id.SHOW_DETAIL_COMMAND, 0, R.string.menu_show_detail);
    menu.add(0, R.id.CREATE_TEMPLATE_COMMAND, 0, R.string.menu_create_template);
    menu.add(0, R.id.CLONE_TRANSACTION_COMMAND, 0, R.string.menu_clone_transaction);
    if (Account.count(null, null) > 1 && Transaction.getType(info.id).equals(Transaction.class)) {
      menu.add(0,R.id.MOVE_TRANSACTION_COMMAND,0,R.string.menu_move_transaction);
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
      mDialogContextId = info.id;
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.CLONE_TRANSACTION);
      }
      else {
        showContribDialog(Feature.CLONE_TRANSACTION);
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
      args.putParcelable("uri",TransactionProvider.ACCOUNTS_URI);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_account));
      args.putString("selection",KEY_ROWID + " != " + mCurrentAccount.id);
      args.putString("column", KEY_LABEL);
      args.putLong("contextTransactionId",info.id);
      SelectFromUriDialogFragment.newInstance(args)
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

  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("TemplateCreateDialogTransactionId",mDialogContextId);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mDialogContextId = savedInstanceState.getLong("TemplateCreateDialogTransactionId");
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
   * if there are already accounts defined, return the first one
   * otherwise create a new account, and return it
   */
  private Account requireAccount() {
    Account account;
    Long accountId = Account.firstId();
    if (accountId == null) {
      account = new Account(
          getString(R.string.app_name),
          0,
          getString(R.string.default_account_description)
      );
      account.save();
    } else {
      account = Account.getInstanceFromDb(accountId);
    }
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
    int current_version = getVersionNumber();
    if (prev_version == current_version)
      return;
    if (prev_version == -1) {
      //prevent preference change listener from firing when preference activity is called first time
      PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
      //edit.putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, mCurrentAccount.id).commit();
      edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version).commit();
      showHelpDialog();
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
      VersionDialogFragment.newInstance(versionInfo, getVersionName())
        .show(getSupportFragmentManager(),"VERSION_INFO");
    }
  }
  /**
   * retrieve information about the current version
   * @return concatenation of versionName, versionCode and buildTime
   * buildTime is automatically stored in property file during build process
   */
  public String getVersionInfo() {
    String version = "";
    String versionname = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = " (revision " + pi.versionCode + ") ";
      versionname = pi.versionName;
      //versiontime = ", " + R.string.installed + " " + sdf.format(new Date(pi.lastUpdateTime));
    } catch (Exception e) {
      Log.e("MyExpenses", "Package info not found", e);
    }
    return versionname + version  + MyApplication.BUILD_DATE;
  }
  /**
   * @return version name
   */
  public String getVersionName() {
    String version = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionName;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }
  /**
   * @return version number (versionCode)
   */
  public int getVersionNumber() {
    int version = -1;
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionCode;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }
  @Override
  public void onClick(View v) {
    dispatchCommand(v.getId(),v.getTag());
  }

  public void onDialogButtonClicked(View v) {
    onClick(v);
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
    if (super.dispatchCommand(command,tag))
      return true;
    Intent i;
    switch (command) {
    case R.id.FEEDBACK_COMMAND:
      i = new Intent(android.content.Intent.ACTION_SEND);
      i.setType("plain/text");
      i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ MyApplication.FEEDBACK_EMAIL });
      i.putExtra(android.content.Intent.EXTRA_SUBJECT,
          "[" + getString(R.string.app_name) + 
          getVersionName() + "] Feedback"
      );
      i.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.feedback_email_message));
      startActivity(i);
      break;
    case R.id.CONTRIB_COMMAND:
      showContribInfoDialog(false);
      break;
    case R.id.INSERT_TA_COMMAND:
      createRow(TYPE_TRANSACTION);
      break;
    case R.id.INSERT_TRANSFER_COMMAND:
      createRow(TYPE_TRANSFER);
      break;
    case R.id.CREATE_ACCOUNT_COMMAND:
      i = new Intent(MyExpenses.this, AccountEdit.class);
      startActivityForResult(i, ACTIVITY_CREATE_ACCOUNT);
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
    case R.id.RESET_ACCOUNT_COMMAND_DO:
      if (Utils.isExternalStorageAvailable()) {
        i = new Intent(this, Export.class);
        //should also be availalbe in the tag
        i.putExtra(KEY_ROWID, mCurrentAccount.id);
        startActivityForResult(i, ACTIVITY_EXPORT);
      } else { 
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      break;
    case R.id.SETTINGS_COMMAND:
      startActivityForResult(new Intent(MyExpenses.this, MyPreferenceActivity.class),ACTIVITY_PREF);
      break;
    case R.id.EDIT_ACCOUNT_COMMAND:
      i = new Intent(MyExpenses.this, AccountEdit.class);
      i.putExtra(KEY_ROWID, mCurrentAccount.id);
      startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
      break;
    case android.R.id.home:
      startActivityForResult(new Intent(MyExpenses.this, ManageAccounts.class),ACTIVITY_PREF);
      break;
    case R.id.BACKUP_COMMAND:
      startActivity(new Intent("myexpenses.intent.backup"));
      break;
    case R.id.WEB_COMMAND:
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://" + MyApplication.HOST + "/#" + (String) tag));
      startActivity(i);
      break;
    case R.id.HELP_COMMAND:
      showHelpDialog();
      break;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      Bundle args = new Bundle();
      args.putInt("id", R.id.NEW_FROM_TEMPLATE_COMMAND);
      args.putParcelable("uri",TransactionProvider.TEMPLATES_URI);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_template));
      args.putString("selection",KEY_ACCOUNTID + " = " + mCurrentAccount.id);
      args.putString("column", KEY_TITLE);
      SelectFromUriDialogFragment.newInstance(args)
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
    default:
      return false;
    }
    return true;
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      scheduledRestart = true;
    }
  }
  @Override
  protected void onResume() {
    super.onResume();
    if(scheduledRestart) {
      scheduledRestart = false;
      Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(i);
    }
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
    mAccountsCursor.moveToPosition(position);
    long accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID));
    setCurrentAccount(Account.getInstanceFromDb(accountId));
    getSupportActionBar().setSelectedNavigationItem(position);
  }
  @Override
  public void contribFeatureCalled(Feature feature) {
    feature.recordUsage();
    Transaction.getInstanceFromDb(mDialogContextId).saveAsNew();
  }
  @Override
  public void contribFeatureNotCalled() {
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    // TODO specify columns
    String[] projection = null;
      return new CursorLoader(this,
          TransactionProvider.ACCOUNTS_URI, projection, null, null, null);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    if (loader.getId() == -1) {
      myAdapter.swapCursor(cursor);
      mAccountsCursor = cursor;
      fillNavigation();
      //select the current account after filling
      mAccountsCursor.moveToFirst();
      int currentPosition = 0;
      while (mAccountsCursor.isAfterLast() == false) {
        if (mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID)) == mCurrentAccount.id) {
          currentPosition = mAccountsCursor.getPosition();
        }
        mAccountsCursor.moveToNext();
      }
      getSupportActionBar().setSelectedNavigationItem(currentPosition);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    myAdapter.swapCursor(null);
  }
  @Override
  public void onPageScrollStateChanged(int arg0) {
    // TODO Auto-generated method stub
    
  }
  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
    // TODO Auto-generated method stub
    
  }
  public void showHelpDialog() {
    HelpDialogFragment.newInstance(getVersionInfo())
      .show(getSupportFragmentManager(),"HELP");
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
    }
  }
  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
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
}