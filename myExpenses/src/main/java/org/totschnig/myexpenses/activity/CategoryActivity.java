package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.InputType;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.viewmodel.data.Category;

import eltos.simpledialogfragment.color.SimpleColorDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class CategoryActivity<T extends CategoryList> extends ProtectedFragmentActivity implements
    SimpleInputDialog.OnDialogResultListener {
  protected static final String DIALOG_NEW_CATEGORY = "dialogNewCat";
  protected static final String DIALOG_EDIT_CATEGORY = "dialogEditCat";
  protected T mListFragment;

  @NonNull abstract public String getAction();
  protected abstract int getContentView();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentManager fm = getSupportFragmentManager();
    setContentView(getContentView());
    setupToolbar(true);
    mListFragment = ((T) fm.findFragmentById(R.id.category_list));
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
    args.putLong(KEY_ROWID, catId);
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

  public void editCategoryColor(Category c) {
    Bundle args = new Bundle();
    args.putLong(KEY_ROWID, c.id);
    SimpleColorDialog.build()
        .allowCustom(true)
        .cancelable(false)
        .neut()
        .extra(args)
        .colorPreset(c.color)
        .show(this, EDIT_COLOR_DIALOG);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (EDIT_COLOR_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      startTaskExecution(
          TaskExecutionFragment.TASK_CATEGORY_COLOR,
          new Long[]{extras.getLong(KEY_ROWID)},
          extras.getInt(SimpleColorDialog.COLOR),
          R.string.progress_dialog_saving);
      finishActionMode();
      return true;
    }
    return false;
  }

  protected void finishActionMode() {
    if (mListFragment != null)
      mListFragment.finishActionMode();
  }
}
