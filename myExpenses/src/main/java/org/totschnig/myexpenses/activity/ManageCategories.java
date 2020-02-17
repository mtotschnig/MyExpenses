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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.select.SelectMainCategoryDialogFragment;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 *
 * @author Michael Totschnig
 */
public class ManageCategories extends CategoryActivity implements
    SimpleInputDialog.OnDialogResultListener, SelectMainCategoryDialogFragment.CategorySelectedListener {

  public static final String ACTION_MANAGE = "MANAGE";
  public static final String ACTION_SELECT_MAPPING = "SELECT_MAPPING";
  public static final String ACTION_SELECT_FILTER = "SELECT_FILTER";

  public enum HelpVariant {
    manage, select_mapping, select_filter
  }

  private Category mCategory;

  @NonNull
  @Override
  public String getAction() {
    Intent intent = getIntent();
    String action = intent.getAction();
    return action == null ? ACTION_SELECT_MAPPING : action;
  }

  @Override
  protected int getContentView() {
    return R.layout.activity_category;
  }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    String action = getAction();
    int title = 0;
    setTheme(getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    switch (action) {
      case Intent.ACTION_MAIN:
      case ACTION_MANAGE:
        setHelpVariant(HelpVariant.manage);
        title = R.string.pref_manage_categories_title;
        break;
      case ACTION_SELECT_FILTER:
        setHelpVariant(HelpVariant.select_filter);
        title = R.string.search_category;
        break;
      case ACTION_SELECT_MAPPING:
        setHelpVariant(HelpVariant.select_mapping);
        title = R.string.select_category;
    }
    if (title != 0) getSupportActionBar().setTitle(title);
    if (action.equals(ACTION_SELECT_MAPPING) || action.equals(ACTION_MANAGE)) {
      configureFloatingActionButton(R.string.menu_create_main_cat);
    } else {
      findViewById(R.id.CREATE_COMMAND).setVisibility(View.GONE);
    }
  }

/*  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem item = menu.findItem(R.id.SETUP_CATEGORIES_DEFAULT_COMMAND);
    if (item != null) {
      Utils.menuItemSetEnabledAndVisible(item, getResources().getBoolean(R.bool.has_localized_categories));
    }
    return super.onPrepareOptionsMenu(menu);
  }*/

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    String action = getAction();
    if (!action.equals(ACTION_SELECT_FILTER)) {
      inflater.inflate(R.menu.categories, menu);
    }
    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    switch (command) {
      case R.id.CREATE_COMMAND:
        createCat(null);
        return true;
      case R.id.DELETE_COMMAND_DO:
        finishActionMode();
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_CATEGORY,
            (Long[]) tag,
            null,
            R.string.progress_dialog_deleting);
        return true;
      case R.id.CANCEL_CALLBACK_COMMAND:
        finishActionMode();
        return true;
      case R.id.SETUP_CATEGORIES_DEFAULT_COMMAND:
        importCats();
        return true;
      case R.id.EXPORT_CATEGORIES_COMMAND_ISO88591:
        exportCats("ISO-8859-1");
        return true;
      case R.id.EXPORT_CATEGORIES_COMMAND_UTF8:
        exportCats("UTF-8");
        return true;
    }
    return false;
  }

  private void importCats() {
    startTaskExecution(
        TaskExecutionFragment.TASK_SETUP_CATEGORIES,
        null,
        null,
        R.string.menu_categories_setup_default);
  }

  private void exportCats(String encoding) {
    Result appDirStatus = AppDirHelper.checkAppDir(this);
    if (appDirStatus.isSuccess()) {
      startTaskExecution(
          TaskExecutionFragment.TASK_EXPORT_CATEGORIES,
          null,
          encoding,
          R.string.menu_categories_export);
    } else {
      showSnackbar(appDirStatus.print(this), Snackbar.LENGTH_LONG);
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if ((DIALOG_NEW_CATEGORY.equals(dialogTag) || DIALOG_EDIT_CATEGORY.equals(dialogTag))
        && which == BUTTON_POSITIVE) {
      Long parentId = null;
      if (extras.containsKey(DatabaseConstants.KEY_PARENTID)) {
        parentId = extras.getLong(DatabaseConstants.KEY_PARENTID);
      }
      mCategory = new Category(
          extras.getLong(KEY_ROWID),
          extras.getString(KEY_LABEL),
          parentId, extras.getInt(KEY_COLOR), extras.getString(KEY_ICON));
      startDbWriteTask();
      finishActionMode();
      return true;
    }
    return super.onResult(dialogTag, which, extras);
  }

  @Override
  public void onCategorySelected(Bundle args) {
    finishActionMode();
    final long target = args.getLong(SelectMainCategoryDialogFragment.KEY_RESULT);
    startTaskExecution(
        TaskExecutionFragment.TASK_MOVE_CATEGORY,
        ArrayUtils.toObject(args.getLongArray(TaskExecutionFragment.KEY_OBJECT_IDS)),
        target == 0L ? null : target,
        R.string.progress_dialog_saving);
  }

  @Override
  public void onPostExecute(Uri result) {
    if (result == null) {
      showSnackbar(getString(R.string.already_defined,
          mCategory != null ? mCategory.getLabel() : ""),
          Snackbar.LENGTH_LONG);
    }
    super.onPostExecute(result);
  }

  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    if (!(result instanceof Result)) {
      return;
    }
    Result r = (Result) result;
    if (r.isSuccess()) {
      switch (taskId) {
        case TaskExecutionFragment.TASK_EXPORT_CATEGORIES:
          Result<Uri> uriResult = (Result<Uri>) result;
          Uri uri = uriResult.getExtra();
          if (PrefKey.PERFORM_SHARE.getBoolean(false)) {
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(uri);
            Result shareResult = ShareUtils.share(this, uris,
                PrefKey.SHARE_TARGET.getString("").trim(),
                "text/qif");
            if (!shareResult.isSuccess()) {
              showSnackbar(shareResult.print(this), Snackbar.LENGTH_LONG);
            }
          }
          break;
        case TaskExecutionFragment.TASK_MOVE_CATEGORY:
          mListFragment.reset();
          break;
        case TaskExecutionFragment.TASK_DELETE_CATEGORY: {
          showSnackbar(r.print(this), Snackbar.LENGTH_LONG);
        }
      }
    }

    if (taskId != TaskExecutionFragment.TASK_DELETE_CATEGORY /*handled in super*/) {
      final String print = r.print0(this);
      if (print != null) {
        showSnackbar(print, Snackbar.LENGTH_LONG);
      }
    }
  }

  @Override
  public Model getObject() {
    return mCategory;
  }

}