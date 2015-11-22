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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.PlanList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Utils;

import java.util.List;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;

public class ManageTemplates extends TabbedActivity implements
    ConfirmationDialogListener {
  public enum HelpVariant {
    templates,plans
  }

  public long calledFromCalendarWithId = 0;
  int mCurrentPosition = 0;
  
  private int monkey_state = 0;

  @Override
  public boolean onKeyUp (int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_CAMERA) {
      switch (monkey_state) {
      case 0:
        ((PlanList) getSupportFragmentManager().findFragmentByTag(
            mSectionsPagerAdapter.getFragmentName(1)))
          .listFocus();
        return true;
      }
    }
    return super.onKeyUp(keyCode, event);
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(getString(R.string.menu_manage_plans));

    mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        finishActionMode();
        mCurrentPosition = position;
        helpVariant = position == 0 ? HelpVariant.templates : HelpVariant.plans;
      }
    });

    String uriString = getIntent().getStringExtra(Events.CUSTOM_APP_URI);
    if (uriString != null) {
      List <String> uriPath = Uri.parse(uriString).getPathSegments();
      try {
        calledFromCalendarWithId = Long.parseLong(uriPath.get(2));
        mViewPager.setCurrentItem(1);
      } catch (Exception e) {
        Utils.reportToAcra(e);
      }
    }
    helpVariant = HelpVariant.templates;
  }

  @Override
  protected void setupTabs(Bundle savedInstanceState) {
    mSectionsPagerAdapter.addFragment(new TemplatesList(), getString(R.string.menu_manage_plans_tab_templates));
    mSectionsPagerAdapter.addFragment(new PlanList(), getString(R.string.menu_manage_plans_tab_plans));
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch(command) {
    case R.id.CREATE_COMMAND:
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSACTION);
      i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true);
      startActivity(i);
      return true;
    case R.id.DELETE_COMMAND_DO:
      finishActionMode();
      startTaskExecution(
          TaskExecutionFragment.TASK_DELETE_TEMPLATES,
          (Long[])tag,
          null,
          R.string.progress_dialog_deleting);
      return true;
    case R.id.EDIT_COMMAND:
      finishActionMode();
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra(DatabaseConstants.KEY_TEMPLATEID,((Long)tag));
      //TODO check what to do on Result
      startActivityForResult(i, EDIT_TRANSACTION_REQUEST);
      return true;
    case R.id.CANCEL_CALLBACK_COMMAND:
      finishActionMode();
      return true;
    case android.R.id.home:
      Intent upIntent = NavUtils.getParentActivityIntent(this);
      if (shouldUpRecreateTask(this)) {
          // This activity is NOT part of this app's task, so create a new task
          // when navigating up, with a synthesized back stack.
          TaskStackBuilder.create(this)
                  // Add all of this activity's parents to the back stack
                  .addNextIntentWithParentStack(upIntent)
                  // Navigate up to the closest parent
                  .startActivities();
      } else {
          // This activity is part of this app's task, so simply
          // navigate up to the logical parent activity.
          NavUtils.navigateUpTo(this, upIntent);
      }
      return true;
    case R.id.CREATE_INSTANCE_SAVE_COMMAND:
      startTaskExecution(
          TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE,
          (Long[])tag,
          null,
          0);
      return true;
    case R.id.CREATE_INSTANCE_EDIT_COMMAND:
      Intent intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra(KEY_TEMPLATEID, (Long) tag);
      intent.putExtra(KEY_INSTANCEID, -1L);
      startActivity(intent);
      return true;
    }
    return super.dispatchCommand(command, tag);
   }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch(taskId) {
    case TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE:
      Integer successCount = (Integer) o;
      String msg = successCount == 0 ?  getString(R.string.save_transaction_error) :
        getResources().getQuantityString(R.plurals.save_transaction_from_template_success, successCount, successCount);
      Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }
    PlanList pl = (PlanList) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(1));
    pl.refresh();
  }

  public void finishActionMode() {
    ContextualActionBarFragment f =
    ((ContextualActionBarFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(mCurrentPosition)));
    if (f!=null) {
      f.finishActionMode();
    }
  }
  @Override
  public void onPositive(Bundle args) {
    long id = args.getLong(DatabaseConstants.KEY_ROWID);
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    switch(command) {
    case R.id.CREATE_INSTANCE_SAVE_COMMAND:
      MyApplication.PrefKey.TEMPLATE_CLICK_DEFAULT.putString("SAVE");
      dispatchCommand(
          command,
          new Long[] {id});
      break;
    }
  }
  @Override
  public void onNegative(Bundle args) {
    long id = args.getLong(DatabaseConstants.KEY_ROWID);
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE);
    switch(command) {
    case R.id.CREATE_INSTANCE_EDIT_COMMAND:
      MyApplication.PrefKey.TEMPLATE_CLICK_DEFAULT.putString("EDIT");
      dispatchCommand(
          command,
          id);
      break;
    }
  }
  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  public void requestPermission(View v) {
    requestCalendarPermission();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case ProtectionDelegate.PERMISSIONS_REQUEST_WRITE_CALENDAR: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          PlanList pl = (PlanList) getSupportFragmentManager().findFragmentByTag(
              mSectionsPagerAdapter.getFragmentName(1));
          pl.setupList();
        }
      }
    }
  }
}
