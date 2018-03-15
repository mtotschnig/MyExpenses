package org.totschnig.myexpenses.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.lang.ref.WeakReference;

public class ProtectedCursorLoader extends CursorLoader {
  private WeakReference<FragmentActivity> activityReference;

  public ProtectedCursorLoader(FragmentActivity context, Uri uri) {
    super(context, uri, null, null, null, null);
    activityReference = new WeakReference<>(context);
  }

  @Override
  public Cursor loadInBackground() {
    try {
      return super.loadInBackground();
    } catch (TransactionDatabase.SQLiteDowngradeFailedException |
        TransactionDatabase.SQLiteUpgradeFailedException e) {
      //TODO this currently is dead code (with the exception of Gingerbread) since database is initialized in TASK_INIT
      //TODO evaluate if this is still relevant and needs to be migrated to handling of TASK_INIT
      CrashHandler.report(e);
      String msg = e instanceof TransactionDatabase.SQLiteDowngradeFailedException ?
          ("Database cannot be downgraded from a newer version. Please either uninstall MyExpenses, " +
              "before reinstalling, or upgrade to a new version.") :
          "Database upgrade failed. Please contact support@myexpenses.mobi !";
      MessageDialogFragment f = MessageDialogFragment.newInstance(
          0,
          msg,
          new MessageDialogFragment.Button(android.R.string.ok, R.id.QUIT_COMMAND, null),
          null,
          null);
      f.setCancelable(false);
      f.show(activityReference.get().getSupportFragmentManager(), "DOWN_OR_UP_GRADE");
      return null;
    } catch (SQLiteException e) {
      String msg = String.format(
          "Loading of transactions failed (%s). Probably the sum of the entered amounts exceeds the storage limit !"
          , e.getMessage());
      MessageDialogFragment f = MessageDialogFragment.newInstance(
          0,
          msg,
          new MessageDialogFragment.Button(android.R.string.ok, R.id.QUIT_COMMAND, null),
          null,
          null);
      f.setCancelable(false);
      f.show(activityReference.get().getSupportFragmentManager(), "SQLITE_EXCEPTION");
      return null;
    }
  }
}
