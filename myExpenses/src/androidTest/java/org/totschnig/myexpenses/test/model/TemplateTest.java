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

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;

public class TemplateTest extends ModelTest {
  private Account mAccount1, mAccount2;
  private long categoryId, payeeId;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mAccount1 = new Account("TestAccount 1", 100, "Main account");
    mAccount1.save();
    mAccount2 = new Account("TestAccount 2", 100, "Secondary account");
    mAccount2.save();
    categoryId = Category.write(0, "TestCategory", null);
    payeeId = Payee.maybeWrite("N.N");

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Account.delete(mAccount1.getId());
    Account.delete(mAccount2.getId());
    Category.delete(categoryId);
  }

  public void testTemplateFromTransaction() {
    Long start = mAccount1.getTotalBalance().getAmountMinor();
    Long amount = (long) 100;
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId());
    assert op1 != null;
    op1.setAmount(new Money(mAccount1.getCurrencyUnit(), amount));
    op1.setComment("test transaction");
    op1.save();
    assertEquals(mAccount1.getTotalBalance().getAmountMinor(), start + amount);
    Template t = new Template(op1, "Template");
    t.save();
    Transaction op2 = Transaction.getInstanceFromTemplate(t.getId());
    op2.save();
    assertEquals(mAccount1.getTotalBalance().getAmountMinor(), start + 2 * amount);
    Template restored;
    restored = Template.getInstanceFromDb(t.getId());
    assertNotNull(restored);
    assertEquals(t, restored);

    Template.delete(t.getId(), false);
    assertNull("Template deleted, but can still be retrieved", Template.getInstanceFromDb(t.getId()));
  }

  public void testTemplate() {
    Template template = buildTemplate();
    Template restored = Template.getInstanceFromDb(template.getId());
    assertEquals(template, restored);
  }

  public void testTransactionFromTemplate() {
    Template template = buildTemplate();
    Transaction transaction = Transaction.getInstanceFromTemplate(template);
    assertEquals(template.getCatId(), transaction.getCatId());
    assertEquals(template.getAccountId(), transaction.getAccountId());
    assertEquals(template.getPayeeId(), transaction.getPayeeId());
    assertEquals(template.getMethodId(), transaction.getMethodId());
    assertEquals(template.getComment(), transaction.getComment());
  }

  public void testGetTypedNewInstanceTransaction() {
    newInstanceTestHelper(TYPE_TRANSACTION);
  }

  public void testGetTypedNewInstanceTransfer() {
    newInstanceTestHelper(TYPE_TRANSFER);
  }

  public void testGetTypedNewInstanceSplit() {
    newInstanceTestHelper(TYPE_SPLIT);
  }

  /**
   *
   */
  private void newInstanceTestHelper(int type) {
    Template t, restored;
    t = Template.getTypedNewInstance(type, mAccount1.getId(), false, null);
    assert t != null;
    t.setTitle("Template");
    t.save();
    assertEquals(t.operationType(), type);
    restored = Template.getInstanceFromDb(t.getId());
    assertNotNull(restored);
    assertEquals(t, restored);
  }

  private Template buildTemplate() {
    Template t = new Template(mAccount1, TYPE_TRANSACTION, null);
    t.setCatId(categoryId);
    t.setPayeeId(payeeId);
    t.setComment("Some comment");
    final long methodId = PaymentMethod.find(PaymentMethod.PreDefined.CHEQUE.name());
    assert methodId > -1;
    t.setMethodId(methodId);
    t.save();
    return t;
  }
}
