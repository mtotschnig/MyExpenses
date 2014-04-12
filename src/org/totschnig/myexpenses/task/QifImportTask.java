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
//based on Financisto

package org.totschnig.myexpenses.task;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.mozilla.universalchardet.UniversalDetector;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.qif.QifAccount;
import org.totschnig.myexpenses.export.qif.QifBufferedReader;
import org.totschnig.myexpenses.export.qif.QifCategory;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.export.qif.QifParser;
import org.totschnig.myexpenses.export.qif.QifTransaction;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class QifImportTask extends AsyncTask<Void, String, Void> {
  private final TaskExecutionFragment taskExecutionFragment;
  private QifDateFormat dateFormat;
  private long accountId;
  private int totalCategories=0;
  private final Map<String, Long> payeeToId = new HashMap<String, Long>();
  private final Map<String, Long> categoryToId = new HashMap<String, Long>();
  private final Map<String, QifAccount> accountTitleToAccount = new HashMap<String, QifAccount>();
  String filePath;
  /**
   * should we handle parties/categories?
   */
  boolean withPartiesP, withCategoriesP, withTransactionsP;

  public QifImportTask(TaskExecutionFragment taskExecutionFragment,Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.dateFormat = (QifDateFormat) b.getSerializable(TaskExecutionFragment.KEY_DATE_FORMAT);
    this.accountId = b.getLong(DatabaseConstants.KEY_ACCOUNTID);
    this.filePath = b.getString(TaskExecutionFragment.KEY_FILE_PATH);
    this.withPartiesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_PARTIES);
    this.withCategoriesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_CATEGORIES);
    this.withTransactionsP = b.getBoolean(TaskExecutionFragment.KEY_WITH_TRANSACTIONS);
  }

  @Override
  protected void onPostExecute(Void result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_QIF_IMPORT, null);
    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      for (String progress: values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }

  @Override
  protected Void doInBackground(Void... params) {
    long t0 = System.currentTimeMillis();
    QifBufferedReader r;
    QifParser parser;
    try {
      String encoding = detectEncoding(filePath);
      Log.i("DEBUG","Encoding : " + encoding);
      r = new QifBufferedReader(new BufferedReader(new InputStreamReader(
          new FileInputStream(filePath), encoding != null ? encoding : "UTF-8")));
    } catch (FileNotFoundException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_file_not_found,filePath));
      return null;
    } catch (IOException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_other_exception,e.getMessage()));
      return null;
    }
    parser = new QifParser(r, dateFormat);
    try {
      parser.parse();
      long t1 = System.currentTimeMillis();
      Log.i(MyApplication.TAG, "QIF Import: Parsing done in "
          + TimeUnit.MILLISECONDS.toSeconds(t1 - t0) + "s");
      publishProgress(MyApplication.getInstance()
          .getString(
              R.string.qif_parse_result,
              String.valueOf(parser.accounts.size()),
              String.valueOf(parser.categories.size()),
              String.valueOf(parser.payees.size())));
      doImport(parser);
      return(null);
    } catch (IOException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_other_exception,e.getMessage()));
      return null;
    } finally {
      try {
        r.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private String detectEncoding(String filePath) throws IOException {
    byte[] buf = new byte[4096];
    java.io.FileInputStream fis = new java.io.FileInputStream(filePath);

    // (1)
    UniversalDetector detector = new UniversalDetector(null);

    // (2)
    int nread;
    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
      detector.handleData(buf, 0, nread);
    }
    // (3)
    detector.dataEnd();

    // (4)
    String encoding = detector.getDetectedCharset();
    if (encoding != null) {
      System.out.println("Detected encoding = " + encoding);
    } else {
      System.out.println("No encoding detected.");
    }

    // (5)
    detector.reset();
    fis.close();
    return encoding;
  }

  private void doImport(QifParser parser) {
    if (withPartiesP) {
      int totalParties = insertPayees(parser.payees);
      publishProgress(totalParties == 0 ? 
          MyApplication.getInstance().getString(R.string.import_parties_none):
          MyApplication.getInstance().getString(R.string.import_parties_success,totalParties));
    }
    /*
     * insertProjects(parser.classes); long t2 = System.currentTimeMillis();
     * Log.i(MyApplication.TAG, "QIF Import: Inserting projects done in "+
     * TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
     */
    if (withCategoriesP) {
      insertCategories(parser.categories);
      publishProgress(totalCategories == 0 ? 
        MyApplication.getInstance().getString(R.string.import_categories_none):
        MyApplication.getInstance().getString(R.string.import_categories_success,totalCategories));
    }
    if (withTransactionsP) {
      if (accountId == 0) {
        if (!MyApplication.getInstance().isContribEnabled
            && parser.accounts.size() + Account.count(null, null) > 5) {
          publishProgress(
              MyApplication.getInstance()
                .getString(R.string.qif_parse_failure_found_multiple_accounts)
              + " "
              + MyApplication.getInstance()
                .getText(R.string.contrib_feature_accounts_unlimited_description)
              + " "
              + MyApplication.getInstance()
                .getText(R.string.dialog_contrib_reminder_remove_limitation));
          return;
        }
        int totalAccounts = insertAccounts(parser.accounts);
        publishProgress(totalAccounts == 0 ? 
          MyApplication.getInstance().getString(R.string.import_accounts_none):
          MyApplication.getInstance().getString(R.string.import_accounts_success,totalAccounts));
      } else {
        if (parser.accounts.size() > 1) {
          publishProgress(
              MyApplication.getInstance()
                  .getString(R.string.qif_parse_failure_found_multiple_accounts)
                  + " "
                  + MyApplication.getInstance()
                      .getString(R.string.qif_parse_failure_found_multiple_accounts_cannot_merge));
          return;
        }
      }
      insertTransactions(parser.accounts);
    }
  }

  private int insertPayees(Set<String> payees) {
    int count = 0;
    for (String payee : payees) {
      Long id = payeeToId.get(payee);
      if (id == null) {
        id = Payee.find(payee);
        if (id == -1) {
          id = Payee.maybeWrite(payee);
          if (id != -1)
            count++;
        }
        if (id != -1) {
          payeeToId.put(payee, id);
        }
      }
    }
    return count;
  }

  private void insertCategories(Set<QifCategory> categories) {
    for (QifCategory category : categories) {
      String name = extractCategoryName(category.name);
      insertCategory(name);
    }
  }

  private String extractCategoryName(String name) {
    int i = name.indexOf('/');
    if (i != -1) {
      name = name.substring(0, i);
    }
    return name;
  }

  private void insertCategory(String name) {
    if (isChildCategory(name)) {
      insertChildCategory(name);
    } else {
      insertRootCategory(name);
    }
  }

  private boolean isChildCategory(String name) {
    return name.contains(":");
  }

  private Long insertRootCategory(String name) {
    Long id = categoryToId.get(name);
    if (id == null) {
      id = maybeWriteCategory(name,null);
      if (id != -1)
        categoryToId.put(name, id);
    }
    return id;
  }
  private Long maybeWriteCategory(String name,Long parentId) {
    Long id = Category.find(name, null);
    if (id == -1) {
      id = Category.write(0L, name, null);
      if (id != -1)
        totalCategories++;
    }
    return id;
  }

  private Long insertChildCategory(String name) {
    Long id = categoryToId.get(name);
    if (id == null) {
      int i = name.lastIndexOf(':');
      String parentCategoryName = name.substring(0, i);
      String childCategoryName = name.substring(i + 1);
      Long main = insertRootCategory(parentCategoryName);
      if (main != -1) {
        id = maybeWriteCategory(childCategoryName, main);
        if (id != -1)
          categoryToId.put(name, id);
      }
    }
    return id;
  }

  private int insertAccounts(List<QifAccount> accounts) {
    int count = 0;
    for (QifAccount account : accounts) {
      Account a = account.toAccount(Currency.getInstance("EUR"));
      if (a.save() != null)
        count++;
      account.dbAccount = a;
      accountTitleToAccount.put(account.memo, account);
    }
    return count;
  }

  private void insertTransactions(List<QifAccount> accounts) {
    long t0 = System.currentTimeMillis();
    reduceTransfers(accounts);
    long t1 = System.currentTimeMillis();
    Log.i(MyApplication.TAG, "QIF Import: Reducing transfers done in "
        + TimeUnit.MILLISECONDS.toSeconds(t1 - t0) + "s");
    convertUnknownTransfers(accounts);
    long t2 = System.currentTimeMillis();
    Log.i(MyApplication.TAG, "QIF Import: Converting transfers done in "
        + TimeUnit.MILLISECONDS.toSeconds(t2 - t1) + "s");
    int count = accounts.size();
    for (int i = 0; i < count; i++) {
      long t3 = System.currentTimeMillis();
      QifAccount account = accounts.get(i);
      Account a = account.dbAccount;
      int countTransactions = insertTransactions(a, account.transactions);
      publishProgress(countTransactions == 0 ? 
          MyApplication.getInstance().getString(R.string.import_transactions_none,account.memo):
          MyApplication.getInstance().getString(R.string.import_transactions_success,countTransactions,account.memo));
      // this might help GC
      account.transactions.clear();
      long t4 = System.currentTimeMillis();
      Log.i(MyApplication.TAG,
          "QIF Import: Inserting transactions for account " + i + "/" + count
              + " done in " + TimeUnit.MILLISECONDS.toSeconds(t4 - t3) + "s");
    }
  }

  private void reduceTransfers(List<QifAccount> accounts) {
    for (QifAccount fromAccount : accounts) {
      List<QifTransaction> transactions = fromAccount.transactions;
      reduceTransfers(fromAccount, transactions);
    }
  }

  private void reduceTransfers(QifAccount fromAccount,
      List<QifTransaction> transactions) {
    for (QifTransaction fromTransaction : transactions) {
      if (fromTransaction.isTransfer() && fromTransaction.amount < 0) {
        boolean found = false;
        if (!fromTransaction.toAccount.equals(fromAccount.memo)) {
          QifAccount toAccount = accountTitleToAccount
              .get(fromTransaction.toAccount);
          if (toAccount != null) {
            Iterator<QifTransaction> iterator = toAccount.transactions
                .iterator();
            while (iterator.hasNext()) {
              QifTransaction toTransaction = iterator.next();
              if (twoSidesOfTheSameTransfer(fromAccount, fromTransaction,
                  toAccount, toTransaction)) {
                iterator.remove();
                found = true;
                break;
              }
            }
          }
        }
        if (!found) {
          convertIntoRegularTransaction(fromTransaction);
        }
      }
      if (fromTransaction.splits != null) {
        reduceTransfers(fromAccount, fromTransaction.splits);
      }
    }
  }

  private void convertUnknownTransfers(List<QifAccount> accounts) {
    for (QifAccount fromAccount : accounts) {
      List<QifTransaction> transactions = fromAccount.transactions;
      convertUnknownTransfers(fromAccount, transactions);
    }
  }

  private void convertUnknownTransfers(QifAccount fromAccount,
      List<QifTransaction> transactions) {
    for (QifTransaction transaction : transactions) {
      if (transaction.isTransfer() && transaction.amount >= 0) {
        convertIntoRegularTransaction(transaction);
      }
      if (transaction.splits != null) {
        convertUnknownTransfers(fromAccount, transaction.splits);
      }
    }
  }

  private String prependMemo(String prefix, QifTransaction fromTransaction) {
    if (TextUtils.isEmpty(fromTransaction.memo)) {
      return prefix;
    } else {
      return prefix + " | " + fromTransaction.memo;
    }
  }

  private void convertIntoRegularTransaction(QifTransaction fromTransaction) {
    fromTransaction.memo = prependMemo(
        "Transfer: " + fromTransaction.toAccount, fromTransaction);
    fromTransaction.toAccount = null;
  }

  private boolean twoSidesOfTheSameTransfer(QifAccount fromAccount,
      QifTransaction fromTransaction, QifAccount toAccount,
      QifTransaction toTransaction) {
    return toTransaction.isTransfer()
        && toTransaction.toAccount.equals(fromAccount.memo)
        && fromTransaction.toAccount.equals(toAccount.memo)
        && fromTransaction.date.equals(toTransaction.date)
        && fromTransaction.amount == -toTransaction.amount;
  }

  private int insertTransactions(Account a, List<QifTransaction> transactions) {
    int count = 0;
    for (QifTransaction transaction : transactions) {
      Transaction t = transaction.toTransaction(a.id);
      t.payeeId = findPayee(transaction.payee);
      // t.projectId = findProject(transaction.categoryClass);
      findToAccount(transaction, t);

       if (transaction.splits != null) {
         ((SplitTransaction) t).persistForEdit();
         for (QifTransaction split : transaction.splits) {
           Transaction s = split.toTransaction(a.id);
           s.parentId = t.id;
           s.status = org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
           findToAccount(split, s);
           findCategory(split, s);
           s.save();
         }
       } else {
         findCategory(transaction, t);
       }
       if (t.save() != null)
         count++;
    }
    return count;
  }

  private void findToAccount(QifTransaction transaction, Transaction t) {
    if (transaction.isTransfer()) {
      Account toAccount = findAccount(transaction.toAccount);
      if (toAccount != null) {
        t.transfer_account = toAccount.id;
      }
    }
  }

  private Account findAccount(String account) {
    QifAccount a = accountTitleToAccount.get(account);
    return a != null ? a.dbAccount : null;
  }

  public long findPayee(String payee) {
    return findIdInAMap(payee, payeeToId);
  }

  private long findIdInAMap(String project, Map<String, Long> map) {
    if (map.containsKey(project)) {
      return map.get(project);
    }
    return 0;
  }

  private void findCategory(QifTransaction transaction, Transaction t) {
    t.catId = categoryToId.get(transaction.category);
  }
}