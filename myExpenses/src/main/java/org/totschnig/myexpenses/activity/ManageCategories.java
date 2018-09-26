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

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SelectMainCategoryDialogFragment;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;

import java.util.ArrayList;

import eltos.simpledialogfragment.color.SimpleColorDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

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
  public static final String ACTION_DISTRIBUTION = "DISTRIBUTION";
  public static final String ACTION_SELECT_MAPPING = "SELECT_MAPPING";
  public static final String ACTION_SELECT_FILTER = "SELECT_FILTER";

  public enum HelpVariant {
    manage, distribution, select_mapping, select_filter
  }

  private Category mCategory;
  private GestureDetector mDetector;
  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_THRESHOLD_VELOCITY = 100;
  private CategoryList mListFragment;

  @NonNull
  @Override
  public String getAction() {
    Intent intent = getIntent();
    String action = intent.getAction();
    return action == null ? ACTION_SELECT_MAPPING : action;
  }

  public CategoryList getListFragment() {
    return mListFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    String action = getAction();
    int title = 0;
    setTheme(ACTION_DISTRIBUTION.equals(action) ?
        MyApplication.getThemeId() : MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    switch (action) {
      case Intent.ACTION_MAIN:
      case ACTION_MANAGE:
        setHelpVariant(HelpVariant.manage);
        title = R.string.pref_manage_categories_title;
        break;
      case ACTION_DISTRIBUTION: {
        setHelpVariant(HelpVariant.distribution);
        //title is set in categories list
        DisplayMetrics dm = getResources().getDisplayMetrics();

        final int REL_SWIPE_MIN_DISTANCE = (int) (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
        final int REL_SWIPE_MAX_OFF_PATH = (int) (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
        final int REL_SWIPE_THRESHOLD_VELOCITY = (int) (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
        mDetector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onFling(MotionEvent e1, MotionEvent e2,
                                     float velocityX, float velocityY) {
                if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH)
                  return false;
                if (e1.getX() - e2.getX() > REL_SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
                  mListFragment.forward();
                  return true;
                } else if (e2.getX() - e1.getX() > REL_SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
                  mListFragment.back();
                  return true;
                }
                return false;
              }
            });
      }
      break;
      case ACTION_SELECT_FILTER:
        setHelpVariant(HelpVariant.select_filter);
        title = R.string.search_category;
        break;
      case ACTION_SELECT_MAPPING:
        setHelpVariant(HelpVariant.select_mapping);
        title = R.string.select_category;
    }
    setContentView(R.layout.activity_category);
    setupToolbar(true);
    if (title != 0) getSupportActionBar().setTitle(title);
    FragmentManager fm = getSupportFragmentManager();
    mListFragment = ((CategoryList) fm.findFragmentById(R.id.category_list));
    if (action.equals(ACTION_SELECT_MAPPING) || action.equals(ACTION_MANAGE)) {
      configureFloatingActionButton(R.string.menu_create_main_cat);
    } else {
      findViewById(R.id.CREATE_COMMAND).setVisibility(View.GONE);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    String action = getAction();
    if (action.equals(ACTION_DISTRIBUTION)) {
      inflater.inflate(R.menu.distribution, menu);
      inflater.inflate(R.menu.grouping, menu);


      SwitchCompat typeButton = MenuItemCompat.getActionView(menu.findItem(R.id.switchId))
          .findViewById(R.id.TaType);

      typeButton.setOnCheckedChangeListener((buttonView, isChecked) -> mListFragment.setType(isChecked));

    } else if (!action.equals(ACTION_SELECT_FILTER)) {
      inflater.inflate(R.menu.sort, menu);
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

  /**
   * Callback from button
   *
   * @param v
   */
  public void importCats(View v) {
    importCats();
  }

  private void importCats() {
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceGrisbiImport(false, null, true, false),
            ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(
            0, 0, ProgressDialog.STYLE_HORIZONTAL, false), PROGRESS_TAG)
        .commit();

  }

  private void exportCats(String encoding) {
    Result appDirStatus = AppDirHelper.checkAppDir(this);
    if (appDirStatus.isSuccess()) {
      startTaskExecution(
          TaskExecutionFragment.TASK_EXPORT_CATEGRIES,
          null,
          encoding,
          R.string.menu_categories_export);
    } else {
      showSnackbar(appDirStatus.print(this), Snackbar.LENGTH_LONG);
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    final long id = extras.getLong(KEY_ROWID);
    if ((DIALOG_NEW_CATEGORY.equals(dialogTag) || DIALOG_EDIT_CATEGORY.equals(dialogTag))
        && which == BUTTON_POSITIVE) {
      Long parentId = null;
      if (extras.containsKey(DatabaseConstants.KEY_PARENTID)) {
        parentId = extras.getLong(DatabaseConstants.KEY_PARENTID);
      }
      mCategory = new Category(
          id,
          extras.getString(SimpleInputDialog.TEXT),
          parentId);
      startDbWriteTask(false);
      finishActionMode();
      return true;
    }
    if (EDIT_COLOR_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      startTaskExecution(
          TaskExecutionFragment.TASK_CATEGORY_COLOR,
          new Long[]{id},
          extras.getInt(SimpleColorDialog.COLOR),
          R.string.progress_dialog_saving);
      finishActionMode();
      return true;
    }
    return false;
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

  private void finishActionMode() {
    if (mListFragment != null)
      mListFragment.finishActionMode();
  }

  @Override
  public void onPostExecute(Object result) {
    if (result == null) {
      showSnackbar(getString(R.string.already_defined,
          mCategory != null ? mCategory.getLabel() : ""),
          Snackbar.LENGTH_LONG);
    }
    super.onPostExecute(result);
  }

  //callback from grisbi import task
  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    if (!(result instanceof Result)) {
      return;
    }
    Result r = (Result) result;
    if (r.isSuccess()) {
      switch (taskId) {
        case TaskExecutionFragment.TASK_EXPORT_CATEGRIES:
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
          getListFragment().reset();
          break;
      }
    }
    final String print = r.print0(this);
    if (print != null) {
      showSnackbar(print, Snackbar.LENGTH_LONG);
    }
  }

  @Override
  public Model getObject() {
    return mCategory;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (mDetector != null && !mListFragment.mGrouping.equals(Grouping.NONE) && mDetector.onTouchEvent(event)) {
      return true;
    }
    // Be sure to call the superclass implementation
    return super.dispatchTouchEvent(event);
  }
}