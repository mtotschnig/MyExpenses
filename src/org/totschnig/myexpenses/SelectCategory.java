package org.totschnig.myexpenses;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.ExpandableListAdapter;

public class SelectCategory extends ExpandableListActivity {
    private ExpandableListAdapter mAdapter;
    private ExpensesDbAdapter mDbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up our adapter
        // Query for people
        mDbHelper = new ExpensesDbAdapter(SelectCategory.this);
        mDbHelper.open();
		Cursor groupCursor = mDbHelper.fetchMainCategories();

        // Cache the ID column index
        //mGroupIdColumnIndex = groupCursor.getColumnIndexOrThrow(People._ID);

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
        //registerForContextMenu(getExpandableListView());
    }
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    	Intent intent=new Intent();
        intent.putExtra("main_cat", groupPosition);
        intent.putExtra("sub_cat", childPosition);
        setResult(RESULT_OK,intent);
    	finish();
    	return true;
    }
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
    	
        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group
        	String parent_id = groupCursor.getString(groupCursor.getColumnIndexOrThrow("_id"));
        	return mDbHelper.fetchSubCategories(parent_id);
        }
    }
}
