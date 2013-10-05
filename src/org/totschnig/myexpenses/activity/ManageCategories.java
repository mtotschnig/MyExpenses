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

package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.SelectGroupingDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.fragment.CategoryList;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 * @author Michael Totschnig
 *
 */
public class ManageCategories extends ProtectedFragmentActivity implements
    OnChildClickListener, OnGroupClickListener,EditTextDialogListener  {

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
    
    public enum HelpVariant {
      manage,distribution,select
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      Intent intent = getIntent();
      String action = intent.getAction();
      Bundle extras = intent.getExtras();
      if (action != null && action.equals("myexpenses.intent.manage.categories")) {
        helpVariant = HelpVariant.manage;
        setTitle(R.string.pref_manage_categories_title);
      } else if (extras != null) {
        helpVariant = HelpVariant.distribution;
        //title is set in categories list
      } else {
        helpVariant = HelpVariant.select;
        setTitle(R.string.select_category);
      }
      setContentView(R.layout.select_category);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getSupportMenuInflater();
      if (helpVariant.equals(HelpVariant.distribution)) {
        inflater.inflate(R.menu.distribution, menu);
      } else {
        inflater.inflate(R.menu.categories, menu);
      }
      super.onCreateOptionsMenu(menu);
      return true;
    }

    @Override
    public boolean dispatchCommand(int command, Object tag) {
      CategoryList f = ((CategoryList) getSupportFragmentManager().findFragmentById(R.id.category_list));
      switch (command) {
      case R.id.CREATE_COMMAND:
        createCat(null);
        return true;
      case R.id.GROUPING_COMMAND:
        SelectGroupingDialogFragment.newInstance(R.id.GROUPING_COMMAND_DO,f.mGrouping.ordinal())
          .show(getSupportFragmentManager(), "SELECT_GROUPING");
      return true;
      case R.id.GROUPING_COMMAND_DO:
        f.setGrouping(Account.Grouping.values()[(Integer)tag]);
        return true;
      }
      return super.dispatchCommand(command, tag);
     }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

          ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
          int type = ExpandableListView
                  .getPackedPositionType(info.packedPosition);

          menu.add(0,EDIT_CAT,0,R.string.menu_edit_cat);
          if (helpVariant.equals(HelpVariant.distribution))
            return;
          // Menu entries relevant only for the group
          if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            if (helpVariant.equals(HelpVariant.select))
              menu.add(0,SELECT_MAIN_CAT,0,R.string.select_parent_category);
            menu.add(0,CREATE_SUB_CAT,0,R.string.menu_create_sub_cat);
          }
          menu.add(0,DELETE_CAT,0,R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        long cat_id = info.id;
/*        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
          Cursor childCursor = (Cursor) mAdapter.getChild(
              ExpandableListView.getPackedPositionGroup(info.packedPosition),
              ExpandableListView.getPackedPositionChild(info.packedPosition)
          );
          cat_id =  childCursor.getLong(childCursor.getColumnIndexOrThrow("_id"));
        } else  {
          cat_id = mGroupCursor.getLong(mGroupIdColumnIndex);
        }
        */
        String label =   ((TextView) info.targetView.findViewById(R.id.label)).getText().toString();

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
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && Category.countSub(cat_id) > 0) {
              Toast.makeText(this,getString(R.string.not_deletable_subcats_exists), Toast.LENGTH_LONG).show();
            } else if (Transaction.countPerCategory(cat_id) > 0 ) {
              Toast.makeText(this,getString(R.string.not_deletable_mapped_transactions), Toast.LENGTH_LONG).show();
            } else if (Template.countPerCategory(cat_id) > 0 ) {
              Toast.makeText(this,getString(R.string.not_deletable_mapped_templates), Toast.LENGTH_LONG).show();
            } else {
              Category.delete(cat_id);
            }
        }
        return false;
      }
/*     (non-Javadoc)
     * return the sub cat to the calling activity
     * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
*/
    @Override
    public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
      //Log.w("SelectCategory","group = " + groupPosition + "; childPosition:" + childPosition);
      Intent intent=new Intent();
      long sub_cat = id;
      String label =  ((TextView) v.findViewById(R.id.label)).getText().toString();
      intent.putExtra("cat_id",sub_cat);
      intent.putExtra("label", label);
      setResult(RESULT_OK,intent);
      finish();
      return true;
    }
    @Override
    public boolean onGroupClick(ExpandableListView parent, View v,
        int groupPosition, long id) {
      long cat_id = id;
      if (Category.countSub(cat_id) > 0)
        return false;
      String label =   ((TextView) v.findViewById(R.id.label)).getText().toString();
      Intent intent=new Intent();
      intent.putExtra("cat_id",cat_id);
      intent.putExtra("label", label);
      setResult(RESULT_OK,intent);
      finish();
      return true;
    }
    /**
     * presents AlertDialog for adding a new category
     * if label is already used, shows an error
     * @param parent_id
     */
    public void createCat(Long parentId) {
      Bundle args = new Bundle();
      int dialogTitle;
      if (parentId != null) {
        args.putLong("parentId", parentId);
        dialogTitle = R.string.menu_create_sub_cat;
      } else
        dialogTitle = R.string.menu_create_main_cat;
      args.putString("dialogTitle", getString(dialogTitle));
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "CREATE_CATEGORY");
    }
    /**
     * presents AlertDialog for editing an existing category
     * if label is already used, shows an error
     * @param label
     * @param cat_id
     */
    public void editCat(String label, Long catId) {
      Bundle args = new Bundle();
      args.putLong("catId", catId);
      args.putString("dialogTitle", getString(R.string.menu_edit_cat));
      args.putString("value",label);
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "EDIT_CATEGORY");
    }

    /**
     * Callback from button
     * @param v
     */
    public void importCats(View v) {
      Intent i = new Intent(this, GrisbiImport.class);
      startActivity(i);
    }

    @Override
    public void onFinishEditDialog(Bundle args) {
      Long catId,parentId;
      boolean success;
      String value = args.getString("result");
      if ((catId = args.getLong("catId")) != 0L) {
        success = Category.update(value,catId) != -1;
      } else {
        if ((parentId = args.getLong("parentId")) == 0L)
            parentId = null;
        success = Category.create(value,parentId) != -1;
      }
      if (!success) {
          Toast.makeText(ManageCategories.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
        }
    }
}