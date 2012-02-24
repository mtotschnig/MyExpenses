/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.ListActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
//import android.util.DisplayMetrics;
import android.util.Log;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 * @author Michael Totschnig
 *
 */
public class MyExpenses extends ListActivity {
  public static final int ACTIVITY_CREATE=0;
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_SELECT_ACCOUNT=2;

  public static final int INSERT_TA_ID = Menu.FIRST;
  public static final int INSERT_TRANSFER_ID = Menu.FIRST + 1;
  public static final int RESET_ID = Menu.FIRST + 3;
  public static final int DELETE_ID = Menu.FIRST +4;
  public static final int SHOW_DETAIL_ID = Menu.FIRST +5;
  public static final int HELP_ID = Menu.FIRST +6;
  public static final int SELECT_ACCOUNT_ID = Menu.FIRST +7;
  public static final int SETTINGS_ID = Menu.FIRST +8;
  public static final boolean TYPE_TRANSACTION = true;
  public static final boolean TYPE_TRANSFER = false;
  public static final String TRANSFER_EXPENSE = "=>";
  public static final String TRANSFER_INCOME = "<=";
    
  
  private ExpensesDbAdapter mDbHelper;

  private Account mCurrentAccount;
  
  private SharedPreferences mSettings;
  private Cursor mExpensesCursor;

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
    long account_id = mSettings.getLong("current_account", 0);
    mCurrentAccount = new Account(mDbHelper,account_id);
    fillData();
    registerForContextMenu(getListView());
    //DisplayMetrics dm = getResources().getDisplayMetrics();
    //Log.i("SCREEN", dm.widthPixels + ":" + dm.density);
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
    TextView endView= (TextView) findViewById(R.id.end);
    endView.setText(Utils.formatCurrency(mCurrentAccount.getCurrentBalance(),mCurrentAccount.currency));
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
    menu.findItem(INSERT_TRANSFER_ID)
      .setVisible(mDbHelper.getAccountCountWithCurrency(
          mCurrentAccount.currency.getCurrencyCode()) > 1);
    menu.findItem(RESET_ID)
      .setVisible(mExpensesCursor.getCount() > 0);
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, INSERT_TA_ID, 0, R.string.menu_insert_ta);
    menu.add(0, INSERT_TRANSFER_ID, 0, R.string.menu_insert_transfer);
    menu.add(0, RESET_ID,1,R.string.menu_reset);
    menu.add(0, HELP_ID,1,R.string.menu_help);
    menu.add(0, SELECT_ACCOUNT_ID,1,R.string.select_account);
    menu.add(0,SETTINGS_ID,1,R.string.menu_settings);
    return true;
  }

  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
    case INSERT_TA_ID:
      createRow(TYPE_TRANSACTION);
      return true;
    case INSERT_TRANSFER_ID:
      createRow(TYPE_TRANSFER);
      return true;
    case RESET_ID:
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.warning_reset_account)
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              reset();
            }
        })
        .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
      builder.show();
      return true;
    case HELP_ID:
      openHelpDialog();
      return true;
    case SELECT_ACCOUNT_ID:
      Intent i = new Intent(this, SelectAccount.class);
      i.putExtra("current_account", mCurrentAccount.id);
      startActivityForResult(i, ACTIVITY_SELECT_ACCOUNT);
      return true;
    case SETTINGS_ID:
      startActivity(new Intent(this, MyPreferenceActivity.class));
    }
    return super.onMenuItemSelected(featureId, item);
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    menu.add(0, SHOW_DETAIL_ID, 0, R.string.menu_show_detail);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_ID:
      long transfer_peer = mExpensesCursor.getLong(
          mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
      if (transfer_peer == 0)
        mDbHelper.deleteExpense(info.id);
      else
        mDbHelper.deleteTransfer(info.id,transfer_peer);
      fillData();
      return true;
    case SHOW_DETAIL_ID:
      mExpensesCursor.moveToPosition(info.position);
      Toast.makeText(getBaseContext(),
          mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)) +
          "\n" +
          getResources().getString(R.string.payee) + ": " + mExpensesCursor.getString(
              mExpensesCursor.getColumnIndexOrThrow("payee")), Toast.LENGTH_LONG).show();
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
    i.putExtra(ExpensesDbAdapter.KEY_ACCOUNTID,mCurrentAccount.id);
    startActivityForResult(i, ACTIVITY_CREATE);
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
    File appDir = new File("/sdcard/myexpenses/");
    appDir.mkdir();
    File outputFile = new File(appDir, "expenses" + now.format(new Date()) + ".qif");
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
    } catch (IOException e) {
      Log.e("MyExpenses",e.getMessage());
      Toast.makeText(getBaseContext(),getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
    }
    mCurrentAccount.reset();
    fillData();
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

  /* (non-Javadoc)
   * upon return from SelectAccount updates current_account and refreshes view
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_SELECT_ACCOUNT) {
      if (resultCode == RESULT_OK) {
        long account_id = intent.getIntExtra("account_id", 0);
        if (account_id != mCurrentAccount.id) {
          mSettings.edit().putLong("current_account", account_id).commit();
        }
        //refetch account since it might have been edited
        mCurrentAccount = new Account(mDbHelper, account_id);
      }
    }
    fillData();
  }

  /**
   * shows Help screen with link to Changes and Tutorial
   * seen in Mathdoku
   */
  private void openHelpDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.aboutview, null); 
    TextView tv = (TextView)view.findViewById(R.id.aboutVersionCode);
    tv.setText(getVersionInfo());
    new AlertDialog.Builder(MyExpenses.this)
    .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
    .setIcon(R.drawable.about)
    .setView(view)
    .setNeutralButton(R.string.menu_changes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        MyExpenses.this.openChangesDialog();
      }
    })
    .setPositiveButton("Tutorial", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        startActivity( new Intent(MyExpenses.this, Tutorial.class) );
      }
    })
    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        //
      }
    })
    .show();  
  }
  
  
  /**
   * this dialog is shown, when a new version requires to present
   * specific information to the user
   * @param info a String presented to the user in an AlertDialog
   */
  private void openVersionDialog(String info) {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.versiondialog, null);
    TextView versionInfo= (TextView) view.findViewById(R.id.versionInfo);
    versionInfo.setText(info);
    new AlertDialog.Builder(MyExpenses.this)
    .setTitle(R.string.important_version_information)
    .setIcon(R.drawable.about)
    .setView(view)
    .setNeutralButton(R.string.button_continue, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        MyExpenses.this.openHelpDialog();
      }
    })
    .show();
  }
  
  /**
   * opens AlertDialog with ChangeLog
   */
  private void openChangesDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.changeview, null);
    ListView changeList = (ListView) view.findViewById(R.id.changelog);
    final String[][] versions = {
        {
          "1.4.5.2", 
          "February 25 2012",
          "Tutorial translated to German and French. Fixed problem with orientation change during categories import."
        },
        {
          "1.4.5.1",
          "February 17 2012",
          "Problem with export (FTP, email) fixed."
        },
        {
         "1.4.5",
         "February 13 2012",
         "Support for sending QIF file via email. Reduced memory footprint."
        },
        {
          "1.4.4",
          "February 8 2012",
          "FTP upload done in background. Default account created in user's language and currency."
        },
        {
          "1.4.3",
          "February 3 2012",
          "Improved parsing of number input in German locale. Upload of QIF file to FTP server."
        },
        {
          "1.4.1",
          "January 31 2012",
          "Input of transaction amounts now respects user's locale. Bug fix: Opening balance could not be set."
        },
        {
          "1.4.0",
          "January 27 2012",
          "Currencies now handled with java.util.Currency. Support for transfers between accounts."
        },
        {
          "1.3.2",
          "January 17 2012",
          "Improved help and included tutorial."
        },
        {
          "1.3.0",
          "January 13 2012",
          "Cleaned up layout."
        },
        {
          "v1.2.8",
          "January 8 2012",
          "Fixed bug in export."
        },
        {
          "1.2.7",
          "January 7 2012",
          "Added payee field. Ignore empty fields in QIF export."
        },
        {
          "1.2.6",
          "November 6 2011",
          "Better management of categories: Default category files from Grisbi are now included in the app, categories can be deleted and renamed."
        },
        {
          "1.2.0",
          "October 7 2011",
          "Support for multiple accounts"
        },
        {
          "1.1.3",
          "June 5 2011",
          "Use workaround class for bug in SimpleCursorTreeAdapter (NullPointerException when adding the first subcategory) from http://code.google.com/p/android/issues/detail?id=9170; better feedback when category import goes wrong."
        },
        {
          "1.1.2",
          "May 18 2011",
          "Sort categories by usages; Allow assignment of main category; Allow manual creation of categories; Improved handling of categories import."
        },
        {
          "1.0",
          "May 4 2011",
          "Initial version"
        }
    };
    ArrayList<HashMap<String, String>> versionList = new ArrayList<HashMap<String, String>>();
    for (String[] version : versions) {
      HashMap<String, String> map = new HashMap<String, String>();
      map.put("version", version[0]);
      map.put("date", version[1]);
      map.put("changes", version[2]);
      versionList.add(map);
    }
    ListAdapter adapter = new SimpleAdapter(this, versionList , R.layout.version_row, 
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

    
    
    new AlertDialog.Builder(MyExpenses.this)
    .setTitle(R.string.menu_changes)
    .setIcon(R.drawable.about)
    .setView(view)
    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        //
      }
    })
    .show();  
  }

  /**
   * check if this is the first invocation of a new version
   * in which case help dialog is presented
   * also is used for hooking version specific upgrade procedures
   */
  public void newVersionCheck() {
    Editor edit = mSettings.edit();
    int pref_version = mSettings.getInt("currentversion", -1);
    int current_version = getVersionNumber();
    if (pref_version == current_version)
      return;
    if (pref_version == -1) {
      Account account = new Account(
          mDbHelper,
          getString(R.string.app_name),
          0,
          getString(R.string.default_account_description),
          Currency.getInstance(Locale.getDefault())
      );
      long account_id = account.save();
      edit.putLong("current_account", account_id).commit();
      edit.putInt("currentversion", current_version).commit();
      File appDir = new File("/sdcard/myexpenses/");
      appDir.mkdir();
    } else if (pref_version != current_version) {
      edit.putInt("currentversion", current_version).commit();
      if (pref_version < 14) {
        //made current_account long
        edit.putLong("current_account", mSettings.getInt("current_account", 0)).commit();
        String non_conforming = checkCurrencies();
        if (non_conforming.length() > 0 ) {
          openVersionDialog(getString(R.string.version_14_upgrade_info,non_conforming));
          return;
        }
      }
      if (pref_version < 19) {
        //renamed
        edit.putString("share_target",mSettings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
    }
    openHelpDialog();
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
}
