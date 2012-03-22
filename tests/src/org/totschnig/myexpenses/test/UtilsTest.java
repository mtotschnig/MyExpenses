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

package org.totschnig.myexpenses.test;

import java.util.Locale;

import org.totschnig.myexpenses.Utils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class UtilsTest extends TestCase {
  public void testValidateNumber() {
    Locale.setDefault(Locale.ENGLISH);
    Assert.assertEquals(4.7f, Utils.validateNumber("4.7"));
    Assert.assertNull(Utils.validateNumber("4,7"));
    Locale.setDefault(Locale.FRENCH);
    Assert.assertEquals(4.7f, Utils.validateNumber("4,7"));
    Assert.assertNull(Utils.validateNumber("4.7"));
    Locale.setDefault(Locale.GERMAN);
    Assert.assertEquals(4.7f, Utils.validateNumber("4,7"));
    Assert.assertNull(Utils.validateNumber("4.7"));
  }
}
