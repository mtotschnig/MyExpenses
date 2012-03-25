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

package org.totschnig.myexpenses;

import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.example.qberticus.quickactions.BetterPopupWindow;
import org.totschnig.myexpenses.Account.AccountNotFoundException;
import org.totschnig.myexpenses.ButtonBar.MenuButton;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
//import android.util.DisplayMetrics;
import android.text.method.LinkMovementMethod;
import android.util.Log;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 * @author Michael Totschnig
 *
 */
public class MyExpenses extends ListActivity implements OnClickListener,OnLongClickListener, OnSharedPreferenceChangeListener {
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_PREF=2;
  public static final int ACTIVITY_CREATE_ACCOUNT=3;
  public static final int ACTIVITY_EDIT_ACCOUNT=4;
  
  private static final int INSERT_TA_COMMAND_ID = Menu.FIRST + 1;
  private static final int INSERT_TRANSFER_COMMAND_ID = Menu.FIRST + 2;
  private static final int SWITCH_ACCOUNT_COMMAND_ID = Menu.FIRST + 3;
  private static final int CREATE_ACCOUNT_COMMAND_ID = Menu.FIRST + 4;
  private static final int RESET_ACCOUNT_COMMAND_ID = Menu.FIRST + 5;
  private static final int SETTINGS_COMMAND_ID = Menu.FIRST + 6;
  private static final int BACKUP_COMMAND_ID = Menu.FIRST + 7;
  private static final int EDIT_ACCOUNT_COMMAND_ID = Menu.FIRST + 8;
  private static final int HELP_COMMAND_ID = Menu.FIRST + 9;
  private static final int CHANGES_COMMAND_ID = Menu.FIRST + 10;
  private static final int TUTORIAL_COMMAND_ID = Menu.FIRST + 11;
  public static final int DELETE_COMMAND_ID = Menu.FIRST + 12;
  public static final int SHOW_DETAIL_COMMAND_ID = Menu.FIRST + 13;
  
  public static final boolean TYPE_TRANSACTION = true;
  public static final boolean TYPE_TRANSFER = false;
  public static final String TRANSFER_EXPENSE = "=>";
  public static final String TRANSFER_INCOME = "<=";
  static final int HELP_DIALOG_ID = 0;
  static final int CHANGES_DIALOG_ID = 1;
  static final int VERSION_DIALOG_ID = 2;
  static final int RESET_DIALOG_ID = 3;
  static final int BACKUP_DIALOG_ID = 4;
  static final int ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID = 5;
  static final int USE_STANDARD_MENU_DIALOG_ID = 6;
  static final int SELECT_ACCOUNT_DIALOG_ID = 7;

  private String mVersionInfo;
  
  private ExpensesDbAdapter mDbHelper;

  private Account mCurrentAccount;
  
  private SharedPreferences mSettings;
  private Cursor mExpensesCursor;

  private ButtonBar mButtonBar;
  private MenuButton mAddButton;
  private MenuButton mSwitchButton;
  private MenuButton mResetButton;
  private MenuButton mSettingsButton;
  private MenuButton mHelpButton;
  private TextView mTransferButton;
  private boolean mUseStandardMenu;

