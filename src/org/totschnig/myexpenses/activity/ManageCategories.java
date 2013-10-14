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
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.fragment.DbWriteFragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
        .add(DbWriteFragment.newInstance(), "SAVE_TASK")
        .commit();
    }
    @Override
    public void onPostExecute(Uri result) {
      if (result == null)
        Toast.makeText(ManageCategories.this,getString(R.string.already_defined, mCategory.label), Toast.LENGTH_LONG).show();
    }
    @Override
    public Model getObject() {
      return mCategory;
    }
}