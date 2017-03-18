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

package org.totschnig.myexpenses.test.model;

import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.totschnig.myexpenses.model.Account;

import java.util.concurrent.CountDownLatch;

/**
 * This test exercises concurrent creation, instantiation and deletion of accounts. Prior to the changes
 * introduced by this commit, this test failed with {@link java.util.ConcurrentModificationException}
 */
public class AccountArrayTest extends InstrumentationTestCase {
  private static final int TIMES = 5000;
  private final String TAG = "AccountArrayTest";
  Long openingBalance = 100L;
  CountDownLatch signal = new CountDownLatch(TIMES);

  private void insertAccount() {
    Account account = new Account("Account 1", openingBalance, "Account 1");
    account.save();
  }


  public void testConcurrentInstantiationOfMultipleAccounts() throws Throwable {
    insertAccount();

    for (int i = 0; i < TIMES; i++) {
      final AsyncTask<Void, Void, Account> myTask = new AccountLoaderAsyncTask();

      // Execute the async task on the UI thread! THIS IS KEY!
      runTestOnUiThread(() -> {
        myTask.execute();
        insertAccount();
      });

      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }


    /* The testing thread will wait here until the UI thread releases it
     * above with the countDown() or 30 seconds passes and it times out.
     */
    signal.await();
  }

  private class AccountLoaderAsyncTask extends AsyncTask<Void, Void, Account> {

    @Override
    protected Account doInBackground(Void... arg0) {
      return Account.getInstanceFromDb(0);
    }

    @Override
    protected void onPostExecute(Account result) {
      assertNotNull(result);
      new AccountDeleteAsyncTask().execute(result.getId());
    }
  }
  private class AccountDeleteAsyncTask extends AsyncTask<Long, Void, Void> {

    @Override
    protected Void doInBackground(Long... arg0) {
      try {
        Account.delete(arg0[0]);
      } catch (RemoteException | OperationApplicationException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      assertNull(result);
      signal.countDown();
      Log.d(TAG, "" + signal.getCount());
    }
  }
}