  private BetterPopupWindow dw;
  private boolean mButtonBarIsFilled;

/*  private int monkey_state = 0;

  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event) {
    Intent i;
    if (keyCode == KeyEvent.KEYCODE_ENVELOPE) {
      switch (monkey_state) {
      case 0:
        i = new Intent(MyExpenses.this, AccountEdit.class);
        i.putExtra(ExpensesDbAdapter.KEY_ROWID, mCurrentAccount.id);
        startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
        monkey_state = 1;
        return true;
      case 1:
        i = new Intent(this, ExpenseEdit.class);
        i.putExtra("operationType", TYPE_TRANSACTION);
        i.putExtra(ExpensesDbAdapter.KEY_ACCOUNTID,mCurrentAccount.id);
        startActivityForResult(i, ACTIVITY_EDIT);
        monkey_state = 2;
        return true;
      case 2:
        getListView().requestFocus();
        monkey_state = 3;
        return true;
      case 3:
        showDialog(RESET_DIALOG_ID);
        monkey_state = 4;
        return true;
      case 4:
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
    super.onCreate(savedInstanceState);
    setContentView(R.layout.expenses_list);
    mDbHelper = MyApplication.db();
    mSettings = ((MyApplication) getApplicationContext()).getSettings();
    newVersionCheck();
    if (mCurrentAccount == null) {
      long account_id = mSettings.getLong("current_account", 0);
      try {
        mCurrentAccount = new Account(mDbHelper,account_id);
      } catch (AccountNotFoundException e) {
        //for any reason the account stored in pref no longer exists
        mCurrentAccount = requireAccount();
      }
    }
    mUseStandardMenu = mSettings.getBoolean("use_standard_menu", false);
    mButtonBar = (ButtonBar) findViewById(R.id.ButtonBar);
    if (mUseStandardMenu) {
      mButtonBar.setVisibility(View.GONE);
    } else {
      fillButtons();
    }
    fillData();
    registerForContextMenu(getListView());
    mSettings.registerOnSharedPreferenceChangeListener(this);

  }
  private void fillSwitchButton() {
    mSwitchButton.addItem(R.string.menu_accounts_new,CREATE_ACCOUNT_COMMAND_ID);
    final Cursor otherAccounts = mDbHelper.fetchAccountOther(mCurrentAccount.id,false);
    if(otherAccounts.moveToFirst()){
      for (int i = 0; i < otherAccounts.getCount(); i++) {
        TextView accountTV = mSwitchButton.addItem(
            otherAccounts.getString(otherAccounts.getColumnIndex("label")),SWITCH_ACCOUNT_COMMAND_ID);
        accountTV.setTag(
            otherAccounts.getLong(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID)));
        otherAccounts.moveToNext();
      }
    }
    otherAccounts.close();
  }
  private void fillButtons() {
    mAddButton = mButtonBar.addButton(
        R.string.menu_new,
        android.R.drawable.ic_menu_add,
        INSERT_TA_COMMAND_ID);
    mTransferButton = mAddButton.addItem(R.string.transfer,INSERT_TRANSFER_COMMAND_ID);
    mAddButton.addItem(R.string.transaction,INSERT_TA_COMMAND_ID);
    
    mSwitchButton = mButtonBar.addButton(
        R.string.menu_accounts,
        R.drawable.ic_menu_goto,
        SWITCH_ACCOUNT_COMMAND_ID);
    mSwitchButton.setTag(new Long(0));
    fillSwitchButton();
    
    mResetButton = mButtonBar.addButton(
        R.string.menu_reset,
        android.R.drawable.ic_menu_revert,
        RESET_ACCOUNT_COMMAND_ID);
    
    mSettingsButton = mButtonBar.addButton(
        R.string.menu_settings_abrev,
        android.R.drawable.ic_menu_preferences,
        SETTINGS_COMMAND_ID);
    mSettingsButton.addItem(R.string.menu_settings_account,EDIT_ACCOUNT_COMMAND_ID);
    mSettingsButton.addItem(R.string.menu_backup,BACKUP_COMMAND_ID);
    mSettingsButton.addItem(R.string.menu_settings,SETTINGS_COMMAND_ID);
    
    mHelpButton = mButtonBar.addButton(
        R.string.menu_help,
        android.R.drawable.ic_menu_help,
        HELP_COMMAND_ID);
    mHelpButton.addItem(R.string.tutorial,TUTORIAL_COMMAND_ID);
    mHelpButton.addItem(R.string.menu_changes,CHANGES_COMMAND_ID);
    mHelpButton.addItem(R.string.menu_help,HELP_COMMAND_ID);
    mButtonBarIsFilled = true;
  }
  @Override
  public void onStop() {
    super.onStop();
    if (dw != null)
    dw.dismiss();
  }
  /**
   * binds the Cursor for all expenses to the list view
   */
  private void fillData() {
    mExpensesCursor = mDbHelper.fetchExpenseAll(mCurrentAccount.id);
    startManagingCursor(mExpensesCursor);

    setTitle(mCurrentAccount.label);

    TextView startView= (TextView) findViewById(R.id.start);
    startView.setText(Utils.formatCurrency(mCurrentAccount.openingBalance,mCurrentAccount.currency));

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"label",ExpensesDbAdapter.KEY_DATE,ExpensesDbAdapter.KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.date,R.id.amount};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter expense = new SimpleCursorAdapter(this, R.layout.expense_row, mExpensesCursor, from, to)  {
      /* (non-Javadoc)
       * calls {@link #convText for formatting the values retrieved from the cursor}
       * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
       */
      @Override
      public void setViewText(TextView v, String text) {
        switch (v.getId()) {
        case R.id.date:
          text = Utils.convDate(text);
          break;
        case R.id.amount:
          text = Utils.convAmount(text,mCurrentAccount.currency);
          break;
        }
        super.setViewText(v, text);
      }
      /* (non-Javadoc)
       * manipulates the view for amount (setting expenses to red) and
       * category (indicate transfer direction with => or <=
       * @see android.widget.CursorAdapter#getView(int, android.view.View, android.view.ViewGroup)
       */
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        TextView tv1 = (TextView)row.findViewById(R.id.amount);
        Cursor c = getCursor();
        c.moveToPosition(position);
        int col = c.getColumnIndex(ExpensesDbAdapter.KEY_AMOUNT);
        float amount = c.getFloat(col);
        if (amount < 0) {
          tv1.setTextColor(android.graphics.Color.RED);
          // Set the background color of the text.
        }
        else {
          tv1.setTextColor(android.graphics.Color.BLACK);
        }
        TextView tv2 = (TextView)row.findViewById(R.id.category);
        col = c.getColumnIndex(ExpensesDbAdapter.KEY_TRANSFER_PEER);
        if (c.getLong(col) != 0) 
          tv2.setText(((amount < 0) ? TRANSFER_EXPENSE : TRANSFER_INCOME) + tv2.getText());
        return row;
      }
    };
    setListAdapter(expense);
    setCurrentBalance(); 
    configButtons();
  }

  private void setCurrentBalance() {
    TextView endView= (TextView) findViewById(R.id.end);
    endView.setText(Utils.formatCurrency(mCurrentAccount.getCurrentBalance(),mCurrentAccount.currency));    
  }
  
  private void configButtons() {
    //mSwitchButton.setEnabled(mDbHelper.getAccountCount(null) > 1);
    if (!mUseStandardMenu) {
      mResetButton.setEnabled(mExpensesCursor.getCount() > 0);
      mTransferButton.setEnabled(transfersEnabledP());
      mSwitchButton.clearMenu();
      fillSwitchButton();
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
    menu.findItem(SWITCH_ACCOUNT_COMMAND_ID)
      .setVisible(mDbHelper.getAccountCount(null) > 1);
    menu.findItem(INSERT_TRANSFER_COMMAND_ID)
      .setVisible(transfersEnabledP());
    menu.findItem(RESET_ACCOUNT_COMMAND_ID)
      .setVisible(mExpensesCursor.getCount() > 0);
    return true;
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    //numeric shortcuts are used from Monkeyrunner
    menu.add(0, INSERT_TA_COMMAND_ID, 0, R.string.menu_insert_ta)
        .setIcon(android.R.drawable.ic_menu_add)
        .setAlphabeticShortcut('a');
    menu.add(0, INSERT_TRANSFER_COMMAND_ID, 0, R.string.menu_insert_transfer)
        .setIcon(android.R.drawable.ic_menu_add)
        .setAlphabeticShortcut('b');
    menu.add(0, RESET_ACCOUNT_COMMAND_ID,1,R.string.menu_reset)
        .setIcon(android.R.drawable.ic_menu_revert)
        .setAlphabeticShortcut('c');;
    menu.add(0, HELP_COMMAND_ID,1,R.string.menu_help)
        .setIcon(android.R.drawable.ic_menu_help)
        .setAlphabeticShortcut('d');
    menu.add(0, SWITCH_ACCOUNT_COMMAND_ID,1,R.string.select_account)
        .setIcon(android.R.drawable.ic_menu_manage)
        .setAlphabeticShortcut('e');
    menu.add(0,SETTINGS_COMMAND_ID,1,R.string.menu_settings)
        .setIcon(android.R.drawable.ic_menu_preferences)
        .setAlphabeticShortcut('f');
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
  * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
  */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_CREATE_ACCOUNT && resultCode == RESULT_OK && intent != null) {
         switchAccount(intent.getLongExtra("account_id",0));
         return;
    }
    if (requestCode == ACTIVITY_EDIT_ACCOUNT && resultCode == RESULT_OK) {
      //TODO: maybe store currentaccount in application class,
      //thus we would not need to refetch it here
      try {
        mCurrentAccount = new Account(mDbHelper,mCurrentAccount.id);
      } catch (AccountNotFoundException e) {
        //should not happen
        Log.w("MyExpenses","unable to refetch current account " + mCurrentAccount.id);
      }
      fillData();
    }
    //we call configButtons even with RESULT_CANCEL, since we
    //might return from preferences on RESULT_CANCEL, but user has been
    //changing accounts before
    if (requestCode == ACTIVITY_EDIT && resultCode == RESULT_OK) {
      fillData();
    } else {
      configButtons();
    }
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, DELETE_COMMAND_ID, 0, R.string.menu_delete);
    menu.add(0, SHOW_DETAIL_COMMAND_ID, 0, R.string.menu_show_detail);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_COMMAND_ID:
      long transfer_peer = mExpensesCursor.getLong(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
      if (transfer_peer == 0)
        mDbHelper.deleteExpense(info.id);
      else
        mDbHelper.deleteTransfer(info.id,transfer_peer);
      fillData();
      return true;
    case SHOW_DETAIL_COMMAND_ID:
      mExpensesCursor.moveToPosition(info.position);
      Toast.makeText(getBaseContext(),
          mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)) +
          "\n" +
          getString(R.string.payee) + ": " + mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow("payee")), Toast.LENGTH_LONG).show();
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    LayoutInflater li;
    View view;
    switch (id) {
    case HELP_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.aboutview, null);
      TextView tv;
      tv = (TextView)view.findViewById(R.id.aboutVersionCode);
      tv.setText(getVersionInfo());
      tv = (TextView)view.findViewById(R.id.help_project_home);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv = (TextView)view.findViewById(R.id.help_feedback);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv = (TextView)view.findViewById(R.id.help_licence_gpl);
      tv.setMovementMethod(LinkMovementMethod.getInstance());

      return new AlertDialog.Builder(this)
        .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
        .setIcon(R.drawable.about)
        .setView(view)
        .setNeutralButton(R.string.menu_changes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            showDialog(CHANGES_DIALOG_ID);
          }
        })
        .setPositiveButton(R.string.tutorial, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            startActivity( new Intent(MyExpenses.this, Tutorial.class) );
          }
        })
        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            dismissDialog(HELP_DIALOG_ID);
          }
        }).create();
    case CHANGES_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.changeview, null);
      ListView changeList = (ListView) view.findViewById(R.id.changelog);

      ListAdapter adapter = new SimpleAdapter(this, VersionList.get() , R.layout.version_row, 
          new String[] { "version", "date","changes" }, 
          new int[] { R.id.version, R.id.date, R.id.changes }) {
          public boolean isEnabled(int position) 
          { 
            return false; 
          }
          public boolean areAllItemsEnabled() 
          { 
            return false; 
          }
      };
      changeList.setAdapter(adapter);

      
      
      return new AlertDialog.Builder(this)
        .setTitle(R.string.menu_changes)
        .setIcon(R.drawable.about)
        .setView(view)
        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            dismissDialog(CHANGES_DIALOG_ID);
          }
        })
        .create();
    case VERSION_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.versiondialog, null);
      TextView versionInfo= (TextView) view.findViewById(R.id.versionInfo);
      versionInfo.setText(mVersionInfo);
      return new AlertDialog.Builder(this)
        .setTitle(R.string.important_version_information)
        .setIcon(R.drawable.about)
        .setView(view)
        .setNeutralButton(R.string.button_continue, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            showDialog(HELP_DIALOG_ID);
          }
        })
        .create();
    case RESET_DIALOG_ID:
      return new AlertDialog.Builder(this)
        .setMessage(R.string.warning_reset_account)
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              if (Utils.isExternalStorageAvailable())
                reset();
              else 
                Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
            }
        })
        .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dismissDialog(RESET_DIALOG_ID);
          }
        }).create();
    case ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID:
      return new AlertDialog.Builder(this)
        .setMessage(R.string.menu_accounts_explain)
        .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dismissDialog(ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID);
          }
        }).create();
    case USE_STANDARD_MENU_DIALOG_ID:
      return new AlertDialog.Builder(this)
        .setMessage(R.string.suggest_use_standard_menu)
        .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
             mUseStandardMenu = true;
             mSettings.edit().putBoolean("use_standard_menu",true).commit();
             mButtonBar.setVisibility(View.GONE);
             dismissDialog(USE_STANDARD_MENU_DIALOG_ID);
           }
        }).
        setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dismissDialog(USE_STANDARD_MENU_DIALOG_ID);
          }
        }).create();
    case SELECT_ACCOUNT_DIALOG_ID:
      final Cursor otherAccounts = mDbHelper.fetchAccountOther(mCurrentAccount.id,true);
      final String[] accountLabels = Utils.getStringArrayFromCursor(otherAccounts, "label");
      final int[] accountIds = Utils.getIntArrayFromCursor(otherAccounts, ExpensesDbAdapter.KEY_ROWID);
      otherAccounts.close();
      return new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_title_select_account)
        .setSingleChoiceItems(accountLabels, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            //we remove the dialog since the items are different dependend on each invocation
            removeDialog(SELECT_ACCOUNT_DIALOG_ID);
            switchAccount(accountIds[item]);
          }
        }).create();
    }
    return null;
  }
 
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putString("versionInfo", mVersionInfo);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mVersionInfo = savedInstanceState.getString("versionInfo");
  }
  /**
   * start ExpenseEdit Activity for a new transaction/transfer
   * @param type either {@link #TYPE_TRANSACTION} or {@link #TYPE_TRANSFER}
   */
  private void createRow(boolean type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("operationType", type);
    i.putExtra(ExpensesDbAdapter.KEY_ACCOUNTID,mCurrentAccount.id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }

  private void switchAccount(long accountId) {
    //TODO: write a test if the case where the account stored in last_account
    //is deleted, is correctly handled
    //store current account id since we need it for setting last_account in the end
    long current_account_id = mCurrentAccount.id;
    if (accountId == 0) {
      //first check if we have the last_account stored
      accountId = mSettings.getLong("last_account", 0);
      //if for any reason the last_account is identical to the current
      //we ignore it
      if (accountId == mCurrentAccount.id)
        accountId = 0;
      if (accountId != 0) {
        try {
          mCurrentAccount = new Account(mDbHelper, accountId);
        } catch (AccountNotFoundException e) {
         //the account stored in last_account has been deleted 
         accountId = 0; 
        }
      }
      //now we fetch the first account we retrieve
      if (accountId == 0) {
        final Cursor otherAccounts = mDbHelper.fetchAccountOther(mCurrentAccount.id,false);
        if(otherAccounts.moveToFirst()){
          accountId = otherAccounts.getLong(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
        }
        otherAccounts.close();
      }
    }
    if (accountId != 0) {
      try {
        mCurrentAccount = new Account(mDbHelper, accountId);
        mSettings.edit().putLong("current_account", accountId)
          .putLong("last_account", current_account_id)
          .commit();
        fillData();
      } catch (AccountNotFoundException e) {
        //should not happen
        Log.w("MyExpenses","unable to switch to account " + accountId);
      }
    }
  }

  /**
   * writes all transactions of the current account to a QIF file
   * if share_target preference is set, additionally does an FTP upload
   * @throws IOException
   */
  private void exportAll() throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("ddMM-HHmm");
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    Log.i("MyExpenses","now starting export");
    File appDir = Utils.requireAppDir();
    if (appDir == null)
      throw new IOException();
    File outputFile = new File(appDir,
        mCurrentAccount.label.replaceAll("\\W","") + "-" +
        now.format(new Date()) + ".qif");
    if (outputFile.exists()) {
      Toast.makeText(this,String.format(getString(R.string.export_expenses_outputfile_exists), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();
      return;
    }
    FileOutputStream out = new FileOutputStream(outputFile);
    String header = "!Type:Oth L\n";
    out.write(header.getBytes());
    mExpensesCursor.moveToFirst();
    while( mExpensesCursor.getPosition() < mExpensesCursor.getCount() ) {
      String comment = mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
      comment = (comment == null || comment.length() == 0) ? "" : "\nM" + comment;
      String label =  mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow("label"));

      if (label == null || label.length() == 0) {
        label =  "";
      } else {
        long transfer_peer = mExpensesCursor.getLong(
            mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
        if (transfer_peer != 0) {
          label = "[" + label + "]";
        }
        label = "\nL" + label;
      }

      String payee = mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow("payee"));
      payee = (payee == null || payee.length() == 0) ? "" : "\nP" + payee;
      String row = "D"+formatter.format(Timestamp.valueOf(mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE)))) +
          "\nT"+mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)) +
          comment +
          label +
          payee +  
           "\n^\n";
      out.write(row.getBytes());
      mExpensesCursor.moveToNext();
    }
    out.close();
    mExpensesCursor.moveToFirst();
    Toast.makeText(getBaseContext(),String.format(getString(R.string.export_expenses_sdcard_success), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();
    String share_target = mSettings.getString("share_target","");
    if (!share_target.equals("")) {
      Utils.share(MyExpenses.this,outputFile, share_target);
    }
  }
  
  /**
   * triggers export of transactions and resets the account
   * (i.e. deletes transactions and updates opening balance)
   */
  private void reset() {
    try {
      exportAll();
      mCurrentAccount.reset();
      fillData();
    } catch (IOException e) {
      Log.e("MyExpenses",e.getMessage());
      Toast.makeText(getBaseContext(),getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
    }
  }

  /* (non-Javadoc)
   * calls ExpenseEdit with a given rowid
   * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
   */
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    boolean operationType = mExpensesCursor.getLong(
        mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER)) == 0;
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    i.putExtra("operationType", operationType);
    startActivityForResult(i, ACTIVITY_EDIT);
  }
  
  /**
   * this dialog is shown, when a new version requires to present
   * specific information to the user
   * @param info a String presented to the user in an AlertDialog
   */
  private void openVersionDialog(String info) {
    mVersionInfo = info;
    showDialog(VERSION_DIALOG_ID);
  }

  /**
   * if there are already accounts defined, return the first one
   * otherwise create a new account, and return it
   */
  private Account requireAccount() {
    Account account;
    Long account_id = mDbHelper.getFirstAccountId();
    if (account_id == null) {
      account = new Account(
          mDbHelper,
          getString(R.string.app_name),
          0,
          getString(R.string.default_account_description),
          Currency.getInstance(Locale.getDefault())
      );
      account.save();
    } else {
      try {
        account =new Account(mDbHelper,account_id);
      } catch (AccountNotFoundException e) {
        // this should not happen, since we got the account_id from db
        e.printStackTrace();
        throw new RuntimeException();
      }
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
    int prev_version = mSettings.getInt("currentversion", -1);
    int current_version = getVersionNumber();
    if (prev_version == current_version)
      return;
    if (prev_version == -1) {
      //we check if we already have an account
      mCurrentAccount = requireAccount();

      edit.putLong("current_account", mCurrentAccount.id).commit();
      edit.putInt("currentversion", current_version).commit();
    } else if (prev_version != current_version) {
      edit.putInt("currentversion", current_version).commit();
      if (prev_version < 14) {
        //made current_account long
        edit.putLong("current_account", mSettings.getInt("current_account", 0)).commit();
        String non_conforming = checkCurrencies();
        if (non_conforming.length() > 0 ) {
          openVersionDialog(getString(R.string.version_14_upgrade_info,non_conforming));
          return;
        }
      }
      if (prev_version < 19) {
        //renamed
        edit.putString("share_target",mSettings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
      if (prev_version < 26) {
        openVersionDialog(getString(R.string.version_26_upgrade_info));
        return;
      }
    }
    showDialog(HELP_DIALOG_ID);
    return;
  }
 
  /**
   * this utility function was used to check currency upon upgrade to version 14
   * loop through defined accounts and check if currency is a valid ISO 4217 code
   * tries to fix some cases, where currency symbols could have been used
   * @return concatenation of non conforming symbols in use
   */
  private String checkCurrencies() {
    long account_id;
    String currency;
    String non_conforming = "";
    Cursor accountsCursor = mDbHelper.fetchAccountAll();
    accountsCursor.moveToFirst();
    while(!accountsCursor.isAfterLast()) {
         currency = accountsCursor.getString(accountsCursor.getColumnIndex("currency")).trim();
         account_id = accountsCursor.getLong(accountsCursor.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
         try {
           Currency.getInstance(currency);
         } catch (IllegalArgumentException e) {
           Log.d("DEBUG", currency);
           //fix currency for countries from where users appear in the Markets publish console
           if (currency == "RM")
             mDbHelper.updateAccountCurrency(account_id,"MYR");
           else if (currency.equals("₨"))
             mDbHelper.updateAccountCurrency(account_id,"PKR");
           else if (currency.equals("¥"))
             mDbHelper.updateAccountCurrency(account_id,"CNY");
           else if (currency.equals("€"))
             mDbHelper.updateAccountCurrency(account_id,"EUR");
           else if (currency.equals("$"))
             mDbHelper.updateAccountCurrency(account_id,"USD");
           else if (currency.equals("£"))
             mDbHelper.updateAccountCurrency(account_id,"GBP");
           else
             non_conforming +=  currency + " ";
         }
         accountsCursor.moveToNext();
    }
    accountsCursor.close();
    return non_conforming;
  }
  
  /**
   * retrieve information about the current version
   * @return concatenation of versionName, versionCode and buildTime
   * buildTime is automatically stored in property file during build process
   */
  public String getVersionInfo() {
    String version = "";
    String versionname = "";
    String versiontime = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = " (revision " + pi.versionCode + ") ";
      versionname = pi.versionName;
      //versiontime = ", " + R.string.installed + " " + sdf.format(new Date(pi.lastUpdateTime));
    } catch (Exception e) {
      Log.e("MyExpenses", "Package info not found", e);
    }
    try {
      InputStream rawResource = getResources().openRawResource(R.raw.app);
      Properties properties = new Properties();
      properties.load(rawResource);
      versiontime = properties.getProperty("build.date");
    } catch (NotFoundException e) {
      Log.w("MyExpenses","Did not find raw resource");
    } catch (IOException e) {
      Log.w("MyExpenses","Failed to open property file");
    }
    return versionname + version  + versiontime;
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
    return mDbHelper.getAccountCount(mCurrentAccount.currency.getCurrencyCode()) > 1;
  }
  @Override
  public void onClick(View v) {
    dispatchCommand(v.getId(),v.getTag());
  }
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch (command) {
    case INSERT_TA_COMMAND_ID:
      createRow(TYPE_TRANSACTION);
      break;
    case INSERT_TRANSFER_COMMAND_ID:
      createRow(TYPE_TRANSFER);
      break;
    case SWITCH_ACCOUNT_COMMAND_ID:
      int accountCount = mDbHelper.getAccountCount(null);
      if (accountCount > 1) {
        if (tag == null) {
         //we are called from menu
         if (accountCount == 2) {
           switchAccount(0);
         } else {
           //TODO: when the dialog is dismissed with back button,
           //the dialog is not removed, and can get out of sync if
           //the user renames an account or deletes/adds
           //probably need ondismisslistener to handle that case
           showDialog(SELECT_ACCOUNT_DIALOG_ID);
         }
        } else {
          Long accountId = tag != null ? (Long) tag : 0;
          switchAccount(accountId);
        }
      } else {
        showDialog(ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID);
      }
      break;
    case CREATE_ACCOUNT_COMMAND_ID:
      i = new Intent(MyExpenses.this, AccountEdit.class);
      startActivityForResult(i, ACTIVITY_CREATE_ACCOUNT);
      break;
    case RESET_ACCOUNT_COMMAND_ID:
      if (Utils.isExternalStorageAvailable())
        showDialog(RESET_DIALOG_ID);
      else 
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable), 
            Toast.LENGTH_LONG)
            .show();
      break;
    case SETTINGS_COMMAND_ID:
      startActivityForResult(new Intent(MyExpenses.this, MyPreferenceActivity.class),ACTIVITY_PREF);
      break;
    case EDIT_ACCOUNT_COMMAND_ID:
      i = new Intent(MyExpenses.this, AccountEdit.class);
      i.putExtra(ExpensesDbAdapter.KEY_ROWID, mCurrentAccount.id);
      startActivityForResult(i, ACTIVITY_EDIT_ACCOUNT);
      break;
    case BACKUP_COMMAND_ID:
      startActivityForResult(new Intent(MyExpenses.this, Backup.class),ACTIVITY_PREF);
      break;
    case TUTORIAL_COMMAND_ID:
      startActivity( new Intent(MyExpenses.this, Tutorial.class));
      break;
    case CHANGES_COMMAND_ID:
      showDialog(CHANGES_DIALOG_ID);
      break;
    case HELP_COMMAND_ID:
      showDialog(HELP_DIALOG_ID);
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
  @Override
  public boolean onLongClick(View v) {
    MenuButton mb = (MenuButton) v;
    dw = mb.getMenu();
    if (dw == null)
      return false;
    dw.showLikeQuickAction();
    return true;
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals("use_standard_menu")) {
      boolean newValue = mSettings.getBoolean("use_standard_menu", false);
      if (newValue != mUseStandardMenu) {
        if (newValue)
          mButtonBar.setVisibility(View.GONE);
        else {
          mButtonBar.setVisibility(View.VISIBLE);
          if (!mButtonBarIsFilled)
            fillButtons();
        }
      }
      mUseStandardMenu = newValue;
    }
  }
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (!mUseStandardMenu && keyCode == KeyEvent.KEYCODE_MENU) {
      Log.i("MyExpenses", "will react to menu key");
      showDialog(USE_STANDARD_MENU_DIALOG_ID);
      return true;
    }
    return  super.onKeyUp(keyCode, event);
}

}
