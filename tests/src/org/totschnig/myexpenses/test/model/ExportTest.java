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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Result;

import android.util.Log;


public class ExportTest extends ModelTest  {
  Account account1, account2;
  Long openingBalance = 100L,
      expense1 = 10L,
      expense2 = 20L,
      income1 = 30L,
      income2 = 40L,
      transferP = 50L,
      transferN = 60L,
      expense3 = 100L,
      income3 = 100L;
  Long cat1Id, cat2Id;
  String date = new SimpleDateFormat("dd/MM/yyyy",Locale.US).format(new Date());
  File export;
  private void insertData1() {
    Transaction op;
    account1 = new Account("Account 1",openingBalance,"Account 1");
    account1.type = Account.Type.BANK;
    account1.save();
    account2 = new Account("Account 2",openingBalance,"Account 2");
    account2.save();
    cat1Id = Category.create("Main",null);
    cat2Id = Category.create("Sub", cat1Id);
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense1);
    op.methodId = PaymentMethod.find("CHEQUE");
    op.save();
    op.amount = new Money(account1.currency,-expense2);
    op.catId = cat1Id;
    op.payee = "N.N.";
    op.saveAsNew();
    op.amount = new Money(account1.currency,income1);
    op.catId = cat2Id;
    op.payee = null;
    op.saveAsNew();
    op.amount = new Money(account1.currency,income2);
    op.comment = "Note for myself with \"quote\"";
    op.saveAsNew();
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,account1.id);
    op.transfer_account = account2.id;
    op.amount = new Money(account1.currency,transferP);
    op.save();
    op.amount = new Money(account1.currency,-transferN);
    op.saveAsNew();
  }
  private void insertData2() {
    Transaction op;
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense3);
    op.methodId = PaymentMethod.find("CHEQUE");
    op.comment = "Expense inserted after first export";
    op.save();
    op.amount = new Money(account1.currency,income3);
    op.comment = "Income inserted after first export";
    op.payee = "N.N.";
    op.saveAsNew();
  }
  public void testExportQIF() {
    String[] linesQIF = new String[] {
      "!Type:Bank",
      "D" + date,
      "T-0.1",
      "^",
      "D" + date,
      "T-0.2",
      "LMain",
      "PN.N.",
      "^",
      "D" + date,
      "T0.3",
      "LMain:Sub",
      "^",
      "D" + date,
      "T0.4",
      "MNote for myself with \"quote\"",
      "LMain:Sub",
      "^",
      "D" + date,
      "T0.5",
      "L[Account 2]",
      "^",
      "D" + date,
      "T-0.6",
      "L[Account 2]",
      "^"
    };
    try {
      insertData1();
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.QIF, false);
      assertTrue(result.success);
      export = (File) result.extra[0];
      compare(export,linesQIF);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }
  public void testExportCSV() {
    String[] linesCSV = new String[] {
        //{R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
        "\"Split transaction\";\"Date\";\"Payee\";\"Income\";\"Expense\";\"Category\";\"Subcategory\";\"Notes\";\"Method\";",
        "\"\";\"" + date + "\";\"\";0;0.1;\"\";\"\";\"\";\"Cheque\";",
        "\"\";\"" + date + "\";\"N.N.\";0;0.2;\"Main\";\"\";\"\";\"Cheque\";",
        "\"\";\"" + date + "\";\"\";0.3;0;\"Main\";\"Sub\";\"\";\"Cheque\";",
        "\"\";\"" + date + "\";\"\";0.4;0;\"Main\";\"Sub\";\"Note for myself with \"\"quote\"\"\";\"Cheque\";",
        "\"\";\"" + date + "\";\"\";0.5;0;\"Transfer\";\"[Account 2]\";\"\";\"\";",
        "\"\";\"" + date + "\";\"\";0;0.6;\"Transfer\";\"[Account 2]\";\"\";\"\";"
    };
    try {
      insertData1();
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, false);
      assertTrue(result.success);
      export = (File) result.extra[0];
      compare(export,linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }
  public void testExportNotYetExported() {
    String[] linesCSV = new String[] {
        //{R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
        "\"Split transaction\";\"Date\";\"Payee\";\"Income\";\"Expense\";\"Category\";\"Subcategory\";\"Notes\";\"Method\";",
        "\"\";\"" + date + "\";\"\";0;1;\"\";\"\";\"Expense inserted after first export\";\"Cheque\";",
        "\"\";\"" + date + "\";\"N.N.\";1;0;\"\";\"\";\"Income inserted after first export\";\"Cheque\";",
    };
    try {
      insertData1();
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, false);
      assertTrue(result.success);
      account1.markAsExported();
      export = (File) result.extra[0];
      export.delete();
      insertData2();
      result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, true);
      if (!result.success)
        Log.i("DEBUG",getContext().getString(result.message));
      assertTrue(result.success);
      export = (File) result.extra[0];
      compare(export,linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }
  private void compare(File file,String[] lines) {
    try {
      InputStream is = new FileInputStream(file);
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      String line;
      int count = 0;
      while ((line = r.readLine()) != null) {
        assertEquals(lines[count],line);
        count++;
      }
      r.close();
      is.close();
    } catch (IOException e) {
      fail("Could not compare exported file. Error: " + e.getMessage());
    }
  }
  protected void tearDown() throws Exception {
    super.tearDown();
    if (export!=null)
      export.delete();
  }
}
