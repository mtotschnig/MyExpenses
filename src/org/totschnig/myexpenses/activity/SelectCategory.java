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
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
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
public class SelectCategory extends ProtectedFragmentActivity implements
    OnChildClickListener, OnGroupClickListener,EditTextDialogListener  {
    private Button mAddButton;

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

    boolean mManageOnly;
    /**
     * how should categories be sorted, configurable through setting
     */
    

/*    private int monkey_state = 0;

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
      if (keyCode == MyApplication.BACKDOOR_KEY) {
        switch (monkey_state) {
        case 0:
          Intent i = new Intent(this, GrisbiImport.class);
          startActivityForResult(i, ACTIVITY_IMPORT_CATS);
          monkey_state = 1;
          return true;
        case 1:
          createCat(0);
          monkey_state = 2;
          return true;
        case 2:
          getExpandableListView().requestFocus();
          return true;
        }
      }
      return super.onKeyDown(keyCode, event);
    }*/
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getThemeId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_category);
        Intent intent = getIntent();
        String action = intent.getAction();
        mManageOnly = action != null && action.equals("myexpenses.intent.manage.categories");
        if (mManageOnly)
          MyApplication.updateUIWithAppColor(this);
        else
          MyApplication.updateUIWithAccountColor(this);
        setTitle(mManageOnly ? R.string.pref_manage_categories_title : R.string.select_category);
        // Set up our adapter

        mAddButton = (Button) findViewById(R.id.addOperation);
        mAddButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            createCat(null);
          }
        });
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

          ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
          int type = ExpandableListView
                  .getPackedPositionType(info.packedPosition);

          // Menu entries relevant only for the group
          if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            if (!mManageOnly)
              menu.add(0,SELECT_MAIN_CAT,0,R.string.select_parent_category);
            menu.add(0,CREATE_SUB_CAT,0,R.string.menu_create_sub_cat);
          }
          menu.add(0,DELETE_CAT,0,R.string.menu_delete);
          menu.add(0,EDIT_CAT,0,R.string.menu_edit_cat);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
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
      if (mManageOnly)
         return false;
      Intent intent=new Intent();
      long sub_cat = id;
      String label =   ((TextView) v).getText().toString();
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
      if (mManageOnly || Category.countSub(cat_id) > 0)
        return false;
      String label =   ((TextView) v).getText().toString();
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
      if (parentId != null)
        args.putLong("parentId", parentId);
      args.putString("dialogTitle", getString(R.string.create_category));
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
      args.putString("dialogTitle", getString(R.string.edit_category));
      args.putString("value",label);
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "CREATE_CATEGORY");
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
          Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
        }
    }
}