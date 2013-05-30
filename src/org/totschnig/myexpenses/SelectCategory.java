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

import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;

import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
public class SelectCategory extends ProtectedFragmentActivity implements OnChildClickListener, OnGroupClickListener  {
    private Button mAddButton;


    public static final int ACTIVITY_IMPORT_CATS=1;
    static final int CAT_CREATE_DIALOG_ID = 1;
    static final int CAT_EDIT_DIALOG_ID = 2;
    static final int CAT_DIALOG_LABEL_EDIT_ID = 1;
    private long mCatCreateDialogParentId;
    private long mCatEditDialogCatId;
    private String mCatDialogLabel;

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
            createCat(0);
          }
        });
    }
    @Override
    protected Dialog onCreateDialog(final int id) {
      if (id == CAT_EDIT_DIALOG_ID || id == CAT_CREATE_DIALOG_ID) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(id == CAT_CREATE_DIALOG_ID ?
            R.string.create_category :
            R.string.edit_category);
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        //only if the editText has an id, is its value restored after orientation change
        input.setId(CAT_DIALOG_LABEL_EDIT_ID);
        input.setSingleLine();
        Utils.setBackgroundFilter(input, getResources().getColor(R.color.theme_dark_button_color));
        alert.setView(input);
        alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            String value = input.getText().toString();
            if (!value.equals("")) {
              long cat_id = (id == CAT_CREATE_DIALOG_ID ?
                  Category.create(value,mCatCreateDialogParentId) :
                  Category.update(value,mCatEditDialogCatId));
              if (cat_id == -1) {
                Toast.makeText(SelectCategory.this,getString(R.string.category_already_defined, value), Toast.LENGTH_LONG).show();
              }
            } else {
              Toast.makeText(getBaseContext(),getString(R.string.no_title_given), Toast.LENGTH_LONG).show();
            }
          }
        });
        alert.setNegativeButton(android.R.string.no, null);
        return alert.create();
      }
      return null;
    }
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
      if (id == CAT_EDIT_DIALOG_ID || id == CAT_CREATE_DIALOG_ID) {
        EditText input = (EditText) dialog.findViewById(CAT_DIALOG_LABEL_EDIT_ID);
        input.setText(mCatDialogLabel);
      }
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
            } else if (Transaction.countPerCat(cat_id) > 0 ) {
              Toast.makeText(this,getString(R.string.not_deletable_mapped_transactions), Toast.LENGTH_LONG).show();
            } else if (Template.countPerCat(cat_id) > 0 ) {
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
    public void createCat(final long parent_id) {
      mCatDialogLabel = "";
      mCatCreateDialogParentId = parent_id;
      showDialog(CAT_CREATE_DIALOG_ID);
    }
    /**
     * presents AlertDialog for editing an existing category
     * if label is already used, shows an error
     * @param label
     * @param cat_id
     */
    public void editCat(String label, final long cat_id) {
      mCatDialogLabel = label;
      mCatEditDialogCatId = cat_id;
      showDialog(CAT_EDIT_DIALOG_ID);
    }

    /**
     * Callback from button
     * @param v
     */
    public void importCats(View v) {
      Intent i = new Intent(this, GrisbiImport.class);
      startActivityForResult(i, ACTIVITY_IMPORT_CATS);
    }

    //safeguard for orientation change during dialog
    @Override
    protected void onSaveInstanceState(Bundle outState) {
     super.onSaveInstanceState(outState);
     outState.putLong("CatCreateDialogParentId", mCatCreateDialogParentId);
     outState.putLong("CatEditDialogCatId", mCatEditDialogCatId);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
     super.onRestoreInstanceState(savedInstanceState);
     mCatCreateDialogParentId = savedInstanceState.getLong("CatCreateDialogParentId");
     mCatEditDialogCatId = savedInstanceState.getLong("CatEditDialogCatId");
    }
}