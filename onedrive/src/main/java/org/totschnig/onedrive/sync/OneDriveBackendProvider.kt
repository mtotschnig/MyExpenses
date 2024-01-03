package org.totschnig.onedrive.sync

import android.content.Context
import android.net.Uri
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.json.AccountMetaData
import java.io.InputStream

class OneDriveBackendProvider internal constructor(context: Context, folderName: String) :
    AbstractSyncBackendProvider<Metadata>(context) {
    override val accountRes: Metadata
        get() = TODO("Not yet implemented")
    override val sharedPreferencesName: String
        get() = TODO("Not yet implemented")
    override val isEmpty: Boolean
        get() = TODO("Not yet implemented")

    override fun getResInAccountDir(resourceName: String): Metadata? {
        TODO("Not yet implemented")
    }

    override fun saveFileContents(
        toAccountDir: Boolean,
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ): String? {
        TODO("Not yet implemented")
    }

    override fun writeAccount(account: Account, update: Boolean) {
        TODO("Not yet implemented")
    }

    override fun withAccount(account: Account) {
        TODO("Not yet implemented")
    }

    override fun resetAccountData(uuid: String) {
        TODO("Not yet implemented")
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = TODO("Not yet implemented")

    override fun readAccountMetaData(): Result<AccountMetaData> {
        TODO("Not yet implemented")
    }

    override fun getCollection(collectionName: String, require: Boolean): Metadata? {
        TODO("Not yet implemented")
    }

    override fun isCollection(resource: Metadata): Boolean {
        TODO("Not yet implemented")
    }

    override fun nameForResource(resource: Metadata): String? {
        TODO("Not yet implemented")
    }

    override fun childrenForCollection(folder: Metadata?): Collection<Metadata> {
        TODO("Not yet implemented")
    }

    override fun getInputStream(resource: Metadata): InputStream {
        TODO("Not yet implemented")
    }

    override fun saveUriToCollection(
        fileName: String,
        uri: Uri,
        collection: Metadata,
        maybeEncrypt: Boolean
    ) {
        TODO("Not yet implemented")
    }

}