/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")savedInstanceState;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.sql.Timestamp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.Date;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.AsyncTask;
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
import android.util.Log;

public class MyExpenses extends ListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int ACTIVITY_SELECT_ACCOUNT=2;

  private static final int INSERT_EXP_ID = Menu.FIRST;
  private static final int INSERT_INC_ID = Menu.FIRST +1;
  private static final int IMPORT_CAT_ID = Menu.FIRST + 2;
  private static final int RESET_ID = Menu.FIRST + 3;
  private static final int DELETE_ID = Menu.FIRST +4;
  private static final int SHOW_DETAIL_ID = Menu.FIRST +5;
  private static final int HELP_ID = Menu.FIRST +6;
  private static final int SELECT_ACCOUNT_ID = Menu.FIRST +7;

  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;

  private ExpensesDbAdapter mDbHelper;

  int current_account;
  float start;
  float end;
  SharedPreferences settings;
  Cursor expensesCursor;
  String currency;
  
  static final String CATEGORIES_XML = "/sdcard/myexpenses/categories.xml";
  ProgressDialog progressDialog;
  int totalCategories;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.expenses_list);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();
    settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    newVersionCheck();
    current_account = settings.getInt("current_account", 1);
    fillData();
    registerForContextMenu(getListView());
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
  }
  private void fillData() {
    expensesCursor = mDbHelper.fetchAllExpenses(current_account);
    startManagingCursor(expensesCursor);
    Cursor account = mDbHelper.fetchAccount(current_account);
    setTitle(account.getString(account.getColumnIndexOrThrow("label")));
    start = account.getFloat(account.getColumnIndexOrThrow("opening_balance"));
    currency = account.getString(account.getColumnIndexOrThrow("currency"));
    account.close();
    TextView startView= (TextView) findViewById(R.id.start);
    startView.setText(formatCurrency(start));

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"label",ExpensesDbAdapter.KEY_DATE,ExpensesDbAdapter.KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.text1,R.id.date1,R.id.float1};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter expense = new SimpleCursorAdapter(this, R.layout.expense_row, expensesCursor, from, to)  {
      @Override
      public void setViewText(TextView v, String text) {
        super.setViewText(v, convText(v, text));
      }
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        Cursor c = getCursor();
        c.moveToPosition(position);
        int col = c.getColumnIndex(ExpensesDbAdapter.KEY_AMOUNT);
        float amount = c.getFloat(col);
        boolean type = amount > 0;
        if (type == EXPENSE) {
          row.setBackgroundColor(android.graphics.Color.RED);
          // Set the background color of the text.
        }
        else {
          row.setBackgroundColor(android.graphics.Color.BLACK);
        }
        return row;
      }
    };
    setListAdapter(expense);
    TextView endView= (TextView) findViewById(R.id.end);
    end = start + mDbHelper.getSum(current_account);
    endView.setText(formatCurrency(end));
  }
  private String formatCurrency(float amount) {
    return currency + " " + NumberFormat.getInstance().format(amount);
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
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, INSERT_EXP_ID, 0, R.string.menu_insert_exp);
    menu.add(0, INSERT_INC_ID, 0, R.string.menu_insert_inc);
    menu.add(0, IMPORT_CAT_ID,1,R.string.import_categories);
    menu.add(0, RESET_ID,1,R.string.menu_reset);
    menu.add(0, HELP_ID,1,R.string.menu_help);
    menu.add(0, SELECT_ACCOUNT_ID,1,R.string.select_account);
    return true;
  }

  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
    case INSERT_EXP_ID:
      createRow(EXPENSE);
      return true;
    case INSERT_INC_ID:
      createRow(INCOME);
      return true;
    case IMPORT_CAT_ID:
      importCategories();
      return true;
    case RESET_ID:
      reset();
      return true;
    case HELP_ID:
      openHelpDialog();
      return true;
    case SELECT_ACCOUNT_ID:
      Intent i = new Intent(this, SelectAccount.class);
      //i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
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
      mDbHelper.deleteExpense(info.id);
      fillData();
      return true;
    case SHOW_DETAIL_ID:
      expensesCursor.moveToPosition(info.position);
      Toast.makeText(getBaseContext(), expensesCursor.getString(
          expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)), Toast.LENGTH_LONG).show();
      return true;
    }
    return super.onContextItemSelected(item);
  }  
  private void createRow(boolean type) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("type",type);
    i.putExtra("account_id",current_account);
    startActivityForResult(i, ACTIVITY_CREATE);
  }
  private void importCategories() {
    new MyAsyncTask(MyExpenses.this).execute();
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
    expensesCursor.moveToFirst();
    while( expensesCursor.getPosition() < expensesCursor.getCount() ) {
      String row = "D"+formatter.format(Timestamp.valueOf(expensesCursor.getString(
          expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE)))) +
          "\nT"+expensesCursor.getString(
              expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)) +
              "\nM" +expensesCursor.getString(
                  expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)) +
                  "\nL" +expensesCursor.getString(
                      expensesCursor.getColumnIndexOrThrow("label")) +
                      "\n^\n";
      out.write(row.getBytes());
      expensesCursor.moveToNext();
    }
    out.close();
    expensesCursor.moveToFirst();
    Toast.makeText(getBaseContext(),String.format(getString(R.string.export_expenses_sdcard_success), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();

  }
  private void reset() {
    try {
      exportAll();
      mDbHelper.deleteAll(current_account);
      mDbHelper.setOpeningBalance(current_account,end);
      fillData();
    } catch (IOException e) {
      Log.e("MyExpenses",e.getMessage());
      Toast.makeText(getBaseContext(),getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_SELECT_ACCOUNT) {
      if (resultCode == RESULT_OK) {
        current_account = intent.getIntExtra("account_id", 0);
        settings.edit().putInt("current_account", current_account).commit();
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
    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        dialog.dismiss();
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
    int pref_version = settings.getInt("currentversion", -1);
    int current_version = getVersionNumber();
    if (pref_version == -1 || pref_version != current_version) {
      Editor edit = settings.edit();
      edit.putInt("currentversion", current_version);
      if (current_version == 7) {
        String opening_balance = settings.getString("opening_balance", "0");
        long account_id = mDbHelper.createAccount("Default account",opening_balance,"Default account created upon installation","EUR");
        mDbHelper.setAccountAll(account_id);
        edit.putInt("current_account", (int) account_id);
      }
      edit.commit();
      openHelpDialog();
      return;
    }
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
  private class MyAsyncTask extends AsyncTask<Void, Integer, Integer> {
    private Context context;
    NodeList categories;
    NodeList sub_categories;
    FileInputStream catXML;
    Document dom;
    Hashtable<String,String> Foreign2LocalIdMap;
    int totalImported;

    public MyAsyncTask(Context context) {
      this.context = context;
      Foreign2LocalIdMap = new Hashtable<String,String>();
      totalImported = 0; 
    }
    protected void onPreExecute() {
      super.onPreExecute();
      try {
        catXML = new FileInputStream(CATEGORIES_XML);
      } catch (FileNotFoundException e) {
        Toast.makeText(context, "Could not find file "+CATEGORIES_XML, Toast.LENGTH_LONG).show();
        cancel(false);
        return;
      }
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        dom = builder.parse(catXML);
      } catch (SAXParseException e) {
        Log.w("MyExpenses",e.getMessage());
        Toast.makeText(context, "Could not parse file "+CATEGORIES_XML, Toast.LENGTH_LONG).show();
        cancel(false);
        return;
      } catch (Exception e) {
        Toast.makeText(context, "An error occured: "+e.getMessage(), Toast.LENGTH_LONG).show();
        cancel(false);
        return;
      }
      progressDialog = new ProgressDialog(context);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setTitle(getString(R.string.categories_loading,CATEGORIES_XML));
      progressDialog.setProgress(0);
      progressDialog.show();
    }
    protected void onProgressUpdate(Integer... values) {
      progressDialog.setProgress(values[0]);
    }
    protected void onPostExecute(Integer result) {
      progressDialog.dismiss();
      String msg;
      super.onPostExecute(result);
      if (result == -1) {
        msg = getString(R.string.import_categories_failure);
      } else {
        msg = getString(R.string.import_categories_success,result);
      }
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }


    @Override
    protected Integer doInBackground(Void... params) {
      //first we do the parsing
      Element root = dom.getDocumentElement();
      categories = root.getElementsByTagName("Category");
      sub_categories = root.getElementsByTagName("Sub_category");
      totalCategories = categories.getLength() + sub_categories.getLength();
      progressDialog.setMax(totalCategories);

      mDbHelper.open();

      importCatsMain();
      importCatsSub();
      return totalImported;
    }
    private void importCatsMain() {
      int start = 1;
      String label;
      String id;
      long _id;

      for (int i=0;i<categories.getLength();i++){
        NamedNodeMap category = categories.item(i).getAttributes();
        label = category.getNamedItem("Na").getNodeValue();
        id =  category.getNamedItem("Nb").getNodeValue();
        _id = mDbHelper.getCategoryId(label, "0");
        if (_id != -1) {
          Foreign2LocalIdMap.put(id, String.valueOf(_id));
        } else {
          _id = mDbHelper.createCategory(label,"0");
          if (_id != -1) {
            Foreign2LocalIdMap.put(id, String.valueOf(_id));
            totalImported++;
          } else {
            //this should not happen
            Log.w("MyExpenses","could neither retrieve nor store main category " + label);
          }
        }
        if ((start+i) % 10 == 0) {
          publishProgress(start+i);
        }
      }
    }
    private void importCatsSub() {
      int start = categories.getLength() + 1;
      String label;
      //String id;
      String parent_id;
      String mapped_parent_id;
      long _id;
      for (int i=0;i<sub_categories.getLength();i++){
        NamedNodeMap sub_category = sub_categories.item(i).getAttributes();
        label = sub_category.getNamedItem("Na").getNodeValue();
        //id =  sub_category.getNamedItem("Nb").getNodeValue();
        parent_id = sub_category.getNamedItem("Nbc").getNodeValue();
        mapped_parent_id = Foreign2LocalIdMap.get(parent_id);
        //TODO: for the moment, we do not deal with subcategories,
        //if we were not able to import a matching category
        //should check if the main category exists, but the subcategory is new
        if (mapped_parent_id != null) {
          _id = mDbHelper.createCategory(label, Foreign2LocalIdMap.get(parent_id));
          if (_id != -1) {
            totalImported++;
          }
        } else {
          Log.w("MyExpenses","could not store sub category " + label);
        }
        if ((start+i) % 10 == 0) {
          publishProgress(start+i);
        }
      }
    }
  }


}
