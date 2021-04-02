package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.text.InputType;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.AbstractCategoryList;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.viewmodel.data.Category;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import eltos.simpledialogfragment.color.SimpleColorDialog;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SelectColorField;
import eltos.simpledialogfragment.form.SelectIconField;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class CategoryActivity<T extends AbstractCategoryList<?>> extends ProtectedFragmentActivity implements
    SimpleFormDialog.OnDialogResultListener {
  protected static final String DIALOG_NEW_CATEGORY = "dialogNewCat";
  protected static final String DIALOG_EDIT_CATEGORY = "dialogEditCat";
  protected T mListFragment;

  @NonNull
  abstract public String getAction();
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
   */
  public void createCat(Long parentId) {
    Bundle args = new Bundle();
    if (parentId != null) {
      args.putLong(DatabaseConstants.KEY_PARENTID, parentId);
    }
    SimpleFormDialog.build()
        .title(parentId == null ? R.string.menu_create_main_cat : R.string.menu_create_sub_cat)
        .cancelable(false)
        .fields(buildLabelField(null), buildIconField(null))
        .pos(R.string.dialog_button_add)
        .neut()
        .extra(args)
        .show(this, DIALOG_NEW_CATEGORY);
  }

  private Input buildLabelField(String text) {
    return Input.plain(KEY_LABEL).required().hint(R.string.label).text(text)
        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
  }

  private SelectIconField buildIconField(String preset) {
    return SelectIconField.picker(KEY_ICON).icons(BuildConfig.CATEGORY_ICONS).preset(preset).label(R.string.icon);
  }

  /**
   * presents AlertDialog for editing an existing category
   */
  public void editCat(Category category) {
    Bundle args = new Bundle();
    args.putLong(KEY_ROWID, category.getId());
    final Input labelInput = buildLabelField(category.getLabel());
    final SelectIconField iconField = buildIconField(category.getIcon());
    final FormElement<? ,?>[] formElements = category.getParentId() == null ?
        new FormElement[]{labelInput, SelectColorField.picker(KEY_COLOR).label(R.string.color).color(category.getColor()), iconField} :
        new FormElement[]{labelInput, iconField};
    SimpleFormDialog.build()
        .title(R.string.menu_edit_cat)
        .cancelable(false)
        .fields(formElements)
        .pos(R.string.menu_save)
        .neut()
        .extra(args)
        .show(this, DIALOG_EDIT_CATEGORY);
  }

  public void editCategoryColor(Category c) {
    Bundle args = new Bundle();
    args.putLong(KEY_ROWID, c.getId());
    SimpleColorDialog.build()
        .allowCustom(true)
        .cancelable(false)
        .neut()
        .extra(args)
        .colorPreset(c.getColor())
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

  @Override
  public Fragment getCurrentFragment() {
    return mListFragment;
  }
}
