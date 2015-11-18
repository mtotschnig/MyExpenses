package org.totschnig.myexpenses.activity;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.Criteria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.Map;

public abstract class LaunchActivity extends ProtectedFragmentActivity {

  /**
   * check if this is the first invocation of a new version
   * in which case help dialog is presented
   * also is used for hooking version specific upgrade procedures
   * and display information to be presented upon app launch
   */
  public void newVersionCheck() {
    int prev_version = MyApplication.PrefKey.CURRENT_VERSION.getInt(-1);
    int current_version = CommonCommands.getVersionNumber(this);
    if (prev_version < current_version) {
      if (prev_version == -1) {
        return;
      }
      MyApplication.PrefKey.CURRENT_VERSION.putInt(current_version);
      SharedPreferences settings = MyApplication.getInstance().getSettings();
      Editor edit = settings.edit();
      if (prev_version < 19) {
        edit.putString(MyApplication.PrefKey.SHARE_TARGET.getKey(),settings.getString("ftp_target",""));
        edit.remove("ftp_target");
        edit.commit();
      }
      if (prev_version < 28) {
        Log.i("MyExpenses", String.format("Upgrading to version 28: Purging %d transactions from datbase",
            getContentResolver().delete(TransactionProvider.TRANSACTIONS_URI,
                KEY_ACCOUNTID + " not in (SELECT _id FROM accounts)", null)));
      }
      if (prev_version < 30) {
        if (MyApplication.PrefKey.SHARE_TARGET.getString("") != "") {
          edit.putBoolean(MyApplication.PrefKey.SHARE_TARGET.getKey(),true).commit();
        }
      }
      if (prev_version < 40) {
        //this no longer works since we migrated time to utc format
        //  DbUtils.fixDateValues(getContentResolver());
        //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
        //if they are already above both tresholds, so we set some delay
        edit.putLong("nextReminderContrib", Transaction.getSequenceCount() + 23).commit();
      }
      if (prev_version < 132) {
        MyApplication.getInstance().showImportantUpgradeInfo = true;
      }
      if (prev_version < 163) {
       edit.remove("qif_export_file_encoding").commit();
      }
      if (prev_version < 199) {
        //filter serialization format has changed
        for (Map.Entry<String, ?> entry : settings.getAll().entrySet()) {
          String key = entry.getKey();
          String[] keyParts = key.split("_");
          if (keyParts[0].equals("filter")) {
            String val = settings.getString(key,"");
            switch (keyParts[1]) {
              case "method":
              case "payee":
              case "cat":
                int sepIndex = val.indexOf(";");
                edit.putString(key,val.substring(sepIndex+1)+";"+Criteria.escapeSeparator(val.substring(0, sepIndex)));
                break;
              case "cr":
                edit.putString(key, Transaction.CrStatus.values()[Integer.parseInt(val)].name());
                break;
            }
          }
        }
        edit.commit();
      }
      if (prev_version < 202) {
        String appDir = MyApplication.PrefKey.APP_DIR.getString(null);
        if (appDir!=null) {
          MyApplication.PrefKey.APP_DIR.putString(Uri.fromFile(new File(appDir)).toString());
        }
      }
      if (prev_version < 221) {
        MyApplication.PrefKey.SORT_ORDER.putString(
            MyApplication.PrefKey.CATEGORIES_SORT_BY_USAGES_LEGACY.getBoolean(true) ?
                "USAGES" : "ALPHABETIC");
      }
      VersionDialogFragment.newInstance(prev_version)
        .show(getSupportFragmentManager(),"VERSION_INFO");
    }
  }
  /* (non-Javadoc)
   * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
   * The Preferences activity can be launched from activities of this subclass and we handle here 
   * the need to restart if the restore command has been called
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    //configButtons();
    if (requestCode == PREFERENCES_REQUEST && resultCode == RESULT_FIRST_USER) {
      Intent i = new Intent(this, MyExpenses.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      finish();
      startActivity(i);
    }
  }
}
