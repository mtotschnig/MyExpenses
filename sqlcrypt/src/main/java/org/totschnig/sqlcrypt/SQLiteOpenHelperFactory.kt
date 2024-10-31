package org.totschnig.sqlcrypt

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.totschnig.myexpenses.di.SqlCryptProvider
import org.totschnig.myexpenses.util.crypt.PassphraseRepository
import java.io.File
import java.io.IOException
import java.security.SecureRandom

private const val PASSPHRASE_LENGTH = 32

@Keep
class SQLiteOpenHelperFactory : SqlCryptProvider {
    private fun passPhrase(context: Context): ByteArray =
        PassphraseRepository(context, File(context.filesDir, "passphrase.bin")) {
            val random = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SecureRandom.getInstanceStrong()
            } else {
                SecureRandom()
            }
            val result = ByteArray(PASSPHRASE_LENGTH)

            random.nextBytes(result)

            // filter out zero byte values, as SQLCipher does not like them
            while (result.contains(0)) {
                random.nextBytes(result)
            }

            result
        }
            .getPassphrase()

    override fun provideEncryptedDatabase(context: Context): SupportOpenHelperFactory {
        System.loadLibrary("sqlcipher")
        return SupportOpenHelperFactory(passPhrase(context))
    }

    /**
     * https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html
     */
    override fun decrypt(context: Context, encrypted: File, backupDb: File) {
        System.loadLibrary("sqlcipher")
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
            null
        ).use {
            it.version = version
        }
    }

    /**
     * https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html
     */
    override fun encrypt(context: Context, backupFile: File, currentDb: File) {
        System.loadLibrary("sqlcipher")
        if (currentDb.exists()) {
            if (!currentDb.delete())
                throw IOException("File $currentDb exists and cannot be deleted.")
        }
        val version = SQLiteDatabase.openDatabase(
            backupFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use {
            it.version
        }

        SQLiteDatabase.openOrCreateDatabase(
            currentDb.absolutePath,
            passPhrase(context),
            null,
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