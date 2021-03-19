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
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.UiUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;

/**
 * The ViewHolder class for {@link SelectColorField}
 */

class SelectColorViewHolder extends FormElementViewHolder<SelectColorField> implements SimpleDialog.OnDialogResultListener {

  private static final String SAVED_COLOR = "color";
  private static final String COLOR_DIALOG_TAG = "colorPickerDialogTag";

  @BindView(R.id.label)
  TextView label;
  @BindView(R.id.ColorIndicator)
  AppCompatButton colorView;
  @BindView(R.id.ColorEdit)
  ImageView colorEdit;

  int color = ColorField.NONE;


  public SelectColorViewHolder(SelectColorField field) {
    super(field);
  }

  @Override
  protected int getContentViewLayout() {
    return R.layout.simpledialogfragment_form_item_color2;
  }

  @Override
  protected void setUpView(View view, final Context context, Bundle savedInstanceState,
                           final SimpleFormDialog.DialogActions actions) {

    ButterKnife.bind(this, view);

    // Label
    String text = field.getText(context);
    label.setText(text);

    color = savedInstanceState != null ? savedInstanceState.getInt(SAVED_COLOR) : field.getInitialColor(context);
    colorView.setBackgroundColor(color);

    colorView.setOnClickListener(v -> onClick(context, actions));
    colorEdit.setOnClickListener(v -> onClick(context, actions));

  }

  @Override
  protected void saveState(Bundle outState) {
    outState.putInt(SAVED_COLOR, color);
  }

  @Override
  protected void putResults(Bundle results, String key) {
    results.putInt(key, color);
  }

  @Override
  protected boolean focus(final SimpleFormDialog.FocusActions actions) {
    actions.hideKeyboard();
    //colorView.performClick();
    return colorView.requestFocus();

  }

  @Override
  protected boolean posButtonEnabled(Context context) {
    return !field.required || color != ColorField.NONE;
  }

  @Override
  protected boolean validate(Context context) {
    boolean valid = posButtonEnabled(context);
    if (valid) {
      label.setTextColor(UiUtils.getColor(label.getContext(), R.attr.colorOnSurface));
    } else {
      //noinspection deprecation
      label.setTextColor(context.getResources().getColor(R.color.simpledialogfragment_error_color));
    }
    return valid;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if ((COLOR_DIALOG_TAG + field.resultKey).equals(dialogTag)) {
      if (which == BUTTON_POSITIVE && colorView != null) {
        color = extras.getInt(SimpleColorDialog.COLOR, color);
        colorView.setBackgroundColor(color);
      }
      return true;
    }
    return false;
  }

  private void onClick(final Context context,
                       final SimpleFormDialog.DialogActions actions) {
    actions.showDialog(SimpleColorDialog.build()
            .title(field.getText(context))
            .colors(field.colors)
            .allowCustom(field.allowCustom)
            .colorPreset(color)
            .neut(),
        COLOR_DIALOG_TAG + field.resultKey);
  }
}
