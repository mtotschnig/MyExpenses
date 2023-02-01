package org.totschnig.myexpenses.di

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.getkeepsafe.relinker.ReLinker
import com.squareup.sqlbrite3.SqlBrite
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandlerImpl
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import org.totschnig.myexpenses.provider.TransactionDatabase
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

interface SqlCryptProvider {
    fun provideEncryptedDatabase(context: Context): SupportSQLiteOpenHelper.Factory
    fun decrypt(context: Context, encrypted: File, backupDb: File)
    fun encrypt(context: Context, backupFile: File, currentDb: File)
}

@Module
open class DataModule(private val frameWorkSqlite: Boolean = false) {
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
    open fun provideDatabaseName(): (Boolean) -> String = { if (it) "${databaseName}.enc" else databaseName }

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
        val encryptDatabase = prefHandler.encryptDatabase
        return when {
            encryptDatabase -> cryptProvider.provideEncryptedDatabase(appContext)
            frameWorkSqlite -> FrameworkSQLiteOpenHelperFactory()
            else -> {
                ReLinker.loadLibrary(
                    appContext,
                    io.requery.android.database.sqlite.SQLiteDatabase.LIBRARY_NAME
                )
                RequerySQLiteOpenHelperFactory()
            }
        }.create(
            SupportSQLiteOpenHelper.Configuration.builder(appContext)
                .name(provideDatabaseName(encryptDatabase)).callback(
                    //Robolectric uses native Sqlite which as of now does not include Json extension
                    TransactionDatabase(!frameWorkSqlite)
                ).build()
        ).also {
            it.setWriteAheadLoggingEnabled(false)
        }
    }

    @Singleton
    @Provides
    open fun providePeekHelper(): DatabaseVersionPeekHelper =
        DatabaseVersionPeekHelper { path ->
            when {
                frameWorkSqlite -> {
                    SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use {
                        it.version
                    }
                }
                else -> {
                    io.requery.android.database.sqlite.SQLiteDatabase.openDatabase(
                        path,
                        null,
                        io.requery.android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    ).use {
                        it.version
                    }
                }
            }
        }
}