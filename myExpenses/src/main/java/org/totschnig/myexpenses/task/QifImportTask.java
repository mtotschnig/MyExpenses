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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.export.qif.QifAccount;
import org.totschnig.myexpenses.export.qif.QifBufferedReader;
import org.totschnig.myexpenses.export.qif.QifCategory;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.export.qif.QifParser;
import org.totschnig.myexpenses.export.qif.QifTransaction;
import org.totschnig.myexpenses.export.qif.QifUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

public class QifImportTask extends AsyncTask<Void, String, Void> {
  private final TaskExecutionFragment taskExecutionFragment;
  private QifDateFormat dateFormat;
  private String encoding;
  private long accountId;
  private int totalCategories = 0;
  private final Map<String, Long> payeeToId = new HashMap<>();
  private final Map<String, Long> categoryToId = new HashMap<>();
  private final Map<String, QifAccount> accountTitleToAccount = new HashMap<>();
  Uri fileUri;
  /**
   * should we handle parties/categories?
   */
  boolean withPartiesP, withCategoriesP, withTransactionsP;

  private CurrencyUnit currencyUnit;

  public QifImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.dateFormat = (QifDateFormat) b.getSerializable(TaskExecutionFragment.KEY_DATE_FORMAT);
    this.accountId = b.getLong(DatabaseConstants.KEY_ACCOUNTID);
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    this.withPartiesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_PARTIES);
    this.withCategoriesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_CATEGORIES);
    this.withTransactionsP = b.getBoolean(TaskExecutionFragment.KEY_WITH_TRANSACTIONS);
    this.currencyUnit = (CurrencyUnit) b.getSerializable(DatabaseConstants.KEY_CURRENCY);
    this.encoding = b.getString(TaskExecutionFragment.KEY_ENCODING);
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
      for (String progress : values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }

  @Override
  protected Void doInBackground(Void... params) {
    final MyApplication application = MyApplication.getInstance();
    final Context context = ContextHelper.wrap(application, application.getAppComponent().userLocaleProvider().getUserPreferredLocale());
    long t0 = System.currentTimeMillis();
    QifBufferedReader r;
    QifParser parser;
    ContentResolver contentResolver = application.getContentResolver();
    try {
      InputStream inputStream = contentResolver.openInputStream(fileUri);
      r = new QifBufferedReader(
          new BufferedReader(
              new InputStreamReader(
                  inputStream,
                  encoding)));
    } catch (FileNotFoundException e) {
      publishProgress(context.getString(R.string.parse_error_file_not_found, fileUri));
      return null;
    } catch (Exception e) {
      publishProgress(context.getString(R.string.parse_error_other_exception, e.getMessage()));
      return null;
    }
    parser = new QifParser(r, dateFormat, currencyUnit);
    try {
      parser.parse();
      long t1 = System.currentTimeMillis();
      Timber.i("QIF Import: Parsing done in %d s", TimeUnit.MILLISECONDS.toSeconds(t1 - t0));
      publishProgress(MyApplication.getInstance()
          .getString(
              R.string.qif_parse_result,
              String.valueOf(parser.accounts.size()),
              String.valueOf(parser.categories.size()),
              String.valueOf(parser.payees.size())));
      contentResolver.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_BULK_START, null, null);
      doImport(parser, context);
      contentResolver.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_BULK_END, null, null);
      return (null);
    } catch (IOException | IllegalArgumentException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_other_exception, e.getMessage()));
      return null;
    } finally {
      try {
        r.close();
      } catch (IOException e) {
        Timber.e(e);
      }
    }
  }

