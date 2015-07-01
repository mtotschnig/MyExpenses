/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
//based on Financisto

package org.totschnig.myexpenses.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.qif.QifAccount;
import org.totschnig.myexpenses.export.qif.QifBufferedReader;
import org.totschnig.myexpenses.export.qif.QifCategory;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.export.qif.QifParser;
import org.totschnig.myexpenses.export.qif.QifTransaction;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CsvImportTask extends AsyncTask<Void, String, Void> {
  private final TaskExecutionFragment taskExecutionFragment;
  private QifDateFormat dateFormat;
  private String encoding;
  private long accountId;
  Uri fileUri;

  private Currency mCurrency;

  public CsvImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.dateFormat = (QifDateFormat) b.getSerializable(TaskExecutionFragment.KEY_DATE_FORMAT);
    this.accountId = b.getLong(DatabaseConstants.KEY_ACCOUNTID);
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    this.mCurrency = Currency.getInstance(b.getString(DatabaseConstants.KEY_CURRENCY));
    this.encoding = b.getString(TaskExecutionFragment.KEY_ENCODING);
  }

  @Override
  protected void onPostExecute(Void result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CSV_IMPORT, null);
    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      for (String progress: values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }

  @Override
  protected Void doInBackground(Void... params) {
    InputStream inputStream;
    try {
      inputStream = MyApplication.getInstance().getContentResolver().openInputStream(fileUri);
    } catch (FileNotFoundException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_file_not_found,fileUri));
      return null;
    } catch (Exception e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_other_exception,e.getMessage()));
      return null;
    }
    try {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(new InputStreamReader(inputStream));
      for (CSVRecord record : records) {
        publishProgress(record.toString());
      }
      return(null);
    } catch (IOException e) {
      publishProgress(MyApplication.getInstance()
          .getString(R.string.parse_error_other_exception,e.getMessage()));
      return null;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }
}