package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.viewmodel.data.Category;

import eltos.simpledialogfragment.color.SimpleColorDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class CategoryActivity extends ProtectedFragmentActivity {
  protected static final String DIALOG_NEW_CATEGORY = "dialogNewCat";
  protected static final String DIALOG_EDIT_CATEGORY = "dialogEditCat";

  @NonNull abstract public String getAction();

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
}
