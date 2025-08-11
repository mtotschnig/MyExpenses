package org.totschnig.myexpenses.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.acra.util.StreamReader
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.getMimeType
import java.io.*

class StorageAccessFrameworkBackendProvider internal constructor(context: Context, uri: Uri) :
    AbstractSyncBackendProvider<DocumentFile>(context) {
    private val baseDir: DocumentFile =
        DocumentFile.fromTreeUri(context, uri) ?: throw IOException("Cannot create baseDir")
    private lateinit var accountDir: DocumentFile

    private val contentResolver
        get() = context.contentResolver

    private val metaDataFile
        get() = accountDir.findFile(accountMetadataFilename)

    override val accountRes: DocumentFile
        get() = accountDir

    @Throws(IOException::class)
    override fun withAccount(account: Account) {
        super.withAccount(account)
        accountDir = baseDir.getFolder(accountUuid) ?: throw IOException("Cannot create accountDir")
        writeAccount(account, false)
    }

    private fun DocumentFile.getFolder(name: String, require: Boolean = true): DocumentFile? {
        if (!isDirectory) throw IOException("${this.name} is not a directory")
        return findFile(name)?.also {
            if (!it.isDirectory) throw IOException("file $name exists, but is not a directory")
        } ?: (if (require) createDirectory(name) else null)
    }

    @Throws(IOException::class)
    override fun writeAccount(account: Account, update: Boolean) {
        val metaData = metaDataFile
        if (update && metaData == null) throw FileNotFoundException()
        if (update || metaData == null) {
            saveFileContents(
                file = metaData ?: accountDir.createFile(
                    mimeTypeForData,
                    accountMetadataFilename
                ) ?: throw IOException(""),
                fileContents = buildMetadata(account),
                maybeEncrypt = true
            )
            if (!update) {
                createWarningFile()
            }
        }
    }

    override fun readAccountMetaData() = metaDataFile?.let { getAccountMetaData(it) }
        ?: Result.failure(IOException("No metaDatafile"))

    @Throws(IOException::class)
    override fun resetAccountData(uuid: String) {
        //we do not set the member, this needs to be done through withAccount
        baseDir.findFile(uuid)?.takeIf { it.isDirectory }?.let { dir ->
            dir.listFiles().forEach {
                it.delete()
            }
        }
    }

    // currently not used
    override val sharedPreferencesName = "saf" // currently not used

    override fun readFileContents(fromAccountDir: Boolean, fileName: String, maybeDecrypt: Boolean) =
        (if (fromAccountDir) accountDir else baseDir).findFile(fileName)?.let { documentFile ->
            contentResolver.openInputStream(documentFile.uri)?.use { StreamReader(maybeDecrypt(it, maybeDecrypt)).read() }
        }

    @Throws(IOException::class)
    override fun saveUriToCollection(
        fileName: String,
        uri: Uri,
        collection: DocumentFile,
        maybeEncrypt: Boolean
    ) {
        val input = contentResolver.openInputStream(uri)
        val output = collection.createFile(getMimeType(fileName), fileName)
            ?.let { contentResolver.openOutputStream(it.uri) }

        if (input == null) {
            throw IOException("Could not open InputStream $uri")
        }
        if (output == null) {
            throw IOException("Could not open OutputStream $collection")
        }

        input.use { `in` ->
            (if (maybeEncrypt) maybeEncrypt(output) else output).use { `out` ->
                FileCopyUtils.copy(`in`, `out`)
            }
        }
    }

    override fun getResInAccountDir(resourceName: String) = accountDir.findFile(resourceName)

    override fun getCollection(collectionName: String, require: Boolean) =
        baseDir.getFolder(collectionName, require)

    override fun childrenForCollection(folder: DocumentFile?) =
        (folder ?: accountDir).listFiles().filter { it.length() > 0 }

    override fun nameForResource(resource: DocumentFile) = resource.name

    override fun isCollection(resource: DocumentFile) = resource.isDirectory

    override fun getInputStream(resource: DocumentFile) = contentResolver.openInputStream(resource.uri) ?: throw IOException()

    private fun getAccountMetaData(file: DocumentFile) = try {
        getAccountMetaDataFromInputStream(
            contentResolver.openInputStream(file.uri) ?: throw IOException()
        )
    } catch (e: IOException) {
        log().e(e)
        Result.failure(e)
    }

    override fun deleteLockTokenFile() {
        if (accountDir.findFile(LOCK_FILE)?.delete() != true) throw IOException()
    }

    @Throws(IOException::class)
    override fun saveFileContents(
        toAccountDir: Boolean,
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        val base = if (toAccountDir) accountDir else baseDir
        val dir = if (folder == null) base else base.getFolder(folder)!!
        saveFileContents(dir, fileName, fileContents, mimeType, maybeEncrypt)
    }

    private fun saveFileContents(
        folder: DocumentFile,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        val file = (folder.findFile(fileName) ?: folder.createFile(mimeType, fileName)
        ?: throw IOException())
        saveFileContents(file, fileContents, maybeEncrypt)
    }

    @Throws(IOException::class)
    private fun saveFileContents(file: DocumentFile, fileContents: String, maybeEncrypt: Boolean) {
        (contentResolver.openOutputStream(file.uri, "rwt") ?: throw IOException()).use { out ->
            (if (maybeEncrypt) maybeEncrypt(out) else out).bufferedWriter().use {
                it.write(fileContents)
            }
        }
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = baseDir.listFiles().filter { obj -> obj.isDirectory }
            .filter { directory -> verifyRemoteAccountFolderName(directory.name) }
            .mapNotNull { directory -> directory.findFile(accountMetadataFilename) }
            .map { file -> getAccountMetaData(file) }

    override val isEmpty: Boolean
        get() = baseDir.listFiles().isEmpty()

    init {
        if (!baseDir.isDirectory) {
            throw SyncBackendProvider.SyncParseException("No directory $uri")
        }
    }
}