/*  private String detectEncoding(InputStream inputStream) throws IOException {
    byte[] buf = new byte[4096];

    // (1)
    UniversalDetector detector = new UniversalDetector(null);

    // (2)
    int nread;
    while ((nread = inputStream.read(buf)) > 0 && !detector.isDone()) {
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
    return encoding;
  }*/

  private void doImport(QifParser parser, Context context) {
    if (withPartiesP) {
      int totalParties = insertPayees(parser.payees);
      publishProgress(totalParties == 0 ?
          context.getString(R.string.import_parties_none) :
          context.getString(R.string.import_parties_success, String.valueOf(totalParties)));
    }
    /*
     * insertProjects(parser.classes); long t2 = System.currentTimeMillis();
     * Log.i(MyApplication.TAG, "QIF Import: Inserting projects done in "+
     * TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
     */
    if (withCategoriesP) {
      insertCategories(parser.categories);
      publishProgress(totalCategories == 0 ?
          context.getString(R.string.import_categories_none) :
          context.getString(R.string.import_categories_success,
              String.valueOf(totalCategories)));
    }
    if (withTransactionsP) {
      if (accountId == 0) {
        int importedAccounts = insertAccounts(parser.accounts, context);
        publishProgress(importedAccounts == 0 ?
            context.getString(R.string.import_accounts_none) :
            context.getString(R.string.import_accounts_success, String.valueOf(importedAccounts)));
      } else {
        if (parser.accounts.size() > 1) {
          publishProgress(
              context.getString(R.string.qif_parse_failure_found_multiple_accounts)
                  + " "
                  + context.getString(R.string.qif_parse_failure_found_multiple_accounts_cannot_merge));
          return;
        }
        if (parser.accounts.isEmpty()) {
          return;
        }
        Account dbAccount = Account.getInstanceFromDb(accountId);
        parser.accounts.get(0).dbAccount = dbAccount;
        if (dbAccount == null) {
          CrashHandler.report("Exception during QIF import. Did not get instance from DB for id "
              + accountId);
        }
      }
      insertTransactions(parser.accounts, context);
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
      totalCategories += category.insert(categoryToId, true);
    }
  }

  private int insertAccounts(List<QifAccount> accounts, Context context) {
    int nrOfAccounts = Account.count(null, null);

    int importCount = 0;
    for (QifAccount account : accounts) {
      LicenceHandler licenceHandler = ((MyApplication) taskExecutionFragment.requireContext().getApplicationContext()).getAppComponent().licenceHandler();
      if (!licenceHandler.hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED)
          && nrOfAccounts + importCount > 5) {
        publishProgress(
            context.getString(R.string.qif_parse_failure_found_multiple_accounts) + " " +
                ContribFeature.ACCOUNTS_UNLIMITED.buildUsageLimitString(context) +
                ContribFeature.ACCOUNTS_UNLIMITED.buildRemoveLimitation(context, false));
        break;
      }
      long dbAccountId = TextUtils.isEmpty(account.memo) ? -1 : Account.findAnyOpen(account.memo);
      if (dbAccountId != -1) {
        Account dbAccount = Account.getInstanceFromDb(dbAccountId);
        account.dbAccount = dbAccount;
        if (dbAccount == null) {
          CrashHandler.report("Exception during QIF import. Did not get instance from DB for id " +
              dbAccountId);
        }
      } else {
        Account a = account.toAccount(currencyUnit);
        if (TextUtils.isEmpty(a.getLabel())) {
          String displayName = DialogUtils.getDisplayName(fileUri);
          if (FileUtils.getExtension(displayName).equalsIgnoreCase(".qif")) {
            displayName = displayName.substring(0, displayName.lastIndexOf('.'));
          }
          displayName = displayName.replace('-', ' ').replace('_', ' ');
          a.setLabel(displayName);
        }
        if (a.save() != null)
          importCount++;
        account.dbAccount = a;
      }
      accountTitleToAccount.put(account.memo, account);
    }
    return importCount;
  }

  private void insertTransactions(List<QifAccount> accounts, Context context) {
    long t0 = System.currentTimeMillis();
    reduceTransfers(accounts);
    long t1 = System.currentTimeMillis();
    Timber.i("QIF Import: Reducing transfers done in %d s", TimeUnit.MILLISECONDS.toSeconds(t1 - t0));
    convertUnknownTransfers(accounts);
    long t2 = System.currentTimeMillis();
    Timber.i("QIF Import: Converting transfers done in %d s", TimeUnit.MILLISECONDS.toSeconds(t2 - t1));
    int count = accounts.size();
    for (int i = 0; i < count; i++) {
      long t3 = System.currentTimeMillis();
      QifAccount account = accounts.get(i);
      Account a = account.dbAccount;
      int countTransactions = 0;
      if (a != null) {
        countTransactions = insertTransactions(a, account.transactions);
        publishProgress(countTransactions == 0 ?
            context.getString(R.string.import_transactions_none, a.getLabel()) :
            context.getString(R.string.import_transactions_success, countTransactions, a.getLabel()));
      } else {
        publishProgress("Unable to import into QIF account " + account.memo + ". No matching database account found");
      }
      // this might help GC
      account.transactions.clear();
      long t4 = System.currentTimeMillis();
      Timber.i("QIF Import: Inserting %d transactions for account %d/%d done in %d s",
          countTransactions, i, count, TimeUnit.MILLISECONDS.toSeconds(t4 - t3));
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
      if (fromTransaction.isTransfer() && fromTransaction.amount.signum() == -1) {
        boolean found = false;
        if (!fromTransaction.toAccount.equals(fromAccount.memo)) {
          QifAccount toAccount = accountTitleToAccount
              .get(fromTransaction.toAccount);
          if (toAccount != null) {
            Iterator<QifTransaction> iterator = toAccount.transactions
                .iterator();
            while (iterator.hasNext()) {
              QifTransaction toTransaction = iterator.next();
              if (QifUtils.twoSidesOfTheSameTransfer(fromAccount, fromTransaction,
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
      if (transaction.isTransfer() && transaction.amount.signum() >= 0) {
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

  private int insertTransactions(Account a, List<QifTransaction> transactions) {
    int count = 0;
    for (QifTransaction transaction : transactions) {
      Transaction t = transaction.toTransaction(a);
      t.setPayeeId(findPayee(transaction.payee));
      // t.projectId = findProject(transaction.categoryClass);
      findToAccount(transaction, t);

      if (transaction.splits != null) {
        t.save();
        for (QifTransaction split : transaction.splits) {
          Transaction s = split.toTransaction(a);
          s.setParentId(t.getId());
          s.setStatus(STATUS_UNCOMMITTED);
          findToAccount(split, s);
          findCategory(split, s);
          s.save();
        }
      } else {
        findCategory(transaction, t);
      }
      t.save(true);
      count++;
    }
    return count;
  }

  private void findToAccount(QifTransaction transaction, Transaction t) {
    if (transaction.isTransfer()) {
      Account toAccount = findAccount(transaction.toAccount);
      if (toAccount != null) {
        t.setTransferAccountId(toAccount.getId());
      }
    }
  }

  private Account findAccount(String account) {
    QifAccount a = accountTitleToAccount.get(account);
    return a != null ? a.dbAccount : null;
  }

  public Long findPayee(String payee) {
    return findIdInAMap(payee, payeeToId);
  }

  private Long findIdInAMap(String project, Map<String, Long> map) {
    if (map.containsKey(project)) {
      return map.get(project);
    }
    return null;
  }

  private void findCategory(QifTransaction transaction, Transaction t) {
    t.setCatId(categoryToId.get(transaction.category));
  }
}