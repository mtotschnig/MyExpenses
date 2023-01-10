package org.totschnig.sqlcrypt

import androidx.annotation.Keep
import net.sqlcipher.database.SupportFactory
import org.totschnig.myexpenses.di.SqlCryptProvider

@Keep
class SQLiteOpenHelperFactory: SqlCryptProvider {
    override fun provideEncryptedDatabase() = SupportFactory("secret passphrase".toByteArray())
}