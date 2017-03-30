package org.totschnig.myexpenses.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.widget.AbstractWidget;

public class ShortcutHelper {
  public static Intent createIntentForNewTransaction(Context context, int operationType) {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    intent.setComponent(new ComponentName(context.getPackageName(),
        ExpenseEdit.class.getName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

    Bundle extras = new Bundle();
    extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
    extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
    extras.putInt(MyApplication.KEY_OPERATION_TYPE, operationType);
    intent.putExtras(extras);
    return intent;
  }
}
