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

import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable;

import java.util.ArrayList;

public class CsvImportTask extends AsyncTask<Void, String, Void> {
  private final TaskExecutionFragment taskExecutionFragment;
  ArrayList<CSVRecord> data;
  int[] fieldToColumnMap;
  SparseBooleanArrayParcelable discardedRows;

  public CsvImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.data = (ArrayList<CSVRecord>) b.getSerializable(CsvImportDataFragment.KEY_DATASET);
    this.fieldToColumnMap = (int[]) b.getSerializable(CsvImportDataFragment.KEY_FIELD_TO_COLUMN);
    this.discardedRows = b.getParcelable(CsvImportDataFragment.KEY_DISCARDED_ROWS);
  }

  @Override
  protected void onPostExecute(Void result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CSV_IMPORT, result);
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
    for (int i = 0; i < data.size(); i++) {
      if (discardedRows.get(i,false)) {
        publishProgress(String.format("Ignoring row %d",i));
      } else {
        CSVRecord record = data.get(i);
        publishProgress(String.format("Importing row %d",i));
      }
    }
    return null;
  }
}