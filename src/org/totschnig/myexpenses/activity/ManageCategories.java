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
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 * @author Michael Totschnig
 *
 */
public class ManageCategories extends ProtectedFragmentActivity implements
    EditTextDialogListener,DbWriteFragment.TaskCallbacks {
    
    public enum HelpVariant {
      manage,distribution,select
    }

    private Category mCategory;
    private GestureDetector mDetector;
    private static final int SWIPE_MIN_DISTANCE = 50;
    private CategoryList mListFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      Intent intent = getIntent();
      String action = intent.getAction();
      if (action != null && action.equals("myexpenses.intent.manage.categories")) {
        helpVariant = HelpVariant.manage;
        setTitle(R.string.pref_manage_categories_title);
      } else if (action != null && action.equals("myexpenses.intent.distribution")) {
        helpVariant = HelpVariant.distribution;
        //title is set in categories list
        mDetector = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
              if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
                //Toast.makeText(ManageCategories.this,"Right to left", Toast.LENGTH_LONG).show();
                mListFragment.forward();
                return true;
              }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
                //Toast.makeText(ManageCategories.this,"Left to right", Toast.LENGTH_LONG).show();
                mListFragment.back();
                return true;
              }
              return false;
          }
        });
      } else {
        helpVariant = HelpVariant.select;
        setTitle(R.string.select_category);
      }
      setContentView(R.layout.select_category);
      mListFragment = ((CategoryList) getSupportFragmentManager().findFragmentById(R.id.category_list));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
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
      switch (command) {
      case R.id.CREATE_COMMAND:
        createCat(null);
        return true;
      case R.id.GROUPING_COMMAND:
        SelectGroupingDialogFragment.newInstance(R.id.GROUPING_COMMAND_DO,mListFragment.mGrouping.ordinal())
          .show(getSupportFragmentManager(), "SELECT_GROUPING");
        return true;
      case R.id.GROUPING_COMMAND_DO:
        mListFragment.setGrouping(Account.Grouping.values()[(Integer)tag]);
        return true;
      case R.id.DELETE_COMMAND_DO:
        getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_CATEGORY,(Long[])tag, null), "ASYNC_TASK")
        .commit();
      }
      return super.dispatchCommand(command, tag);
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
      Long parentId;
      if ((parentId = args.getLong("parentId")) == 0L)
        parentId = null;
      mCategory = new Category(args.getLong("catId"), args.getString("result"), parentId);
      getSupportFragmentManager().beginTransaction()
        .add(DbWriteFragment.newInstance(false), "SAVE_TASK")
        .commit();
    }
    @Override
    public void onPostExecute(Object result) {
      if (result == null) {
        Toast.makeText(ManageCategories.this,
            getString(R.string.already_defined,
                mCategory != null ? mCategory.label : ""),
            Toast.LENGTH_LONG)
          .show();
      }
      super.onPostExecute(result);
    }
    @Override
    public Model getObject() {
      return mCategory;
    }
    @Override 
    public boolean dispatchTouchEvent(MotionEvent event){
       if (mDetector != null && !mListFragment.mGrouping.equals(Grouping.NONE))
         mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.dispatchTouchEvent(event);
    }

}