package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.PaypalPaymentCompletedCallbackDialog;
import org.totschnig.myexpenses.preference.PrefKey;

public class DeepLinkActivity extends ProtectedFragmentActivity {

  public static final String FRAGMENT_TAG = PaypalPaymentCompletedCallbackDialog.class.getName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(MyApplication.getThemeIdTranslucent());
    if (savedInstanceState == null) {
      if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        Uri data = getIntent().getData();
        String fragment = data.getFragment();
        if ("verify".equals(fragment)) {
          String key = data.getQueryParameter("key");
          PrefKey.ENTER_LICENCE.putString(key);
          CommonCommands.dispatchCommand(this, R.id.VERIFY_LICENCE_COMMAND, key);
          finish();
        } else {
          String tx = data.getQueryParameter("tx");
          if (TextUtils.isEmpty(tx)) {
            //nothing to do for us
            CommonCommands.dispatchCommand(this, R.id.WEB_COMMAND, null);
            finish();
          }
          FragmentManager fm = getSupportFragmentManager();
          if (fm.findFragmentByTag(PaypalPaymentCompletedCallbackDialog.class.getName()) == null) {
            PaypalPaymentCompletedCallbackDialog.newInstance(tx).show(fm,
                FRAGMENT_TAG);
          }
        }
      }
    }
  }
}
