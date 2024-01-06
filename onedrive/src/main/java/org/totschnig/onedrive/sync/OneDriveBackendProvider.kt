package org.totschnig.onedrive.sync

import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.microsoft.graph.http.GraphServiceException
import com.microsoft.graph.logger.DefaultLogger
import com.microsoft.graph.logger.LoggerLevel
import com.microsoft.graph.models.DriveItem
import com.microsoft.graph.models.Folder
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder
import com.microsoft.graph.requests.DriveItemContentStreamRequestBuilder
import com.microsoft.graph.requests.DriveItemRequestBuilder
import com.microsoft.graph.requests.DriveRequestBuilder
import com.microsoft.graph.requests.GraphServiceClient
import okhttp3.Request
import org.acra.util.StreamReader
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.onedrive.getAll
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture

class OneDriveBackendProvider internal constructor(context: Context, folderName: String) :
    AbstractSyncBackendProvider<DriveItem>(context) {
    private lateinit var graphClient: GraphServiceClient<Request>
    private val basePath: String = folderName

    override fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        val authToken = accountManager.peekAuthToken(
            account,
            GenericAccountService.AUTH_TOKEN_TYPE
        )
            ?: throw SyncBackendProvider.AuthException(
                NullPointerException("authToken is null"),
                null /* TODO */
            )
        graphClient = GraphServiceClient.builder()
            .logger(DefaultLogger().also {
                it.loggingLevel = LoggerLevel.DEBUG
            })
            .authenticationProvider {
                CompletableFuture.supplyAsync { authToken }
            }
            .buildClient()
        super.setUp(accountManager, account, encryptionPassword, create)
    }

    private val drive: DriveRequestBuilder
        get() = graphClient.drive()

    private fun itemWithPath(path: String): DriveItemRequestBuilder =
        drive.root().itemWithPath(path)

    private fun itemWithId(id: String) = drive.items(id)

    private val baseFolder: DriveItemRequestBuilder
        get() = itemWithPath(basePath)

    private val accountPath: String
        get() = basePath.appendPath(accountUuid!!)

    private fun String.appendPath(segment: String) = "$this/$segment"

    override val accountRes: DriveItem
        get() = itemWithPath(accountPath).buildRequest().get()!!
    override val sharedPreferencesName = "oneDrive"
    override val isEmpty: Boolean
        get() = baseFolder.children().buildRequest().get()?.currentPage?.isEmpty() == true

    private fun DriveItemRequestBuilder.saveGet() = try {
        buildRequest().get()
    } catch (e: GraphServiceException) {
        null
    }

    private fun DriveItemCollectionRequestBuilder.saveGet() = try {
        buildRequest().get()
    } catch (e: GraphServiceException) {
        null
    }

    private fun DriveItemContentStreamRequestBuilder.saveGet() = try {
        buildRequest().get()
    } catch (e: GraphServiceException) {
        null
    }

    override fun getResInAccountDir(resourceName: String) =
        itemWithPath(accountPath.appendPath(resourceName)).saveGet()

    override fun saveFileContents(
        toAccountDir: Boolean,
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        val base = if (toAccountDir) accountPath else basePath
        val driveFolder = if (folder == null) base else {
            getFolderRequestBuilder(base, folder, true)
            base.appendPath(folder)
        }
        saveInputStream(
            itemWithPath(driveFolder.appendPath(fileName)),
            toInputStream(fileContents, maybeEncrypt)
        )
    }

    private fun getFolderRequestBuilder(
        parentPath: String,
        folder: String,
        require: Boolean
    ): DriveItemRequestBuilder {
        val result = itemWithPath(parentPath.appendPath(folder))
        if (require && result.saveGet() == null) {
            itemWithPath(parentPath).createFolder(folder)
        }
        return result
    }

    private fun saveInputStream(driveFolder: DriveItemRequestBuilder, inputStream: InputStream) {
        inputStream.use {
            driveFolder.content().buildRequest().put(it.readBytes())
        }
    }

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ) = try {
        itemWithPath((if (fromAccountDir) accountPath else basePath).appendPath(fileName))
            .content()
            .buildRequest()
            .get()
    } catch (e: GraphServiceException) {
        null
    }
        ?.use { StreamReader(maybeDecrypt(it, maybeDecrypt)).read() }

    @Throws(IOException::class)
    private fun getExistingAccountFolder(uuid: String) =
        itemWithPath(basePath.appendPath(uuid)).saveGet()


    private fun DriveItemRequestBuilder.createFolder(folderName: String): DriveItem {
        return children()
            .buildRequest()
            .post(DriveItem().apply {
                name = folderName
                folder = Folder()
            })
    }

    override fun writeAccount(account: Account, update: Boolean) {
        val existingAccountFolder = getExistingAccountFolder(account.uuid!!)
        if (existingAccountFolder == null) {
            baseFolder.createFolder(account.uuid!!)
            createWarningFile()
        }
        if (update || existingAccountFolder == null) {
            saveFileContents(
                true,
                null,
                accountMetadataFilename,
                buildMetadata(account),
                mimeTypeForData,
                true
            )
        }
    }

    override fun withAccount(account: Account) {
        setAccountUuid(account)
        writeAccount(account, false)
    }

    override fun resetAccountData(uuid: String) {
        itemWithPath(basePath.appendPath(uuid)).buildRequest().delete()
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = itemWithPath(basePath).children().buildRequest()
            .get()
            ?.getAll()?.filter { it.folder != null && it.name != BACKUP_FOLDER_NAME }
            ?.map {
                getAccountMetaDataFromDriveItem(
                    itemWithId(it.id!!).itemWithPath(
                        accountMetadataFilename
                    )
                )
            }
            ?: emptyList()

    private fun getAccountMetaDataFromDriveItem(driveItem: DriveItemRequestBuilder) =
        getAccountMetaDataFromInputStream(
            driveItem
                .content()
                .buildRequest()
                .get() ?: throw IOException()
        )

    private fun getResourcePath(resource: String) = accountPath.appendPath(resource)

    override fun readAccountMetaData() = getAccountMetaDataFromDriveItem(
        itemWithPath(getResourcePath(accountMetadataFilename))
    )

    override fun getCollection(collectionName: String, require: Boolean): DriveItem {
        return getFolderRequestBuilder(basePath, collectionName, require).buildRequest()
            .get()!!
    }

    override fun isCollection(resource: DriveItem) =
        resource.folder != null

    override fun nameForResource(resource: DriveItem) =
        resource.name

    override fun childrenForCollection(folder: DriveItem?): Collection<DriveItem> {
        return (folder ?: accountRes).let {
            itemWithId(it.id!!).children().saveGet()?.getAll()
        } ?: emptyList()
    }

    override fun getInputStream(resource: DriveItem) =
        itemWithId(resource.id!!).content().saveGet() ?: throw IOException()

    override fun saveUriToCollection(
        fileName: String,
        uri: Uri,
        collection: DriveItem,
        maybeEncrypt: Boolean
    ) {
        saveInputStream(
            itemWithId(collection.id!!).itemWithPath(fileName),
            maybeEncrypt(
                context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Could not read $uri"),
                maybeEncrypt
            )
        )
    }

    override fun deleteLockTokenFile() {
        itemWithPath(getResourcePath(LOCK_FILE)).buildRequest().delete()
    }

}