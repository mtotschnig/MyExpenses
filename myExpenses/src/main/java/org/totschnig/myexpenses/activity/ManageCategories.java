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
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

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
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;

import java.util.ArrayList;

import eltos.simpledialogfragment.input.SimpleInputDialog;

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 *
 * @author Michael Totschnig
 */
public class ManageCategories extends ProtectedFragmentActivity implements
    SimpleInputDialog.OnDialogResultListener, SelectMainCategoryDialogFragment.CategorySelectedListener {

  private static final String DIALOG_NEW_CATEGORY = "dialogNewCat";
  private static final String DIALOG_EDIT_CATEGORY = "dialogEditCat";

  public enum HelpVariant {
    manage, distribution, select_mapping, select_filter
  }

  private Category mCategory;
  private GestureDetector mDetector;
  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_THRESHOLD_VELOCITY = 100;
  private CategoryList mListFragment;

  public CategoryList getListFragment() {
    return mListFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Intent intent = getIntent();
    String action = intent.getAction();
    int title = 0;
    switch (action == null ? "" : action) {
      case Intent.ACTION_MAIN:
      case "myexpenses.intent.manage.categories":
        helpVariant = HelpVariant.manage;
        title = R.string.pref_manage_categories_title;
        break;
      case "myexpenses.intent.distribution":
        helpVariant = HelpVariant.distribution;
        //title is set in categories list
        break;
      case "myexpenses.intent.select_filter":
        helpVariant = HelpVariant.select_filter;
        title = R.string.search_category;
        break;
      default:
        helpVariant = HelpVariant.select_mapping;
        title = R.string.select_category;
    }
    setTheme(helpVariant.equals(HelpVariant.distribution) ?
        MyApplication.getThemeId() : MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    if (helpVariant.equals(HelpVariant.distribution)) {
      DisplayMetrics dm = getResources().getDisplayMetrics();

      final int REL_SWIPE_MIN_DISTANCE = (int) (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
      final int REL_SWIPE_MAX_OFF_PATH = (int) (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
      final int REL_SWIPE_THRESHOLD_VELOCITY = (int) (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
      mDetector = new GestureDetector(this,
          new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
              //http://stackoverflow.com/questions/937313/android-basic-gesture-detection
              try {
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
              } catch (Exception e) {
              }
              return false;
            }
          });
    }
    setContentView(R.layout.select_category);
    setupToolbar(true);
    if (title != 0) getSupportActionBar().setTitle(title);
    FragmentManager fm = getSupportFragmentManager();
    mListFragment = ((CategoryList) fm.findFragmentById(R.id.category_list));
    if (helpVariant.equals(HelpVariant.select_mapping) || helpVariant.equals(HelpVariant.manage)) {
      configureFloatingActionButton(R.string.menu_create_main_cat);
    } else {
      findViewById(R.id.CREATE_COMMAND).setVisibility(View.GONE);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    if (helpVariant.equals(HelpVariant.distribution)) {
      inflater.inflate(R.menu.distribution, menu);
      inflater.inflate(R.menu.grouping, menu);


      SwitchCompat typeButton = (SwitchCompat)
          MenuItemCompat.getActionView(menu.findItem(R.id.switchId))
              .findViewById(R.id.TaType);

      typeButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          mListFragment.setType(isChecked);
        }
      });

    } else if (!helpVariant.equals(HelpVariant.select_filter)) {
      inflater.inflate(R.menu.sort, menu);
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
    return super.dispatchCommand(command, tag);
  }

  /**
   * presents AlertDialog for adding a new category
   * if label is already used, shows an error
   *
   * @param parentId
   */
  public void createCat(Long parentId) {
    Bundle args = new Bundle();
    if (parentId != null) {
      args.putLong(DatabaseConstants.KEY_PARENTID, parentId);
    }
    SimpleInputDialog.build()
        .title(parentId == null ? R.string.menu_create_main_cat : R.string.menu_create_sub_cat)
        .cancelable(false)
        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        .hint(R.string.label)
        .pos(R.string.dialog_button_add)
        .neut()
        .extra(args)
        .show(this, DIALOG_NEW_CATEGORY);
  }

  /**
   * presents AlertDialog for editing an existing category
   * if label is already used, shows an error
   *
   * @param label
   * @param catId
   */
  public void editCat(String label, Long catId) {
    Bundle args = new Bundle();
    args.putLong(DatabaseConstants.KEY_ROWID, catId);
    SimpleInputDialog.build()
        .title(R.string.menu_edit_cat)
        .cancelable(false)
        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        .hint(R.string.label)
        .text(label)
        .pos(R.string.menu_save)
        .neut()
        .extra(args)
        .show(this, DIALOG_EDIT_CATEGORY);
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
    if (appDirStatus.success) {
      startTaskExecution(
          TaskExecutionFragment.TASK_EXPORT_CATEGRIES,
          null,
          encoding,
          R.string.menu_categories_export);
    } else {
      Toast.makeText(this,
          appDirStatus.print(this),
          Toast.LENGTH_LONG)
          .show();
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
          extras.getLong(DatabaseConstants.KEY_ROWID),
          extras.getString(SimpleInputDialog.TEXT),
          parentId);
      startDbWriteTask(false);
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
      Toast.makeText(ManageCategories.this,
          getString(R.string.already_defined,
              mCategory != null ? mCategory.label : ""),
          Toast.LENGTH_LONG)
          .show();
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
    String msg;
    if (r.success) {
      switch (taskId) {
        case TaskExecutionFragment.TASK_GRISBI_IMPORT:
          Integer imported = (Integer) r.extra[0];
          if (imported > 0) {
            msg = getString(R.string.import_categories_success, String.valueOf(imported));
          } else {
            msg = getString(R.string.import_categories_none);
          }
          break;
        case TaskExecutionFragment.TASK_EXPORT_CATEGRIES:
          Uri uri = (Uri) r.extra[0];
          msg = getString(r.getMessage(),
              FileUtils.getPath(MyApplication.getInstance(), uri));
          if (PrefKey.PERFORM_SHARE.getBoolean(false)) {
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(uri);
            ShareUtils.share(this, uris,
                PrefKey.SHARE_TARGET.getString("").trim(),
                "text/qif");
          }
          break;
        case TaskExecutionFragment.TASK_MOVE_CATEGORY:
          getListFragment().reset();
        default:
          msg = r.print(this);
      }
    } else {
      msg = r.print(this);
    }
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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