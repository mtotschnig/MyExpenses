/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import org.totschnig.myexpenses.activity.ManageSyncBackends;

public class GenericAccountService extends Service {
  private static final String TAG = "GenericAccountService";
  public static final String ACCOUNT_TYPE = "org.totschnig.myexpenses.sync";
  public static final String KEY_SYNC_PROVIDER_ID = "sync_provider_id";
  public static final String KEY_SYNC_PROVIDER_LABEL = "sync_provider_label";
  public static final String KEY_SYNC_PROVIDER_URL = "sync_provider_url";
  public static final String KEY_SYNC_PROVIDER_USERNAME = "sync_provider_user_name";
  public static final int DEFAULT_SYNC_FREQUENCY_HOURS = 12;
  public static final int HOUR_IN_SECONDS = 3600;
  private Authenticator mAuthenticator;

  /**
   * Obtain a handle to the {@link Account} used for sync in this application.
   *
   * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
   * has been called)
   */
  public static Account GetAccount(String accountName) {
    // Note: Normally the account name is set to the user's identity (username or email
    // address). However, since we aren't actually using any user accounts, it makes more sense
    // to use a generic string in this case.
    //
    // This string should *not* be localized. If the user switches locale, we would not be
    // able to locate the old account, and may erroneously register multiple accounts.
    return new Account(accountName, ACCOUNT_TYPE);
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "Service created");
    mAuthenticator = new Authenticator(this);
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Service destroyed");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mAuthenticator.getIBinder();
  }

  public class Authenticator extends AbstractAccountAuthenticator {

    public Authenticator(Context context) {
      super(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                 String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse,
                             String s, String s2, String[] strings, Bundle bundle)
        throws NetworkErrorException {
      return createManageSyncBackendIntentBundle();
    }

    @NonNull
    private Bundle createManageSyncBackendIntentBundle() {
      Bundle result = new Bundle();
      result.putParcelable(AccountManager.KEY_INTENT, new Intent(GenericAccountService.this, ManageSyncBackends.class));
      return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                     Account account, Bundle bundle)
        throws NetworkErrorException {
      return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                               Account account, String s, Bundle bundle)
        throws NetworkErrorException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthTokenLabel(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                    Account account, String s, Bundle bundle)
        throws NetworkErrorException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                              Account account, String[] strings)
        throws NetworkErrorException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
      final Bundle result = new Bundle();
      result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
      return result;
    }
  }

}

