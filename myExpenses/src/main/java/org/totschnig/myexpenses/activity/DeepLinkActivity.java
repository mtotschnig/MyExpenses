package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.PaypalPaymentCompletedCallbackDialog;

public class DeepLinkActivity extends ProtectedFragmentActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(MyApplication.getThemeIdTranslucent());
    if (savedInstanceState == null) {
      if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        Uri data = getIntent().getData();
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(PaypalPaymentCompletedCallbackDialog.class.getName()) == null) {
          PaypalPaymentCompletedCallbackDialog.newInstance(data.getQueryParameter("tx"))
              .show(fm, PaypalPaymentCompletedCallbackDialog.class.getName());
        }
      }
    }
  }
}
