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

import java.util.Collections;

public class Gt implements Expression {

  private final String field;
  private final Object value;

  Gt(String field, Object value) {
    this.field = field;
    this.value = value;
  }

  @Override
  public Selection toSelection() {
    return new Selection("(" + field + ">?)", Collections.singletonList(String
        .valueOf(value)));
  }

}
