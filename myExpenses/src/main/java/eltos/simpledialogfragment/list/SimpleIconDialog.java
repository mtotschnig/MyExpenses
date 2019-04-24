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
import android.support.annotation.ArrayRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.totschnig.myexpenses.R;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;


/**
 * A dialog that let's the user select an icon
 */
public class SimpleIconDialog extends CustomListDialog<SimpleIconDialog> {

  public static final String TAG = "SimpleIconDialog.";

  public static final String ICONS = TAG + "colors";
  public static final String KEY_RESID = "resID";

  public static SimpleIconDialog build() {
    return new SimpleIconDialog();
  }

  /**
   * Sets the icons to choose from
   */
  public SimpleIconDialog icons(@ArrayRes int icons) {
    getArguments().putInt(ICONS, icons);
    return this;
  }

  public SimpleIconDialog() {
    grid();
    gridColumnWidth(R.dimen.dialog_icon_item_size);
    choiceMode(SINGLE_CHOICE_DIRECT);
    choiceMin(1);
  }

  @Override
  protected AdvancedAdapter onCreateAdapter() {
    return new IconAdapter(getArguments().getInt(ICONS));
  }

  @Override
  protected Bundle onResult(int which) {
    Bundle result = super.onResult(which);
    final String item = (String) getListView().getAdapter().getItem(result.getInt(SELECTED_SINGLE_POSITION));
    result.putString(KEY_ICON, item);
    result.putInt(KEY_RESID, resolveIcon(item));
    return result;
  }

  private class IconAdapter extends AdvancedAdapter<String> {

    IconAdapter(int icons) {
      setData(getContext().getResources().getStringArray(icons));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ImageView item;

      if (convertView != null) {
        item = (ImageView) convertView;
      } else {
        item = new ImageView(getContext());
      }
      item.setImageResource(resolveIcon(getItem(position)));
      return super.getView(position, item, parent);
    }
  }

  private int resolveIcon(String icon) {
    return getContext().getResources().getIdentifier(icon, "drawable", getContext().getPackageName());
  }
}
