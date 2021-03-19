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

package eltos.simpledialogfragment.form;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.SimpleIconDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;

/**
 * The ViewHolder class for {@link ColorField}
 * <p>
 * This class is used to create a Color Box and to maintain it's functionality
 * <p>
 * Created by eltos on 06.07.2018.
 */

class IconViewHolder extends FormElementViewHolder<SelectIconField> implements SimpleDialog.OnDialogResultListener {

  protected static final String SAVED_ICON = "color";
  private static final String ICON_PICKER_DIALOG_TAG = "iconPickerDialogTag";
  @BindView(R.id.label)
  TextView label;
  @BindView(R.id.icon)
  ImageView icon;
  @BindView(R.id.select)
  Button select;

  String selected;

  IconViewHolder(SelectIconField field) {
    super(field);
  }

  @Override
  protected int getContentViewLayout() {
    return R.layout.simpledialogfragment_form_item_icon;
  }

  @Override
  protected void setUpView(View view, final Context context, Bundle savedInstanceState,
                           final SimpleFormDialog.DialogActions actions) {

    ButterKnife.bind(this, view);

    label.setText(field.getText(context));
    selected = savedInstanceState != null ? savedInstanceState.getString(SAVED_ICON) : field.preset;
    select.setOnClickListener(v -> onClick(actions));
    icon.setOnClickListener(v -> onClick(actions));
    if (selected != null) {
      updateIcon(selected, context.getResources().getIdentifier(selected, "drawable", context.getPackageName()));
    }
    updateViewVisibility();
  }

  private void updateIcon(String label, int resId) {
    icon.setContentDescription(label);
    icon.setImageResource(resId);
  }

  @Override
  protected void saveState(Bundle outState) {
    outState.putString(SAVED_ICON, selected);
  }

  @Override
  protected void putResults(Bundle results, String key) {
    results.putString(key, selected);
  }

  @Override
  protected boolean focus(final SimpleFormDialog.FocusActions actions) {
    return label.requestFocus();
  }

  @Override
  protected boolean posButtonEnabled(Context context) {
    return true;
  }

  @Override
  protected boolean validate(Context context) {
    return true;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if ((ICON_PICKER_DIALOG_TAG + field.resultKey).equals(dialogTag)) {
      if (which == BUTTON_POSITIVE) {
        selected = extras.getString(KEY_ICON);
        updateIcon(selected, extras.getInt(SimpleIconDialog.KEY_RESID));
        updateViewVisibility();
      } else if (which == BUTTON_NEGATIVE) {
        selected = null;
        updateViewVisibility();
      }
      return true;
    }
    return false;
  }

  private void updateViewVisibility() {
    final boolean hasSelection = selected != null;
    icon.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
    select.setVisibility(hasSelection ? View.GONE : View.VISIBLE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      label.setLabelFor(hasSelection ? R.id.icon : R.id.select);
    }
  }

  private void onClick(final SimpleFormDialog.DialogActions actions) {
    final SimpleIconDialog iconDialog = SimpleIconDialog.build()
        .icons(field.iconArray)
        .neut();
    if(selected != null) {
      iconDialog.neg(R.string.menu_remove);
    }
    actions.showDialog(iconDialog,
        ICON_PICKER_DIALOG_TAG + field.resultKey);
  }
}
