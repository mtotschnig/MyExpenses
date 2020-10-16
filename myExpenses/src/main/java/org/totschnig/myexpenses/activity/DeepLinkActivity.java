package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import static android.text.TextUtils.isEmpty;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_VALIDATE_LICENCE;

public class DeepLinkActivity extends ProtectedFragmentActivity {
  private boolean isPdt = true; //PaypalDataTransfer

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        Uri data = getIntent().getData();
        if (data == null) {
          showWebSite();
        } else if (data.getLastPathSegment().equals("callback.html")) {
          super.showMessage(Utils.getTextWithAppName(this, R.string.licence_migration_info));
        } else if ("verify".equals(data.getFragment())) { //callback2.html
          boolean isSandbox = data.getBooleanQueryParameter("sandbox", false);
          if (isSandbox == BuildConfig.DEBUG) {//prevent a sandbox call from hitting production app, and vice versa
            String existingKey = PrefKey.NEW_LICENCE.getString("");
            String existingEmail = PrefKey.LICENCE_EMAIL.getString("");
            String key = data.getQueryParameter("key");
            String email = data.getQueryParameter("email");
            isPdt = data.getBooleanQueryParameter("isPdt", true);

            if (isEmpty(key) || isEmpty(email)) {
              showMessage("Missing parameter key and/or email");
            } else if (existingKey.equals("") || (existingKey.equals(key) && existingEmail.equals(email)) ||
                !MyApplication.getInstance().getLicenceHandler().isContribEnabled()) {
              PrefKey.NEW_LICENCE.putString(key);
              PrefKey.LICENCE_EMAIL.putString(email);
              startTaskExecution(TASK_VALIDATE_LICENCE, new String[]{}, null, R.string.progress_validating_licence);
            } else {
              showMessage(String.format(
                  "There is already a licence active on this device, key: %s", existingKey));
            }
          } else {
            showMessage(String.format("%s app was called from %s environment",
                BuildConfig.DEBUG ? "Debug" : "Production",
                isSandbox ? "Sandbox" : "Live"));
          }
        } else {
          showWebSite();
        }
      }
    }
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish();
  }

  public void showMessage(final CharSequence message) {
    String messageToShow = isPdt ? getString(R.string.paypal_callback_info) + " " : "";
    messageToShow += message;
    super.showMessage(messageToShow);
  }

  private void showWebSite() {
    dispatchCommand(R.id.WEB_COMMAND, null);
    finish();
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
      if (o instanceof Result) {
        Result r = ((Result) o);
        showMessage(r.print(this));
      }
    }
  }
}
