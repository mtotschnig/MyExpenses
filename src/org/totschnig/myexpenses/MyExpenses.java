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
import java.net.URI;
import java.sql.Timestamp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import org.example.qberticus.quickactions.BetterPopupWindow;
import org.totschnig.myexpenses.ButtonBar.Action;
import org.totschnig.myexpenses.ButtonBar.MenuButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 * @author Michael Totschnig
 *
 */
public class MyExpenses extends Activity
  implements OnClickListener,OnLongClickListener, OnSharedPreferenceChangeListener, OnPageChangeListener {
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_PREF=2;
  public static final int ACTIVITY_CREATE_ACCOUNT=3;
  public static final int ACTIVITY_EDIT_ACCOUNT=4;

  public static final boolean TYPE_TRANSACTION = true;
  public static final boolean TYPE_TRANSFER = false;
  public static final boolean ACCOUNT_BUTTON_CYCLE = false;
  public static final boolean ACCOUNT_BUTTON_TOGGLE = true;
  public static final String TRANSFER_EXPENSE = "=> ";
  public static final String TRANSFER_INCOME = "<= ";
  static final int RESET_DIALOG_ID = 3;
  static final int ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID = 5;
  static final int USE_STANDARD_MENU_DIALOG_ID = 6;
  static final int SELECT_ACCOUNT_DIALOG_ID = 7;
  static final int TEMPLATE_TITLE_DIALOG_ID = 8;
  static final int SELECT_TEMPLATE_DIALOG_ID = 9;
  static final int MORE_ACTIONS_DIALOG_ID = 10;
  static final int DONATE_DIALOG_ID = 11;
  static final int CONFIRM_RESTORE_DIALOG_ID = 12;
  
  static final String HOST = "myexpenses.totschnig.org";
  static final String FEEDBACK_EMAIL = "michael@totschnig.org";

  private String mVersionInfo;
  private ArrayList<Action> mMoreItems;
  
  public SimpleCursorAdapter mAdapter;
  private ExpensesDbAdapter mDbHelper;

  private Account mCurrentAccount;
  
  private void setCurrentAccount(Account mCurrentAccount) {
    this.mCurrentAccount = mCurrentAccount;
    MyApplication.setCurrentAccountColor(mCurrentAccount.color);
  }
  private SharedPreferences mSettings;
  private Cursor mAccountsCursor;
  private Cursor mExpensesCursor;
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
   * stores the transaction from which a template is to be created
   */
  private long mTemplateCreateDialogTransactionId;

  private BetterPopupWindow dw;
  private boolean mButtonBarIsFilled;
  /**
   * for the SELECT_ACCOUNT_DIALOG we store the context from which we are called
   * if null, we call from SWITCH_ACCOUNT if a long we call from MOVE_TRANSACTION
   */
  private long mSelectAccountContextId = 0L;
  private int mCurrentDialog = 0;

  private int colorExpense;
  private int colorIncome;

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
        showDialogWrapper(CONFIRM_RESTORE_DIALOG_ID);
        return;
      }
    }
    setup();
  }
  private void setup() {
    mDbHelper = MyApplication.db();
    newVersionCheck();
    mAccountsCursor = mDbHelper.fetchAccountAll();
    startManagingCursor(mAccountsCursor);
    //MyApplication.updateUIWithAccountColor(this);
    mUseStandardMenu = mSettings.getBoolean(MyApplication.PREFKEY_USE_STANDARD_MENU, false);
    mButtonBar = (ButtonBar) findViewById(R.id.ButtonBar);
    if (mUseStandardMenu) {
      hideButtonBar();
    } else {
      fillButtons();
    }
    mSettings.registerOnSharedPreferenceChangeListener(this);

    Resources.Theme theme = getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    theme.resolveAttribute(R.attr.pageMargin,color, true);
    myAdapter = new MyViewPagerAdapter();
    myPager = (ViewPager) this.findViewById(R.id.viewpager);
    myPager.setAdapter(this.myAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin(10);
    myPager.setPageMarginDrawable(color.resourceId);
    if (mCurrentAccount == null) {
      long account_id = mSettings.getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
      try {
        setCurrentAccount(Account.getInstanceFromDb(account_id));
      } catch (DataObjectNotFoundException e) {
        //for any reason the account stored in pref no longer exists
        setCurrentAccount(requireAccount());
      }
    }
    moveToCurrentAccount();
  }
  private void moveToCurrentAccount() {
    mAccountsCursor.moveToFirst();
    int currentPosition = 0;
    while (mAccountsCursor.isAfterLast() == false) {
      if (mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(ExpensesDbAdapter.KEY_ROWID)) == mCurrentAccount.id) {
        currentPosition = mAccountsCursor.getPosition();
      }
      mAccountsCursor.moveToNext();
    }
    myPager.setCurrentItem(currentPosition);
    updateUIforCurrentAccount();
  }
  private void fillSwitchButton() {
    mSwitchButton.clearMenu();
    final Cursor otherAccounts = mDbHelper.fetchAccountOther(mCurrentAccount.id,false);
    if(otherAccounts.moveToFirst()){
      for (int i = 0; i < otherAccounts.getCount(); i++) {
        mSwitchButton.addItem(
            otherAccounts.getString(otherAccounts.getColumnIndex("label")),
            R.id.SWITCH_ACCOUNT_COMMAND,
            otherAccounts.getLong(otherAccounts.getColumnIndex(ExpensesDbAdapter.KEY_ROWID)));
        otherAccounts.moveToNext();
      }
    }
    mSwitchButton.addItem(R.string.menu_accounts_new,R.id.CREATE_ACCOUNT_COMMAND);
    mSwitchButton.addItem(R.string.menu_accounts_summary,R.id.ACCOUNT_OVERVIEW_COMMAND);
    otherAccounts.close();
  }
  private void fillAddButton() {
    mAddButton.clearMenu();
    final Cursor templates = mDbHelper.fetchTemplates(mCurrentAccount.id);
    boolean gotTemplates = templates.moveToFirst();
    boolean gotTransfers = transfersEnabledP();
    if (gotTransfers) {
      mAddButton.addItem(R.string.transfer,R.id.INSERT_TRANSFER_COMMAND);
    }
    if(gotTemplates) {
      for (int i = 0; i < templates.getCount(); i++) {
        mAddButton.addItem(
            templates.getString(templates.getColumnIndex(ExpensesDbAdapter.KEY_TITLE)),
            R.id.NEW_FROM_TEMPLATE_COMMAND,
            templates.getLong(templates.getColumnIndex(ExpensesDbAdapter.KEY_ROWID)));
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
    mAddButton.setComparator(new Comparator<Button>() {
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
    });
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
    mHelpButton.addItem("News",R.id.WEB_COMMAND,"news");
    mHelpButton.addItem(R.string.menu_faq,R.id.WEB_COMMAND,"faq");
    mHelpButton.addItem(R.string.donate,R.id.DONATE_COMMAND);
    mHelpButton.addItem("Feedback",R.id.FEEDBACK_COMMAND);
    mButtonBarIsFilled = true;
  }
  @Override
  public void onStop() {
    super.onStop();
    if (dw != null)
    dw.dismiss();
  }

  private void setCurrentBalance() {
    ((TextView) findViewById(R.id.label)).setText(mCurrentAccount.label);
    ((TextView) findViewById(R.id.end)).setText(Utils.formatCurrency(mCurrentAccount.getCurrentBalance()));
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
      .setVisible(mDbHelper.getAccountCount(null) > 1);
    menu.findItem(R.id.INSERT_TRANSFER_COMMAND)
      .setVisible(transfersEnabledP());
    menu.findItem(R.id.RESET_ACCOUNT_COMMAND)
      .setVisible(mCurrentAccount.getSize() > 0);
    menu.findItem(R.id.NEW_FROM_TEMPLATE_COMMAND)
      .setVisible(mDbHelper.getTemplateCount(mCurrentAccount.id) > 0);
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    //numeric shortcuts are used from Monkeyrunner
    menu.add(0, R.id.INSERT_TA_COMMAND, 0, R.string.menu_insert_ta)
        .setIcon(android.R.drawable.ic_menu_add);
    menu.add(0, R.id.INSERT_TRANSFER_COMMAND, 0, R.string.menu_insert_transfer)
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
         mAccountsCursor.requery();
         myAdapter.notifyDataSetChanged();
         switchAccount(intent.getLongExtra("account_id",0));
         return;
    }
    //we refresh when returning from ACTIVITY_PREF with RESULT_CANCEL,
    //since we might have edited accounts from there
    if (resultCode == RESULT_OK || requestCode == ACTIVITY_PREF) {
      mAccountsCursor.requery();
      myAdapter.notifyDataSetChanged();
      updateUIforCurrentAccount();
    }
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    menu.add(0, R.id.SHOW_DETAIL_COMMAND, 0, R.string.menu_show_detail);
    menu.add(0, R.id.CREATE_TEMPLATE_COMMAND, 0, R.string.menu_create_template);
    if (mDbHelper.getAccountCount(null) > 1) {
      menu.add(0,R.id.MOVE_TRANSACTION_COMMAND,0,R.string.menu_move_transaction);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      long transfer_peer = Transaction.getInstanceFromDb(info.id).transfer_peer;
      if (transfer_peer == 0) {
        Transaction.delete(info.id);
      }
      else {
        Transfer.delete(info.id,transfer_peer);
      }
      myAdapter.notifyDataSetChanged();
      configButtons();
      return true;
    case R.id.SHOW_DETAIL_COMMAND:
      Transaction t = Transaction.getInstanceFromDb(info.id);
      String method = "";
      if (t.methodId != 0) {
        try {
          method= PaymentMethod.getInstanceFromDb(t.methodId).getDisplayLabel(this);
        } catch (DataObjectNotFoundException e) {
        }
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
      mSelectAccountContextId = info.id;
      showDialogWrapper(SELECT_ACCOUNT_DIALOG_ID);
      return true;
    case R.id.CREATE_TEMPLATE_COMMAND:
      mTemplateCreateDialogTransactionId = info.id;
      showDialogWrapper(TEMPLATE_TITLE_DIALOG_ID);
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
    case R.id.HELP_DIALOG_ID:
      DisplayMetrics displaymetrics = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
      int minWidth = (int) (displaymetrics.widthPixels*0.9f);
      if (minWidth / displaymetrics.density > 650)
        minWidth = (int) (650 * displaymetrics.density);
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.aboutview, null);
      view.setMinimumWidth(minWidth);
      ((TextView)view.findViewById(R.id.aboutVersionCode)).setText(getVersionInfo());
      ((TextView)view.findViewById(R.id.help_licence_gpl)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.help_quick_guide)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.help_whats_new)).setMovementMethod(LinkMovementMethod.getInstance());
      Utils.setDialogOneButton(view,
          android.R.string.ok,0,null);
      return new AlertDialog.Builder(this)
        .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
        .setIcon(R.drawable.icon)
        .setView(view)
        .create();
    case R.id.VERSION_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.versiondialog, null);
      ((TextView) view.findViewById(R.id.versionInfoChanges))
        .setText(R.string.help_whats_new);
      if (mVersionInfo != "") {
        tv = (TextView) view.findViewById(R.id.versionInfoImportant);
        tv.setText(mVersionInfo);
        tv.setVisibility(View.VISIBLE);
        ((TextView) view.findViewById(R.id.versionInfoImportantHeading)).setVisibility(View.VISIBLE);
      }
      Utils.setDialogThreeButtons(view,
          R.string.menu_help,R.id.HELP_COMMAND,null,
          R.string.donate,R.id.DONATE_COMMAND,null,
          android.R.string.ok,0,null);
      return new AlertDialog.Builder(this)
        .setTitle(getString(R.string.new_version) + " : " + getVersionName())
        .setIcon(R.drawable.icon)
        .setView(view)
        .create();
    case RESET_DIALOG_ID:
      return Utils.createMessageDialog(this,R.string.warning_reset_account,R.id.RESET_ACCOUNT_COMMAND_DO,null)
        .create();
    case ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID:
      return Utils.createMessageDialog(this,R.string.menu_accounts_explain,R.id.CREATE_ACCOUNT_COMMAND,null)
        .create();
    case USE_STANDARD_MENU_DIALOG_ID:
      return Utils.createMessageDialog(this,R.string.suggest_use_standard_menu,R.id.USE_STANDARD_MENU_COMMAND,null)
        .create();
    //SELECT_ACCOUNT_DIALOG is used both from SWITCH_ACCOUNT and MOVE_TRANSACTION
    case SELECT_ACCOUNT_DIALOG_ID:
      final Cursor otherAccounts = mDbHelper.fetchAccountOther(mCurrentAccount.id,false);
      final String[] accountLabels = Utils.getStringArrayFromCursor(otherAccounts, "label");
      final Long[] accountIds = Utils.getLongArrayFromCursor(otherAccounts, ExpensesDbAdapter.KEY_ROWID);
      otherAccounts.close();
      return new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_title_select_account)
        .setSingleChoiceItems(accountLabels, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            //we remove the dialog since the items are different dependent on each invocation
            removeDialog(SELECT_ACCOUNT_DIALOG_ID);
            if (mSelectAccountContextId == 0L) {
              switchAccount(accountIds[item]);
            }
            else {
              mDbHelper.moveTransaction(mSelectAccountContextId,accountIds[item]);
              myAdapter.notifyDataSetChanged();
              configButtons();
            }
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            removeDialog(SELECT_ACCOUNT_DIALOG_ID);
          }
        })
        .create();
    case R.id.FTP_DIALOG_ID:
      return Utils.sendWithFTPDialog((Activity) this);
    case TEMPLATE_TITLE_DIALOG_ID:
      // Set an EditText view to get user input 
      final EditText input = new EditText(this);
      //only if the editText has an id, is its value restored after orientation change
      input.setId(1);
      input.setSingleLine();
      Utils.setBackgroundFilter(input, getResources().getColor(R.color.theme_dark_button_color));
      return new AlertDialog.Builder(this)
      .setTitle(R.string.dialog_title_template_title)
      .setView(input)
      .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String title = input.getText().toString();
          if (!title.equals("")) {
            input.setText("");
            dismissDialog(TEMPLATE_TITLE_DIALOG_ID);
            if ((new Template(Transaction.getInstanceFromDb(mTemplateCreateDialogTransactionId),title)).save() == -1) {
              Toast.makeText(getBaseContext(),getString(R.string.template_title_exists,title), Toast.LENGTH_LONG).show();
            } else {
              Toast.makeText(getBaseContext(),getString(R.string.template_create_success,title), Toast.LENGTH_LONG).show();
            }
            if (!mUseStandardMenu) {
              fillAddButton();
            }
          } else {
            Toast.makeText(getBaseContext(),getString(R.string.no_text_given), Toast.LENGTH_LONG).show();
          }
        }
      })
      .setNegativeButton(android.R.string.no, null)
      .create();
    case SELECT_TEMPLATE_DIALOG_ID:
      final Cursor templates = mDbHelper.fetchTemplates(mCurrentAccount.id);
      final String[] templateTitles = Utils.getStringArrayFromCursor(templates, "title");
      final Long[] templateIds = Utils.getLongArrayFromCursor(templates, ExpensesDbAdapter.KEY_ROWID);
      templates.close();
      return new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_title_select_account)
        .setSingleChoiceItems(templateTitles, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            //TODO: check if we could renounce removing the dialog here, remove it only when a new template is defined
            //or account is switched
            removeDialog(SELECT_TEMPLATE_DIALOG_ID);
            Transaction.getInstanceFromTemplate(templateIds[item]).save();
            myAdapter.notifyDataSetChanged();
            configButtons();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            removeDialog(SELECT_TEMPLATE_DIALOG_ID);
          }
        })
        .create();
    case MORE_ACTIONS_DIALOG_ID:
      int howMany = mMoreItems.size();
      final String[] moreTitles = new String[howMany];
      final int[] moreIds = new int[howMany];
      final Object[] moreTags = new Object[howMany];
      int count = 0;
      for(Iterator<Action> i = mMoreItems.iterator();i.hasNext();) {
        Action action = i.next();
        moreTitles[count] = action.text;
        moreIds[count] = action.id;
        moreTags[count] = action.tag;
        count++;
      }
      return new AlertDialog.Builder(this)
      .setTitle(R.string.menu_more)
      .setSingleChoiceItems(moreTitles, -1,new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
          removeDialog(MORE_ACTIONS_DIALOG_ID);
          dispatchCommand(moreIds[item], moreTags[item]);
        }
      })
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          removeDialog(MORE_ACTIONS_DIALOG_ID);
        }
      })
      .create();
    case DONATE_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.messagedialog, null);
      tv = (TextView)view.findViewById(R.id.message_text);
      tv.setText(R.string.donate_dialog_text);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      Utils.setDialogTwoButtons(view,
          R.string.donate_button_flattr,R.id.WEB_COMMAND,"flattr",
          R.string.donate_button_paypal,R.id.WEB_COMMAND,"paypal"
      );
      return new AlertDialog.Builder(this)
        .setTitle(R.string.donate)
        .setView(view)
        .create();
    case CONFIRM_RESTORE_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.messagedialog, null);
      tv = (TextView)view.findViewById(R.id.message_text);
      tv.setText(R.string.dialog_confirm_restore_on_install);
      Utils.setDialogTwoButtons(view,
          android.R.string.yes,R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,new Boolean(true),
          android.R.string.no,R.id.HANDLE_RESTORE_ON_INSTALL_COMMAND,new Boolean(false)
      );
      return new AlertDialog.Builder(this)
        .setCancelable(false)
        .setView(view)
        .create();
     }
    return null;
  }


  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putString("versionInfo", mVersionInfo);
   outState.putLong("SelectAccountContextId", mSelectAccountContextId);
   outState.putLong("TemplateCreateDialogTransactionId",mTemplateCreateDialogTransactionId);
   outState.putSerializable("MoreItems",mMoreItems);
   outState.putInt("currentDialog",mCurrentDialog);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mVersionInfo = savedInstanceState.getString("versionInfo");
   mSelectAccountContextId = savedInstanceState.getLong("SelectAccountContextId");
   mTemplateCreateDialogTransactionId = savedInstanceState.getLong("TemplateCreateDialogTransactionId");
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
    i.putExtra(ExpensesDbAdapter.KEY_ACCOUNTID,mCurrentAccount.id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }

  private void switchAccount(long accountId) {
    //TODO: write a test if the case where the account stored in last_account
    //is deleted, is correctly handled
    //store current account id since we need it for setting last_account in the end
    long current_account_id = mCurrentAccount.id;
    if (accountId == 0) {
      if (mSettings.getBoolean(MyApplication.PREFKEY_ACCOUNT_BUTTON_BEHAVIOUR,ACCOUNT_BUTTON_CYCLE)) {
        //first check if we have the last_account stored
        accountId = mSettings.getLong(MyApplication.PREFKEY_LAST_ACCOUNT, 0);
        //if for any reason the last_account is identical to the current
        //we ignore it
        if (accountId == mCurrentAccount.id)
          accountId = 0;
        if (accountId != 0) {
          try {
            Account.getInstanceFromDb(accountId);
          } catch (DataObjectNotFoundException e) {
           //the account stored in last_account has been deleted 
           accountId = 0; 
          }
        }
      }
      //cycle behaviour
      if (accountId == 0) {
        accountId = mDbHelper.fetchAccountIdNext(mCurrentAccount.id);
      }
    }
    if (accountId != 0) {
      try {
        setCurrentAccount(Account.getInstanceFromDb(accountId));
        moveToCurrentAccount();
        Toast.makeText(getBaseContext(),getString(R.string.switch_account,mCurrentAccount.label), Toast.LENGTH_SHORT).show();
        mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, accountId)
          .putLong(MyApplication.PREFKEY_LAST_ACCOUNT, current_account_id)
          .commit();
      } catch (DataObjectNotFoundException e) {
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
  private boolean exportAll() throws IOException {
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
      return false;
    }
    OutputStreamWriter out = new OutputStreamWriter(
        new FileOutputStream(outputFile),
        mSettings.getString(MyApplication.PREFKEY_QIF_EXPORT_FILE_ENCODING, "UTF-8"));
    String header = "!Type:" + mCurrentAccount.type.getQifName() + "\n";
    out.write(header);
    mExpensesCursor = mDbHelper.fetchTransactionAll(mCurrentAccount.id);
    mExpensesCursor.moveToFirst();
    while( mExpensesCursor.getPosition() < mExpensesCursor.getCount() ) {
      String comment = mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
      comment = (comment == null || comment.length() == 0) ? "" : "\nM" + comment;
      String full_label = "";
      String label_main =  mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_LABEL_MAIN));

      if (label_main != null && label_main.length() > 0) {
        long transfer_peer = mExpensesCursor.getLong(
            mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
        if (transfer_peer != 0) {
          full_label = "[" + label_main + "]";
        } else {
          full_label = label_main;
          String label_sub =  mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_LABEL_SUB));
          if (label_sub != null && label_sub.length() > 0) {
            full_label += ":" + label_sub;
          }
        }
        full_label = "\nL" + full_label;
      }

      String payee = mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow("payee"));
      payee = (payee == null || payee.length() == 0) ? "" : "\nP" + payee;
      String dateStr = formatter.format(Timestamp.valueOf(mExpensesCursor.getString(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE))));
      long amount = mExpensesCursor.getLong(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT));
      String amountStr = new Money(mCurrentAccount.currency,amount)
          .getAmountMajor().toPlainString();
      String row = "D"+ dateStr +
          "\nT" + amountStr +
          comment +
          full_label +
          payee +  
           "\n^\n";
      out.write(row);
      mExpensesCursor.moveToNext();
    }
    out.close();
    mExpensesCursor.moveToFirst();
    Toast.makeText(getBaseContext(),String.format(getString(R.string.export_expenses_sdcard_success), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();
    if (mSettings.getBoolean(MyApplication.PREFKEY_PERFORM_SHARE,false)) {
      share(outputFile, mSettings.getString(MyApplication.PREFKEY_SHARE_TARGET,"").trim());
    }
    return true;
  }
  
  private void share(File file,String target) {
    URI uri = null;
    Intent intent;
    String scheme = "mailto";
    if (!target.equals("")) {
      uri = Utils.validateUri(target);
      if (uri == null) {
        Toast.makeText(getBaseContext(),getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
        return;
      }
      scheme = uri.getScheme();
    }
    //if we get a String that does not include a scheme, we interpret it as a mail address
    if (scheme == null) {
      scheme = "mailto";
    }
    if (scheme.equals("ftp")) {
      intent = new Intent(android.content.Intent.ACTION_SENDTO);
      intent.setDataAndType(android.net.Uri.parse(target),"text/qif");
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
      if (!Utils.isIntentAvailable(this,intent)) {
        Toast.makeText(getBaseContext(),R.string.no_app_handling_ftp_available, Toast.LENGTH_LONG).show();
        return;
      }
      startActivity(intent);
    } else if (scheme.equals("mailto")) {
      intent = new Intent(android.content.Intent.ACTION_SEND);
      intent.setType("text/qif");
      if (uri != null) {
        String address = uri.getSchemeSpecificPart();
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ address });
      }
      intent.putExtra(Intent.EXTRA_SUBJECT,R.string.export_expenses);
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
      if (!Utils.isIntentAvailable(this,intent)) {
        Toast.makeText(getBaseContext(),R.string.no_app_handling_email_available, Toast.LENGTH_LONG).show();
        return;
      }
      //if we got mail address, we launch the default application
      //if we are called without target, we launch the chooser in order to make action more explicit
      if (uri != null) {
        startActivity(intent);
      } else {
        startActivity(Intent.createChooser(
            intent,getString(R.string.share_sending)));
      }
    } else {
      Toast.makeText(getBaseContext(),getString(R.string.share_scheme_not_supported,scheme), Toast.LENGTH_LONG).show();
      return;
    }
  }

  
  /**
   * triggers export of transactions and resets the account
   * (i.e. deletes transactions and updates opening balance)
   */
  private void reset() {
    try {
      if (exportAll()) {
        mCurrentAccount.reset();
        myAdapter.notifyDataSetChanged();
        configButtons();
      }
    } catch (IOException e) {
      Log.e("MyExpenses",e.getMessage());
      Toast.makeText(getBaseContext(),getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
    }
  }

  /**
   * if there are already accounts defined, return the first one
   * otherwise create a new account, and return it
   */
  private Account requireAccount() {
    Account account;
    Currency currency;
    Long accountId = mDbHelper.getFirstAccountId();
    if (accountId == null) {
      account = new Account(
          getString(R.string.app_name),
          0,
          getString(R.string.default_account_description)
      );
      account.save();
    } else {
      try {
        account =Account.getInstanceFromDb(accountId);
      } catch (DataObjectNotFoundException e) {
        // this should not happen, since we got the account_id from db
        e.printStackTrace();
        throw new RuntimeException(e);
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
    mVersionInfo = "";
    Editor edit = mSettings.edit();
    int prev_version = mSettings.getInt(MyApplication.PREFKEY_CURRENT_VERSION, -1);
    int current_version = getVersionNumber();
    if (prev_version == current_version)
      return;
    if (prev_version == -1) {
      //we check if we already have an account
      setCurrentAccount(requireAccount());

      edit.putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, mCurrentAccount.id).commit();
      edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version).commit();
      showDialogWrapper(R.id.HELP_DIALOG_ID);
    } else if (prev_version != current_version) {
      edit.putInt(MyApplication.PREFKEY_CURRENT_VERSION, current_version).commit();
      if (prev_version < 14) {
        //made current_account long
        edit.putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, mSettings.getInt(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0)).commit();
        String non_conforming = checkCurrencies();
        if (non_conforming.length() > 0 ) {
          mVersionInfo += getString(R.string.version_14_upgrade_info,non_conforming) + "\n";
        }
      }
      if (prev_version < 19) {
        //renamed
        edit.putString(MyApplication.PREFKEY_SHARE_TARGET,mSettings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
      if (prev_version < 26) {
        mVersionInfo += getString(R.string.version_26_upgrade_info) + "\n";
        return;
      }
      if (prev_version < 28) {
        Log.i("MyExpenses",String.format("Upgrading to version 28: Purging %d transactions from datbase",
            mDbHelper.purgeTransactions()));
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
            showDialogWrapper(R.id.FTP_DIALOG_ID);
            return;
          }
        }
      }
      if (prev_version < 34) {
        mVersionInfo += getString(R.string.version_34_upgrade_info)+ "\n";
      }
      if (prev_version < 35) {
        mVersionInfo += getString(R.string.version_35_upgrade_info)+ "\n";
      }
      showDialogWrapper(R.id.VERSION_DIALOG_ID);
    }
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
    return mDbHelper.getAccountCount(mCurrentAccount.currency.getCurrencyCode()) > 1;
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
      startActivityForResult(i, ACTIVITY_EDIT);
      return true;
    }
    return false;
  }
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch (command) {
    case R.id.FEEDBACK_COMMAND:
      i = new Intent(android.content.Intent.ACTION_SEND);
      i.setType("plain/text");
      i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ FEEDBACK_EMAIL });
      i.putExtra(android.content.Intent.EXTRA_SUBJECT,
          "[" + getString(R.string.app_name) + 
          getVersionName() + "] Feedback"
      );
      i.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.feedback_email_message));
      startActivity(i);
      break;
    case R.id.DONATE_COMMAND:
      showDialogWrapper(DONATE_DIALOG_ID);
      break;
    case R.id.INSERT_TA_COMMAND:
      createRow(TYPE_TRANSACTION);
      break;
    case R.id.INSERT_TRANSFER_COMMAND:
      createRow(TYPE_TRANSFER);
      break;
    case R.id.SWITCH_ACCOUNT_COMMAND:
      int accountCount = mDbHelper.getAccountCount(null);
      if (accountCount > 1) {
        if (tag == null) {
         //we are called from menu
         if (accountCount == 2) {
           switchAccount(0);
         } else {
           mSelectAccountContextId = 0L;
           showDialogWrapper(SELECT_ACCOUNT_DIALOG_ID);
         }
        } else {
          Long accountId = tag != null ? (Long) tag : 0;
          switchAccount(accountId);
        }
      } else {
        showDialogWrapper(ACCOUNTS_BUTTON_EXPLAIN_DIALOG_ID);
      }
      break;
    case R.id.CREATE_ACCOUNT_COMMAND:
      i = new Intent(MyExpenses.this, AccountEdit.class);
      startActivityForResult(i, ACTIVITY_CREATE_ACCOUNT);
      break;
    case R.id.RESET_ACCOUNT_COMMAND:
      if (Utils.isExternalStorageAvailable()) {
        showDialogWrapper(RESET_DIALOG_ID);
      } else {
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      break;
    case R.id.RESET_ACCOUNT_COMMAND_DO:
      if (Utils.isExternalStorageAvailable()) {
        reset();
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
      i.putExtra(ExpensesDbAdapter.KEY_ROWID, mCurrentAccount.id);
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
      i.setData(Uri.parse("http://" + HOST + "/#" + (String) tag));
      startActivity(i);
      break;
    case R.id.HELP_COMMAND:
      showDialogWrapper(R.id.HELP_DIALOG_ID);
      break;
    case R.id.NEW_FROM_TEMPLATE_COMMAND:
      if (tag == null) {
          showDialogWrapper(SELECT_TEMPLATE_DIALOG_ID);
      } else {
        Transaction.getInstanceFromTemplate((Long) tag).save();
        myAdapter.notifyDataSetChanged();
        configButtons();
      }
      break;
    case R.id.MORE_ACTION_COMMAND:
      mMoreItems = (ArrayList<Action>) tag;
      showDialogWrapper(MORE_ACTIONS_DIALOG_ID);
      break;
    case R.id.RATE_COMMAND:
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
        MyApplication.backupRestore();
      }
      mDbHelper = MyApplication.db();
      setup();
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

  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (!mUseStandardMenu && keyCode == KeyEvent.KEYCODE_MENU) {
      Log.i("MyExpenses", "will react to menu key");
      showDialogWrapper(USE_STANDARD_MENU_DIALOG_ID);
      return true;
    }
    return  super.onKeyUp(keyCode, event);
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
  private class MyViewPagerAdapter extends PagerAdapter {
    @Override
    public void destroyItem(View collection, int position, Object view) {
      ((ViewPager) collection).removeView((View) view);
    }
    @Override
    public int getItemPosition(Object object) {
      return POSITION_NONE;
  }
    @Override
    public int getCount() {
      return mAccountsCursor.getCount(); // Number of pages usually set with .length() or .size()
    }

    /**
     * Create the page for the given position. The adapter is responsible
     * for adding the view to the container given here, although it only
     * must ensure this is done by the time it returns from
     * {@link #finishUpdate()}.
     * 
     * @param container
     *            The containing View in which the page will be shown.
     * @param position
     *            The page position to be instantiated.
     * @return Returns an Object representing the new page. This does not
     *         need to be a View, but can be some other container of the
     *         page.
     */
    @Override
    public Object instantiateItem(View collection, int position) {
      Account account;
      mAccountsCursor.moveToPosition(position);
      long accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
      try {
        account = Account.getInstanceFromDb(accountId);
      } catch (DataObjectNotFoundException e) {
        // this should not happen, since we got the account_id from db
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      mExpensesCursor = mDbHelper.fetchTransactionAll(accountId);
      //startManagingCursor(mExpensesCursor);
      final LayoutInflater inflater = (LayoutInflater) MyExpenses.this
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View v = inflater.inflate(R.layout.expenses_list, null, false);
      int textColor = Utils.getTextColorForBackground(account.color);
      TextView tv = (TextView) v.findViewById(R.id.label);
      tv.setText(account.label);
      tv.setTextColor(textColor);
      tv = (TextView) v.findViewById(R.id.end);
      tv.setText(Utils.formatCurrency(account.getCurrentBalance()));
      tv.setTextColor(textColor);
      v.findViewById(R.id.heading).setBackgroundColor(account.color);
      ListView lv = (ListView) v.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{ExpensesDbAdapter.KEY_LABEL_MAIN,ExpensesDbAdapter.KEY_DATE,ExpensesDbAdapter.KEY_AMOUNT};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.category,R.id.date,R.id.amount};

      final SimpleDateFormat dateFormat;
      final String categorySeparator, commentSeparator;
      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        dateFormat =  new SimpleDateFormat("dd.MM HH:mm");
        categorySeparator = " : ";
        commentSeparator = " / ";
      } else {
        dateFormat = new SimpleDateFormat("dd.MM\nHH:mm");
        categorySeparator = " :\n";
        commentSeparator = "<br>";
      }
      // Now create a simple cursor adapter and set it to display
      lv.setAdapter(new SimpleCursorAdapter(MyExpenses.this, R.layout.expense_row, mExpensesCursor, from, to)  {
        /* (non-Javadoc)
         * calls {@link #convText for formatting the values retrieved from the cursor}
         * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
         */
        @Override
        public void setViewText(TextView v, String text) {
          switch (v.getId()) {
          case R.id.date:
            text = Utils.convDate(text,dateFormat);
            break;
          case R.id.amount:
            text = Utils.convAmount(text,mCurrentAccount.currency);
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
          long amount = c.getLong(col);
          if (amount < 0) {
            tv1.setTextColor(colorExpense);
            // Set the background color of the text.
          }
          else {
            tv1.setTextColor(colorIncome);
          }
          TextView tv2 = (TextView)row.findViewById(R.id.category);
          col = c.getColumnIndex(ExpensesDbAdapter.KEY_TRANSFER_PEER);
          String catText = (String) tv2.getText();
          if (c.getLong(col) != 0) {
            catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
          } else {
            col = c.getColumnIndex(ExpensesDbAdapter.KEY_LABEL_SUB);
            String label_sub = c.getString(col);
            if (label_sub != null && label_sub.length() > 0) {
              catText += categorySeparator + label_sub;
            }
          }
          col = c.getColumnIndex(ExpensesDbAdapter.KEY_COMMENT);
          String comment = c.getString(col);
          if (comment != null && comment.length() > 0) {
            catText += (catText.equals("") ? "" : commentSeparator) + "<i>" + comment + "</i>";
          }
          tv2.setText(Html.fromHtml(catText));
          return row;
        }
      });
      lv.setEmptyView(v.findViewById(R.id.empty));
      lv.setOnItemClickListener(new OnItemClickListener()
      {
           @Override
           public void onItemClick(AdapterView<?> a, View v,int position, long id)
           {
             Intent i = new Intent(MyExpenses.this, ExpenseEdit.class);
             i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
             //i.putExtra("operationType", operationType);
             startActivityForResult(i, ACTIVITY_EDIT);
           }
      });
      registerForContextMenu(lv);
      ((ViewPager) collection).addView(v, 0);
      return v;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
    }

    @Override
    public Parcelable saveState() {
      return null;
    }
  }
  @Override
  public void onPageScrollStateChanged(int arg0) {
  }
  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
  }
  @Override
  public void onPageSelected(int position) {
    mAccountsCursor.moveToPosition(position);
    long accountId = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(ExpensesDbAdapter.KEY_ROWID));
    mSettings.edit().putLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, accountId)
    .putLong(MyApplication.PREFKEY_LAST_ACCOUNT, mCurrentAccount.id)
    .commit();
    try {
      setCurrentAccount(Account.getInstanceFromDb(accountId));
    } catch (DataObjectNotFoundException e) {
      // this should not happen, since we got the account_id from db
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    updateUIforCurrentAccount();
  }
  public void updateUIforCurrentAccount() {
    View divider = findViewById(R.id.ButtonBarDividerTop);
    if (divider != null) {
      divider.setBackgroundColor(mCurrentAccount.color);
      findViewById(R.id.ButtonBarDividerBottom).setBackgroundColor(mCurrentAccount.color);
    }
    configButtons();
  }
}
