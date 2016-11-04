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

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!Objects.equals(intent.getAction(), Intents.SMS_RECEIVED_ACTION)) {
      return;
    }

    SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
    SmsMessage message = messages[0];
    String from = message.getDisplayOriginatingAddress();
    String body = message.getDisplayMessageBody();

    // Process only messages from SBOL.
    if (!Objects.equals(from, "900")) {
      return;
    }

    // Process only SBOL messages about deposit.
    Matcher matcher = SBOL_DEPOSIT_PATTERN.matcher(body);
    if (!matcher.matches()) {
      return;
    }

    // Parse roubles from SMS message and convert them to kopecks.
    BigDecimal amount = new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(100));

    // Create a new transaction.
    Transaction transaction = new Transaction(0, amount.longValue());
    transaction.setCatId(1L);
    transaction.save();
  }
}
