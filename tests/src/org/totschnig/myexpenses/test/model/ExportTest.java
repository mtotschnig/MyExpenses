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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Result;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ExportTest extends ModelTest  {
  Account account1, account2;
  private SharedPreferences settings;
  Long openingBalance = 100L,
      expense1 = 10L,
      expense2 = 20L,
      income1 = 30L,
      income2 = 40L,
      transferP = 50L,
      transferN = 60L;
  Long cat1Id, cat2Id;
  private void insertData() {
    Transaction op;
    account1 = new Account("Account 1",openingBalance,"Account 1");
    account1.save();
    account2 = new Account("Account 2",openingBalance,"Account 2");
    account2.save();
    cat1Id = Category.create("Main",null);
    cat2Id = Category.create("Sub", cat1Id);
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense1);
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
    op.comment = "Note for myself";
    op.saveAsNew();
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,account1.id);
    op.transfer_account = account2.id;
    op.amount = new Money(account1.currency,transferP);
    op.save();
    op.amount = new Money(account1.currency,-transferN);
    op.saveAsNew();
  }
  public void testExport() {
    MyApplication app = (MyApplication) getContext().getApplicationContext();
    settings = app.getSharedPreferences("functest",Context.MODE_PRIVATE);
    app.setSettings(settings);
    insertData();
    String date = new SimpleDateFormat("dd/MM/yyyy",Locale.US).format(new Date());
    String[] linesQIF = new String[] {
      "!Type:Cash",
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
      "MNote for myself",
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
    String[] linesCSV = new String[] {
        //{R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
        "\"Date\";\"Payee\";\"Income\";\"Expense\";\"Category\";\"Subcategory\";\"Notes\";\"Method\";",
        "\"" + date + "\";\"\";0;0.1;\"\";\"\";\"\";\"\";",
        "\"" + date + "\";\"N.N.\";0;0.2;\"Main\";\"\";\"\";\"\";",
        "\"" + date + "\";\"\";0.3;0;\"Main\";\"Sub\";\"\";\"\";",
        "\"" + date + "\";\"\";0.4;0;\"Main\";\"Sub\";\"Note for myself\";\"\";",
        "\"" + date + "\";\"\";0.5;0;\"Transfer\";\"[Account 2]\";\"\";\"\";",
        "\"" + date + "\";\"\";0;0.6;\"Transfer\";\"[Account 2]\";\"\";\"\";"
    };
    try {
      Result result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.QIF);
      assertTrue(result.success);
      InputStream is = new FileInputStream((File) result.extra[0]);
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      String line;
      int count = 0;
      while ((line = r.readLine()) != null) {
        Log.i("DEBUG","comparing " + line + " and " +linesQIF[count]);
        assertEquals(linesQIF[count],line);
        count++;
      }
      r.close();
      is.close();

      result = account1.exportAll(getContext().getCacheDir(),Account.ExportFormat.CSV);
      assertTrue(result.success);
      is = new FileInputStream((File) result.extra[0]);
      r = new BufferedReader(new InputStreamReader(is));
      count = 0;
      while ((line = r.readLine()) != null) {
        Log.i("DEBUG","comparing " + line + " and " +linesCSV[count]);
        assertEquals(linesCSV[count],line);
        count++;
      }
      r.close();
      is.close();
 
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }
}
