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

package org.totschnig.myexpenses.test.misc;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.totschnig.myexpenses.util.PdfHelper;
import org.totschnig.myexpenses.util.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class UtilsTest extends TestCase {
  public void testValidateNumber() {
    DecimalFormat nfDLocal;
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator('.');
    nfDLocal = new DecimalFormat("#0.###", symbols);
    Assert.assertEquals(0, Utils.validateNumber(nfDLocal, "4.7").compareTo(new BigDecimal("4.7")));
    Assert.assertNull(Utils.validateNumber(nfDLocal, "4,7"));
    symbols.setDecimalSeparator(',');
    nfDLocal = new DecimalFormat("#0.###", symbols);
    Assert.assertEquals(0, Utils.validateNumber(nfDLocal, "4,7").compareTo(new BigDecimal("4.7")));
    Assert.assertNull(Utils.validateNumber(nfDLocal, "4.7"));
    nfDLocal = new DecimalFormat("#0");
    nfDLocal.setParseIntegerOnly(true);
    Assert.assertEquals(0, Utils.validateNumber(nfDLocal, "470").compareTo(new BigDecimal(470)));
    Assert.assertNull(Utils.validateNumber(nfDLocal, "470.123"));
  }

  public void testPdfHelper() {
    Assert.assertFalse(PdfHelper.hasAnyRtl("test"));
    Assert.assertTrue(PdfHelper.hasAnyRtl("مصروفاتي"));
    Assert.assertTrue(PdfHelper.hasAnyRtl("הנושאים שלי"));
  }
}
