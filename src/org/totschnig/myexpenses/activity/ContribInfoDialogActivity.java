package org.totschnig.myexpenses.activity;

import java.io.Serializable;
import java.util.UUID;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.contrib.Config;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.Distrib;
import org.totschnig.myexpenses.util.Utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

  public class ContribInfoDialogActivity extends FragmentActivity
      implements MessageDialogListener,ContribIFace {
    protected long sequenceCount;
    public final static String KEY_FEATURE = "feature";
    public static final String KEY_TAG = "tag";
    private OpenIabHelper mHelper;
    private boolean mSetupDone;
    private String mPayload = UUID.randomUUID().toString();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MyApplication.getInstance().isContribEnabled()) {
          DonateDialogFragment.newInstance().show(
              getSupportFragmentManager(), "CONTRIB");
          return;
        }

        mHelper = Distrib.getIabHelper(this);
        if (mHelper!=null) {
          mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(MyApplication.TAG, "Setup finished.");
  
                if (!result.isSuccess()) {
                  mSetupDone = false;
                    // Oh noes, there was a problem.
                  complain("Problem setting up in-app billing: " + result);
                  return;
                }
                mSetupDone = true;
                Log.d(MyApplication.TAG, "Setup successful.");
            }
          });
        }

        Feature f = (Feature) getIntent().getSerializableExtra(KEY_FEATURE);

        if (f==null) {
          sequenceCount = getIntent().getLongExtra(
              ContribInfoDialogFragment.KEY_SEQUENCE_COUNT, -1);
          ContribInfoDialogFragment.newInstance(
              sequenceCount)
            .show(getSupportFragmentManager(),"CONTRIB_INFO");
        } else {
          ContribDialogFragment.newInstance(f,
              getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(),"CONTRIB");
        }
    }
    @Override
    public boolean dispatchCommand(int command, Object tag) {
      switch (command) {
      case R.id.REMIND_LATER_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(
            sequenceCount+MyExpenses.TRESHOLD_REMIND_CONTRIB);
        break;
      case R.id.REMIND_NO_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(-1);
      }
      finish();
      return true;
    }

    private void contribBuyBlackBerry() {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("appworld://content/57168887"));
      if (Utils.isIntentAvailable(this,i)) {
        startActivity(i);
      } else {
        Toast.makeText(
            this,
            R.string.error_accessing_market,
            Toast.LENGTH_LONG)
            .show();
      }
    }
    
    public void contribBuyDo() {
      if (MyApplication.getInstance().isContribEnabled()) {
          DonateDialogFragment.newInstance().show(
              getSupportFragmentManager(), "CONTRIB");
          return;
      }
      if (MyApplication.market.equals(Distrib.Market.BLACKBERRY)) {
        contribBuyBlackBerry();
        return;
      }
      if (mHelper==null) {
        finish();
        return;
      }
      if (!mSetupDone) {
        complain("Billing setup is not completed yet");
        finish();
        return;
      }
      final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
          new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
          Log.d(MyApplication.TAG,
              "Purchase finished: " + result + ", purchase: " + purchase);
          if (result.isFailure()) {
            Log.w(MyApplication.TAG,
                "Purchase failed: " + result + ", purchase: " + purchase);
            complain(getString(R.string.premium_failed_or_canceled));
          } else if (!verifyDeveloperPayload(purchase)) {
            complain("Error purchasing. Authenticity verification failed.");
          } else {
            Log.d(MyApplication.TAG, "Purchase successful.");

            if (purchase.getSku().equals(Config.SKU_PREMIUM)) {
              // bought the premium upgrade!
              Log.d(MyApplication.TAG,
                  "Purchase is premium upgrade. Congratulating user.");
              Toast.makeText(
                  ContribInfoDialogActivity.this,
                  Utils.concatResStrings(
                      ContribInfoDialogActivity.this,
                      R.string.premium_unlocked,R.string.thank_you),
                      Toast.LENGTH_SHORT).show();
              Distrib.registerPurchase(ContribInfoDialogActivity.this);
            }
          }
          finish();
        }

        private boolean verifyDeveloperPayload(Purchase purchase) {
          if (mPayload==null) {
            return true;
          }
          String payload = purchase.getDeveloperPayload();
          if (payload==null) {
            return false;
          }
          return payload.equals(mPayload);
        }
    };
    //setWaitScreen(true);
    mHelper.launchPurchaseFlow(
        ContribInfoDialogActivity.this,
        Config.SKU_PREMIUM,
        ProtectedFragmentActivity.PURCHASE_PREMIUM_REQUEST,
        mPurchaseFinishedListener,
        mPayload
        );
    }
//    private void setWaitScreen(boolean set) {
//      final FragmentManager m = getSupportFragmentManager();
//      if(set) {
//        m.beginTransaction()
//          .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_setup_purchase),"PROGRESS")
//          .commit();
//      } else {
//        ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
//        if (f!=null) {
//            m.beginTransaction().remove(f).commitAllowingStateLoss();
//        }
//      }
//    }

    void complain(String message) {
      Log.e(MyApplication.TAG, "**** InAppPurchase Error: " + message);
      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageDialogDismissOrCancel() {
     finish();
    }

    @Override
    public void contribFeatureCalled(Feature feature, Serializable tag) {
      Intent i = new Intent();
      i.putExtra(KEY_FEATURE, feature);
      i.putExtra(KEY_TAG,tag);
      setResult(RESULT_OK, i);
      finish();
    }

    @Override
    public void contribFeatureNotCalled() {
      finish();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d(MyApplication.TAG,
          "onActivityResult() requestCode: " + requestCode +
          " resultCode: " + resultCode + " data: " + data);

      // Pass on the activity result to the helper for handling
      if (mHelper==null || !mHelper.handleActivityResult(requestCode, resultCode, data)) {
          // not handled, so handle it ourselves (here's where you'd
          // perform any handling of activity results not related to in-app
          // billing...
          super.onActivityResult(requestCode, resultCode, data);
      } else {
          Log.d(MyApplication.TAG, "onActivityResult handled by IABUtil.");
      }
    }
    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(MyApplication.TAG, "Destroying helper.");
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

}
