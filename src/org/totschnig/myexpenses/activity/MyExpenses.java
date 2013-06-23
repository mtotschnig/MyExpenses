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

import org.example.qberticus.quickactions.BetterPopupWindow;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.SelectAccountDialogFragment;
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
import org.totschnig.myexpenses.ui.ButtonBar;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.ButtonBar.Action;
import org.totschnig.myexpenses.ui.ButtonBar.MenuButton;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;  
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
    OnClickListener,OnLongClickListener, OnSharedPreferenceChangeListener, 
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    EditTextDialogListener {
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
  
  private ArrayList<Action> mMoreItems;
  
  //private ExpensesDbAdapter mDbHelper;

  private Account mCurrentAccount;
  
  private void setCurrentAccount(Account newAccount) {
    long current_account_id = mCurrentAccount != null? mCurrentAccount.id : 0;
    this.mCurrentAccount = newAccount;
    MyApplication.setCurrentAccountColor(newAccount.color);
    if (current_account_id != newAccount.id)
      mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, newAccount.id)
      .putLong(MyApplication.PREFKEY_LAST_ACCOUNT, current_account_id)
      .commit();
    updateUIforCurrentAccount();
  }
  private SharedPreferences mSettings;
  private Cursor mAccountsCursor;
  //private Cursor mExpensesCursor;
  private MyViewPagerAdapter myAdapter;
  private ViewPager myPager;

  private ButtonBar mButtonBar;
  private MenuButton mAddButton;
  private MenuButton mSwitchButton;
  private MenuButton mResetButton;
  private MenuButton mSettingsButton;
  private MenuButton mHelpButton;
  private boolean mUseStandardMenu;
  private boolean scheduledRestart = false;

  /**
   * several dialogs need an object on which they operate, and this object must survive
   * orientation change, and the call to the contrib dialog, currently there are three use cases for this:
   * 3) CLONE_TRANSACTION: the transaction to be cloned
   */
  private long mDialogContextId = 0L;

  private BetterPopupWindow dw;
  private boolean mButtonBarIsFilled;

  private int mCurrentDialog = 0;

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
    setTheme(MyApplication.getThemeIdNoTitle());
    super.onCreate(savedInstanceState);
    //boolean titled = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    setContentView(R.layout.viewpager);
