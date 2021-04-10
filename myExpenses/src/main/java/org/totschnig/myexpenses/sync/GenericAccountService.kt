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
package org.totschnig.myexpenses.sync

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.util.Pair
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import timber.log.Timber

class GenericAccountService : Service() {
    private lateinit var mAuthenticator: Authenticator
    override fun onCreate() {
        Timber.i("Service created")
        mAuthenticator = Authenticator(this)
    }

    override fun onDestroy() {
        Timber.i("Service destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator.iBinder
    }

    inner class Authenticator(private val mContext: Context) : AbstractAccountAuthenticator(mContext) {
        override fun editProperties(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                    s: String): Bundle {
            throw UnsupportedOperationException()
        }

        override fun addAccount(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                s: String, s2: String, strings: Array<String>, bundle: Bundle): Bundle {
            return createManageSyncBackendIntentBundle()
        }

        private fun createManageSyncBackendIntentBundle(): Bundle {
            val result = Bundle()
            result.putParcelable(AccountManager.KEY_INTENT, Intent(this@GenericAccountService, ManageSyncBackends::class.java))
            return result
        }

        override fun confirmCredentials(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                        account: Account, bundle: Bundle): Bundle? {
            return null
        }

        override fun getAuthToken(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                  account: Account, authTokenType: String, bundle: Bundle): Bundle {
            val accountManager = AccountManager.get(mContext)
            val authToken = accountManager.peekAuthToken(account, authTokenType)
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        override fun getAuthTokenLabel(s: String): String {
            throw UnsupportedOperationException()
        }

        override fun updateCredentials(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                       account: Account, s: String, bundle: Bundle): Bundle {
            throw UnsupportedOperationException()
        }

        override fun hasFeatures(accountAuthenticatorResponse: AccountAuthenticatorResponse,
                                 account: Account, strings: Array<String>): Bundle {
            throw UnsupportedOperationException()
        }

        override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle {
            val result = Bundle()
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
            return result
        }
    }

    companion object {
        const val AUTH_TOKEN_TYPE = "Default"
        private const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".sync"
        const val KEY_SYNC_PROVIDER_URL = "sync_provider_url"
        const val KEY_SYNC_PROVIDER_USERNAME = "sync_provider_user_name"
        const val KEY_PASSWORD_ENCRYPTION = "passwordEncryption"
        const val DEFAULT_SYNC_FREQUENCY_HOURS = 12
        const val HOUR_IN_SECONDS = 3600
        const val KEY_BROKEN = "broken"
        const val KEY_ENCRYPTED = "encrypted"

        /**
         * Obtain a handle to the [Account] used for sync in this application.
         *
         * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
         * has been called)
         */
        @JvmStatic
        fun getAccount(accountName: String?): Account {
            // Note: Normally the account name is set to the user's identity (username or email
            // address). However, since we aren't actually using any user accounts, it makes more sense
            // to use a generic string in this case.
            //
            // This string should *not* be localized. If the user switches locale, we would not be
            // able to locate the old account, and may erroneously register multiple accounts.
            return Account(accountName, ACCOUNT_TYPE)
        }

        fun getAccountNamesWithEncryption(context: Context): List<Pair<String, Boolean>> {
            return getAccounts(context)
                    .map { account: Account -> Pair.create(account.name, AccountManager.get(context).getUserData(account, KEY_ENCRYPTED) != null) }
        }

        @JvmStatic
        fun storePassword(contentResolver: ContentResolver?, accountName: String, encryptionPassword: String?) {
            DbUtils.storeSetting(contentResolver, getPasswordKey(accountName), encryptionPassword)
        }

        @JvmStatic
        fun loadPassword(contentResolver: ContentResolver?, accountName: String): String? {
            return DbUtils.loadSetting(contentResolver, getPasswordKey(accountName))
        }

        private fun getPasswordKey(accountName: String): String {
            return "$accountName - $KEY_PASSWORD_ENCRYPTION"
        }

        fun updateAccountsIsSyncable(context: MyApplication, licenceHandler: LicenceHandler, prefHandler: PrefHandler) {
            val isSyncable = licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)
            val accountManager = AccountManager.get(context)
            getAccounts(context)
                    .filter { account: Account? -> accountManager.getUserData(account, KEY_BROKEN) == null }
                    .forEach { account: Account? ->
                        if (isSyncable) {
                            activateSync(account, prefHandler)
                        } else {
                            deactivateSync(account)
                        }
                    }
        }

        @JvmStatic
        fun getAccounts(context: Context): Array<Account> = try {
            AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE)
        } catch (e: SecurityException) {
            CrashHandler.report(e)
            emptyArray()
        }

        @JvmStatic
        fun getAccountNames(context: Context): Array<String> = getAccounts(context).map { it.name }.toTypedArray()

        @JvmStatic
        fun activateSync(account: Account?, prefHandler: PrefHandler) {
            ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, true)
            ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 1)
            ContentResolver.addPeriodicSync(account, TransactionProvider.AUTHORITY, Bundle.EMPTY, (
                    prefHandler.getInt(PrefKey.SYNC_FREQUCENCY, DEFAULT_SYNC_FREQUENCY_HOURS) * HOUR_IN_SECONDS).toLong())
        }

        @JvmStatic
        fun deactivateSync(account: Account?) {
            if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
                ContentResolver.cancelSync(account, TransactionProvider.AUTHORITY)
                ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, false)
                ContentResolver.removePeriodicSync(account, TransactionProvider.AUTHORITY, Bundle.EMPTY)
                ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 0)
            }
        }
    }
}