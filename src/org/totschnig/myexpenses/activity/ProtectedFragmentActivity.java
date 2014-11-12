/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.contrib.Config;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * @author Michael Totschnig
 *
 */
public class ProtectedFragmentActivity extends ActionBarActivity
    implements MessageDialogListener, OnSharedPreferenceChangeListener,
    TaskExecutionFragment.TaskCallbacks,DbWriteFragment.TaskCallbacks {
  public static final int CALCULATOR_REQUEST = 0;
  public static final int EDIT_TRANSACTION_REQUEST=1;
  public static final int EDIT_ACCOUNT_REQUEST=2;
  public static final int PREFERENCES_REQUEST=3;
  public static final int CREATE_ACCOUNT_REQUEST=4;
  public static final int FILTER_CATEGORY_REQUEST=5;
  public static final int FILTER_COMMENT_REQUEST=6;
  public static final int TEMPLATE_TITLE_REQUEST=7;
  public static final int EDIT_SPLIT_REQUEST = 8;
  public static final int SELECT_CATEGORY_REQUEST = 9;
  public static final int EDIT_EVENT_REQUEST = 10;
  public static final int PICK_COLOR_REQUEST = 11;
  public static final int PURCHASE_PREMIUM_REQUEST = 12;
  public static final int CONTRIB_REQUEST = 13;
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  private boolean scheduledRestart = false;
  public Enum<?> helpVariant = null;
  protected int colorExpense;
  public int getColorExpense() {
    return colorExpense;
  }

  protected int colorIncome;
  public int getColorIncome() {
    return colorIncome;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
      .detectDiskReads()
      .detectDiskWrites()
      .detectNetwork()   // or .detectAll() for all detectable problems
      .penaltyLog()
      .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
      .detectLeakedSqlLiteObjects()
      //.detectLeakedClosableObjects()
      .penaltyLog()
      .penaltyDeath()
      .build());
    }

    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);
    setLanguage();
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    Resources.Theme theme = getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
  }

  private ProtectionDelegate getProtection() {
    if (protection == null) {
      protection = new ProtectionDelegate(this);
    }
    return protection;
  }
  @Override
  protected void onPause() {
    super.onPause();
    getProtection().handleOnPause(pwDialog);
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    MyApplication.getInstance().getSettings().unregisterOnSharedPreferenceChangeListener(this);
  }
  @Override
  protected void onResume() {
    Log.d("DEBUG","ProtectedFragmentActivity onResume");
    super.onResume();
    if(scheduledRestart) {
      scheduledRestart = false;
      if (android.os.Build.VERSION.SDK_INT>=11)
        recreate();
      else {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
      }
    } else {
      pwDialog = getProtection().hanldeOnResume(pwDialog);
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PrefKey.UI_THEME_KEY.getKey()) ||
        key.equals(MyApplication.PrefKey.UI_LANGUAGE.getKey()) ||
        key.equals(MyApplication.PrefKey.UI_FONTSIZE.getKey())) {
      scheduledRestart = true;
    }
  }
  public void onMessageDialogDismissOrCancel() {
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.common, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (dispatchCommand(item.getItemId(),null)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command, tag)) {
      return true;
    }
    return false;
  }
  @Override
  public void onPreExecute() {
  }
  @Override
  public void onProgressUpdate(Object progress) {
    getProtection().updateProgressDialog(progress);
  }
  @Override
  public void onCancelled() {
    getProtection().removeAsyncTaskFragment(false);
  }
  @Override
  public void onPostExecute(int taskId, Object o) {
    getProtection().removeAsyncTaskFragment(taskId);
    switch(taskId) {
    case TaskExecutionFragment.TASK_DELETE_TRANSACTION:
    case TaskExecutionFragment.TASK_DELETE_ACCOUNT:
    case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
    case TaskExecutionFragment.TASK_DELETE_CATEGORY:
    case TaskExecutionFragment.TASK_DELETE_PAYEES:
    case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
      Boolean success = (Boolean) o;
      if (!success) {
        Toast.makeText(this, "There was an error deleting the object. Please contact support@myexenses.mobi !", Toast.LENGTH_LONG).show();
      }
      break;
    }
  }

  protected void setLanguage() {
    MyApplication.getInstance().setLanguage();
  }
  @Override
  public Model getObject() {
    return null;
  }
  @Override
  public void onPostExecute(Object result) {
    FragmentManager m = getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    t.remove(m.findFragmentByTag("SAVE_TASK"));
    t.remove(m.findFragmentByTag("PROGRESS"));
    t.commitAllowingStateLoss();
  }
  
  /**
   * starts the given task, only if no task is currently executed,
   * informs user through toast in that case
   * @param taskId
   * @param objectIds
   * @param extra
   * @param progressMessage if 0 no progress dialog will be shown
   */
  public <T> void startTaskExecution(int taskId, T[] objectIds, Serializable extra, int progressMessage) {
    getProtection().startTaskExecution(taskId,objectIds,extra,progressMessage);
  }
  
  public void startDbWriteTask(boolean returnSequenceCount) {
    getSupportFragmentManager().beginTransaction()
    .add(DbWriteFragment.newInstance(returnSequenceCount), "SAVE_TASK")
    .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_saving),"PROGRESS")
    .commit();
  }
  
  /**
   * Workaround for broken {@link NavUtils#shouldUpRecreateTask(android.app.Activity, Intent)}
   * @see <a href="http://stackoverflow.com/a/20643984/1199911">http://stackoverflow.com/a/20643984/1199911</a>
   * @param from
   * @return
   */
  protected final boolean shouldUpRecreateTask(Activity from){
    return from.getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false);
  }
  /* 
   * @see android.support.v7.app.ActionBarActivity#onBackPressed()
   * https://code.google.com/p/android/issues/detail?id=25517
   */
  @Override
  public void onBackPressed() {
    try {
      super.onBackPressed();
    } catch (IllegalStateException e) {
      Utils.reportToAcra(e);
      finish();
    }
  }

  public void toggleCrStatus(View v) {
    Long id = (Long) v.getTag();
    if (id != -1) {
      startTaskExecution(
          TaskExecutionFragment.TASK_TOGGLE_CRSTATUS,
          new Long[] {id},
          null,
          0);
    }
  }

  public void contribBuyDo() {
    Intent i = new Intent(Intent.ACTION_VIEW);
    if (MyApplication.getInstance().isContribEnabled()) {
        DonateDialogFragment.newInstance().show(
            getSupportFragmentManager(), "CONTRIB");
    } else {
//      i.setData(Uri.parse(MyApplication.MARKET_PREFIX
//          + "org.totschnig.myexpenses.contrib"));
//      if (Utils.isIntentAvailable(ctx, i)) {
//        ctx.startActivity(i);
//      } else {
//        Toast.makeText(ctx, R.string.error_accessing_market, Toast.LENGTH_LONG)
//            .show();
//      }
      OpenIabHelper.Options.Builder builder =
          new OpenIabHelper.Options.Builder()
            .setVerifyMode(OpenIabHelper.Options.VERIFY_EVERYTHING)
            .addStoreKeys(Config.STORE_KEYS_MAP);

      final OpenIabHelper mHelper = new OpenIabHelper(this,builder.build());
      final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(MyApplication.TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(MyApplication.TAG, "Purchase successful.");

            if (purchase.getSku().equals(Config.SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(MyApplication.TAG, "Purchase is premium upgrade. Congratulating user.");
                //TODO
                //alert("Thank you for upgrading to premium!");
                MyApplication.getInstance().setContribEnabled(true);
                setWaitScreen(false);
            }
        }

        private boolean verifyDeveloperPayload(Purchase purchase) {
          // TODO
          return true;
        }
    };

      final FragmentManager m = getSupportFragmentManager();
      m.beginTransaction()
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_setup_purchase),"PROGRESS")
        .commit();
      mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            Log.d(MyApplication.TAG, "Setup finished.");

            if (!result.isSuccess()) {
                // Oh noes, there was a problem.
              Toast.makeText(ProtectedFragmentActivity.this, "Problem setting up in-app billing: ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(MyApplication.TAG, "Setup successful. Querying inventory.");
            m.beginTransaction().remove(m.findFragmentByTag("PROGRESS")).commit();
            mHelper.launchPurchaseFlow(ProtectedFragmentActivity.this, Config.SKU_PREMIUM, ProtectedFragmentActivity.PURCHASE_PREMIUM_REQUEST, mPurchaseFinishedListener);
        }
      });
    }
  }
  private void setWaitScreen(boolean set) {
    final FragmentManager m = getSupportFragmentManager();
    if(set) {
      m.beginTransaction()
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_setup_purchase),"PROGRESS")
        .commit();
    } else {
      ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
      if (f!=null) {
          m.beginTransaction().remove(f).commitAllowingStateLoss();
      }
    }
  }

  void complain(String message) {
    Log.e(MyApplication.TAG, "**** InAppPurchase Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == CONTRIB_REQUEST && resultCode == RESULT_OK) {
      ((ContribIFace) this).contribFeatureCalled(
          (Feature) intent.getSerializableExtra(ContribInfoDialogActivity.KEY_FEATURE),
          intent.getSerializableExtra(ContribInfoDialogActivity.KEY_TAG));
    }
  }
}
