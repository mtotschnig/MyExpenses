package org.totschnig.myexpenses.util;

import android.content.ContentResolver;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;

import java.util.Currency;

public class SampleDataHelper {

    // Add a single sample transaction
    public static void insertSampleTransaction(ContentResolver contentResolver) {
        Transaction transaction = new Transaction();

        // Set sample data fields
        transaction.setAmount(new Money(1000, Currency.getInstance("USD"))); // $10.00
        transaction.setPayee("Sample Payee");
        transaction.setComment("This is a sample transaction");
        transaction.setDate(System.currentTimeMillis() / 1000); // seconds since epoch
        transaction.setAccountId(1); // You may need to select a valid account ID

        // Save to DB
        transaction.save(contentResolver);
    }

    // Add multiple sample transactions
    public static void insertSampleTransactions(ContentResolver contentResolver) {
        for (int i = 0; i < 5; i++) {
            Transaction transaction = new Transaction();
            transaction.setAmount(new Money(500 + i * 100, Currency.getInstance("USD")));
            transaction.setPayee("Sample Payee #" + (i + 1));
            transaction.setComment("Sample transaction " + (i + 1));
            transaction.setDate(System.currentTimeMillis() / 1000 - (i * 86400)); // Days ago
            transaction.setAccountId(1); // Update as appropriate
            transaction.save(contentResolver);
        }
    }
}