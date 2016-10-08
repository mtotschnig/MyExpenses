package org.totschnig.myexpenses.export.qif;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.Date;

public class QifUtilTwoSidesOfTheSameTransferTest extends TestCase {

  public void testShouldMatchTwoSidesOfSameTransfer() {
    Date now = new Date(System.currentTimeMillis());

    QifAccount fromAccount = new QifAccount();
    fromAccount.memo = "Konto 1";
    QifAccount toAccount = new QifAccount();
    toAccount.memo = "Konto 2";
    QifTransaction fromTransaction = new QifTransaction();
    fromTransaction.toAccount = toAccount.memo;
    fromTransaction.date = now;
    fromTransaction.amount = new BigDecimal(5);
    QifTransaction toTransaction = new QifTransaction();
    toTransaction.toAccount = fromAccount.memo;
    toTransaction.date = now;
    toTransaction.amount = new BigDecimal(-5);
    Assert.assertTrue(QifUtils.twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction));
  }

  public void testShouldDistinguishNonMatchingAmounts() {
    Date now = new Date(System.currentTimeMillis());

    QifAccount fromAccount = new QifAccount();
    fromAccount.memo = "Konto 1";
    QifAccount toAccount = new QifAccount();
    toAccount.memo = "Konto 2";
    QifTransaction fromTransaction = new QifTransaction();
    fromTransaction.toAccount = toAccount.memo;
    fromTransaction.date = now;
    fromTransaction.amount = new BigDecimal(5);
    QifTransaction toTransaction = new QifTransaction();
    toTransaction.toAccount = fromAccount.memo;
    toTransaction.date = now;
    toTransaction.amount = new BigDecimal(-6);
    Assert.assertFalse(QifUtils.twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction));
  }

  public void testShouldDistinguishNonMatchingDates() {
    Date now = new Date(System.currentTimeMillis());

    QifAccount fromAccount = new QifAccount();
    fromAccount.memo = "Konto 1";
    QifAccount toAccount = new QifAccount();
    toAccount.memo = "Konto 2";
    QifTransaction fromTransaction = new QifTransaction();
    fromTransaction.toAccount = toAccount.memo;
    fromTransaction.date = now;
    fromTransaction.amount = new BigDecimal(5);
    QifTransaction toTransaction = new QifTransaction();
    toTransaction.toAccount = fromAccount.memo;
    toTransaction.date = new Date(System.currentTimeMillis()-100000);
    toTransaction.amount = new BigDecimal(-5);
    Assert.assertFalse(QifUtils.twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction));
  }

  public void testShouldDistinguishNonMatchingAccounts() {
    Date now = new Date(System.currentTimeMillis());

    QifAccount fromAccount = new QifAccount();
    fromAccount.memo = "Konto 1";
    QifAccount toAccount = new QifAccount();
    toAccount.memo = "Konto 2";
    QifTransaction fromTransaction = new QifTransaction();
    fromTransaction.toAccount = toAccount.memo;
    fromTransaction.date = now;
    fromTransaction.amount = new BigDecimal(5);
    QifTransaction toTransaction = new QifTransaction();
    toTransaction.toAccount = "Konto 3";
    toTransaction.date = now;
    toTransaction.amount = new BigDecimal(-5);
    Assert.assertFalse(QifUtils.twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction));
  }
}
