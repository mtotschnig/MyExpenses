package org.totschnig.myexpenses.task;

import android.accounts.AccountManager;
import android.os.Bundle;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.DropboxBackendProvider;
import org.totschnig.myexpenses.util.Result;

import java.util.Locale;

import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_URL;

class DropboxSetupTask extends ExtraTask<Result> {

  public DropboxSetupTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected Result doInBackground(Bundle... params) {
    String userLocale = Locale.getDefault().toString();
    DbxRequestConfig requestConfig = new DbxRequestConfig(BuildConfig.APPLICATION_ID, userLocale);
    DbxClientV2 dbxClient = new DbxClientV2(requestConfig, params[0].getString(AccountManager.KEY_AUTHTOKEN));
    try {
      String userName = dbxClient.users().getCurrentAccount().getName().getDisplayName();
      String folderName = params[0].getString(KEY_SYNC_PROVIDER_URL);
      if (DropboxBackendProvider.exists(dbxClient, "/" + folderName)) {
        return new Result(true, 0, userName, folderName);
      } else {
        return new Result(false, R.string.dropbox_folder_not_found);
      }
    } catch (DbxException e) {
      return new Result(false, e.getMessage());
    }
  }
}
