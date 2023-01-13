package org.totschnig.sqlcrypt

import android.content.Context
import androidx.annotation.Keep
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.totschnig.myexpenses.di.SqlCryptProvider
import java.io.File

@Keep
class SQLiteOpenHelperFactory: SqlCryptProvider {
    private fun passPhrase(context: Context): ByteArray = PassphraseRepository(context).getPassphrase()

    override fun provideEncryptedDatabase(context: Context) = SupportFactory(passPhrase(context))

    /**
     * https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html
     */
    override fun decrypt(db: SupportSQLiteDatabase, backupDb: File) {
        SQLiteDatabase.openOrCreateDatabase(
            backupDb.absolutePath,
            "",
            null
        ).close() // create an empty database

        //language=text
        val version = db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''").use {
            it.bindString(1, backupDb.absolutePath)
            it.execute()
            db.execSQL("SELECT sqlcipher_export('plaintext')")
            db.execSQL("DETACH DATABASE plaintext")

            db.version
        }

        SQLiteDatabase.openOrCreateDatabase(
            backupDb.absolutePath,
            "",
            null
        ).use {
            it.version = version
        }
    }

    /**
     * https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html
     */
    override fun encrypt(context: Context, backupFile: File, currentDb: File) {
        val version = SQLiteDatabase.openDatabase(
            backupFile.absolutePath,
            "",
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use {
            it.version
        }

        SQLiteDatabase.openOrCreateDatabase(
            currentDb.absolutePath,
            passPhrase(context),
            null
        ).use { db ->
            //language=text
            db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''").use {
                it.bindString(1, backupFile.absolutePath)
                it.execute()
                db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
                db.rawExecSQL("DETACH DATABASE plaintext")
                db.version = version
            }
        }
    }
}