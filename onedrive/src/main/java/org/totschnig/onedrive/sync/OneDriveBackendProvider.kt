package org.totschnig.onedrive.sync

import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
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
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication.GetAccountCallback
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.acra.util.StreamReader
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.onedrive.R
import org.totschnig.onedrive.getAll
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@RequiresApi(Build.VERSION_CODES.N)
class OneDriveBackendProvider internal constructor(context: Context, folderName: String) :
    AbstractSyncBackendProvider<DriveItem>(context) {
    private lateinit var graphClient: GraphServiceClient<Request>
    private val basePath: String = folderName

    override suspend fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        val accessToken = withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                PublicClientApplication.createMultipleAccountPublicClientApplication(
                    context.applicationContext,
                    R.raw.msal_config,
                    object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                        override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                            application.getAccount(
                                accountManager.getUserData(account, KEY_MICROSOFT_ACCOUNT),
                                object : GetAccountCallback {
                                    override fun onTaskCompleted(result: IAccount) {
                                        application.acquireTokenSilentAsync(
                                            AcquireTokenSilentParameters.Builder()
                                                .withScopes(listOf("Files.ReadWrite.All"))
                                                .fromAuthority("https://login.microsoftonline.com/common")
                                                .forAccount(result)
                                                .withCallback(object: SilentAuthenticationCallback {
                                                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                                        cont.resume(authenticationResult.accessToken)
                                                    }

                                                    override fun onError(exception: MsalException) {
                                                        cont.resumeWithException(exception)
                                                    }
                                                })
                                                .build()
                                        )
                                    }

                                    override fun onError(exception: MsalException) {
                                        cont.resumeWithException(exception)
                                    }
                                })
                        }

                        override fun onError(exception: MsalException) {
                            cont.resumeWithException(exception)
                        }

                    }
                )
            }
        }
        graphClient = GraphServiceClient.builder()
            .logger(DefaultLogger().also {
                it.loggingLevel = LoggerLevel.DEBUG
            })
            .authenticationProvider {
                CompletableFuture.supplyAsync { accessToken }
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
    ) = itemWithPath((if (fromAccountDir) accountPath else basePath).appendPath(fileName))
        .content()
        .saveGet()
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
            ?.getAll()?.filter { it.folder != null && verifyRemoteAccountFolderName(it.name) }
            ?.mapNotNull {
                getAccountMetaDataFromDriveItem(
                    itemWithId(it.id!!).itemWithPath(
                        accountMetadataFilename
                    )
                )
            }
            ?: emptyList()

    private fun getAccountMetaDataFromDriveItem(driveItem: DriveItemRequestBuilder) =
        driveItem.content().saveGet()?.let { getAccountMetaDataFromInputStream(it) }

    private fun getResourcePath(resource: String) = accountPath.appendPath(resource)

    override fun readAccountMetaData() = getAccountMetaDataFromDriveItem(
        itemWithPath(getResourcePath(accountMetadataFilename))
    ) ?: Result.failure(IOException("No metaDatafile"))

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

    companion object {
        const val KEY_MICROSOFT_ACCOUNT = "microsoftAccount"
    }

}