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

import android.net.Uri;

import androidx.annotation.Nullable;

import org.totschnig.myexpenses.db2.Repository;

import java.util.UUID;

public abstract class Model implements IModel {
  private long id = 0L;
  private String uuid;

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public static String generateUuid() {
    return UUID.randomUUID().toString();
  }

  String requireUuid() {
    if (android.text.TextUtils.isEmpty(uuid)) {
      uuid = generateUuid();
    }
    return uuid;
  }

  @Nullable
  public abstract Uri save(Repository repository);
}
