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

package org.totschnig.myexpenses.test.model;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;

public class TemplateTest extends ModelTest  {
  private Account mAccount1;
  private Account mAccount2;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mAccount1 = new Account("TestAccount 1",100,"Main account");
      mAccount1.save();
      mAccount2 = new Account("TestAccount 2",100,"Secondary account");
      mAccount2.save();
  }
  public void testTemplateCreatedFromTransaction() {
    Long start = mAccount1.getTotalBalance().getAmountMinor();
    Long amount = (long) 100;
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId());
    op1.amount = new Money(mAccount1.currency,amount);
    op1.comment = "test transaction";
    op1.save();
    assertEquals(mAccount1.getTotalBalance().getAmountMinor().longValue(), start+amount);
    Template t = new Template(op1,"Template");
    t.save();
    Transaction op2  = Transaction.getInstanceFromTemplate(t.getId());
    op2.save();
    assertEquals(mAccount1.getTotalBalance().getAmountMinor().longValue(), start+2*amount);
    Template restored;
    restored = Template.getInstanceFromDb(t.getId());
    assertEquals(t,restored);

    Template.delete(t.getId());
    assertNull("Template deleted, but can still be retrieved",Template.getInstanceFromDb(t.getId()));
  }

  public void testGetTypedNewInstanceTransaction() {
    newInstanceTestHelper(MyExpenses.TYPE_TRANSACTION);
  }
  public void testGetTypedNewInstanceTransfer() {
    newInstanceTestHelper(MyExpenses.TYPE_TRANSFER);
  }
  /**
   * 
   */
  protected void newInstanceTestHelper(int type) {
    Template t,restored;
    t = Template.getTypedNewInstance(type, mAccount1.getId());
    t.title = "Template";
    t.save();
    restored = Template.getInstanceFromDb(t.getId());
    assertEquals(t,restored);
  }
}
