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
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.PermissionHelper;

import java.io.Serializable;
import java.util.List;

public class ManageTemplates extends ProtectedFragmentActivity implements
    ConfirmationDialogListener, ContribIFace {

  public static final int NOT_CALLED = -1;
  private long calledFromCalendarWithId = NOT_CALLED;

  private TemplatesList mListFragment;

  public enum HelpVariant {
    templates, plans
  }

  public long getCalledFromCalendarWithId() {
    return calledFromCalendarWithId;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdEditDialog());
    helpVariant = HelpVariant.templates;
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_templates);
    setupToolbar(true);
    setTitle(getString(R.string.menu_manage_plans));


    String uriString = getIntent().getStringExtra(Events.CUSTOM_APP_URI);
    if (uriString != null) {
      List<String> uriPath = Uri.parse(uriString).getPathSegments();
      try {
        calledFromCalendarWithId = Long.parseLong(uriPath.get(uriPath.size() - 1)); //legacy uri had account_id/template_id
        if (calledFromCalendarWithId == 0) { //ignore 0 that were introduced by legacy bug
          calledFromCalendarWithId = NOT_CALLED;
        }
      } catch (Exception e) {
        AcraHelper.report(e);
      }
    }
    configureFloatingActionButton(R.string.menu_create_template);

    mListFragment = ((TemplatesList) getSupportFragmentManager().findFragmentById(R.id.templates_list));
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch (command) {
      case R.id.CREATE_COMMAND:
        i = new Intent(this, ExpenseEdit.class);
        i.putExtra(MyApplication.KEY_OPERATION_TYPE, Transaction.TYPE_TRANSACTION);
        i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true);
        startActivity(i);
        return true;
      case R.id.DELETE_COMMAND_DO:
        finishActionMode();
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_TEMPLATES,
            (Long[]) tag,
            null,
            R.string.progress_dialog_deleting);
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
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE:
        Integer successCount = (Integer) o;
        String msg = successCount == 0 ? getString(R.string.save_transaction_error) :
            getResources().getQuantityString(R.plurals.save_transaction_from_template_success, successCount, successCount);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
  }

  public void finishActionMode() {
    mListFragment.finishActionMode();
  }

  @Override
  public void onPositive(Bundle args) {
    long id = args.getLong(DatabaseConstants.KEY_ROWID);
    boolean isSplit = args.getBoolean(TemplatesList.KEY_IS_SPLIT);
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    switch (command) {
      case R.id.CREATE_INSTANCE_SAVE_COMMAND:
        PrefKey.TEMPLATE_CLICK_DEFAULT.putString("SAVE");
        if (isSplit) {
          mListFragment.requestSplitTransaction(new Long[]{id});
        } else {
          mListFragment.dispatchCreateInstanceSaveDo(new Long[]{id});
        }
        break;
    }
  }

  @Override
  public void onNegative(Bundle args) {
    long id = args.getLong(DatabaseConstants.KEY_ROWID);
    boolean isSplit = args.getBoolean(TemplatesList.KEY_IS_SPLIT);
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE);
    switch (command) {
      case R.id.CREATE_INSTANCE_EDIT_COMMAND:
        PrefKey.TEMPLATE_CLICK_DEFAULT.putString("EDIT");
        if (isSplit) {
          mListFragment.requestSplitTransaction(id);
        } else {
          mListFragment.dispatchCreateInstanceEditDo(id);
        }
        break;
    }
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mListFragment.refresh();
        }
      }
    }
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (feature.equals(ContribFeature.SPLIT_TRANSACTION)) {
      if (tag instanceof Long) {
        mListFragment.dispatchCreateInstanceEditDo((Long) tag);
      } else if (tag instanceof Long[]) {
        mListFragment.dispatchCreateInstanceSaveDo((Long[]) tag);
      }
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {

  }
}
