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
import android.net.Uri;
import android.os.Bundle;

import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.Serializable;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class ManageTemplates extends ProtectedFragmentActivity implements
    ConfirmationDialogListener, ContribIFace {

  public static final int NOT_CALLED = -1;
  private long calledFromCalendarWithId = NOT_CALLED;

  @Override
  @VisibleForTesting
  public TemplatesList getCurrentFragment() {
    return mListFragment;
  }

  private TemplatesList mListFragment;

  public enum HelpVariant {
    templates, plans, planner
  }

  public long getCalledFromCalendarWithId() {
    return calledFromCalendarWithId;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHelpVariant(HelpVariant.templates);
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
        CrashHandler.report(e);
      }
    }
    configureFloatingActionButton(R.string.menu_create_template);

    mListFragment = ((TemplatesList) getSupportFragmentManager().findFragmentById(R.id.templates_list));
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    Intent i;
    if (command == R.id.CREATE_COMMAND) {
      i = new Intent(this, ExpenseEdit.class);
      i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true);
      startActivity(i);
      return true;
    } else if (command == R.id.DELETE_COMMAND_DO) {
      finishActionMode();
      startTaskExecution(
          TaskExecutionFragment.TASK_DELETE_TEMPLATES,
          (Long[]) tag,
          CALENDAR.hasPermission(this),
          R.string.progress_dialog_deleting);
      return true;
    } else if (command == R.id.CANCEL_CALLBACK_COMMAND) {
      finishActionMode();
      return true;
    }
    return false;
  }

  @Override
  protected void doHome() {
    Intent upIntent = NavUtils.getParentActivityIntent(this);
    if (isTaskRoot()) {
      // create new task
      TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent)
          .startActivities();
    } else {
      // Stay in same task
      NavUtils.navigateUpTo(this, upIntent);
    }
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    if (taskId == TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE) {
      Integer successCount = (Integer) o;
      String msg = successCount == 0 ? getString(R.string.save_transaction_error) :
          getResources().getQuantityString(R.plurals.save_transaction_from_template_success, successCount, successCount);
      mListFragment.showSnackbar(msg);
    }
  }

  public void finishActionMode() {
    mListFragment.finishActionMode();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
      if (PermissionHelper.allGranted(grantResults)) {
        mListFragment.loadData();
      }
    }
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (feature.equals(ContribFeature.SPLIT_TRANSACTION)) {
      if (tag instanceof Long) {
        mListFragment.dispatchCreateInstanceEditDo((Long) tag);
      } else if (tag instanceof Long[]) {
        mListFragment.dispatchCreateInstanceSaveDo((Long[]) tag, null);
      }
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {

  }
}
