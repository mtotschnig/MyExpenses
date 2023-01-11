package org.totschnig.sqlcrypt

import android.content.Context
import androidx.annotation.Keep
import net.sqlcipher.database.SupportFactory
import org.totschnig.myexpenses.di.SqlCryptProvider

@Keep
class SQLiteOpenHelperFactory: SqlCryptProvider {
    override fun provideEncryptedDatabase(context: Context) =
        SupportFactory(PassphraseRepository(context).getPassphrase())
}