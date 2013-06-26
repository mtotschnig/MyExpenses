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
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class ManageTemplates extends ProtectedFragmentActivity implements
    OnChildClickListener,ContribIFace {

  private static final int DELETE_TEMPLATE = Menu.FIRST;
  private static final int CREATE_INSTANCE_EDIT = Menu.FIRST +1;
  private static final int CREATE_INSTANCE_SAVE = Menu.FIRST +2;
  private static final int NEW_TRANSACTION = Menu.FIRST +4;
  private static final int NEW_TRANSFER = Menu.FIRST +5;
  
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
    } else {
      menu.add(0,NEW_TRANSACTION,0,R.string.menu_create_template_for_transaction);
      menu.add(0,NEW_TRANSFER,0,R.string.menu_create_template_for_transfer);
    }
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
      ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
      int type = ExpandableListView.getPackedPositionType(info.packedPosition);
      long id =  info.id;
      Intent intent;
      if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
        switch(item.getItemId()) {
          case DELETE_TEMPLATE:   
            Template.delete(id);
            break;
          case CREATE_INSTANCE_SAVE:
            if (Transaction.getInstanceFromTemplate(id).save() == null)
              Toast.makeText(getBaseContext(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
            else
              Toast.makeText(getBaseContext(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
            break;
          case CREATE_INSTANCE_EDIT:
            intent = new Intent(this, ExpenseEdit.class);
            intent.putExtra("template_id", id);
            intent.putExtra("instantiate", true);
            startActivity(intent);
            break;
        }
      } else {
        intent = new Intent(this, ExpenseEdit.class);
        intent.putExtra("operationType",
            item.getItemId() == NEW_TRANSACTION ? MyExpenses.TYPE_TRANSACTION : MyExpenses.TYPE_TRANSFER);
        intent.putExtra("newTemplate", true);
        intent.putExtra(DatabaseConstants.KEY_ACCOUNTID,id);
        startActivity(intent);
      }
      return true;
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
  public void contribFeatureCalled(Feature feature) {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra("template_id", mTemplateId);
    i.putExtra("instantiate", false);
    startActivity(i);
  }
  @Override
  public void contribFeatureNotCalled() {
    // TODO Auto-generated method stub
    
  }
  @Override
  public boolean onChildClick(ExpandableListView parent, View v,
      int groupPosition, int childPosition, long id) {
    mTemplateId = id;
    if (MyApplication.getInstance().isContribEnabled) {
      contribFeatureCalled(Feature.EDIT_TEMPLATE);
    } else {
      showContribDialog(Feature.EDIT_TEMPLATE);
    }
    return true;
  }
}
