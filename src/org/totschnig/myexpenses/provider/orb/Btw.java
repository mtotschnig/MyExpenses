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

public class Btw implements Expression {

  private final String field;
  private final Object value1;
  private final Object value2;

  Btw(String field, Object value1, Object value2) {
    this.field = field;
    this.value1 = value1;
    this.value2 = value2;
  }

  @Override
  public Selection toSelection() {
    ArrayList<String> args = new ArrayList<String>();
    args.add(String.valueOf(value1));
    args.add(String.valueOf(value2));
    return new Selection("(" + field + " between ? and ?)", args);
  }

}
