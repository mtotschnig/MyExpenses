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

import android.os.Parcel;

public class SelectIconField extends FormElement<SelectIconField, IconViewHolder> {

  String preset;
  String[] iconArray;

  private SelectIconField(String resultKey) {
    super(resultKey);
  }

  public static SelectIconField picker(String key) {
    return new SelectIconField(key);
  }

  public SelectIconField preset(String preset) {
    this.preset = preset;
    return this;
  }

  public SelectIconField icons(String[] iconArray) {
    this.iconArray = iconArray;
    return this;
  }

  @Override
  public IconViewHolder buildViewHolder() {
    return new IconViewHolder(this);
  }

  protected SelectIconField(Parcel in) {
    super(in);
    preset = in.readString();
    iconArray = in.createStringArray();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(preset);
    dest.writeStringArray(iconArray);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<SelectIconField> CREATOR = new Creator<SelectIconField>() {
    @Override
    public SelectIconField createFromParcel(Parcel in) {
      return new SelectIconField(in);
    }

    @Override
    public SelectIconField[] newArray(int size) {
      return new SelectIconField[size];
    }
  };

}
