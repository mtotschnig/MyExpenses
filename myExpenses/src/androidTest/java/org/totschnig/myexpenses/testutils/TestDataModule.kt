package org.totschnig.myexpenses.testutils

import android.content.Context
import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.DataModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import org.totschnig.myexpenses.provider.TransactionDatabase
import org.totschnig.myexpenses.util.ResultUnit
import timber.log.Timber
import java.util.UUID

open class TestDataModule(
    private val forceEncryptProvider: () -> Boolean = { withEncryption }
) : DataModule() {
    private val randomDataName: String = UUID.randomUUID().toString()

    private val forceEncrypt: Boolean
        get() = forceEncryptProvider()

    override val databaseName = randomDataName
    override val uiSettingsName = super.uiSettingsName + "- " + randomDataName

    override fun providePrefHandler(context: MyApplication, sharedPreferences: SharedPreferences): PrefHandler {
        return TestPrefHandler(context, sharedPreferences, randomDataName, forceEncryptProvider)
    }

    override fun provideSharedPreferences(application: MyApplication): SharedPreferences =
            application.getSharedPreferences(randomDataName, Context.MODE_PRIVATE)

    override fun providePeekHelper(prefHandler: PrefHandler): DatabaseVersionPeekHelper =
        DatabaseVersionPeekHelper { _, _ -> ResultUnit }

    override fun provideSQLiteOpenHelper(
        appContext: MyApplication,
        prefHandler: PrefHandler,
        provideDatabaseName: (Boolean) -> String
    ): SupportSQLiteOpenHelper {
        if (forceEncrypt) {
            return super.provideSQLiteOpenHelper(appContext, prefHandler, provideDatabaseName)
        }
        val encryptDatabase = prefHandler.encryptDatabase
        Timber.w("building SupportSQLiteOpenHelper (encryptDatabase %b)", encryptDatabase)
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(appContext)
                .name(provideDatabaseName(encryptDatabase))
                .callback(TransactionDatabase(appContext, prefHandler))
                .build()
        )
    }
}