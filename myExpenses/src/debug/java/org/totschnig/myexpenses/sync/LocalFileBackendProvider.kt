package org.totschnig.myexpenses.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.internal.Preconditions
import org.acra.util.StreamReader
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.getMimeType
import java.io.*

class LocalFileBackendProvider internal constructor(context: Context, uri: Uri) :
    AbstractSyncBackendProvider<DocumentFile, DocumentFile>(context) {
    private val baseDir: DocumentFile =
        DocumentFile.fromTreeUri(context, uri) ?: throw IOException("Cannot create baseDir")
    private lateinit var accountDir: DocumentFile

    private val contentResolver
        get() = context.contentResolver

    private val metaDataFile
        get() = accountDir.findFile(accountMetadataFilename)

    @Throws(IOException::class)
    override fun withAccount(account: Account) {
        setAccountUuid(account)
        accountDir = baseDir.requireFolder(account.uuid!!)
        writeAccount(account, false)
    }

    private fun DocumentFile.requireFolder(name: String): DocumentFile {
        check(isDirectory)
        return findFile(name)?.also {
            if (!it.isDirectory) throw IOException("file exists, but is no directory")
        } ?: createDirectory(name) ?: throw IOException("cannot create directory")
    }

    @Throws(IOException::class)
    override fun writeAccount(account: Account, update: Boolean) {
        val metaData = metaDataFile
        if (update && metaData == null) throw FileNotFoundException()
        if (update || metaData == null) {
            saveFileContents(
                file = metaData ?: accountDir.createFile(
                    MIME_TYPE_JSON,
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
    override val sharedPreferencesName: String
        get() = "local_file_backend" // currently not used

    @Throws(IOException::class)
    override fun readEncryptionToken() =
        baseDir.findFile(ENCRYPTION_TOKEN_FILE_NAME)?.let { documentFile ->
            contentResolver.openInputStream(documentFile.uri)?.use { StreamReader(it).read() }
        }

    @Throws(IOException::class)
    override fun getInputStreamForPicture(relativeUri: String): InputStream {
        return accountDir.findFile(relativeUri)?.let { contentResolver.openInputStream(it.uri) }
            ?: throw FileNotFoundException()
    }

    @Throws(IOException::class)
    override fun saveUriToAccountDir(fileName: String, uri: Uri) {
        saveUriToFolder(fileName, uri, accountDir, true)
    }

    @Throws(IOException::class)
    private fun saveUriToFolder(
        fileName: String,
        uri: Uri,
        folder: DocumentFile,
        maybeEncrypt: Boolean
    ) {
        val input = contentResolver.openInputStream(uri)
        val output = folder.createFile(getMimeType(fileName), fileName)
            ?.let { contentResolver.openOutputStream(it.uri) }

        if (input == null) {
            throw IOException("Could not open InputStream $uri")
        }
        if (output == null) {
            throw IOException("Could not open OutputStream $folder")
        }

        input.use { `in` ->
            (if (maybeEncrypt) maybeEncrypt(output) else output).use { `out` ->
                FileCopyUtils.copy(`in`, `out`)
            }
        }
    }

    @Throws(IOException::class)
    override fun storeBackup(uri: Uri, fileName: String) {
        val backupDir = baseDir.requireFolder(BACKUP_FOLDER_NAME)
        saveUriToFolder(fileName, uri, backupDir, false)
    }

    override val storedBackups: List<String>
        get() = baseDir.findFile(BACKUP_FOLDER_NAME)?.listFiles()?.mapNotNull { it.name }
            ?: emptyList()

    @Throws(FileNotFoundException::class)
    override fun getInputStreamForBackup(backupFile: String): InputStream {
        return baseDir.requireFolder(BACKUP_FOLDER_NAME).findFile(backupFile)?.uri?.let {
            contentResolver.openInputStream(it)
        } ?: throw FileNotFoundException()
    }

    override fun getLastSequence(start: SequenceNumber): SequenceNumber {
        val fileComparator = Comparator { o1: DocumentFile, o2: DocumentFile ->
            Utils.compare(
                getSequenceFromFileName(o1.name!!),
                getSequenceFromFileName(o2.name!!)
            )
        }
        val lastShardOptional = accountDir.listFiles().filter { file ->
            file.isDirectory && file.name?.let {
                isAtLeastShardDir(
                    start.shard,
                    it
                )
            } == true
        }.maxWithOrNull(fileComparator)
        val lastShard: DocumentFile?
        val lastShardInt: Int
        val reference: Int
        if (lastShardOptional != null) {
            lastShard = lastShardOptional
            lastShardInt = getSequenceFromFileName(lastShard.name!!)
            reference = if (lastShardInt == start.shard) start.number else 0
        } else {
            if (start.shard > 0) return start
            lastShard = accountDir
            lastShardInt = 0
            reference = start.number
        }
        return lastShard.listFiles().filter { file ->
            file.name?.let {
                isNewerJsonFile(
                    reference,
                    it
                )
            } == true
        }
            .maxWithOrNull(fileComparator)
            ?.let { file -> SequenceNumber(lastShardInt, getSequenceFromFileName(file.name!!)) }
            ?: start
    }

    override fun collectionForShard(shardNumber: Int) =
        if (shardNumber == 0) accountDir else accountDir.findFile("_$shardNumber")

    override fun childrenForCollection(folder: DocumentFile) = folder.listFiles().asList()

    override fun nameForResource(resource: DocumentFile) = resource.name

    override fun lock() {}

    override fun getChangeSetFromResource(shardNumber: Int, resource: DocumentFile): ChangeSet {
        val inputStream = contentResolver.openInputStream(resource.uri) ?: throw IOException()
        return getChangeSetFromInputStream(
            SequenceNumber(shardNumber, getSequenceFromFileName(resource.name!!)), inputStream
        )
    }

    private fun getAccountMetaData(file: DocumentFile): Result<AccountMetaData> {
        return try {
            val inputStream = contentResolver.openInputStream(file.uri)
            getAccountMetaDataFromInputStream(inputStream)
        } catch (e: IOException) {
            log().e(e)
            Result.failure(e)
        }
    }

    override fun unlock() {}
    override fun toString(): String {
        return baseDir.toString()
    }

    @Throws(IOException::class)
    override fun saveFileContentsToAccountDir(
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        Preconditions.checkNotNull(accountDir)
        val dir = if (folder == null) accountDir else accountDir.requireFolder(folder)
        saveFileContents(dir, fileName, fileContents, mimeType, maybeEncrypt)
    }

    @Throws(IOException::class)
    override fun saveFileContentsToBase(
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        saveFileContents(baseDir, fileName, fileContents, mimeType, maybeEncrypt)
    }

    override val existingLockToken: String?
        get() = null

    override fun writeLockToken(lockToken: String) {}

    private fun saveFileContents(folder: DocumentFile, fileName: String, fileContents: String, mimeType: String, maybeEncrypt: Boolean) {
        saveFileContents(folder.createFile(mimeType, fileName) ?: throw  IOException(), fileContents, maybeEncrypt)
    }

    @Throws(IOException::class)
    private fun saveFileContents(file: DocumentFile, fileContents: String, maybeEncrypt: Boolean) {
        (contentResolver.openOutputStream(file.uri) ?: throw IOException()).use {
            OutputStreamWriter(if (maybeEncrypt) maybeEncrypt(it) else it).write(fileContents)
        }
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = baseDir.listFiles().filter { obj -> obj.isDirectory }
            .filter { directory -> directory.name != BACKUP_FOLDER_NAME }
            .mapNotNull { directory -> directory.findFile(accountMetadataFilename) }
            .map { file -> getAccountMetaData(file) }

    override val isEmpty: Boolean
        get() = baseDir.listFiles().isEmpty()

    init {
        if (!baseDir.isDirectory) {
            throw RuntimeException("No directory $uri")
        }
    }
}