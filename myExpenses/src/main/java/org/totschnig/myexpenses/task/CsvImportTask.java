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
import android.text.TextUtils;

import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable;

import java.util.ArrayList;
import java.util.Currency;

public class CsvImportTask extends AsyncTask<Void, Integer, Result> {
  private final TaskExecutionFragment taskExecutionFragment;
  ArrayList<CSVRecord> data;
  int[] fieldToColumnMap;
  SparseBooleanArrayParcelable discardedRows;
  private long accountId;
  private Currency mCurrency;

  public CsvImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.data = (ArrayList<CSVRecord>) b.getSerializable(CsvImportDataFragment.KEY_DATASET);
    this.fieldToColumnMap = (int[]) b.getSerializable(CsvImportDataFragment.KEY_FIELD_TO_COLUMN);
    this.discardedRows = b.getParcelable(CsvImportDataFragment.KEY_DISCARDED_ROWS);
    this.accountId = b.getLong(DatabaseConstants.KEY_ACCOUNTID);
    this.mCurrency = Currency.getInstance(b.getString(DatabaseConstants.KEY_CURRENCY));
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CSV_IMPORT, result);
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onProgressUpdate(values[0]);
    }
  }

  @Override
  protected Result doInBackground(Void... params) {
    int totalImported = 0, totalDiscarded = 0, totalFailed = 0;
    if (accountId==0) {
      Account a = new Account();
      a.currency = mCurrency;
      a.label = MyApplication.getInstance().getString(R.string.pref_import_title,"CSV");
      a.save();
      accountId = a.getId();
    }
    for (int i = 0; i < data.size(); i++) {
      if (discardedRows.get(i,false)) {
        totalDiscarded++;
      } else {
        CSVRecord record = data.get(i);

        totalImported++;
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (totalImported%10==0) {
          publishProgress(totalImported);
        }
      }
    }
    return new Result(true,
        0,
        Integer.valueOf(totalImported),
        Integer.valueOf(totalFailed),
        Integer.valueOf(totalDiscarded));
  }
}