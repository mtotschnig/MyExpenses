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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import android.net.Uri;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

@Deprecated
public class Category extends Model {
  public final static String NO_CATEGORY_ASSIGNED_LABEL = "—"; //emdash
  private String label;
  private final Long parentId;
  private final int color;
  private final String icon;

  /**
   * we currently do not need a full representation of a category as an object
   * when we create an instance with an id, we only want to alter its label
   * and are not interested in its parentId
   * when we create an instance with a parentId, it is a new instance
   */
  public Category(Long id, String label, Long parentId) {
    this(id, label, parentId, 0, null);
  }

  public Category(Long id, String label, Long parentId, int color, String icon) {
    this.setId(id);
    this.setLabel(label);
    this.parentId = parentId;
    this.color = color;
    this.icon = icon;
  }

  public static final String[] PROJECTION = new String[]{KEY_ROWID, KEY_LABEL, KEY_PARENTID};
  public static final Uri CONTENT_URI = TransactionProvider.CATEGORIES_URI;

  @Override
  public Uri save() {
    throw new NotImplementedException();
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = StringUtils.strip(label);
  }

}