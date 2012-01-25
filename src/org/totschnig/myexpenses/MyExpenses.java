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
import java.text.NumberFormat;
import java.sql.Timestamp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.Currency;
import java.util.Date;


import android.app.AlertDialog;
import android.app.ListActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class MyExpenses extends ListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int ACTIVITY_SELECT_ACCOUNT=2;

  private static final int INSERT_TA_ID = Menu.FIRST;
  private static final int INSERT_TRANSFER_ID = Menu.FIRST + 1;
  private static final int RESET_ID = Menu.FIRST + 3;
  private static final int DELETE_ID = Menu.FIRST +4;
  private static final int SHOW_DETAIL_ID = Menu.FIRST +5;
  private static final int HELP_ID = Menu.FIRST +6;
  private static final int SELECT_ACCOUNT_ID = Menu.FIRST +7;
  public static final boolean TYPE_TRANSACTION = true;
  public static final boolean TYPE_TRANSFER = false;

  private ExpensesDbAdapter mDbHelper;

  private int mCurrentAccount;
  private String mCurrency;
  private float mStart;
  private float mEnd;
  
  private SharedPreferences mSettings;
  private Cursor mExpensesCursor;
  //this boolean stores if a new account has been added in SelectAccount
  //we need this to recreate options menu

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.expenses_list);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();
    mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    newVersionCheck();
    mCurrentAccount = mSettings.getInt("current_account", 1);
    fillData();
    registerForContextMenu(getListView());
    DisplayMetrics dm = getResources().getDisplayMetrics();
    Log.i("SCREEN", dm.widthPixels + ":" + dm.density);
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
  }
  private void fillData() {
    mExpensesCursor = mDbHelper.fetchExpenseAll(mCurrentAccount);
    startManagingCursor(mExpensesCursor);
    Cursor account = mDbHelper.fetchAccount(mCurrentAccount);
    setTitle(account.getString(account.getColumnIndexOrThrow("label")));
    mStart = account.getFloat(account.getColumnIndexOrThrow("opening_balance"));
    mCurrency = account.getString(account.getColumnIndexOrThrow("currency")).trim();
    account.close();
    TextView startView= (TextView) findViewById(R.id.start);
    startView.setText(formatCurrency(mStart));

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"label",ExpensesDbAdapter.KEY_DATE,ExpensesDbAdapter.KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.text1,R.id.date1,R.id.float1};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter expense = new SimpleCursorAdapter(this, R.layout.expense_row, mExpensesCursor, from, to)  {
      @Override
      public void setViewText(TextView v, String text) {
        super.setViewText(v, convText(v, text));
      }
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        TextView tv1 = (TextView)row.findViewById(R.id.float1);
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
        return row;
      }
    };
    setListAdapter(expense);
    TextView endView= (TextView) findViewById(R.id.end);
    mEnd = mStart + mDbHelper.getExpenseSum(mCurrentAccount);
    endView.setText(formatCurrency(mEnd));
  }
  private String formatCurrency(float amount) {
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    try {
      nf.setCurrency(Currency.getInstance(mCurrency));
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",mCurrency + " is not defined in ISO 4217");
    }
    
    return nf.format(amount);
  }
  private String convText(TextView v, String text) {
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM HH:mm");
    float amount;
    switch (v.getId()) {
    case R.id.date1:
      return formatter.format(Timestamp.valueOf(text));
    case R.id.float1:
      try {
        amount = Float.valueOf(text);
      } catch (NumberFormatException e) {
        amount = 0;
      }
      return formatCurrency(amount);
    }
    return text;
  } 

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem item = menu.findItem(INSERT_TRANSFER_ID);
    item.setVisible(mDbHelper.getAccountCountWithCurrency(mCurrency) > 1);
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
      reset();
      return true;
    case HELP_ID:
      openHelpDialog();
      return true;
    case SELECT_ACCOUNT_ID:
      Intent i = new Intent(this, SelectAccount.class);
      i.putExtra("current_account", mCurrentAccount);
      startActivityForResult(i, ACTIVITY_SELECT_ACCOUNT);
      return true;
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
      int transfer_peer = mExpensesCursor.getInt(
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
  private void createRow(boolean type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("operationType", type);
    i.putExtra(ExpensesDbAdapter.KEY_ACCOUNTID,mCurrentAccount);
    startActivityForResult(i, ACTIVITY_CREATE);
  }

  private void exportAll() throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("ddMM-HHmm");
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    Log.e("MyExpenses","now starting export");
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
      label = (label == null || label.length() == 0) ? "" : "\nL" + label;
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

  }
  private void reset() {
    try {
      exportAll();
      mDbHelper.deleteExpenseAll(mCurrentAccount);
      mDbHelper.updateAccountOpeningBalance(mCurrentAccount,mEnd);
      fillData();
    } catch (IOException e) {
      Log.e("MyExpenses",e.getMessage());
      Toast.makeText(getBaseContext(),getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    boolean operationType = mExpensesCursor.getInt(
        mExpensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER)) == 0;
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    i.putExtra("operationType", operationType);
    startActivityForResult(i, ACTIVITY_EDIT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_SELECT_ACCOUNT) {
      if (resultCode == RESULT_OK) {
        mCurrentAccount = intent.getIntExtra("account_id", 0);
        mSettings.edit().putInt("current_account", mCurrentAccount).commit();
      }
    }
    fillData();
  }
  //from Mathdoku
  private void openHelpDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.aboutview, null); 
    TextView tv = (TextView)view.findViewById(R.id.aboutVersionCode);
    tv.setText(getVersionName() + " (revision " + getVersionNumber() + ")");
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
    .show();  
  }
  
  //this dialog is shown, when a new version requires to present
  //specific information to the user
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
  
  private void openChangesDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.changeview, null);
    
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

  public void newVersionCheck() {
    Editor edit = mSettings.edit();
    int pref_version = mSettings.getInt("currentversion", -1);
    int current_version = getVersionNumber();
    if (pref_version == -1) {
      long account_id = mDbHelper.createAccount("Default account","0","Default account created upon installation","EUR");
      edit.putInt("current_account", (int) account_id).commit();      
    }
    if (pref_version != current_version) {
      edit.putInt("currentversion", current_version).commit();
      if (pref_version < 16) {
        String non_conforming = checkCurrencies();
        if (non_conforming.length() > 0 ) {
          openVersionDialog(getString(R.string.version_14_upgrade_info,non_conforming));
          return;
        }
      }
      openHelpDialog();
    }
    return;
  }
 
  //loop through defined accounts and check if currency is a valid ISO 4217 code
  //returns String concatenation of non conforming symbols in use
  private String checkCurrencies() {
    String account_id;
    String currency;
    String non_conforming = "";
    Cursor accountsCursor = mDbHelper.fetchAccountAll();
    accountsCursor.moveToFirst();
    while(!accountsCursor.isAfterLast()) {
         currency = accountsCursor.getString(accountsCursor.getColumnIndex("currency")).trim();
         account_id = accountsCursor.getString(accountsCursor.getColumnIndex("_id"));
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

  public String getVersionName() {
    String versionname = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      versionname = pi.versionName;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return versionname;
  }
}
