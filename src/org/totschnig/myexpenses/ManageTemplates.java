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



import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ManageTemplates extends ExpandableListActivity {
  //private static final int DELETE_CONFIRM_DIALOG_ID = 1;
  private MyExpandableListAdapter mAdapter;
  private ExpensesDbAdapter mDbHelper;
  private Cursor mAccountsCursor;
  private long mDeleteTemplateId;
  private String mDeleteTemplateTitle;

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_templates);
      setTitle(R.string.pref_manage_templates_title);
      mDbHelper = MyApplication.db();
      
      ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_templates);

      mAccountsCursor = mDbHelper.fetchAccountAll();
      startManagingCursor(mAccountsCursor);
      mAdapter = new MyExpandableListAdapter(mAccountsCursor,
          this,
          android.R.layout.simple_expandable_list_item_1,
          android.R.layout.simple_expandable_list_item_1,
          new String[]{"label"},
          new int[] {android.R.id.text1},
          new String[] {"title"},
          new int[] {android.R.id.text1});

  setListAdapter(mAdapter);
  }
  @Override
  public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    //Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
    mDeleteTemplateId = id;
    Cursor childCursor = (Cursor) mAdapter.getChild(groupPosition,childPosition);
    mDeleteTemplateTitle = childCursor.getString(childCursor.getColumnIndexOrThrow("title"));
    showDialog(0);
    return true;
  }
  @Override
  protected Dialog onCreateDialog(final int id) {
    return new AlertDialog.Builder(this)
    .setMessage(getString(R.string.dialog_confirm_delete_template,mDeleteTemplateTitle))
    .setCancelable(false)
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          mDbHelper.deleteTemplate(mDeleteTemplateId);
          mAccountsCursor.requery();
        }
    })
    .setNegativeButton(android.R.string.no, null).create();
  }
 
  //safeguard for orientation change during dialog
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("DeleteTemplateId", mDeleteTemplateId);
   outState.putString("DeleteTemplateTitle", mDeleteTemplateTitle);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mDeleteTemplateId = savedInstanceState.getLong("DeleteTemplateId");
   mDeleteTemplateTitle = savedInstanceState.getString("DeleteTemplateTitle");
  }

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
      long account_id = groupCursor.getLong(mAccountsCursor.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ROWID));
      Cursor itemsCursor = mDbHelper.fetchTemplates(account_id);
      startManagingCursor(itemsCursor);
      return itemsCursor;
    }
  }
}
