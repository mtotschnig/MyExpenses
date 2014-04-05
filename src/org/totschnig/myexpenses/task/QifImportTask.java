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

import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

public class QifImportTask extends AsyncTask<String, String, Void> {
  private final TaskExecutionFragment taskExecutionFragment;
  private QifDateFormat dateFormat;
  private long accountId;
  private final Map<String, Long> payeeToId = new HashMap<String, Long>();
  private final Map<String, Long> categoryToId = new HashMap<String, Long>();
  private final Map<String, QifAccount> accountTitleToAccount = new HashMap<String, QifAccount>();

  public QifImportTask(TaskExecutionFragment taskExecutionFragment,
      QifDateFormat qifDateFormat, long accountId) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.dateFormat = qifDateFormat;
    this.accountId = accountId;
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
  protected Void doInBackground(String... params) {
    long t0 = System.currentTimeMillis();
    QifBufferedReader r;
    try {
      r = new QifBufferedReader(new BufferedReader(new InputStreamReader(
          new FileInputStream(params[0]), "UTF-8")));
    } catch (UnsupportedEncodingException e) {
      return null;
    } catch (FileNotFoundException e) {
      return null;
    }
    QifParser parser = new QifParser(r, dateFormat);
    try {
      parser.parse();
    } catch (IOException e) {
      return null;
    }
    long t1 = System.currentTimeMillis();
    Log.i(MyApplication.TAG, "QIF Import: Parsing done in "
        + TimeUnit.MILLISECONDS.toSeconds(t1 - t0) + "s");
    if (parser.accounts.size() > 1 && accountId != 0) {
      publishProgress(
          this.taskExecutionFragment
              .getString(R.string.qif_parse_failure_found_multiple_accounts)
              + " "
              + this.taskExecutionFragment
                  .getString(R.string.qif_parse_failure_found_multiple_accounts_cannot_merge));
      return(null);
    }
    if (accountId == 0 && !MyApplication.getInstance().isContribEnabled
        && parser.accounts.size() + Account.count(null, null) > 5) {
      publishProgress(
          this.taskExecutionFragment
              .getString(R.string.qif_parse_failure_found_multiple_accounts)
              + " "
              + Html.fromHtml(this.taskExecutionFragment
                  .getString(R.string.contrib_feature_accounts_unlimited_description)
                  + " "
                  + this.taskExecutionFragment
                      .getString(R.string.dialog_contrib_reminder_remove_limitation)));
      return(null);
    }
    publishProgress(this.taskExecutionFragment
        .getString(
            R.string.qif_parse_result,
            String.valueOf(parser.accounts.size()),
            String.valueOf(parser.categories.size()),
            String.valueOf(parser.payees.size())));
    doImport(parser);
    return(null);
  }

  private void doImport(QifParser parser) {
    insertPayees(parser.payees);
    publishProgress("Inserting payees done");
    /*
     * insertProjects(parser.classes); long t2 = System.currentTimeMillis();
     * Log.i(MyApplication.TAG, "QIF Import: Inserting projects done in "+
     * TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
     */
    insertCategories(parser.categories);
    publishProgress("Inserting categories done");
    if (accountId == 0) {
      insertAccounts(parser.accounts);
      publishProgress("Inserting accounts done");
    }
    insertTransactions(parser.accounts);
    publishProgress("Inserting transactions done");
  }

  private void insertPayees(Set<String> payees) {
    for (String payee : payees) {
      Long id = Payee.maybeWrite(payee);
      if (id != null) {
        payeeToId.put(payee, id);
      }
    }
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
      id = Category.find(name, null);
      if (id == -1) {
        id = Category.write(0L, name, null);
      }
      categoryToId.put(name, id);
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
      id = Category.find(childCategoryName, main);
      if (id == -1) {
        id = Category.write(0L, childCategoryName, main);
      }
      categoryToId.put(name, id);
    }
    return id;
  }

  private void insertAccounts(List<QifAccount> accounts) {
    for (QifAccount account : accounts) {
      Account a = account.toAccount(Currency.getInstance("EUR"));
      a.save();
      account.dbAccount = a;
      accountTitleToAccount.put(account.memo, account);
    }
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
      insertTransactions(a, account.transactions);
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

  private void insertTransactions(Account a, List<QifTransaction> transactions) {
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
       t.save();
    }
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