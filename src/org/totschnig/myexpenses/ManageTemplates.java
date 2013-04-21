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

import java.util.ArrayList;

import org.totschnig.myexpenses.ButtonBar.Action;

import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class ManageTemplates extends ExpandableListActivity implements ContribIFace {
  //private static final int DELETE_CONFIRM_DIALOG_ID = 1;
  private MyExpandableListAdapter mAdapter;
  private ExpensesDbAdapter mDbHelper;
  private Cursor mAccountsCursor;

  private static final int DELETE_TEMPLATE = Menu.FIRST;
  private static final int CREATE_INSTANCE_EDIT = Menu.FIRST +1;
  private static final int CREATE_INSTANCE_SAVE = Menu.FIRST +2;
  private static final int EDIT_TEMPLATE = Menu.FIRST +3;
  
  /**
   * stores the template to be edited
   */
  private long mTemplateId;

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_templates);
      setTitle(R.string.pref_manage_templates_title);
      MyApplication.updateUIWithAppColor(this);
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
  registerForContextMenu(getExpandableListView());
  }
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    int type = ExpandableListView
            .getPackedPositionType(info.packedPosition);

    // Menu entries relevant only for the group
    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      menu.add(0,DELETE_TEMPLATE,0,R.string.menu_delete);
      menu.add(0,CREATE_INSTANCE_EDIT,0,R.string.menu_create_transaction_from_template_and_edit);
      menu.add(0,CREATE_INSTANCE_SAVE,0,R.string.menu_create_transaction_from_template_and_save);
      menu.add(0,EDIT_TEMPLATE,0,R.string.menu_edit_template);
    }
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
      long id;
      ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
      int type = ExpandableListView.getPackedPositionType(info.packedPosition);
      if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {         
        Cursor childCursor = (Cursor) mAdapter.getChild(
            ExpandableListView.getPackedPositionGroup(info.packedPosition),
            ExpandableListView.getPackedPositionChild(info.packedPosition)
        );
        id =  childCursor.getLong(childCursor.getColumnIndexOrThrow("_id"));
        Intent intent;
        switch(item.getItemId()) {
          case DELETE_TEMPLATE:   
            mDbHelper.deleteTemplate(id);
            mAccountsCursor.requery();
            break;
          case CREATE_INSTANCE_SAVE:
            if (Transaction.getInstanceFromTemplate(id).save() == -1)
              Toast.makeText(getBaseContext(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
            else
              Toast.makeText(getBaseContext(),getString(R.string.save_transaction_success), Toast.LENGTH_LONG).show();
            break;
          case CREATE_INSTANCE_EDIT:
            intent = new Intent(this, ExpenseEdit.class);
            intent.putExtra("template_id", id);
            intent.putExtra("instantiate", true);
            startActivity(intent);
            break;
          case EDIT_TEMPLATE:
            mTemplateId = id;
            if (Utils.isContribEnabled(this)) {
              contribFeatureCalled("edit_template");
            } else {
              showDialog(R.id.CONTRIB_DIALOG_ID);
            }
        }
      }
      return true;
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
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("TemplateId",mTemplateId);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mTemplateId = savedInstanceState.getLong("TemplateId");
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case R.id.CONTRIB_DIALOG_ID:
      return Utils.contribDialog(this,MyApplication.CONTRIB_FEATURE_EDIT_TEMPLATE);
    }
    return null;
  }

  @Override
  public void contribFeatureCalled(String feature) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("template_id", mTemplateId);
    i.putExtra("instantiate", false);
    startActivity(i);
  }
  @Override
  public void contribFeatureNotCalled() {
    // TODO Auto-generated method stub
    
  }
}
