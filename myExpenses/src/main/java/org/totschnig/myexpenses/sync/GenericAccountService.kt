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
import android.app.Application
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
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

    inner class Authenticator(private val mContext: Context) :
        AbstractAccountAuthenticator(mContext) {
        override fun editProperties(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            s: String
        ): Bundle {
            throw UnsupportedOperationException()
        }

        override fun addAccount(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            accountType: String,
            authTokenType: String?,
            requiredFeatures: Array<String>?,
            options: Bundle
        ): Bundle {
            return createManageSyncBackendIntentBundle()
        }

        private fun createManageSyncBackendIntentBundle(): Bundle {
            val result = Bundle()
            result.putParcelable(
                AccountManager.KEY_INTENT,
                Intent(this@GenericAccountService, ManageSyncBackends::class.java)
            )
            return result
        }

        override fun confirmCredentials(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            account: Account, bundle: Bundle
        ): Bundle? {
            return null
        }

        override fun getAuthToken(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            account: Account, authTokenType: String, bundle: Bundle
        ): Bundle {
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

        override fun updateCredentials(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            account: Account, s: String, bundle: Bundle
        ): Bundle {
            throw UnsupportedOperationException()
        }

        override fun hasFeatures(
            accountAuthenticatorResponse: AccountAuthenticatorResponse,
            account: Account, strings: Array<String>
        ): Bundle {
            throw UnsupportedOperationException()
        }

        override fun getAccountRemovalAllowed(
            response: AccountAuthenticatorResponse,
            account: Account
        ): Bundle {
            val result = Bundle()
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
            return result
        }
    }

    companion object {
        const val AUTH_TOKEN_TYPE = "Default"
        const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".sync"
        const val KEY_SYNC_PROVIDER_URL = "sync_provider_url"
        const val KEY_SYNC_PROVIDER_USERNAME = "sync_provider_user_name"
        const val KEY_PASSWORD_ENCRYPTION = "passwordEncryption"
        private const val DEFAULT_SYNC_FREQUENCY_HOURS = 12
        private const val HOUR_IN_SECONDS = 3600L
        const val KEY_BROKEN = "broken"
        const val KEY_ENCRYPTED = "encrypted"

        /**
         * @return true if sync was requested, false if account is not syncable (deactivated)
         */
        fun requestSync(
            accountName: String,
            manual: Boolean = true,
            expedited: Boolean = true,
            uuid: String? = null,
            extras: Bundle = Bundle()
        ): Boolean {
            val account = getAccount(accountName)
            return if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
                ContentResolver.requestSync(
                    account,
                    TransactionProvider.AUTHORITY, extras.apply {
                        if (manual) {
                            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        }
                        if (expedited) {
                            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                        }
                        if (uuid != null) {
                            putString(KEY_UUID, uuid)
                        }
                    }
                )
                true
            } else false
        }

        /**
         * Obtain a handle to the [Account] used for sync in this application.
         *
         * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
         * has been called)
         */
        @JvmStatic
        fun getAccount(accountName: String): Account {
            // Note: Normally the account name is set to the user's identity (username or email
            // address). However, since we aren't actually using any user accounts, it makes more sense
            // to use a generic string in this case.
            //
            // This string should *not* be localized. If the user switches locale, we would not be
            // able to locate the old account, and may erroneously register multiple accounts.
            return Account(accountName, ACCOUNT_TYPE)
        }

        suspend fun getSyncBackendProvider(
            context: Context,
            syncAccountName: String
        ): Result<SyncBackendProvider> =
            SyncBackendProviderFactory.get(context, getAccount(syncAccountName), false).onFailure {
                CrashHandler.report(it, "Provider", syncAccountName)
            }

        fun getAccountNamesWithEncryption(context: Context): List<Pair<String, Boolean>> {
            return getAccounts(context)
                .map { account: Account ->
                    account.name to (AccountManager.get(context).getUserData(account, KEY_ENCRYPTED) != null)
                }
        }

        fun storePassword(
            context: Context,
            account: Account,
            encryptionPassword: String?
        ) {
            AccountManager.get(context)
                .setUserData(account, KEY_PASSWORD_ENCRYPTION, encryptionPassword)
        }

        fun loadPassword(context: Context, accountName: String): String? {
            return loadPassword(context, getAccount(accountName))
        }

        fun loadPassword(context: Context, account: Account): String? {
            return AccountManager.get(context).getUserData(account, KEY_PASSWORD_ENCRYPTION)
        }

        fun migratePasswords(context: Context) {
            getAccounts(context).forEach { account ->
                val legacyPasswordKey = "${account.name} - $KEY_PASSWORD_ENCRYPTION"
                DbUtils.loadSetting(context.contentResolver, legacyPasswordKey)?.let {
                    Timber.i("Migrated password for ${account.name}")
                    storePassword(context, account, it)
                    context.contentResolver.delete(
                        TransactionProvider.SETTINGS_URI,
                        DatabaseConstants.KEY_KEY + " = ?", arrayOf(legacyPasswordKey)
                    )
                }
            }
        }

        fun updateAccountsIsSyncable(
            context: Application,
            licenceHandler: LicenceHandler,
            prefHandler: PrefHandler
        ) {
            val isSyncable = licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)
            val accountManager = AccountManager.get(context)
            getAccounts(context)
                .filter { account ->
                    accountManager.getUserData(
                        account,
                        KEY_BROKEN
                    ) == null
                }
                .forEach { account ->
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
        fun getAccountNames(context: Context): Array<String> =
            getAccounts(context).map { it.name }.toTypedArray()

        fun activateSync(account: String, prefHandler: PrefHandler) {
            activateSync(getAccount(account), prefHandler)
        }

        private fun activateSync(account: Account, prefHandler: PrefHandler) {
            ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 1)
            configureAutomaticAndPeriod(account, prefHandler)
        }

        fun configureAutomaticAndPeriod(account: Account, prefHandler: PrefHandler) {
            val pollFrequency = getSyncFrequency(prefHandler)
            ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, pollFrequency > 0)
            if (pollFrequency > 0) {
                ContentResolver.addPeriodicSync(
                    account, TransactionProvider.AUTHORITY, Bundle.EMPTY,
                    pollFrequency
                )
            } else {
                ContentResolver.removePeriodicSync(
                    account,
                    TransactionProvider.AUTHORITY,
                    Bundle.EMPTY
                )
            }
        }

        private fun getSyncFrequency(prefHandler: PrefHandler) =
            prefHandler.getInt(
                PrefKey.SYNC_FREQUCENCY,
                DEFAULT_SYNC_FREQUENCY_HOURS
            ) * HOUR_IN_SECONDS

        fun deactivateSync(account: Account) {
            if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
                ContentResolver.cancelSync(account, TransactionProvider.AUTHORITY)
                ContentResolver.setSyncAutomatically(account, TransactionProvider.AUTHORITY, false)
                ContentResolver.removePeriodicSync(
                    account,
                    TransactionProvider.AUTHORITY,
                    Bundle.EMPTY
                )
                ContentResolver.setIsSyncable(account, TransactionProvider.AUTHORITY, 0)
            }
        }
    }
}

fun AccountManager.getSyncProviderUrl(account: Account) =
    getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL)
        ?: throw SyncBackendProvider.SyncParseException(NullPointerException("sync_provider_url is null"))