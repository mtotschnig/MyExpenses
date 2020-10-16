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

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import timber.log.Timber;

public class CsvParseTask extends AsyncTask<Void, String, ArrayList<CSVRecord>> {
  private final TaskExecutionFragment taskExecutionFragment;
  private char delimiter;
  private String encoding;
  Uri fileUri;


  public CsvParseTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.delimiter = b.getChar(TaskExecutionFragment.KEY_DELIMITER);
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    this.encoding = b.getString(TaskExecutionFragment.KEY_ENCODING);
  }

  @Override
  protected void onPostExecute(ArrayList<CSVRecord> result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CSV_PARSE, result);
    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      for (String progress : values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }

  @Override
  protected ArrayList<CSVRecord> doInBackground(Void... params) {
    InputStream inputStream;
    Context context = taskExecutionFragment.requireContext();
    try {
      inputStream = context.getContentResolver().openInputStream(fileUri);
    } catch (FileNotFoundException e) {
      publishProgress(context.getString(R.string.parse_error_file_not_found, fileUri));
      return null;
    } catch (Exception e) {
      publishProgress(context.getString(R.string.parse_error_other_exception, e.getMessage()));
      return null;
    }
    try {
      return (ArrayList<CSVRecord>) CSVFormat.DEFAULT.withDelimiter(delimiter)
          .parse(new InputStreamReader(inputStream, encoding)).getRecords();
    } catch (IOException e) {
      publishProgress(context.getString(R.string.parse_error_other_exception, e.getMessage()));
      return null;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          Timber.e(e);
        }
      }
    }
  }
}