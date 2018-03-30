package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.os.Bundle;

import com.annimon.stream.Exceptional;

import org.totschnig.myexpenses.sync.webdav.WebDavClient;

import java.security.cert.X509Certificate;

public class TestLoginTask extends AsyncTask<Void, Void, Exceptional> {
  public static String KEY_URL = "url";
  public static String KEY_USERNAME = "username";
  public static String KEY_PASSWORD = "password";
  public static String KEY_CERTIFICATE = "certificate";

  private final TaskExecutionFragment taskExecutionFragment;
  private String url;
  private String userName;
  private String password;
  private X509Certificate trustedCertificate;

  TestLoginTask(TaskExecutionFragment taskExecutionFragment, Bundle args) {
    this.taskExecutionFragment = taskExecutionFragment;
    url = args.getString(KEY_URL);
    userName = args.getString(KEY_USERNAME);
    password = args.getString(KEY_PASSWORD);
    trustedCertificate = (X509Certificate) args.getSerializable(KEY_CERTIFICATE);
  }

  @Override
  protected Exceptional doInBackground(Void... params) {
    try {
      WebDavClient client = new WebDavClient(url, userName, password, trustedCertificate);
      client.testLogin();
      client.testClass2Locking();
      return Exceptional.of(() -> null);
    } catch (Exception e) {
      return Exceptional.of(e);
    }
  }

  @Override
  protected void onPostExecute(Exceptional result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN, result);
    }
  }
}
