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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.util.Result;

import android.util.Log;


public class ExportTest extends ModelTest  {
  Account account1, account2;
  Long openingBalance = 100L,
      expense1 = 10L, //status cleared
      expense2 = 20L,
      income1 = 30L,
      income2 = 40L,
      transferP = 50L, //status reconciled
      transferN = 60L,
      expense3 = 100L,
      income3 = 100L,
      split1 = 70L,
      part1 = 40L,
      part2 = 30L;
      
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
    cat1Id = Category.write(0,"Main",null);
    cat2Id = Category.write(0,"Sub", cat1Id);
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense1);
    op.methodId = PaymentMethod.find("CHEQUE");
    op.crStatus = Transaction.CrStatus.CLEARED;
    op.referenceNumber = "1";
    op.save();
    op.amount = new Money(account1.currency,-expense2);
    op.catId = cat1Id;
    op.payee = "N.N.";
    op.crStatus = Transaction.CrStatus.UNRECONCILED;
    op.referenceNumber = "2";
    op.saveAsNew();
    op.amount = new Money(account1.currency,income1);
    op.catId = cat2Id;
    op.payee = null;
    op.methodId = null;
    op.referenceNumber = null;
    op.saveAsNew();
    op.amount = new Money(account1.currency,income2);
    op.comment = "Note for myself with \"quote\"";
    op.saveAsNew();
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,account1.id);
    op.transfer_account = account2.id;
    op.amount = new Money(account1.currency,transferP);
    op.crStatus = Transaction.CrStatus.RECONCILED;
    op.save();
    op.crStatus = Transaction.CrStatus.UNRECONCILED;
    op.amount = new Money(account1.currency,-transferN);
    op.saveAsNew();
    SplitTransaction split = (SplitTransaction) Transaction.getTypedNewInstance(MyExpenses.TYPE_SPLIT, account1.id);
    split.amount = new Money(account1.currency,split1);
    Transaction part = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id,split.id);
    part.amount =  new Money(account1.currency,part1);
    part.catId = cat1Id;
    part.status = org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
    part.save();
    part.amount =  new Money(account1.currency,part2);
    part.catId = cat2Id;
    part.saveAsNew();
    split.save();
  }
  private void insertData2() {
    Transaction op;
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense3);
    op.methodId = PaymentMethod.find("CHEQUE");
    op.comment = "Expense inserted after first export";
    op.referenceNumber = "3";
    op.save();
    op.amount = new Money(account1.currency,income3);
    op.comment = "Income inserted after first export";
    op.payee = "N.N.";
    op.methodId = null;
    op.referenceNumber = null;
    op.saveAsNew();
  }
  public void testExportQIF() {
    String[] linesQIF = new String[] {
      "!Account",
      "NAccount 1",
      "TBank",
      "^",
      "!Type:Bank",
      "D" + date,
      "T-0.10",
      "C*",
      "N1",
      "^",
      "D" + date,
      "T-0.20",
      "LMain",
      "PN.N.",
      "N2",
      "^",
      "D" + date,
      "T0.30",
      "LMain:Sub",
      "^",
      "D" + date,
      "T0.40",
      "MNote for myself with \"quote\"",
      "LMain:Sub",
      "^",
      "D" + date,
      "T0.50",
      "L[Account 2]",
      "CX",
      "^",
      "D" + date,
      "T-0.60",
      "L[Account 2]",
      "^",
      "D" + date,
      "T0.70",
      "L" + getContext().getString(R.string.split_transaction),
      "SMain",
      "$0.40",
      "SMain:Sub",
      "$0.30",
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
  //TODO: add split lines
  public void testExportCSV() {
    String[] linesCSV = new String[] {
        csvHeader(),
        "\"\";\"" + date + "\";\"\";0;0.10;\"\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"*\";\"1\";",
        "\"\";\"" + date + "\";\"N.N.\";0;0.20;\"Main\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"\";\"2\";",
        "\"\";\"" + date + "\";\"\";0.30;0;\"Main\";\"Sub\";\"\";\"\";\"\";\"\";",
        "\"\";\"" + date + "\";\"\";0.40;0;\"Main\";\"Sub\";\"Note for myself with \"\"quote\"\"\";\"\";\"\";\"\";",
        "\"\";\"" + date + "\";\"\";0.50;0;\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"X\";\"\";",
        "\"\";\"" + date + "\";\"\";0;0.60;\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"\";\"\";"
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
  public void testExportCSVCustomFormat() {
    String[] linesCSV = new String[] {
        csvHeader(),
        "\"\";\"" + date + "\";\"\";0;0,10;\"\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
        + "\";\"*\";\"1\";",
        "\"\";\"" + date + "\";\"N.N.\";0;0,20;\"Main\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"\";\"2\";",
        "\"\";\"" + date + "\";\"\";0,30;0;\"Main\";\"Sub\";\"\";\"\";\"\";\"\";",
        "\"\";\"" + date + "\";\"\";0,40;0;\"Main\";\"Sub\";\"Note for myself with \"\"quote\"\"\";\"\";\"\";\"\";",
        "\"\";\"" + date + "\";\"\";0,50;0;\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"X\";\"\";",
        "\"\";\"" + date + "\";\"\";0;0,60;\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"\";\"\";"
    };
    try {
      insertData1();
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, false, "dd/MM/yyyy",',');
      assertTrue(result.success);
      export = (File) result.extra[0];
      compare(export,linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }
  public void testExportNotYetExported() {
    String[] linesCSV = new String[] {
        csvHeader(),
        "\"\";\"" + date + "\";\"\";0;1.00;\"\";\"\";\"Expense inserted after first export\";\""
            + getContext().getString(R.string.pm_cheque) + "\";\"\";\"3\";",
        "\"\";\"" + date + "\";\"N.N.\";1.00;0;\"\";\"\";\"Income inserted after first export\";\"\";\"\";\"\";"
    };
    try {
      insertData1();
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, false);
      assertTrue("Export failed with message: " + getContext().getString(result.getMessage()),result.success);
      account1.markAsExported();
      export = (File) result.extra[0];
      export.delete();
      insertData2();
      result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV, true);
      assertTrue("Export failed with message: " + getContext().getString(result.getMessage()),result.success);
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
        Log.i("DEBUG",line);
        assertEquals("Lines do not match", lines[count],line);
        count++;
      }
      r.close();
      is.close();
    } catch (IOException e) {
      fail("Could not compare exported file. Error: " + e.getMessage());
    }
  }
  private String csvHeader() {
    StringBuilder sb = new StringBuilder();
    int[] resArray = {
        R.string.split_transaction,
        R.string.date,R.string.payee,
        R.string.income,
        R.string.expense,
        R.string.category,
        R.string.subcategory,
        R.string.comment,
        R.string.method,
        R.string.status,
        R.string.reference_number};
    for(int res : resArray)
    {
      sb.append("\"");
      sb.append(getContext().getString(res));
      sb.append("\";");
    }
    return sb.toString();
  }
  protected void tearDown() throws Exception {
    super.tearDown();
    if (export!=null) {
      export.delete();
    }
  }
}
