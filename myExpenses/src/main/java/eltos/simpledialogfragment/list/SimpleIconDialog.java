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

package eltos.simpledialogfragment.list;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.UiUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;


/**
 * A dialog that let's the user select an icon
 */
public class SimpleIconDialog extends SimpleRVDialog<SimpleIconDialog> {

  public static final String TAG = "SimpleIconDialog.";

  public static final String ICONS = TAG + "icons";
  public static final String KEY_RESID = "resID";

  public static SimpleIconDialog build() {
    return new SimpleIconDialog();
  }

  /**
   * Sets the icons to choose from
   * @param icons
   */
  public SimpleIconDialog icons(String[] icons) {
    getArguments().putStringArray(ICONS, icons);
    return this;
  }

  public SimpleIconDialog() {
    gridColumnWidth(R.dimen.category_icon_selector_column_width);
  }

  @Override
  protected ClickableAdapter onCreateAdapter() {
    return new IconAdapter(getArguments().getStringArray(ICONS));
  }

  @Override
  protected Bundle onResult(int selectedPosition) {
    final String item = ((IconAdapter) getAdapter()).getItem(selectedPosition);
    Bundle result = new Bundle(2);
    result.putString(KEY_ICON, item);
    result.putInt(KEY_RESID, UiUtils.resolveIcon(getContext(), item));
    return result;
  }

  private class IconAdapter extends ClickableAdapter<IconViewHolder> {

    private final String[] data;

    IconAdapter(String[] icons) {
      this.data = icons;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
      return new IconViewHolder(getLayoutInflater().inflate(R.layout.image_view_category_icon_grid, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder iconViewHolder, int position) {
      super.onBindViewHolder(iconViewHolder, position);
      final String item = getItem(position);
      final ImageView itemView = (ImageView) iconViewHolder.itemView;
      itemView.setContentDescription(item);
      itemView.setImageResource(UiUtils.resolveIcon(getContext(), item));
    }

    @Override
    public int getItemCount() {
      return data.length;
    }

    public String getItem(int position) {
      return data[position];
    }
  }

  private class IconViewHolder extends RecyclerView.ViewHolder {

    public IconViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }
}
