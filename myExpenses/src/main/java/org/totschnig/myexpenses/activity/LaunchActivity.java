package org.totschnig.myexpenses.activity;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.contrib.Config;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.util.Distrib;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.Map;

public abstract class LaunchActivity extends ProtectedFragmentActivity {
  
  private OpenIabHelper mHelper;
  private String tag = LaunchActivity.class.getName();
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final String contribStatus = MyApplication.getInstance().getContribStatus();
    if (contribStatus.equals(Distrib.STATUS_DISABLED) ||
        contribStatus.equals(Distrib.STATUS_ENABLED_TEMPORARY)) {

      mHelper = Distrib.getIabHelper(this);
      if (mHelper!=null) {
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
          public void onIabSetupFinished(IabResult result) {
            Log.d(tag, "Setup finished.");
            if (mHelper==null) {
              return;
            }
            if (result.isSuccess()) {
              mHelper.queryInventoryAsync(false,new QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(
                    IabResult result,
                    Inventory inventory) {
                  if (mHelper==null || inventory==null) {
                    return;
                  }
                  // Do we have the premium upgrade?
                  Purchase premiumPurchase =
                      inventory.getPurchase(Config.SKU_PREMIUM);
                  if (premiumPurchase!=null&&premiumPurchase.getPurchaseState()==0) {
                    Distrib.registerPurchase(LaunchActivity.this);
                  } else if (contribStatus.equals(Distrib.STATUS_ENABLED_TEMPORARY)) {
                    Distrib.maybeCancel(LaunchActivity.this);
                  }
                }
              });
            }
          }
        });
      }
    }
  }

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
        Log.i("MyExpenses",String.format("Upgrading to version 28: Purging %d transactions from datbase",
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
  // We're being destroyed. It's important to dispose of the helper here!
  @Override
  public void onDestroy() {
      super.onDestroy();

      // very important:
      Log.d(tag, "Destroying helper.");
      if (mHelper != null) mHelper.dispose();
      mHelper = null;
  }
}
