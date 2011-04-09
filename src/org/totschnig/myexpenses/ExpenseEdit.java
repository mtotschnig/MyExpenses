/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.sql.Timestamp;
import java.util.Date;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

public class ExpenseEdit extends Activity {

	private EditText mDateText;
	private EditText mTimeText;
	private EditText mAmountText;
    private EditText mCommentText;
    private Button categoryButton;
    private Long mRowId;
    private boolean type;
    private ExpensesDbAdapter mDbHelper;
    private int main_cat_id;
    private int sub_cat_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new ExpensesDbAdapter(this);
        mDbHelper.open();
        setContentView(R.layout.one_expense);
        
        mDateText = (EditText) findViewById(R.id.Date);
        mTimeText = (EditText) findViewById(R.id.Time);
        mAmountText = (EditText) findViewById(R.id.Amount);
        mCommentText = (EditText) findViewById(R.id.Comment);
      
        Button confirmButton = (Button) findViewById(R.id.Confirm);
       
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(ExpensesDbAdapter.KEY_ROWID) 
                							: null;
        Bundle extras = getIntent().getExtras();
		if (mRowId == null) {
			mRowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID) 
									: 0;
		}
		type = extras.getBoolean("type");
		
        confirmButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        	    setResult(RESULT_OK);
        	    saveState();
        	    finish();
        	}
          
        });
        categoryButton = (Button) findViewById(R.id.Category);
        categoryButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        		startSelectCategory();
        	} 
        });
		populateFields();
    }
    private void startSelectCategory() {
    	if ( mDbHelper.getCategoriesCount() == 0 ) {
    		Toast.makeText(this, "No categories imported yet", Toast.LENGTH_LONG).show();
    	} else {
    		Intent i = new Intent(this, SelectCategory.class);
		    //i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
		    startActivityForResult(i, 0);
    	}
   	}
//    protected Dialog onCreateDialog(int id) {
//    	//TODO check for id
///*    	ArrayList<Category> items = new ArrayList<Category>();
//    	items.add(new Category("red"));
//    	items.add(new Category("green"));
//    	items.add(new Category("blue"));
//    	Dialog dialog = new Dialog(this);
//    	dialog.setTitle("Pick a category");
//    	dialog.setContentView(R.layout.select_category);
//    	ListView catlist = (ListView) dialog.findViewById(R.id.maincatlist);
//    	catlist.setAdapter(new ArrayAdapter<Category>(this,R.layout.category_row,items));
//    	return dialog;
//*/    
//    }
//    
    private void populateFields() {
    	float amount;
    	TableLayout mScreen = (TableLayout) findViewById(R.id.Table);
        if (mRowId != 0) {
            Cursor note = mDbHelper.fetchExpense(mRowId);
            startManagingCursor(note);
            String dateString = note.getString(
    	            note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE));
            Timestamp date = Timestamp.valueOf(dateString);
            setDate(date);
            try {
      		  amount = Float.valueOf(note.getString(
      	            note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)));
      	  } catch (NumberFormatException e) {
      		  amount = 0;
      	  }
      	  if (amount > 0) {
      		  type = MyExpenses.INCOME;
      		  setTitle(R.string.menu_edit_inc);
      	  } else {
      		  amount = 0 - amount;
      		  type = MyExpenses.EXPENSE;
      		  setTitle(R.string.menu_edit_exp);
      	  }
            mAmountText.setText(Float.toString(amount));
            mCommentText.setText(note.getString(
                    note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT)));
            main_cat_id = note.getInt(note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_MAINCATID));
            sub_cat_id = note.getInt(note.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_SUBCATID));
            categoryButton.setText(note.getString(note.getColumnIndexOrThrow("label")));
        } else {
        	Date date =  new Date();
        	setDate(date);
        	if (type == MyExpenses.INCOME) {
        		setTitle(R.string.menu_insert_inc);
        	} else {
        		setTitle(R.string.menu_insert_exp);
        	}
        }
        if (type == MyExpenses.INCOME) {
       		mScreen.setBackgroundColor(android.graphics.Color.BLACK);
        } else {
        	mScreen.setBackgroundColor(android.graphics.Color.RED);
        }
    }
    private void setDate(Date date) {
    	mDateText.setText((date.getYear()+1900) + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate()));
        mTimeText.setText(pad(date.getHours()) + ":" + pad(date.getMinutes()));
    }
    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ExpensesDbAdapter.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        //saveState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        //populateFields();
    }
    
    private void saveState() {
        String amount = mAmountText.getText().toString();
        String comment = mCommentText.getText().toString();
        String strDate = mDateText.getText().toString() + " " + mTimeText.getText().toString() + ":00.0";
    	if (type == MyExpenses.EXPENSE) {
    		amount = "-"+ amount;
    	}
        if (mRowId == 0) {
            long id = mDbHelper.createExpense(strDate, amount, comment,String.valueOf(main_cat_id),String.valueOf(sub_cat_id));
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateExpense(mRowId, strDate, amount, comment,String.valueOf(main_cat_id),String.valueOf(sub_cat_id));
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
    	if (intent != null) {
	        //Here we will have to set the category for the expense
	        main_cat_id = intent.getIntExtra("main_cat",0);
	        sub_cat_id = intent.getIntExtra("sub_cat",0);
	        categoryButton.setText(intent.getStringExtra("label"));
	        Toast.makeText(this, "Select category returned main_cat :" +main_cat_id+";sub_cat :"+sub_cat_id, Toast.LENGTH_LONG).show();
    	}
    }
}
