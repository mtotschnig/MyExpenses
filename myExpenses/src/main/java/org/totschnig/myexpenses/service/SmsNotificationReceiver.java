package org.totschnig.myexpenses.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

import org.totschnig.myexpenses.model.Transaction;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses SMS notifications from banks and creates new transactions.
 */
public class SmsNotificationReceiver extends BroadcastReceiver {
    // Deposit from Sberbank Online (SBOL).
    private static final Pattern SBOL_DEPOSIT_PATTERN = Pattern.compile("^.*перевел\\(а\\) Вам ([0-9]+\\.[0-9]+) .*$");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\b RUR|USD|рублей \\b");

    private String from;
    private String body;
    private Matcher match_sbol;
    private Matcher match_money;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Objects.equals(intent.getAction(), Intents.SMS_RECEIVED_ACTION)) {
            return;
        }

        SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
        SmsMessage message = messages[0];
        from = message.getDisplayOriginatingAddress();
        body = message.getDisplayMessageBody();


        // Process only messages from SBOL.
        switch (from) {
            case "900":
            case "Alfa-Bank":
            case "VTB24":
            case "Tinkoff":
                match_sbol = SBOL_DEPOSIT_PATTERN.matcher(body);
                match_money = MONEY_PATTERN.matcher(body);
                break;
            default:
                return;
        }


        // Process only SBOL messages about deposit.
        BigDecimal amount;
        if (match_sbol.matches()) {
            // Parse roubles from SMS message and convert them to kopecks.
            amount = new BigDecimal(match_sbol.group(1)).multiply(BigDecimal.valueOf(100));
        } else if (match_money.matches()) {
            amount = new BigDecimal(match_money.group(1)).multiply(BigDecimal.valueOf(100));
        } else return;

        // Create a new transaction.
        Transaction transaction = new Transaction(0, amount.longValue());
        transaction.setCatId(1L);
        transaction.save();
    }
}
