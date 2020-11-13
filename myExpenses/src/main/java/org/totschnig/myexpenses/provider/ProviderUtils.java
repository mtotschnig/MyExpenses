package org.totschnig.myexpenses.provider;

import android.os.Bundle;
import android.text.TextUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.totschnig.myexpenses.export.CategoryInfo;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;

import java.util.HashMap;
import java.util.Map;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.ACCOUNT_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.AMOUNT_MICROS;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.CATEGORY_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.COMMENT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.CURRENCY;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.DATE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.METHOD_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.PAYEE_NAME;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.REFERENCE_NUMBER;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TRANSFER_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;

public class ProviderUtils {
  private ProviderUtils() {
  }

  //TODO add tags to contract
  public static Transaction buildFromExtras(Bundle extras) throws NotImplementedException {
    Transaction transaction;
    switch (extras.getInt(OPERATION_TYPE)) {
      case TYPE_TRANSFER:
        transaction = Transfer.getNewInstance(0);
        long transferAccountId = -1;
        String transferAccountLabel = extras.getString(TRANSFER_ACCOUNT_LABEL);
        if (!TextUtils.isEmpty(transferAccountLabel)) {
          transferAccountId = Account.findAnyOpen(transferAccountLabel);
        }
        if (transferAccountId != -1) {
          transaction.setTransferAccountId(transferAccountId);
          transaction.setLabel(transferAccountLabel);
        }
        break;
      case TYPE_SPLIT:
        throw new NotImplementedException("Building split transaction not yet implemented");
      default: {
        transaction = Transaction.getNewInstance(0);
      }
    }
    long accountId = -1;
    String accountLabel = extras.getString(ACCOUNT_LABEL);
    if (!TextUtils.isEmpty(accountLabel)) {
      accountId = Account.findAnyOpen(accountLabel);
    }
    if (accountId == -1) {
      String currency = extras.getString(CURRENCY);
      if (currency != null) {
        accountId = Account.findAnyByCurrency(currency);
      }
    }
    Account account = Account.getInstanceFromDb(Math.max(accountId, 0));
    if (account == null) {
      return null;
    }
    transaction.setAccountId(account.getId());
    long amountMicros = extras.getLong(AMOUNT_MICROS);
    if (amountMicros != 0) {
      transaction.setAmount(Money.buildWithMicros(account.getCurrencyUnit(), amountMicros));
    }
    long date = extras.getLong(DATE);
    if (date != 0) {
      transaction.setDate(date);
    }
    String payeeName = extras.getString(PAYEE_NAME);
    if (!TextUtils.isEmpty(payeeName)) {
      transaction.setPayee(payeeName);
    }
    if (!(transaction instanceof Transfer)) {
      String categoryLabel = extras.getString(CATEGORY_LABEL);
      if (!TextUtils.isEmpty(categoryLabel)) {
        final Map<String, Long> categoryToId = new HashMap<>();
        new CategoryInfo(categoryLabel).insert(categoryToId, false);
        Long catId = categoryToId.get(categoryLabel);
        if (catId != null) {
          transaction.setCatId(catId);
          transaction.setLabel(categoryLabel);
        }
      }
    }
    String comment = extras.getString(COMMENT);
    if (!TextUtils.isEmpty(comment)) {
      transaction.setComment(comment);
    }
    String methodLabel = extras.getString(METHOD_LABEL);
    if (!TextUtils.isEmpty(methodLabel)) {
      long methodId = PaymentMethod.find(methodLabel);
      if (methodId > -1) {
        transaction.setMethodId(methodId);
      }
    }
    String referenceNumber = extras.getString(REFERENCE_NUMBER);
    if (!TextUtils.isEmpty(referenceNumber)) {
      transaction.setReferenceNumber(referenceNumber);
    }
    return transaction;
  }
}
