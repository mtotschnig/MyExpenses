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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import timber.log.Timber;

public class GenericAccountService extends Service {
  public static final String ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".sync";
  public static final String KEY_SYNC_PROVIDER_URL = "sync_provider_url";
  public static final String KEY_SYNC_PROVIDER_USERNAME = "sync_provider_user_name";
  public static final String KEY_PASSWORD_ENCRYPTION = "passwordEncryption";
  public static final int DEFAULT_SYNC_FREQUENCY_HOURS = 12;
  public static final int HOUR_IN_SECONDS = 3600;
  public static final String KEY_BROKEN = "broken";
  public static final String KEY_ENCRYPTED = "encrypted";
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

  public static List<Pair<String, Boolean>> getAccountNamesWithEncryption(Context context) {
    return getAccountsAsStream(context)
        .map(account -> Pair.create(account.name, AccountManager.get(context).getUserData(account, KEY_ENCRYPTED) != null))
        .collect(Collectors.toList());
  }

  public static void storePassword(ContentResolver contentResolver, String accountName, String encryptionPassword) {
    DbUtils.storeSetting(contentResolver, getPasswordKey(accountName), encryptionPassword);
  }

  public static String loadPassword(ContentResolver contentResolver, String accountName) {
    return DbUtils.loadSetting(contentResolver, getPasswordKey(accountName));
  }

  @NonNull
  private static String getPasswordKey(String accountName) {
    return accountName + " - " + KEY_PASSWORD_ENCRYPTION;
  }

  @Override
  public void onCreate() {
    Timber.i("Service created");
    mAuthenticator = new Authenticator(this);
  }

  @Override
  public void onDestroy() {
    Timber.i("Service destroyed");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mAuthenticator.getIBinder();
  }

  public static void updateAccountsIsSyncable(MyApplication context, LicenceHandler licenceHandler) {
    boolean isSyncable = licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION);
    AccountManager accountManager = AccountManager.get(context);

    getAccountsAsStream(context)
        .filter(account -> accountManager.getUserData(account, KEY_BROKEN) == null)
        .forEach(account -> {
          if (isSyncable) {
            activateSync(account);
          } else {
            deactivateSync(account);
          }
        });
  }

  public static Account[] getAccountsAsArray(Context context) {
    try {
      return AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE);
    } catch (SecurityException e) {
      CrashHandler.report(e);
    }
    return new Account[0];
  }

  public static Stream<Account> getAccountsAsStream(Context context) {
    return Stream.of(getAccountsAsArray(context));
  }

  public static void activateSync(Account account) {
    ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, true);
    ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 1);
    ContentResolver.addPeriodicSync(account, TransactionProvider.AUTHORITY, Bundle.EMPTY,
        PrefKey.SYNC_FREQUCENCY.getInt(GenericAccountService.DEFAULT_SYNC_FREQUENCY_HOURS) * GenericAccountService.HOUR_IN_SECONDS);
  }

  public static void deactivateSync(Account account) {
    if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
      ContentResolver.cancelSync(account, TransactionProvider.AUTHORITY);
      ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, false);
      ContentResolver.removePeriodicSync(account, TransactionProvider.AUTHORITY, Bundle.EMPTY);
      ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 0);
    }
  }

  public class Authenticator extends AbstractAccountAuthenticator {

    public static final String AUTH_TOKEN_TYPE = "Default";
    private final Context mContext;

    public Authenticator(Context context) {
      super(context);
      mContext = context;
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
                               Account account, String authTokenType, Bundle bundle)
        throws NetworkErrorException {
      AccountManager accountManager = AccountManager.get(mContext);
      String authToken = accountManager.peekAuthToken(account, authTokenType);

      Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
      return result;
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

