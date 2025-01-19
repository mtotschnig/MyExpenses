package org.totschnig.myexpenses.di

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandlerImpl
import org.totschnig.myexpenses.provider.DATABASE_VERSION
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import org.totschnig.myexpenses.provider.TransactionDatabase
import org.totschnig.myexpenses.provider.doRepairRequerySchema
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

interface SqlCryptProvider {
    fun provideEncryptedDatabase(context: Context): SupportSQLiteOpenHelper.Factory
    fun decrypt(context: Context, encrypted: File, backupDb: File)
    fun encrypt(context: Context, backupFile: File, currentDb: File)
}

@Module
open class DataModule(private val shouldInsertDefaultTransferCategory: Boolean = true) {
    companion object {
        val cryptProvider: SqlCryptProvider by lazy {
            Class.forName("org.totschnig.sqlcrypt.SQLiteOpenHelperFactory")
                .getDeclaredConstructor().newInstance() as SqlCryptProvider
        }
    }

    open val databaseName = "data"

    open val uiSettingsName = "UI-Settings"

    @Provides
    @Named(AppComponent.UI_SETTINGS_DATASTORE_NAME)
    @Singleton
    open fun provideUiSettingsDataStoreName(): String = uiSettingsName

    @Provides
    @Named(AppComponent.DATABASE_NAME)
    @Singleton
    @JvmSuppressWildcards
    fun provideDatabaseName(): (Boolean) -> String =
        { if (it) "${databaseName}.enc" else databaseName }

    @Provides
    @Singleton
    open fun providePrefHandler(
        context: MyApplication,
        sharedPreferences: SharedPreferences,
    ): PrefHandler = PrefHandlerImpl(context, sharedPreferences)

    @Singleton
    @Provides
    open fun provideSharedPreferences(
        application: MyApplication,
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    @Singleton
    @Provides
    fun providePreferencesDataStore(
        appContext: MyApplication,
        @Named(AppComponent.UI_SETTINGS_DATASTORE_NAME) uiSettingsDataStoreName: String
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile(uiSettingsDataStoreName) }
        )
    }

    @Provides
    fun provideSQLiteOpenHelper(
        appContext: MyApplication,
        prefHandler: PrefHandler,
        @Named(AppComponent.DATABASE_NAME) provideDatabaseName: (@JvmSuppressWildcards Boolean) -> String,
    ): SupportSQLiteOpenHelper {
        val encryptDatabase = prefHandler.encryptDatabase
        Timber.w("building SupportSQLiteOpenHelper (encryptDatabase %b)", encryptDatabase)
        return when {
            encryptDatabase -> cryptProvider.provideEncryptedDatabase(appContext)
            else -> FrameworkSQLiteOpenHelperFactory()
        }.create(
            SupportSQLiteOpenHelper.Configuration.builder(appContext)
                .name(provideDatabaseName(encryptDatabase)).callback(
                    TransactionDatabase(
                        appContext,
                        prefHandler,
                        shouldInsertDefaultTransferCategory
                    )
                ).build()
        ).also {
            it.setWriteAheadLoggingEnabled(false)
        }
    }

    @Singleton
    @Provides
    open fun providePeekHelper(prefHandler: PrefHandler): DatabaseVersionPeekHelper =
        DatabaseVersionPeekHelper { context, path ->
            kotlin.runCatching {
                val version = try {
                    SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
                } catch (e: SQLiteException) {
                    CrashHandler.report(e)
                    doRepairRequerySchema(path)
                    null
                }?.use { database ->
                    database.version.also {
                        if (it > DATABASE_VERSION)
                            throw Throwable(
                                context.getString(
                                    R.string.restore_cannot_downgrade, it, DATABASE_VERSION
                                )
                            )
                    }
                }
                if (version == 132 || version == 133) {
                    doRepairRequerySchema(path)
                }
                SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use {
                    if (!try {
                            it.isDatabaseIntegrityOk
                        } catch (_: SQLiteDatabaseCorruptException) {
                            false
                        }
                    ) {
                        throw Exception("Database integrity check failed")
                    }
                }
            }
        }
}