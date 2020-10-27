package org.totschnig.myexpenses.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;

import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class PrintTask extends AsyncTask<Void, String, Result<Uri>> {
  private final TaskExecutionFragment taskExecutionFragment;
  private long accountId;
  private WhereFilter filter;

  PrintTask(TaskExecutionFragment taskExecutionFragment, Bundle extras) {
    this.taskExecutionFragment = taskExecutionFragment;
    accountId = extras.getLong(KEY_ROWID);
    filter = new WhereFilter(extras.getParcelableArrayList(TransactionList.KEY_FILTER));
  }

  /*
   * (non-Javadoc) reports on success triggering restart if needed
   * 
   * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
   */
  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(TaskExecutionFragment.TASK_PRINT, result);
    }
  }

  /* (non-Javadoc)
   * this is where the bulk of the work is done via calls to {@link #importCatsMain()}
   * and {@link #importCatsSub()}
   * sets up {@link #categories} and {@link #sub_categories}
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected Result<Uri> doInBackground(Void... ignored) {
    Account account;
    final MyApplication application = MyApplication.getInstance();
    final Context context = ContextHelper.wrap(application, application.getAppComponent().userLocaleProvider().getUserPreferredLocale());
    DocumentFile appDir = AppDirHelper.getAppDir(application);
    if (appDir == null) {
      return Result.ofFailure(R.string.external_storage_unavailable);
    }
    account = Account.getInstanceFromDb(accountId);
    try {
      return new PdfPrinter(account, appDir, filter).print(context);
    } catch (Exception e) {
      Timber.e(e, "Error while printing");
      return Result.ofFailure(R.string.export_sdcard_failure, appDir.getName(), e.getMessage());
    }
  }
}
