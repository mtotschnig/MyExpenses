//Copyright (c) 2010 Denis Solonenko (Financisto)
//made available under the terms of the GNU Public License v2.0
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.totschnig.myexpenses.export.qif.QifUtils.isTransferCategory;
import static org.totschnig.myexpenses.export.qif.QifUtils.parseDate;
import static org.totschnig.myexpenses.export.qif.QifUtils.parseMoney;
import static org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar;

import androidx.annotation.Nullable;

/**
 * Created by IntelliJ IDEA. User: Denis Solonenko Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

  public long id;
  public Date date;
  public BigDecimal amount = new BigDecimal(0);
  @Nullable
  public String payee;
  public String memo;
  @Nullable
  public String category;
  @Nullable
  public String categoryClass;
  public String toAccount;
  public String project;
  public String status;
  public String number;

  public List<QifTransaction> splits;

  public boolean isSplit() {
    return splits != null;
  }

  public boolean isOpeningBalance() {
    return payee != null && payee.equals("Opening Balance");
  }

  public void setSplits(List<QifTransaction> splits) {
    this.splits = splits;
  }

  public void readFrom(QifBufferedReader r, QifDateFormat dateFormat, CurrencyUnit currency)
      throws IOException {
    QifTransaction split = null;
    String line;
    while ((line = r.readLine()) != null) {
      if (line.startsWith("^")) {
        break;
      }
      if (line.startsWith("D")) {
        this.date = parseDate(trimFirstChar(line), dateFormat);
      } else if (line.startsWith("T")) {
        this.amount = parseMoney(trimFirstChar(line), currency);
      } else if (line.startsWith("P")) {
        this.payee = trimFirstChar(line);
      } else if (line.startsWith("M")) {
        this.memo = trimFirstChar(line);
      } else if (line.startsWith("C")) {
        this.status = trimFirstChar(line);
      } else if (line.startsWith("N")) {
        this.number = trimFirstChar(line);
      } else if (line.startsWith("L")) {
        parseCategory(this, line);
      } else if (line.startsWith("S")) {
        addSplit(split);
        split = new QifTransaction();
        parseCategory(split, line);
      } else if (line.startsWith("$")) {
        if (split != null) {
          split.amount = parseMoney(trimFirstChar(line), currency);
        }
      } else if (line.startsWith("E") && split != null) {
        split.memo = trimFirstChar(line);
      }
    }
    addSplit(split);
    adjustSplitsDatetime();
  }

  private void adjustSplitsDatetime() {
    if (splits != null) {
      for (QifTransaction split : splits) {
        split.date = this.date;
      }
    }
  }

  private void parseCategory(QifTransaction t, String line) {
    String category = trimFirstChar(line);
    int i = category.indexOf('/');
    if (i != -1) {
      t.categoryClass = category.substring(i + 1);
      category = category.substring(0, i);
    }
    if (isTransferCategory(category)) {
      t.toAccount = category.substring(1, category.length() - 1);
    } else {
      t.category = category;
    }
  }

  private void addSplit(QifTransaction split) {
    if (split == null) {
      return;
    }
    if (splits == null) {
      splits = new ArrayList<>();
    }
    splits.add(split);
  }

  // public static QifTransaction fromTransaction(Transaction transaction,
  // Map<Long, Category> categoriesMap, Map<Long, Account> accountsMap) {
  // QifTransaction qifTransaction = new QifTransaction();
  // qifTransaction.amount = transaction.amount.getAmountMinor();
  // qifTransaction.memo = transaction.comment;
  // if (transaction.transfer_account != null) {
  // Account toAccount = accountsMap.get(transaction.transfer_account);
  // qifTransaction.toAccount = toAccount.label;
  // //TODO: test if from and to accounts have different currencies
  // }
  // Category category = categoriesMap.get(transaction.catId);
  // if (category != null) {
  // QifCategory qifCategory = QifCategory.fromCategory(category);
  // qifTransaction.category = qifCategory.name;
  // }
  // qifTransaction.isSplit = transaction.parentId != null;
  // return qifTransaction;
  // }

  public Transaction toTransaction(Account a) {
    Transaction t;
    Money m = new Money(a.getCurrencyUnit(), amount);
    if (isSplit()) {
      t = new SplitTransaction(a.getId(), m);
    } else if (isTransfer()) {
      t = new Transfer(a.getId(), m);
    } else {
      t = new Transaction(a.getId(), m);
    }
    if (date != null) {
      t.setDate(date);
    }
    t.setComment(memo);
    t.setCrStatus(CrStatus.fromQifName(status));
    t.setReferenceNumber(number);
    return t;
  }

  public boolean isTransfer() {
    return toAccount != null;
  }
}
