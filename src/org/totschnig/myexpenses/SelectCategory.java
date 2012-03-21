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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 * @author Michael Totschnig
 *
 */
public class SelectCategory extends ExpandableListActivity {
    private MyExpandableListAdapter mAdapter;
    private ExpensesDbAdapter mDbHelper;
    private Cursor mGroupCursor;
    /**
     * create a new main category
     */
    private static final int CREATE_MAIN_CAT = Menu.FIRST;
    /**
     * create a new sub category
     */
    private static final int CREATE_SUB_CAT = Menu.FIRST+2;
    /**
     * return the main cat to the calling activity
     */
    private static final int SELECT_MAIN_CAT = Menu.FIRST+1;
    /**
     * edit the category label
     */
    private static final int EDIT_CAT = Menu.FIRST+3;
    /**
     * delete the category after checking if
     * there are mapped transactions or subcategories
     */
    private static final int DELETE_CAT = Menu.FIRST+4;

    int mGroupIdColumnIndex;
    

    private int monkey_state = 0;

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENVELOPE) {
        switch (monkey_state) {
        case 0:
          getExpandableListView().requestFocus();
          return true;
        }
      }
      return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_category);
        setTitle(R.string.select_category);
        // Set up our adapter
        mDbHelper = MyApplication.db();
        mGroupCursor = mDbHelper.fetchCategoryMain();
        startManagingCursor(mGroupCursor);

        // Cache the ID column index
        mGroupIdColumnIndex = mGroupCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ROWID);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(mGroupCursor,
                this,
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[]{"label"},
                new int[] {android.R.id.text1},
                new String[] {"label"},
                new int[] {android.R.id.text1});

        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

    	    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    	    int type = ExpandableListView
    	            .getPackedPositionType(info.packedPosition);
    	
    	    // Menu entries relevant only for the group
    	    if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
    	    	menu.add(0,SELECT_MAIN_CAT,0,R.string.select_parent_category);
    	    	menu.add(0,CREATE_SUB_CAT,0,R.string.menu_create_sub_cat);
    	    }
    	    menu.add(0,DELETE_CAT,0,R.string.menu_delete_cat);
    	    menu.add(0,EDIT_CAT,0,R.string.menu_edit_cat);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, CREATE_MAIN_CAT, 0, R.string.menu_create_main_cat)
            .setIcon(android.R.drawable.ic_menu_add)
            .setAlphabeticShortcut('a');
/*        menu.add(0, IMPORT_CAT_ID,1,R.string.import_categories)
            .setIcon(R.drawable.squiggle)
            .setAlphabeticShortcut('b');*/
        return true;
    }
    
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case CREATE_MAIN_CAT:
            createCat(0);
            return true;
/*        case IMPORT_CAT_ID:
          importCategories();
          return true;*/
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long cat_id;
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {         
          Cursor childCursor = (Cursor) mAdapter.getChild(
              ExpandableListView.getPackedPositionGroup(info.packedPosition),
              ExpandableListView.getPackedPositionChild(info.packedPosition)
          );
          cat_id =  childCursor.getLong(childCursor.getColumnIndexOrThrow("_id"));
        } else  {
          cat_id = mGroupCursor.getLong(mGroupIdColumnIndex);
        }
        String label =   ((TextView) info.targetView).getText().toString();
        
    		switch(item.getItemId()) {
    			case SELECT_MAIN_CAT:  	
    	    	Intent intent=new Intent();		    	
    	      intent.putExtra("cat_id", cat_id);
    	      intent.putExtra("label", label);
    	      setResult(RESULT_OK,intent);
    	    	finish();
    	      return true;
    			case CREATE_SUB_CAT:
    				createCat(cat_id);
    				return true;
    			case EDIT_CAT:
    			  editCat(label,cat_id);
    			  return true;
    			case DELETE_CAT:
    			  if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && mDbHelper.getCategoryCountSub(cat_id) > 0) {
    			    Toast.makeText(this,getString(R.string.not_deletable_subcats_exists), Toast.LENGTH_LONG).show();
    			  } else if (mDbHelper.getExpenseCount(cat_id) > 0 ) {
    			    Toast.makeText(this,getString(R.string.not_deletable_mapped_expenses), Toast.LENGTH_LONG).show();
    			  } else {
    			    mDbHelper.deleteCategory(cat_id);
    			    mGroupCursor.requery();
    			  }
        }
    		return false;
    	}
    /* (non-Javadoc)
     * return the sub cat to the calling activity 
     * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
     */
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	//Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    	Intent intent=new Intent();
    	long sub_cat = id;
    	Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    	String label =  childCursor.getString(childCursor.getColumnIndexOrThrow("label"));
        intent.putExtra("cat_id",sub_cat);
        intent.putExtra("label", label);
        setResult(RESULT_OK,intent);
    	finish();
    	return true;
    }
    /**
     * Mapping the categories table into the ExpandableList
     * @author Michael Totschnig
     *
     */
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter2 {
    	
        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }
        /* (non-Javadoc)
         * returns a cursor with the subcategories for the group
         * @see android.widget.CursorTreeAdapter#getChildrenCursor(android.database.Cursor)
         */
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group
        	long parent_id = groupCursor.getLong(mGroupIdColumnIndex);
        	Cursor itemsCursor = mDbHelper.fetchCategorySub(parent_id);
        	startManagingCursor(itemsCursor);
        	return itemsCursor;

        }
    }
    
    /**
     * presents AlertDialog for adding a new category
     * if label is already used, shows an error
     * @param parent_id
     */
    public void createCat(final long parent_id) {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle(R.string.create_category);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	alert.setView(input);

    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String value = input.getText().toString();
    	  if (mDbHelper.createCategory(value,parent_id) != -1) {
    		  mGroupCursor.requery();
    		  //mAdapter.notifyDataSetChanged();
    	  } else {
    		  Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
    	  }
          //getExpandableListView().invalidateViews();
    	  }
    	});

    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	  public void onClick(DialogInterface dialog, int whichButton) {
    		  dialog.dismiss();
    	  }
    	});

    	alert.show();
    }
    /**
     * presents AlertDialog for editing an existing category
     * if label is already used, shows an error
     * @param label
     * @param cat_id
     */
    public void editCat(String label, final long cat_id) {
      AlertDialog.Builder alert = new AlertDialog.Builder(this);
      alert.setTitle(R.string.edit_category);

      // Set an EditText view to get user input 
      final EditText input = new EditText(this);
      input.setText(label);
      alert.setView(input);

      alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String value = input.getText().toString();
        if (mDbHelper.updateCategoryLabel(value,cat_id) != -1) {
          mGroupCursor.requery();
          //mAdapter.notifyDataSetChanged();
        } else {
          Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
        }
          //getExpandableListView().invalidateViews();
        }
      });

      alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          dialog.dismiss();
        }
      });
      alert.show();
    }
}