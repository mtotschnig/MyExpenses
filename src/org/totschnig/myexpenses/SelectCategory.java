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
    private Cursor groupCursor;
    int groupIdColumnIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.select_category);
        // Set up our adapter
        mDbHelper = new ExpensesDbAdapter(SelectCategory.this);
        mDbHelper.open();
		groupCursor = mDbHelper.fetchMainCategories();

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
        //registerForContextMenu(getExpandableListView());
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
    	int main_cat = groupCursor.getInt(groupIdColumnIndex);
    	int sub_cat = (int) id;
    	Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    	String label =  childCursor.getString(childCursor.getColumnIndexOrThrow("label"));
        intent.putExtra("main_cat", main_cat);
        intent.putExtra("sub_cat",sub_cat);
        intent.putExtra("label", label);
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
        	String parent_id = groupCursor.getString(groupIdColumnIndex);
        	return mDbHelper.fetchSubCategories(parent_id);
        }
    }
}
