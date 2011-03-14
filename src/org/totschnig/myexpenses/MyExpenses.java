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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyExpenses extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    private static final int ACTIVITY_PREF=2;
    
    private static final int INSERT_EXP_ID = Menu.FIRST;
    private static final int INSERT_INC_ID = Menu.FIRST +1;
    private static final int PREF_ID = Menu.FIRST + 2;
    private static final int RESET_ID = Menu.FIRST + 3;
    private static final int DELETE_ID = Menu.FIRST +4;
    
    public static final boolean INCOME = true;
    public static final boolean EXPENSE = false;

    private ExpensesDbAdapter mDbHelper;
    private SimpleDateFormat formatter = new SimpleDateFormat("dd.MM KK:mm");

	float start;
	float end;
	SharedPreferences settings;
	Cursor expensesCursor;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expenses_list);
        mDbHelper = new ExpensesDbAdapter(this);
        mDbHelper.open();
        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        fillData();
        registerForContextMenu(getListView());
    }
    
    private void fillData() {
    	expensesCursor = mDbHelper.fetchAllExpenses();
        startManagingCursor(expensesCursor);
    	try {
    		  start = Float.parseFloat(settings.getString("start_amount", "0"));
    		} catch (NumberFormatException e) {
    			start = 0;
    		}
        TextView startView= (TextView) findViewById(R.id.start);
        startView.setText(NumberFormat.getCurrencyInstance().format(start));
        
        // Create an array to specify the fields we want to display in the list
        String[] from = new String[]{ExpensesDbAdapter.KEY_COMMENT,ExpensesDbAdapter.KEY_DATE,ExpensesDbAdapter.KEY_AMOUNT};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
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
        end = start + mDbHelper.getSum();
        endView.setText(NumberFormat.getCurrencyInstance().format(end));
    }
    private String convText(TextView v, String text) {
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
        	  return NumberFormat.getCurrencyInstance().format(amount);
        }
          return text;
    } 

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_EXP_ID, 0, R.string.menu_insert_exp);
        menu.add(0, INSERT_INC_ID, 0, R.string.menu_insert_inc);
        menu.add(0, PREF_ID,1,R.string.edit_preferences);
        menu.add(0, RESET_ID,1,R.string.menu_reset);
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
        case PREF_ID:
        	editPreferences();
        	return true;
        case RESET_ID:
        	reset();
        }
        return super.onMenuItemSelected(featureId, item);
    }
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case DELETE_ID:
    		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        mDbHelper.deleteExpense(info.id);
	        fillData();
	        return true;
		}
		return super.onContextItemSelected(item);
	}	
    private void createRow(boolean type) {
        Intent i = new Intent(this, ExpenseEdit.class);
        i.putExtra("type",type);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    private void editPreferences() {
    	Intent i = new Intent(this, MyPreferenceActivity.class);
        startActivityForResult(i, ACTIVITY_PREF);
    }
    private void exportAll() {
    	SimpleDateFormat now = new SimpleDateFormat("ddMM-KKmm");
    	Log.e("MyExpenses","now starting export");
    	try {
    		File appDir = new File("/sdcard/myexpenses/");
    		appDir.mkdir();
    		File outputFile = new File(appDir, "expenses" + now.format(new Date()) + ".csv");
    		FileOutputStream out = new FileOutputStream(outputFile);
        	expensesCursor.moveToFirst();
        	while( expensesCursor.getPosition() < expensesCursor.getCount() ) {
        		String row = expensesCursor.getString(
        				expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE)) +
        				"," +
        				expensesCursor.getString(
                				expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)) +
          				"," +
          				expensesCursor.getString(
                   				expensesCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)) +
                   		"\n";
        		out.write(row.getBytes());
        		expensesCursor.moveToNext();
        	}
    	    out.close();
    	} catch (IOException e) {
    		Log.e("MyExpenses",e.getMessage());
    	}
    }
    private void reset() {
    	exportAll();
    	mDbHelper.deleteAll();
    	settings.edit().putString("start_amount", Float.toString(end)).commit();
    	fillData();
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
        fillData();
    }
}
