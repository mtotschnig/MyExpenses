package org.totschnig.myexpenses.task;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.Result;

import java.util.Locale;

class DropboxSetupTask extends AsyncTask<Void, Void, Result> {
  private final TaskExecutionFragment taskExecutionFragment;

  public DropboxSetupTask(TaskExecutionFragment taskExecutionFragment) {
    this.taskExecutionFragment = taskExecutionFragment;

  }

  @Override
  protected Result doInBackground(Void... params) {
    final String accessToken = Auth.getOAuth2Token();
    if (accessToken != null) {
        String userLocale = Locale.getDefault().toString();
        DbxRequestConfig requestConfig = new DbxRequestConfig(BuildConfig.APPLICATION_ID, userLocale);
        DbxClientV2 mDbxClient = new DbxClientV2(requestConfig, accessToken);
        try {
          return new Result(true, 0, mDbxClient.users().getCurrentAccount().getName().getDisplayName());
        } catch (DbxException e) {
          return null;
        }
    } else {
      return new Result(false, "Dropbox Oauth Token is null");
    }
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_DROPBOX_SETUP, result);
    }
  }
}
