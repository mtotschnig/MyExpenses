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
import com.squareup.sqlbrite3.SqlBrite
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandlerImpl
import org.totschnig.myexpenses.provider.DATABASE_VERSION
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import org.totschnig.myexpenses.provider.TransactionDatabase
import org.totschnig.myexpenses.provider.doRepairRequerySchema
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
open class DataModule {
    companion object {
        val cryptProvider: SqlCryptProvider
            get() = Class.forName("org.totschnig.sqlcrypt.SQLiteOpenHelperFactory")
                .newInstance() as SqlCryptProvider
    }

    open val databaseName = "data"

    @Provides
    @Named(AppComponent.DATABASE_NAME)
    @Singleton
    @JvmSuppressWildcards
    open fun provideDatabaseName(): (Boolean) -> String =
        { if (it) "${databaseName}.enc" else databaseName }

    @Provides
    @Singleton
    fun provideSqlBrite(application: MyApplication) =
        SqlBrite.Builder().build().wrapContentProvider(
            application.contentResolver, Schedulers.io()
        )

    @Provides
    @Singleton
    open fun providePrefHandler(
        context: MyApplication,
        sharedPreferences: SharedPreferences
    ): PrefHandler {
        return PrefHandlerImpl(context, sharedPreferences)
    }

    @Singleton
    @Provides
    open fun provideSharedPreferences(
        application: MyApplication
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    @Singleton
    @Provides
    fun providePreferencesDataStore(appContext: MyApplication): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("UI-Settings") }
        )
    }

    @Provides
    fun provideSQLiteOpenHelper(
        appContext: MyApplication,
        prefHandler: PrefHandler,
        @Named(AppComponent.DATABASE_NAME) provideDatabaseName: (@JvmSuppressWildcards Boolean) -> String
    ): SupportSQLiteOpenHelper {
        Timber.w("building SupportSQLiteOpenHelper")
        val encryptDatabase = prefHandler.encryptDatabase
        return when {
            encryptDatabase -> cryptProvider.provideEncryptedDatabase(appContext)
            else -> FrameworkSQLiteOpenHelperFactory()
        }.create(
            SupportSQLiteOpenHelper.Configuration.builder(appContext)
                .name(provideDatabaseName(encryptDatabase)).callback(
                    //Robolectric uses native Sqlite which as of now does not include Json extension
                    TransactionDatabase(prefHandler)
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
                        } catch (e: SQLiteDatabaseCorruptException) {
                            false
                        }
                    ) {
                        throw Exception("Database integrity check failed")
                    }
                }
            }
        }
}