package org.totschnig.myexpenses.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.SimpleToastActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.widget.AbstractWidgetKt;

import java.util.Collections;

import androidx.annotation.RequiresApi;
import timber.log.Timber;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;

public class ShortcutHelper {
  public static Intent createIntentForNewSplit(Context context) {
    return createIntentForNewTransaction(context, TYPE_SPLIT);
  }

  public static Intent createIntentForNewTransfer(Context context) {
    return createIntentForNewTransaction(context, TYPE_TRANSFER);
  }

  public static Intent createIntentForNewTransaction(Context context, int operationType) {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    intent.setComponent(new ComponentName(context.getPackageName(),
        ExpenseEdit.class.getName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

    Bundle extras = new Bundle();
    extras.putBoolean(AbstractWidgetKt.EXTRA_START_FROM_WIDGET, true);
    extras.putBoolean(AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
    extras.putInt(OPERATION_TYPE, operationType);
    extras.putBoolean(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true);
    intent.putExtras(extras);
    return intent;
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  public static void configureSplitShortcut(Context context, boolean contribEnabled) {
    ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

    Intent intent;
    if (contribEnabled) {
      intent = createIntentForNewSplit(context);
    } else {
      intent = ContribInfoDialogActivity.Companion.getIntentFor(context, ContribFeature.SPLIT_TRANSACTION);
    }
    ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "split")
        .setShortLabel(context.getString(R.string.split_transaction))
        .setIcon(Icon.createWithResource(context, R.drawable.ic_menu_split_shortcut))
        .setIntent(intent)
        .build();
    if (shortcutManager != null) {
      try {
        shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  public static void configureTransferShortcut(Context context, boolean transferEnabled) {
    ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

    Intent intent;
    if (transferEnabled) {
      intent = createIntentForNewTransfer(context);
    } else {
      intent = new Intent(context, SimpleToastActivity.class)
          .setAction(Intent.ACTION_MAIN)
          .putExtra(SimpleToastActivity.KEY_MESSAGE, context.getString(R.string.dialog_command_disabled_insert_transfer));
    }
    ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "transfer")
        .setShortLabel(context.getString(R.string.transfer))
        .setIcon(Icon.createWithResource(context, R.drawable.ic_menu_forward_shortcut))
        .setIntent(intent)
        .build();
    if (shortcutManager != null) {
      try {
        shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
      } catch (Exception e) {
        Timber.e(e);
      }
    }

  }
}
