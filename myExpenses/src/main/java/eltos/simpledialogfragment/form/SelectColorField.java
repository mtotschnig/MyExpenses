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
import android.os.Parcel;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorInt;
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener;
import eltos.simpledialogfragment.color.SimpleColorDialog;

/**
 * A color element to be used with {@link SimpleFormDialog}
 * <p>
 * One can pick a color here
 * <p>
 * This will add a ColorInt to resource bundle containing the color.
 * <p>
 * Based on {@link ColorField} by eltos
 */

public class SelectColorField extends FormElement<SelectColorField, SelectColorViewHolder> {
  private @ColorInt
  int preset;
  protected int[] colors = SimpleColorDialog.DEFAULT_COLORS;
  protected boolean allowCustom = true;

  private SelectColorField(String resultKey) {
    super(resultKey);
  }

  /**
   * Factory method for a color field.
   *
   * @param key the key that can be used to receive the final state from the bundle in
   *            {@link OnDialogResultListener#onResult}
   */
  public static SelectColorField picker(String key) {
    return new SelectColorField(key);
  }


  /**
   * Sets the initial color
   *
   * @param preset initial state
   */
  public SelectColorField color(@ColorInt int preset) {
    this.preset = preset;
    return this;
  }

  /**
   * Sets the colors to choose from
   * Default is the {@link SimpleColorDialog#DEFAULT_COLORS} set
   *
   * @param colors array of rgb-colors
   */
  public SelectColorField colors(@ColorInt int[] colors) {
    this.colors = colors;
    return this;
  }

  /**
   * Sets the color pallet to choose from
   * May be one of {@link SimpleColorDialog#MATERIAL_COLOR_PALLET},
   * {@link SimpleColorDialog#MATERIAL_COLOR_PALLET_DARK},
   * {@link SimpleColorDialog#MATERIAL_COLOR_PALLET_LIGHT},
   * {@link SimpleColorDialog#BEIGE_COLOR_PALLET} or a custom pallet
   *
   * @param context       a context to resolve the resource
   * @param colorArrayRes color array resource id
   */
  public SelectColorField colors(Context context, @ArrayRes int colorArrayRes) {
    return colors(context.getResources().getIntArray(colorArrayRes));
  }

  /**
   * Set this to true to show a field with a color picker option
   *
   * @param allow allow custom picked color if true
   */
  public SelectColorField allowCustom(boolean allow) {
    allowCustom = allow;
    return this;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public SelectColorViewHolder buildViewHolder() {
    return new SelectColorViewHolder(this);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////


  protected @ColorInt
  int getInitialColor(Context context) {
    return preset;
  }


  protected SelectColorField(Parcel in) {
    super(in);
    preset = in.readInt();
    colors = in.createIntArray();
    allowCustom = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeInt(preset);
    dest.writeIntArray(colors);
    dest.writeByte((byte) (allowCustom ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<SelectColorField> CREATOR = new Creator<SelectColorField>() {
    @Override
    public SelectColorField createFromParcel(Parcel in) {
      return new SelectColorField(in);
    }

    @Override
    public SelectColorField[] newArray(int size) {
      return new SelectColorField[size];
    }
  };

}