//    if(titled){
//      getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_layout);
//    }
    mSettings = MyApplication.getInstance().getSettings();
    if (mSettings.getInt("currentversion", -1) == -1) {
      if (MyApplication.backupExists()) {
        showDialogWrapper(R.id.CONFIRM_RESTORE_DIALOG);
        return;
      }
    }
    setup();
  }
  private void setup() {
    newVersionCheck();
    getSupportLoaderManager().initLoader(0, null, this);
    mUseStandardMenu = mSettings.getBoolean(MyApplication.PREFKEY_USE_STANDARD_MENU, false);
    mButtonBar = (ButtonBar) findViewById(R.id.ButtonBar);
    if (mUseStandardMenu) {
      hideButtonBar();
    } else {
      fillButtons();
    }
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
    if (mCurrentAccount == null) {
      long account_id = mSettings.getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
      try {
        setCurrentAccount(Account.getInstanceFromDb(account_id));
      } catch (DataObjectNotFoundException e) {
        //for any reason the account stored in pref no longer exists
        setCurrentAccount(requireAccount());
      }
    }
  }
  private void moveToNextAccount() {
    int currentPosition = myPager.getCurrentItem();
    currentPosition++;
    if (currentPosition >= myAdapter.getCount())
      currentPosition = 0;
    myPager.setCurrentItem(currentPosition);
  }
  private void moveToAccount(long accountId) {
    mAccountsCursor.moveToFirst();
    int currentPosition = 0;
    while (mAccountsCursor.isAfterLast() == false) {
      if (mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_ROWID)) == accountId) {
        currentPosition = mAccountsCursor.getPosition();
      }
      mAccountsCursor.moveToNext();
    }
    //if the account we are looking for no longer exists, which can happen if it was deleted in Manage Accounts
    //we move to the first account
    myPager.setCurrentItem(currentPosition);
    //onPageSelected is not triggered by setCurrentItem, but it makes sure that
    //currentaccount is set correctly
    onPageSelected(currentPosition);
  }
  private void fillSwitchButton() {
    mSwitchButton.clearMenu();
    final Cursor otherAccounts = getContentResolver().query(TransactionProvider.ACCOUNTS_URI,
        new String[] {KEY_ROWID, "label"}, KEY_ROWID + " != " + mCurrentAccount.id, null,null);
    if(otherAccounts.moveToFirst()){
      for (int i = 0; i < otherAccounts.getCount(); i++) {
        mSwitchButton.addItem(
            otherAccounts.getString(otherAccounts.getColumnIndex("label")),
            R.id.SWITCH_ACCOUNT_COMMAND,
            otherAccounts.getLong(otherAccounts.getColumnIndex(KEY_ROWID)));
        otherAccounts.moveToNext();
      }
    }
    mSwitchButton.addItem(R.string.menu_accounts_new,R.id.CREATE_ACCOUNT_COMMAND);
    mSwitchButton.addItem(R.string.menu_accounts_summary,R.id.ACCOUNT_OVERVIEW_COMMAND);
    otherAccounts.close();
  }
  private void fillAddButton() {
    mAddButton.clearMenu();
    final Cursor templates = getContentResolver().query(
        TransactionProvider.TEMPLATES_URI,
        null, "account_id = ?", new String[] {String.valueOf(mCurrentAccount.id)}, null);
    boolean gotTemplates = templates.moveToFirst();
    boolean gotTransfers = transfersEnabledP();
    if (gotTransfers) {
      mAddButton.addItem(R.string.transfer,R.id.INSERT_TRANSFER_COMMAND);
    }
    if(gotTemplates) {
      for (int i = 0; i < templates.getCount(); i++) {
        mAddButton.addItem(
            templates.getString(templates.getColumnIndex(KEY_TITLE)),
            R.id.NEW_FROM_TEMPLATE_COMMAND,
            templates.getLong(templates.getColumnIndex(KEY_ROWID)));
        templates.moveToNext();
      }
    }
    templates.close();
  }
  private void fillButtons() {
    mAddButton = mButtonBar.addButton(
        R.string.menu_new,
        android.R.drawable.ic_menu_add,
        R.id.INSERT_TA_COMMAND);
    //templates are sorted by usages, so that most often used templates are displayed in the menu
    //but in the menu we want them to appear in alphabetical order, and we want the other commands
    //in fixed positions
/*    mAddButton.setComparator(new Comparator<Button>() {
      public int compare(Button a, Button b) {
        if (a.getId() == R.id.MORE_ACTION_COMMAND) {
          return 1;
        }
        if (a.getId() == R.id.NEW_FROM_TEMPLATE_COMMAND) {
          if (b.getId() == R.id.NEW_FROM_TEMPLATE_COMMAND) {
            return ((String)b.getText()).compareToIgnoreCase((String) a.getText());
          }
          return 1;
        }
        if (a.getId() == R.id.INSERT_TRANSFER_COMMAND) {
          return 1;
        }
        return -1;
      }
    });*/
    mSwitchButton = mButtonBar.addButton(
        R.string.menu_accounts,
        R.drawable.ic_menu_goto,
        R.id.SWITCH_ACCOUNT_COMMAND);
    mSwitchButton.setTag(0L);
    
    mResetButton = mButtonBar.addButton(
        R.string.menu_reset_abrev,
        android.R.drawable.ic_menu_revert,
        R.id.RESET_ACCOUNT_COMMAND);
    
    mSettingsButton = mButtonBar.addButton(
        R.string.menu_settings_abrev,
        android.R.drawable.ic_menu_preferences,
        R.id.SETTINGS_COMMAND);
    mSettingsButton.addItem(R.string.menu_backup,R.id.BACKUP_COMMAND);
    mSettingsButton.addItem(R.string.menu_settings_account,R.id.EDIT_ACCOUNT_COMMAND);
    
    mHelpButton = mButtonBar.addButton(
        R.string.menu_help,
        android.R.drawable.ic_menu_help,
        R.id.HELP_COMMAND);
    mHelpButton.addItem(R.string.tutorial,R.id.WEB_COMMAND,"tutorial_r4");
    mHelpButton.addItem(R.string.help_heading_news,R.id.WEB_COMMAND,"news");
    mHelpButton.addItem(R.string.menu_faq,R.id.WEB_COMMAND,"faq");
    mHelpButton.addItem(R.string.menu_contrib,R.id.CONTRIB_COMMAND);
    mHelpButton.addItem("Feedback",R.id.FEEDBACK_COMMAND);
    mButtonBarIsFilled = true;
  }
  @Override
  public void onStop() {
    super.onStop();
    if (dw != null)
    dw.dismiss();
  }
  
  private void configButtons() {
    if (!mUseStandardMenu) {
      mResetButton.setEnabled(mCurrentAccount.getSize() > 0);
      fillSwitchButton();
      fillAddButton();
    }
  }
  
  /* (non-Javadoc)
* here we check if we have other accounts with the same category,
* only under this condition do we make the Insert Transfer Activity
* available
* @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
*/
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (!mUseStandardMenu)
      return false;
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.SWITCH_ACCOUNT_COMMAND)
      .setVisible(Account.count(null, null) > 1);
    menu.findItem(R.id.INSERT_TRANSFER_COMMAND)
      .setVisible(transfersEnabledP());
    menu.findItem(R.id.RESET_ACCOUNT_COMMAND)
      .setVisible(mCurrentAccount.getSize() > 0);
    menu.findItem(R.id.NEW_FROM_TEMPLATE_COMMAND)
      .setVisible(Template.countPerAccount(mCurrentAccount.id) > 0);
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    //numeric shortcuts are used from Monkeyrunner
    menu.add(0, R.id.INSERT_TA_COMMAND, 0, R.string.menu_create_transaction)
        .setIcon(android.R.drawable.ic_menu_add);
    menu.add(0, R.id.INSERT_TRANSFER_COMMAND, 0, R.string.menu_create_transfer)
        .setIcon(android.R.drawable.ic_menu_add);
    menu.add(0, R.id.NEW_FROM_TEMPLATE_COMMAND, 0, R.string.menu_new_from_template)
        .setIcon(android.R.drawable.ic_menu_add);
    menu.add(0, R.id.RESET_ACCOUNT_COMMAND,1,R.string.menu_reset)
        .setIcon(android.R.drawable.ic_menu_revert);
    menu.add(0, R.id.HELP_COMMAND,1,R.string.menu_help)
        .setIcon(android.R.drawable.ic_menu_help);
    menu.add(0, R.id.SWITCH_ACCOUNT_COMMAND,1,R.string.menu_change_account)
        .setIcon(R.drawable.ic_menu_goto);
    menu.add(0,R.id.SETTINGS_COMMAND,1,R.string.menu_settings)
        .setIcon(android.R.drawable.ic_menu_preferences);
    return true;
  }
  
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (dispatchCommand(item.getItemId(),null))
      return true;
    else
      return super.onMenuItemSelected(featureId, item);
  }

  /* (non-Javadoc)
  * upon return from CREATE or EDIT we call fillData to renew state of reset button
  * and to update current ballance
  * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
  */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_CREATE_ACCOUNT && resultCode == RESULT_OK && intent != null) {
      //we cannot use switchaccount yet, since the cursor has not yet been swapped yet
      setCurrentAccount(Account.getInstanceFromDb(intent.getLongExtra("account_id",0)));
      return;
    }
    updateUIforCurrentAccount();
    if (requestCode == ACTIVITY_EDIT && resultCode == RESULT_OK) {
      long nextReminder = mSettings.getLong("nextReminderRate",TRESHOLD_REMIND_RATE);
      long transactionCount = Transaction.getTransactionSequence();
      if (nextReminder != -1 && transactionCount >= nextReminder) {
        showDialogWrapper(R.id.REMIND_RATE_DIALOG);
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
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    menu.add(0, R.id.SHOW_DETAIL_COMMAND, 0, R.string.menu_show_detail);
    menu.add(0, R.id.CREATE_TEMPLATE_COMMAND, 0, R.string.menu_create_template);
    menu.add(0, R.id.CLONE_TRANSACTION_COMMAND, 0, R.string.menu_clone_transaction);
    if (Account.count(null, null) > 1) {
      menu.add(0,R.id.MOVE_TRANSACTION_COMMAND,0,R.string.menu_move_transaction);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Transaction t;
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
      SelectAccountDialogFragment.newInstance(mCurrentAccount.id, info.id)
        .show(getSupportFragmentManager(), "SELECT_ACCOUNT");
      return true;
    case R.id.CREATE_TEMPLATE_COMMAND:
      Bundle args = new Bundle();
      args.putLong("transactionId", info.id);
      args.putString("dialogTitle", getString(R.string.dialog_title_template_title));
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "TEMPLATE_TITLE");
      return true;
    }
    return super.onContextItemSelected(item);
  }

  /**
   * @param id we store the dialog id, so that we can dismiss it in our generic button handler
   */
  public void showDialogWrapper(int id) {
    mCurrentDialog = id;
    showDialog(id);
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    LayoutInflater li;
    View view;
    TextView tv;
    switch (id) {
    case R.id.CONFIRM_RESTORE_DIALOG:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.messagedialog, null);
      tv = (TextView)view.findViewById(R.id.message_text);
      tv.setText(R.string.dialog_confirm_restore_on_install);
      DialogUtils.setDialogTwoButtons(view,
          android.R.string.yes,R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(true),
          android.R.string.no,R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,Boolean.valueOf(false)
      );
      return new AlertDialog.Builder(this)
        .setCancelable(false)
        .setView(view)
        .create();
    case R.id.REMIND_RATE_DIALOG:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.messagedialog, null);
      tv = (TextView)view.findViewById(R.id.message_text);
      tv.setText(R.string.dialog_remind_rate);
      DialogUtils.setDialogThreeButtons(view,
          R.string.dialog_remind_no,R.id.REMIND_NO_COMMAND,"Rate",
          R.string.dialog_remind_later,R.id.REMIND_LATER_COMMAND,"Rate",
          R.string.dialog_remind_rate_yes,R.id.RATE_COMMAND,null);
      return new AlertDialog.Builder(this)
        .setTitle(R.string.app_name)
        .setView(view)
        .setCancelable(false)
        .create();
    }
    return super.onCreateDialog(id);
  }


  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("TemplateCreateDialogTransactionId",mDialogContextId);
   outState.putSerializable("MoreItems",mMoreItems);
   outState.putInt("currentDialog",mCurrentDialog);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mDialogContextId = savedInstanceState.getLong("TemplateCreateDialogTransactionId");
   mMoreItems = (ArrayList<Action>) savedInstanceState.getSerializable("MoreItems");
   mCurrentDialog = savedInstanceState.getInt("currentDialog");
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

  private void switchAccount(long accountId) {
    //TODO: write a test if the case where the account stored in last_account
    //is deleted, is correctly handled
    //store current account id since we need it for setting last_account in the end
    if (accountId == 0) {
      if (mSettings.getBoolean(MyApplication.PREFKEY_ACCOUNT_BUTTON_BEHAVIOUR,ACCOUNT_BUTTON_CYCLE) == ACCOUNT_BUTTON_TOGGLE) {
        //first check if we have the last_account stored
        accountId = mSettings.getLong(MyApplication.PREFKEY_LAST_ACCOUNT, 0);
        //if for any reason the last_account is identical to the current
        //we ignore it
        if (accountId == mCurrentAccount.id)
          accountId = 0;
      }
    }
    //cycle behaviour
    if (accountId == 0) {
      moveToNextAccount();
    } else {
      moveToAccount(accountId);
    }
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
  
  public boolean transfersEnabledP() {
    return Account.countPerCurrency(mCurrentAccount.currency) > 1;
  }
  @Override
  public void onClick(View v) {
    dispatchCommand(v.getId(),v.getTag());
  }

  public void onDialogButtonClicked(View v) {
    if (mCurrentDialog != 0)
      dismissDialog(mCurrentDialog);
    onClick(v);
  }
  public boolean dispatchLongCommand(int command, Object tag) {
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
  }
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
    case R.id.SWITCH_ACCOUNT_COMMAND:
      int accountCount = Account.count(null, null);
      if (accountCount > 1) {
        Long accountId = tag != null ? (Long) tag : 0;
        switchAccount(accountId);
      } else {
        MessageDialogFragment.newInstance(R.string.dialog_title_menu_accounts_explain,R.string.menu_accounts_explain,R.id.CREATE_ACCOUNT_COMMAND,null)
          .show(getSupportFragmentManager(),"ACCOUNTS_BUTTON_EXPLAIN");
          
      }
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
    case R.id.ACCOUNT_OVERVIEW_COMMAND:
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
      Transaction.getInstanceFromTemplate((Long) tag).save();
      //myAdapter.notifyDataSetChanged();
      configButtons();
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
    case R.id.USE_STANDARD_MENU_COMMAND:
      mUseStandardMenu = true;
      mSettings.edit().putBoolean(MyApplication.PREFKEY_USE_STANDARD_MENU,true).commit();
      hideButtonBar();
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
    if (dw != null) {
      dw.dismiss();
      dw = null;
    }
    return true;
  }
  private void hideButtonBar() {
    findViewById(R.id.ButtonBarDividerTop).setVisibility(View.GONE);
    findViewById(R.id.ButtonBarDividerBottom).setVisibility(View.GONE);
    mButtonBar.setVisibility(View.GONE);
  }
  private void showButtonBar() {
    findViewById(R.id.ButtonBarDividerTop).setVisibility(View.VISIBLE);
    findViewById(R.id.ButtonBarDividerBottom).setVisibility(View.VISIBLE);
    mButtonBar.setVisibility(View.VISIBLE);
  }
  @Override
  public boolean onLongClick(View v) {
    if (v instanceof MenuButton) {
      int height = myPager.getHeight();
      MenuButton mb = (MenuButton) v;
      dw = mb.getMenu(height);
      if (dw == null)
        return false;
      dw.showLikeQuickAction();
      return true;
    } else {
      return dispatchLongCommand(v.getId(),v.getTag());
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_USE_STANDARD_MENU)) {
      boolean newValueB = mSettings.getBoolean(MyApplication.PREFKEY_USE_STANDARD_MENU, false);
      if (newValueB != mUseStandardMenu) {
        if (newValueB)
          hideButtonBar();
        else {
          showButtonBar();
          if (!mButtonBarIsFilled)
            fillButtons();
            fillSwitchButton();
        }
      }
      mUseStandardMenu = newValueB;
    }
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
  }
  public void updateUIforCurrentAccount() {
    View divider = findViewById(R.id.ButtonBarDividerTop);
    if (divider != null) {
      divider.setBackgroundColor(mCurrentAccount.color);
      findViewById(R.id.ButtonBarDividerBottom).setBackgroundColor(mCurrentAccount.color);
    }
    configButtons();
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
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    // TODO specify columns
    String[] projection = null;
    CursorLoader cursorLoader = new CursorLoader(this,
        TransactionProvider.ACCOUNTS_URI, projection, null, null, null);
    return cursorLoader;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    myAdapter.swapCursor(cursor);
    mAccountsCursor = cursor;
    moveToAccount(mCurrentAccount.id);
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
    fillAddButton();
  }
}
