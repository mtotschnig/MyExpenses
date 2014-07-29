/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.provider.filter;

import org.totschnig.myexpenses.provider.DatabaseConstants;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class SingleCategoryCriteria extends Criteria {

  private final long categoryId;
  private final String label;

  public SingleCategoryCriteria(long categoryId, String label) {
    super(DatabaseConstants.KEY_CATID, WhereFilter.Operation.EQ, String
        .valueOf(categoryId));
    this.categoryId = categoryId;
    this.label = label;
  }

  public String toStringExtra() {
    StringBuilder sb = new StringBuilder();
    sb.append(DatabaseConstants.KEY_CATID).append(",EQ,").append(categoryId);
    return sb.toString();
  }

  // public static Criteria fromStringExtra(String extra) {
  // String[] a = extra.split(",");
  // return new SingleCategoryCriteria(Long.parseLong(a[2]));
  // }

  public long getCategoryId() {
    return categoryId;
  }

  @Override
  public String prettyPrint() {
    // TODO Auto-generated method stub
    return label;
  }
}
