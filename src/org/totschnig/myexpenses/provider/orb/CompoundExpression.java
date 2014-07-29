/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package org.totschnig.myexpenses.provider.orb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

class CompoundExpression implements Expression {

  private final String op;
  private final LinkedList<Expression> expressions = new LinkedList<Expression>();

  CompoundExpression(String op, Expression e) {
    this.op = op;
    this.expressions.add(e);
  }

  CompoundExpression(String op, Expression... e) {
    this.op = op;
    this.expressions.addAll(Arrays.asList(e));
  }

  @Override
  public Selection toSelection() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    ArrayList<String> list = new ArrayList<String>();
    boolean first = true;
    for (Expression e : expressions) {
      if (!first) {
        sb.append(" ").append(op).append(" ");
      }
      Selection s = e.toSelection();
      sb.append(s.selection);
      list.addAll(s.selectionArgs);
      first = false;
    }
    sb.append(")");
    return new Selection(sb.toString(), list);
  }

}
