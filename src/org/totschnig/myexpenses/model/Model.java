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

package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.MyApplication;

import android.content.ContentResolver;
import android.net.Uri;

public abstract class Model {
  private Long id = 0L;
  private static ContentResolver cr;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public static ContentResolver cr() {
    return cr != null ? cr : MyApplication.getInstance().getContentResolver();
  }

  public static void setContentResolver(ContentResolver crIn) {
    cr = crIn;
  }
  public abstract Uri save();
}
