package org.totschnig.myexpenses.sync

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import dagger.internal.Preconditions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.StreamReader
import java.io.*

class LocalFileBackendProvider internal constructor(context: Context?, filePath: String) :
    AbstractSyncBackendProvider(
        context!!
    ) {
    private val baseDir: File = File(filePath)
    private lateinit var accountDir: File

    @Throws(IOException::class)
    override fun withAccount(account: Account) {
        setAccountUuid(account)
        accountDir = File(baseDir, account.uuid!!)
        accountDir.mkdir()
        if (accountDir.isDirectory) {
            writeAccount(account, false)
        } else {
            throw IOException("Cannot create account dir")
        }
    }

    @Throws(IOException::class)
    override fun writeAccount(account: Account, update: Boolean) {
        val metaData = File(accountDir, accountMetadataFilename)
        if (update || !metaData.exists()) {
            saveFileContents(metaData, buildMetadata(account), true)
            if (!update) {
                createWarningFile()
            }
        }
    }

    override fun readAccountMetaData(): Result<AccountMetaData> {
        return getAccountMetaDataFromFile(File(accountDir, accountMetadataFilename))
    }

    @Throws(IOException::class)
    override fun resetAccountData(uuid: String) {
        //we do not set the member, this needs to be done through withAccount
        val accountDir = File(baseDir, uuid)
        if (accountDir.isDirectory) {
            accountDir.list()?.forEach {
                if (!File(accountDir, it).delete()) {
                    throw IOException("Cannot reset account dir")
                }
            }
        }
    }

    // currently not used
    override val sharedPreferencesName: String
        get() = "local_file_backend" // currently not used

    @Throws(IOException::class)
    override fun readEncryptionToken(): String? {
        return try {
            StreamReader(FileInputStream(File(baseDir, ENCRYPTION_TOKEN_FILE_NAME))).read()
        } catch (e: FileNotFoundException) {
            null
        }
    }

    @Throws(IOException::class)
    override fun getInputStreamForPicture(relativeUri: String): InputStream {
        return FileInputStream(File(accountDir, relativeUri))
    }

    @Throws(IOException::class)
    override fun saveUriToAccountDir(fileName: String, uri: Uri) {
        context.contentResolver.openInputStream(uri).use { input ->
            maybeEncrypt(
                FileOutputStream(
                    File(accountDir, fileName)
                )
            ).use { output ->
                if (input == null) {
                    throw IOException("Could not open InputStream $uri")
                }
                FileCopyUtils.copy(input, output)
            }
        }
    }

    @Throws(IOException::class)
    private fun saveUriToFolder(fileName: String, uri: Uri, folder: File) {
        FileCopyUtils.copy(uri, Uri.fromFile(File(folder, fileName)))
    }

    @Throws(IOException::class)
    override fun storeBackup(uri: Uri, fileName: String) {
        val backupDir = File(baseDir, BACKUP_FOLDER_NAME)
        backupDir.mkdir()
        if (!backupDir.isDirectory) {
            throw IOException("Unable to create directory for backups")
        }
        saveUriToFolder(fileName, uri, backupDir)
    }

    override val storedBackups: List<String>
        get() = File(baseDir, BACKUP_FOLDER_NAME).list()?.asList() ?: emptyList()

    @Throws(FileNotFoundException::class)
    override fun getInputStreamForBackup(backupFile: String): InputStream {
        return FileInputStream(File(File(baseDir, BACKUP_FOLDER_NAME), backupFile))
    }

    override fun getLastSequence(start: SequenceNumber): SequenceNumber {
        val fileComparator = Comparator { o1: File, o2: File ->
            Utils.compare(
                getSequenceFromFileName(o1.name),
                getSequenceFromFileName(o2.name)
            )
        }
        val lastShardOptional = accountDir.listFiles { file: File ->
            file.isDirectory && isAtLeastShardDir(
                start.shard,
                file.name
            )
        }?.maxWithOrNull(fileComparator)
        val lastShard: File?
        val lastShardInt: Int
        val reference: Int
        if (lastShardOptional != null) {
            lastShard = lastShardOptional
            lastShardInt = getSequenceFromFileName(lastShard.name)
            reference = if (lastShardInt == start.shard) start.number else 0
        } else {
            if (start.shard > 0) return start
            lastShard = accountDir
            lastShardInt = 0
            reference = start.number
        }
        return lastShard.listFiles { file: File ->
            isNewerJsonFile(
                reference,
                file.name
            )
        }
            ?.maxWithOrNull(fileComparator)
            ?.let { file: File -> SequenceNumber(lastShardInt, getSequenceFromFileName(file.name)) }
            ?: start
    }

    private fun filterFiles(sequenceNumber: SequenceNumber): List<Pair<Int, File>> {
        Preconditions.checkNotNull(accountDir)
        return buildList {
            var nextShard = sequenceNumber.shard
            var startNumber = sequenceNumber.number
            while (true) {
                val nextShardDir =
                    if (nextShard == 0) accountDir else File(accountDir, "_$nextShard")
                if (nextShardDir.isDirectory) {
                    nextShardDir.listFiles { file: File -> isNewerJsonFile(startNumber, file.name) }
                        ?.also {
                            it.sortBy { getSequenceFromFileName(it.name) }
                        }
                        ?.map { file: File -> Pair.create(nextShard, file) }
                        ?.forEach { add(it) }
                    nextShard++
                    startNumber = 0
                } else {
                    break
                }
            }
        }
    }

    override fun lock() {}

    @Throws(IOException::class)
    override fun getChangeSetSince(
        sequenceNumber: SequenceNumber,
        context: Context
    ): ChangeSet? {
        val changeSets: MutableList<ChangeSet> = ArrayList()
        for (file in filterFiles(sequenceNumber)) {
            changeSets.add(getChangeSetFromFile(file))
        }
        return merge(changeSets)
    }

    @Throws(IOException::class)
    private fun getChangeSetFromFile(file: Pair<Int, File>): ChangeSet {
        val inputStream = FileInputStream(file.second)
        return getChangeSetFromInputStream(
            SequenceNumber(file.first, getSequenceFromFileName(file.second.name)), inputStream
        )
    }

    private fun getAccountMetaDataFromFile(file: File): Result<AccountMetaData> {
        return try {
            val inputStream = FileInputStream(file)
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
        val dir = if (folder == null) accountDir else File(accountDir, folder)
        dir.mkdir()
        if (dir.isDirectory) {
            saveFileContents(File(dir, fileName), fileContents, maybeEncrypt)
        } else {
            throw IOException("Cannot create dir")
        }
    }

    @Throws(IOException::class)
    override fun saveFileContentsToBase(
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        saveFileContents(File(baseDir, fileName), fileContents, maybeEncrypt)
    }

    override val existingLockToken: String?
        get() = null

    override fun writeLockToken(lockToken: String) {}

    @Throws(IOException::class)
    private fun saveFileContents(file: File, fileContents: String, maybeEncrypt: Boolean) {
        val out: OutputStreamWriter
        val fileOutputStream = FileOutputStream(file)
        out =
            OutputStreamWriter(if (maybeEncrypt) maybeEncrypt(fileOutputStream) else fileOutputStream)
        out.write(fileContents)
        out.close()
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = baseDir.listFiles { obj: File -> obj.isDirectory }
            ?.filter { directory: File -> directory.name != BACKUP_FOLDER_NAME }
            ?.map { directory: File? -> File(directory, accountMetadataFilename) }
            ?.filter { obj: File -> obj.exists() }
            ?.map { file: File -> getAccountMetaDataFromFile(file) } ?: emptyList()

    override val isEmpty: Boolean
        get() = baseDir.list()?.size ?: 0 == 0

    init {
        if (!baseDir.isDirectory) {
            throw RuntimeException("No directory $filePath")
        }
    }
}