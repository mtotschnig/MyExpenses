package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import static android.text.TextUtils.isEmpty;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_VALIDATE_LICENCE;

public class DeepLinkActivity extends ProtectedFragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        Uri data = getIntent().getData();
        if (data == null) {
          showWebSite();
        } else if (data.getLastPathSegment().equals("callback.html")) {
          showToast(getString(R.string.licence_migration_info));
          finish();
        } else if ("verify".equals(data.getFragment())) { //callback2.html
          String existingKey = PrefKey.NEW_LICENCE.getString("");
          String existingEmail = PrefKey.LICENCE_EMAIL.getString("");
          String key = data.getQueryParameter("key");
          String email = data.getQueryParameter("email");
          if (isEmpty(key) || isEmpty(email)) {
            showToast("Missing parameter key and/or email");
            finish();
          } else if (existingKey.equals("") || (existingKey.equals(key) && existingEmail.equals(email))) {
            if (existingKey.equals("")) {
              PrefKey.NEW_LICENCE.putString(key);
              PrefKey.LICENCE_EMAIL.putString(email);
            }
            startTaskExecution(TASK_VALIDATE_LICENCE, new String[]{}, null, R.string.progress_validating_licence);
          } else {
            showToast(String.format("There is already a licence active on this device, key: %s", existingKey));
            finish();
          }
        } else {
          showWebSite();
        }
      }
    }
  }

  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private void showWebSite() {
    CommonCommands.dispatchCommand(this, R.id.WEB_COMMAND, null);
    finish();
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
      if (o instanceof Result) {
        Result r = ((Result) o);
        showToast(r.print(this));
      }
    }
    finish();
  }
}
