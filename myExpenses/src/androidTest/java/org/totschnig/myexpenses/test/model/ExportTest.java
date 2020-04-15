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

import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.Exporter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.util.Result;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.documentfile.provider.DocumentFile;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;


public class ExportTest extends ModelTest {
  private static final String FILE_NAME = "TEST";
  private Account account1, account2;
  private Long openingBalance = 100L,
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
  Date base = new Date(117,11, 15, 12, 0, 0);
  long baseSinceEpoch = base.getTime();
  String date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(base);
  Uri export;
  private DocumentFile outDir;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    outDir = DocumentFile.fromFile(getContext().getCacheDir());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (export != null) {
      //noinspection ResultOfMethodCallIgnored
      new File(export.getPath()).delete();
    }
    if (account1 != null) {
      Account.delete(account1.getId());
    }
    if (account2 != null) {
      Account.delete(account2.getId());
    }
    if (cat1Id != null) Category.delete(cat1Id);
    if (cat2Id != null) Category.delete(cat2Id);
  }

  private void insertData1() {
    Transaction op;
    account1 = new Account("Account 1", openingBalance, "Account 1");
    account1.setType(AccountType.BANK);
    account1.save();
    account2 = new Account("Account 2", openingBalance, "Account 2");
    account2.save();
    cat1Id = Category.write(0, "Main", null);
    cat2Id = Category.write(0, "Sub", cat1Id);
    op = Transaction.getNewInstance(account1.getId());
    if (op == null) {
      fail();
      return;
    }
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense1));
    op.setMethodId(PaymentMethod.find("CHEQUE"));
    op.setCrStatus(CrStatus.CLEARED);
    op.setReferenceNumber("1");
    op.setDate(new Date(baseSinceEpoch));
    op.save();

    op.setAmount(new Money(account1.getCurrencyUnit(), -expense2));
    op.setCatId(cat1Id);
    op.setPayee("N.N.");
    op.setCrStatus(CrStatus.UNRECONCILED);
    op.setReferenceNumber("2");
    op.setDate(new Date(baseSinceEpoch + 1000));
    op.saveAsNew();

    op.setAmount(new Money(account1.getCurrencyUnit(), income1));
    op.setCatId(cat2Id);
    op.setPayee(null);
    op.setMethodId(null);
    op.setReferenceNumber(null);
    op.setDate(new Date(baseSinceEpoch + 2000));
    op.saveAsNew();
    ContentValues contentValues = new ContentValues(1);
    contentValues.put(KEY_PICTURE_URI, "file://sdcard/picture.png");
    getMockContentResolver().update(ContentUris.withAppendedId(Transaction.CONTENT_URI,op.getId()), contentValues, null, null);

    op.setAmount(new Money(account1.getCurrencyUnit(), income2));
    op.setComment("Note for myself with \"quote\"");
    op.setDate(new Date(baseSinceEpoch + 3000));
    op.saveAsNew();

    op = Transfer.getNewInstance(account1.getId(), account2.getId());
    if (op == null) {
      fail();
      return;
    }
    op.setAmount(new Money(account1.getCurrencyUnit(), transferP));
    op.setCrStatus(CrStatus.RECONCILED);
    op.setDate(new Date(baseSinceEpoch + 4000));
    op.save();

    op.setCrStatus(CrStatus.UNRECONCILED);
    op.setAmount(new Money(account1.getCurrencyUnit(), -transferN));
    op.setDate(new Date(baseSinceEpoch + 5000));
    op.saveAsNew();

    SplitTransaction split = SplitTransaction.getNewInstance(account1.getId());
    if (split == null) {
      fail();
      return;
    }
    split.setAmount(new Money(account1.getCurrencyUnit(), split1));
    split.setDate(new Date(baseSinceEpoch + 6000));
    Transaction part = Transaction.getNewInstance(account1.getId(), split.getId());
    if (part == null) {
      fail();
      return;
    }
    part.setAmount(new Money(account1.getCurrencyUnit(), part1));
    part.setCatId(cat1Id);
    part.setStatus(STATUS_UNCOMMITTED);
    part.save();
    part.setAmount(new Money(account1.getCurrencyUnit(), part2));
    part.setCatId(cat2Id);
    part.saveAsNew();
    split.save();
  }

  private void insertData2() {
    Transaction op;
    op = Transaction.getNewInstance(account1.getId());
    if (op == null) {
      fail();
      return;
    }
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense3));
    op.setMethodId(PaymentMethod.find("CHEQUE"));
    op.setComment("Expense inserted after first export");
    op.setReferenceNumber("3");
    op.setDate(new Date(baseSinceEpoch));
    op.save();
    op.setAmount(new Money(account1.getCurrencyUnit(), income3));
    op.setComment("Income inserted after first export");
    op.setPayee("N.N.");
    op.setMethodId(null);
    op.setReferenceNumber(null);
    op.setDate(new Date(baseSinceEpoch + 1000));
    op.saveAsNew();
  }

  private void insertData3() {
    Transaction op;
    account1 = new Account("Account 1", openingBalance, "Account 1");
    account1.setType(AccountType.BANK);
    account1.save();
    account2 = new Account("Account 2", openingBalance, "Account 2");
    account2.save();
    cat1Id = Category.write(0, "Main", null);
    cat2Id = Category.write(0, "Sub", cat1Id);
    op = Transaction.getNewInstance(account1.getId());
    if (op == null) {
      fail();
      return;
    }
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense1));
    op.setMethodId(PaymentMethod.find("CHEQUE"));
    op.setCrStatus(CrStatus.CLEARED);
    op.setReferenceNumber("1");
    op.setDate(new Date(baseSinceEpoch));
    op.save();

    op = Transaction.getNewInstance(account2.getId());
    if (op == null) {
      fail();
      return;
    }
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense1));
    op.setMethodId(PaymentMethod.find("CHEQUE"));
    op.setCrStatus(CrStatus.CLEARED);
    op.setReferenceNumber("1");
    op.setDate(new Date(baseSinceEpoch));
    op.save();
  }

  public void testExportQIF() {
    String[] linesQIF = new String[]{
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
        "LMain",
        "SMain",
        "$0.40",
        "SMain:Sub",
        "$0.30",
        "^"
    };
    try {
      insertData1();
      Result<Uri> result = exportAll(account1, ExportFormat.QIF, false, false, false);
      assertTrue(result.isSuccess());
      export = result.getExtra();
      compare(new File(export.getPath()), linesQIF);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }

  //TODO: add split lines
  public void testExportCSV() {
    String[] linesCSV = new String[]{
        csvHeader(';', false),
        "\"\";\"" + date + "\";\"\";\"0\";\"0.10\";\"\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"*\";\"1\";\"\"",
        "\"\";\"" + date + "\";\"N.N.\";\"0\";\"0.20\";\"Main\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"\";\"2\";\"\"",
        "\"\";\"" + date + "\";\"\";\"0.30\";\"0\";\"Main\";\"Sub\";\"\";\"\";\"\";\"\";\"picture.png\"",
        "\"\";\"" + date + "\";\"\";\"0.40\";\"0\";\"Main\";\"Sub\";\"Note for myself with \"\"quote\"\"\";\"\";\"\";\"\";\"\"",
        "\"\";\"" + date + "\";\"\";\"0.50\";\"0\";\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"X\";\"\";\"\"",
        "\"\";\"" + date + "\";\"\";\"0\";\"0.60\";\"" + getContext().getString(R.string.transfer)
            + "\";\"[Account 2]\";\"\";\"\";\"\";\"\";\"\"",
        "\"*\";\"" + date + "\";\"\";\"0.70\";\"0\";\"Main\";\"\";\"\";\"\";\"\";\"\";\"\"",
        "\"-\";\"" + date + "\";\"\";\"0.40\";\"0\";\"Main\";\"\";\"\";\"\";\"\";\"\";\"\"",
        "\"-\";\"" + date + "\";\"\";\"0.30\";\"0\";\"Main\";\"Sub\";\"\";\"\";\"\";\"\";\"\"",
        ""
    };
    try {
      insertData1();
      Result<Uri> result = exportAll(account1, ExportFormat.CSV, false, false, false);
      assertTrue(result.isSuccess());
      export = result.getExtra();
      compare(new File(export.getPath()), linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }

  public void testExportCSVCustomFormat() {
    String date = new SimpleDateFormat("M/d/yyyy", Locale.US).format(base);
    String[] linesCSV = new String[]{
        csvHeader(',', false),
        "\"\",\"" + date + "\",\"\",\"0\",\"0,10\",\"\",\"\",\"\",\"" + getContext().getString(R.string.pm_cheque)
            + "\",\"*\",\"1\",\"\"",
        "\"\",\"" + date + "\",\"N.N.\",\"0\",\"0,20\",\"Main\",\"\",\"\",\"" + getContext().getString(R.string.pm_cheque)
            + "\",\"\",\"2\",\"\"",
        "\"\",\"" + date + "\",\"\",\"0,30\",\"0\",\"Main\",\"Sub\",\"\",\"\",\"\",\"\",\"picture.png\"",
        "\"\",\"" + date + "\",\"\",\"0,40\",\"0\",\"Main\",\"Sub\",\"Note for myself with \"\"quote\"\"\",\"\",\"\",\"\",\"\"",
        "\"\",\"" + date + "\",\"\",\"0,50\",\"0\",\"" + getContext().getString(R.string.transfer)
            + "\",\"[Account 2]\",\"\",\"\",\"X\",\"\",\"\"",
        "\"\",\"" + date + "\",\"\",\"0\",\"0,60\",\"" + getContext().getString(R.string.transfer)
            + "\",\"[Account 2]\",\"\",\"\",\"\",\"\",\"\"",
        "\"*\",\"" + date + "\",\"\",\"0,70\",\"0\",\"Main\",\"\",\"\",\"\",\"\",\"\",\"\"",
        "\"-\",\"" + date + "\",\"\",\"0,40\",\"0\",\"Main\",\"\",\"\",\"\",\"\",\"\",\"\"",
        "\"-\",\"" + date + "\",\"\",\"0,30\",\"0\",\"Main\",\"Sub\",\"\",\"\",\"\",\"\",\"\"",
        ""
    };
    try {
      insertData1();
      Result<Uri> result = new Exporter(account1, null, outDir, FILE_NAME, ExportFormat.CSV, false, "M/d/yyyy", ',', "UTF-8", ',', false, false)
          .export();
      assertTrue(result.isSuccess());
      export = result.getExtra();
      compare(new File(export.getPath()), linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }

  public void testExportNotYetExported() {
    String[] linesCSV = new String[]{
        csvHeader(';', false),
        "\"\";\"" + date + "\";\"\";\"0\";\"1.00\";\"\";\"\";\"Expense inserted after first export\";\""
            + getContext().getString(R.string.pm_cheque) + "\";\"\";\"3\";\"\"",
        "\"\";\"" + date + "\";\"N.N.\";\"1.00\";\"0\";\"\";\"\";\"Income inserted after first export\";\"\";\"\";\"\";\"\"",
        ""
    };
    try {
      insertData1();
      Result<Uri> result = exportAll(account1, ExportFormat.CSV, false, false, false);
      assertTrue("Export failed with message: " + getContext().getString(result.getMessage()), result.isSuccess());
      account1.markAsExported(null);
      export = result.getExtra();
      //noinspection ResultOfMethodCallIgnored
      new File(export.getPath()).delete();
      insertData2();
      result = exportAll(account1, ExportFormat.CSV, true, false, false);
      assertTrue("Export failed with message: " + getContext().getString(result.getMessage()), result.isSuccess());
      export = result.getExtra();
      compare(new File(export.getPath()), linesCSV);
    } catch (IOException e) {
      fail("Could not export expenses. Error: " + e.getMessage());
    }
  }

  public void testExportMultipleAccountsToOneFileCSV() throws IOException {
    insertData3();
    String[] linesCSV = new String[]{
        csvHeader(';', true),
        "\"" + account1.getLabel() +  "\";\"\";\"" + date + "\";\"\";\"0\";\"0.10\";\"\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"*\";\"1\";\"\"",
        "\"" + account2.getLabel() +  "\";\"\";\"" + date + "\";\"\";\"0\";\"0.10\";\"\";\"\";\"\";\"" + getContext().getString(R.string.pm_cheque)
            + "\";\"*\";\"1\";\"\"",
        ""
    };
    exportAll(account1, ExportFormat.CSV, false, false, true);
    Result<Uri> result =exportAll(account2, ExportFormat.CSV, false, true, true);
    export = result.getExtra();
    compare(new File(export.getPath()), linesCSV);
  }

  public void testExportMultipleAccountsToOneFileQIF() throws IOException {
    insertData3();
    String[] linesQIF = new String[]{
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
        "!Account",
        "NAccount 2",
        "TCash",
        "^",
        "!Type:Cash",
        "D" + date,
        "T-0.10",
        "C*",
        "N1",
        "^",
    };
    exportAll(account1, ExportFormat.QIF, false, false, true);
    Result<Uri> result =exportAll(account2, ExportFormat.QIF, false, true, true);
    export = result.getExtra();
    compare(new File(export.getPath()), linesQIF);
  }

  private void compare(File file, String[] lines) {
    try {
      InputStream is = new FileInputStream(file);
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      String line;
      int count = 0;
      while ((line = r.readLine()) != null) {
        Log.i("DEBUG", line);
        assertEquals("Lines do not match", lines[count], line);
        count++;
      }
      r.close();
      is.close();
    } catch (IOException e) {
      fail("Could not compare exported file. Error: " + e.getMessage());
    }
  }

  private String csvHeader(char separator, boolean withAccountColumn) {
    StringBuilder sb = new StringBuilder();
    int[] resArray = {
        R.string.split_transaction,
        R.string.date, R.string.payer_or_payee,
        R.string.income,
        R.string.expense,
        R.string.category,
        R.string.subcategory,
        R.string.comment,
        R.string.method,
        R.string.status,
        R.string.reference_number,
        R.string.picture};
    if (withAccountColumn) {
      sb.append('"').append(getContext().getString(R.string.account)).append('"').append(separator);
    }
    for (int res : resArray) {
      sb.append('"').append(getContext().getString(res)).append('"').append(separator);
    }
    return sb.toString();
  }

  private Result<Uri> exportAll(Account account, ExportFormat format, boolean notYetExportedP, boolean append, boolean withAccountColumn)
      throws IOException {
    return new Exporter(account, null, outDir, FILE_NAME, format, notYetExportedP, "dd/MM/yyyy", '.', "UTF-8", ';', append, withAccountColumn)
        .export();
  }
}
