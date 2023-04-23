package org.totschnig.sqlcrypt

import android.content.Context
import androidx.annotation.Keep
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.totschnig.myexpenses.di.SqlCryptProvider
import java.io.File
import java.io.IOException

@Keep
class SQLiteOpenHelperFactory : SqlCryptProvider {
    private fun passPhrase(context: Context): ByteArray =
        PassphraseRepository(context).getPassphrase()

    override fun provideEncryptedDatabase(context: Context) = SupportFactory(passPhrase(context))

    /**
     * https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html
     */
    override fun decrypt(context: Context, encrypted: File, backupDb: File) {
        val originalDb = SQLiteDatabase.openDatabase(
            encrypted.absolutePath,
            passPhrase(context),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
            null
        )

        SQLiteDatabase.openOrCreateDatabase(
            backupDb.absolutePath,
            "",
            null
        ).close() // create an empty database

        //language=text
        val version = originalDb.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''").use {
            it.bindString(1, backupDb.absolutePath)
            it.execute()
            originalDb.rawExecSQL("SELECT sqlcipher_export('plaintext')")
            originalDb.rawExecSQL("DETACH DATABASE plaintext")

            originalDb.version
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
        SQLiteDatabase.loadLibs(context)
        if (currentDb.exists()) {
            if (!currentDb.delete())
                throw IOException("File $currentDb exists and cannot be deleted.")
        }
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