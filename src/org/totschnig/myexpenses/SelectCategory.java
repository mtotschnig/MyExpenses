package org.totschnig.myexpenses;

import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class SelectCategory extends ExpandableListActivity {
    private MyExpandableListAdapter mAdapter;
    private ExpensesDbAdapter mDbHelper;
    private Cursor groupCursor;
    private static final int CREATE_MAIN_CAT = Menu.FIRST;
    private static final int CREATE_SUB_CAT = Menu.FIRST+2;
    private static final int SELECT_MAIN_CAT = Menu.FIRST+1;
    private static final int EDIT_CAT = Menu.FIRST+3;
    private static final int DELETE_CAT = Menu.FIRST+4;
    int groupIdColumnIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_category);
        setTitle(R.string.select_category);
        // Set up our adapter
        mDbHelper = new ExpensesDbAdapter(SelectCategory.this);
        mDbHelper.open();
        groupCursor = mDbHelper.fetchMainCategories();
        startManagingCursor(groupCursor);

        // Cache the ID column index
        groupIdColumnIndex = groupCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ROWID);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(groupCursor,
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
    	
    	    // Only create a context menu for the group
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
        menu.add(0, CREATE_MAIN_CAT, 0, R.string.menu_create_main_cat);
        return true;
    }
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case CREATE_MAIN_CAT:
            createCat("0");
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int cat_id;
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {         
          Cursor childCursor = (Cursor) mAdapter.getChild(
              ExpandableListView.getPackedPositionGroup(info.packedPosition),
              ExpandableListView.getPackedPositionChild(info.packedPosition)
          );
          cat_id =  childCursor.getInt(childCursor.getColumnIndexOrThrow("_id"));
        } else  {
            cat_id = groupCursor.getInt(groupIdColumnIndex);
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
    				createCat(String.valueOf(cat_id));
    				return true;
    			case EDIT_CAT:
    			  editCat(label,String.valueOf(cat_id));
    			  return true;
    			case DELETE_CAT:
    			  if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && mDbHelper.getSubCatCount(cat_id) > 0) {
    			    Toast.makeText(SelectCategory.this,"nicht löschbar wegen unterkategorien", Toast.LENGTH_LONG).show();
    			  } else if (mDbHelper.getExpensesCount(cat_id) > 0 ) {
    			    Toast.makeText(SelectCategory.this,"nicht löschbar wegen zugeordneten ausgaben", Toast.LENGTH_LONG).show();
    			  } else {
    			    mDbHelper.deleteCat(cat_id);
    			    groupCursor.requery();
    			  }
        }
    		return false;
    	}

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	//Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    	Intent intent=new Intent();
    	int sub_cat = (int) id;
    	Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    	String label =  childCursor.getString(childCursor.getColumnIndexOrThrow("label"));
        intent.putExtra("cat_id",sub_cat);
        intent.putExtra("label", label);
        setResult(RESULT_OK,intent);
    	finish();
    	return true;
    }
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter2 {
    	
        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group
        	String parent_id = groupCursor.getString(groupIdColumnIndex);
        	Cursor itemsCursor = mDbHelper.fetchSubCategories(parent_id);
        	startManagingCursor(itemsCursor);
        	return itemsCursor;

        }
    }
    public void createCat(final String parent_id) {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle(R.string.create_category);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	alert.setView(input);

    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String value = input.getText().toString();
    	  if (mDbHelper.createCategory(value,parent_id) != -1) {
    		  groupCursor.requery();
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
    public void editCat(String label, final String cat_id) {
      AlertDialog.Builder alert = new AlertDialog.Builder(this);
      alert.setTitle(R.string.edit_category);

      // Set an EditText view to get user input 
      final EditText input = new EditText(this);
      input.setText(label);
      alert.setView(input);

      alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String value = input.getText().toString();
        if (mDbHelper.renameCategory(value,cat_id) != -1) {
          groupCursor.requery();
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