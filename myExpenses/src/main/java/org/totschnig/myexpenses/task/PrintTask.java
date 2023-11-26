package org.totschnig.myexpenses.task;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.filter.FilterPersistenceKt.KEY_FILTER;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.ExceptionUtilsKt;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import timber.log.Timber;

public class PrintTask extends AsyncTask<Void, String, Result<Uri>> {
    private final TaskExecutionFragment taskExecutionFragment;
    private long accountId;
    private WhereFilter filter;
    private long currentBalance;

    PrintTask(TaskExecutionFragment taskExecutionFragment, Bundle extras) {
        this.taskExecutionFragment = taskExecutionFragment;
        accountId = extras.getLong(KEY_ROWID);
        filter = new WhereFilter(extras.getParcelableArrayList(KEY_FILTER));
        currentBalance = extras.getLong(KEY_CURRENT_BALANCE);
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
        final MyApplication application = MyApplication.Companion.getInstance();
        final Context context = application.getWrappedContext();
        DocumentFile appDir = AppDirHelper.INSTANCE.getAppDirLegacy(application);
        if (appDir == null) {
            return Result.ofFailure(R.string.io_error_appdir_null);
        }
        try {
            return new PdfPrinter(accountId, appDir, filter, currentBalance).print(context);
        } catch (Exception e) {
            CrashHandler.report(e);
            return Result.ofFailure(R.string.export_sdcard_failure, appDir.getName(), ExceptionUtilsKt.getSafeMessage(e));
        }
    }
}
