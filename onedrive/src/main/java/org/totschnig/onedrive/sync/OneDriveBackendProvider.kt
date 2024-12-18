package org.totschnig.onedrive.sync

import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import com.microsoft.graph.http.GraphServiceException
import com.microsoft.graph.logger.DefaultLogger
import com.microsoft.graph.logger.LoggerLevel
import com.microsoft.graph.models.DriveItem
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet
import com.microsoft.graph.models.DriveItemUploadableProperties
import com.microsoft.graph.models.Folder
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder
import com.microsoft.graph.requests.DriveItemContentStreamRequestBuilder
import com.microsoft.graph.requests.DriveItemRequestBuilder
import com.microsoft.graph.requests.DriveRequestBuilder
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.graph.tasks.IProgressCallback
import com.microsoft.graph.tasks.LargeFileUploadTask
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
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.onedrive.R
import org.totschnig.onedrive.getAll
import java.io.FileNotFoundException
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
                                    override fun onTaskCompleted(result: IAccount?) {
                                        if (result == null) {
                                            cont.resumeWithException(
                                                SyncBackendProvider.SyncParseException(
                                                    "Unable to retrieve Microsoft Account. Remove backend and add again."
                                                )
                                            )
                                        } else {
                                            application.acquireTokenSilentAsync(
                                                AcquireTokenSilentParameters.Builder()
                                                    .withScopes(listOf("Files.ReadWrite.All"))
                                                    .fromAuthority("https://login.microsoftonline.com/common")
                                                    .forAccount(result)
                                                    .withCallback(object :
                                                        SilentAuthenticationCallback {
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
        graphClient = GraphServiceClient.builder().also {
            if (context.injector.prefHandler().shouldDebug) {
                it.logger(DefaultLogger().also {
                    it.loggingLevel = LoggerLevel.DEBUG
                })
            }
        }
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
        get() = basePath.appendPath(accountUuid)

    private fun String.appendPath(segment: String) = "$this/$segment"

    override val accountRes: DriveItem
        get() = itemWithPath(accountPath).buildRequest().get()
            ?: throw FileNotFoundException("accountRes not found")
    override val sharedPreferencesName = "oneDrive"
    override val isEmpty: Boolean
        get() = baseFolder.children().safeGet()?.currentPage?.isEmpty() == true

    private fun Exception.asIO() = if (this is IOException) this else
        IOException(this.also { CrashHandler.report(it) })

    private fun <T> safeWrite(write: () -> T): T = try {
        write()
    } catch (e: Exception) {
        throw e.asIO()
    }

    private fun <T> safeRead(read: () -> T?): T? = try {
        read()
    } catch (_: GraphServiceException) {
        null
    } catch (e: Exception) {
        throw e.asIO()
    }

    private fun DriveItemRequestBuilder.safeGet() = safeRead { buildRequest().get() }

    private fun DriveItemCollectionRequestBuilder.safeGet() = safeRead { buildRequest().get() }

    private fun DriveItemContentStreamRequestBuilder.safeGet() = safeRead { buildRequest().get() }

    override fun getResInAccountDir(resourceName: String) =
        itemWithPath(accountPath.appendPath(resourceName)).safeGet()

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
        if (require && result.safeGet() == null) {
            itemWithPath(parentPath).createFolder(folder)
        }
        return result
    }

    private fun saveInputStream(driveFolder: DriveItemRequestBuilder, inputStream: InputStream) =
        inputStream.use {
            safeWrite {
                driveFolder.content().buildRequest().put(it.readBytes())
                    ?: throw IOException("Upload failed")
            }
        }

    private fun largeFileUpload(
        driveFolder: DriveItemRequestBuilder,
        inputStream: InputStream,
        streamSize: Long
    ) {
        inputStream.use {
            val uploadParams = DriveItemCreateUploadSessionParameterSet
                .newBuilder().withItem(DriveItemUploadableProperties()).build()
            val uploadSession = safeWrite {
                driveFolder.createUploadSession(uploadParams).buildRequest().post()
            } ?: throw IOException("Could not create upload session")
            val callback = IProgressCallback { current, max ->
                log().i("Uploaded $current bytes of $max total bytes", current, max)
            }

            val clientWithoutAuth = GraphServiceClient.builder()
                .authenticationProvider {
                    CompletableFuture.supplyAsync { null }
                }
                .buildClient()

            val largeFileUploadTask = LargeFileUploadTask(
                uploadSession, clientWithoutAuth, it, streamSize, DriveItem::class.java
            )

            largeFileUploadTask.upload(0, null, callback)
        }
    }

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ) = itemWithPath((if (fromAccountDir) accountPath else basePath).appendPath(fileName))
        .content()
        .safeGet()
        ?.use { StreamReader(maybeDecrypt(it, maybeDecrypt)).read() }

    @Throws(IOException::class)
    private fun getExistingAccountFolder(uuid: String) =
        itemWithPath(basePath.appendPath(uuid)).safeGet()


    private fun DriveItemRequestBuilder.createFolder(folderName: String) =
        safeWrite {
            children().buildRequest().post(DriveItem().apply {
                name = folderName
                folder = Folder()
            })
        }

    override fun writeAccount(account: Account, update: Boolean) {
        val existingAccountFolder = getExistingAccountFolder(accountUuid)
        if (existingAccountFolder == null) {
            baseFolder.createFolder(accountUuid)
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
        super.withAccount(account)
        writeAccount(account, false)
    }

    override fun resetAccountData(uuid: String) {
        safeWrite {
            itemWithPath(basePath.appendPath(uuid)).buildRequest().delete()
        }
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = itemWithPath(basePath).children().safeGet()
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
        driveItem.content().safeGet()?.let { getAccountMetaDataFromInputStream(it) }

    private fun getResourcePath(resource: String) = accountPath.appendPath(resource)

    override fun readAccountMetaData() = getAccountMetaDataFromDriveItem(
        itemWithPath(getResourcePath(accountMetadataFilename))
    ) ?: Result.failure(IOException("No metaDatafile"))

    override fun getCollection(collectionName: String, require: Boolean): DriveItem? {
        return getFolderRequestBuilder(basePath, collectionName, require).safeGet()
    }

    override fun isCollection(resource: DriveItem) =
        resource.folder != null

    override fun nameForResource(resource: DriveItem) =
        resource.name

    override fun childrenForCollection(folder: DriveItem?): Collection<DriveItem> {
        return (folder ?: accountRes).let {
            itemWithId(it.id!!).children().safeGet()?.getAll()
        }?.filter { it.size?.compareTo(0) != 0 } ?: emptyList()
    }

    override fun getInputStream(resource: DriveItem) =
        itemWithId(resource.id!!).content().safeGet() ?: throw IOException()

    override fun saveUriToCollection(
        fileName: String,
        uri: Uri,
        collection: DriveItem,
        maybeEncrypt: Boolean
    ) {
        val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")
            ?.use { it.length }
            ?: context.contentResolver.query(
                uri, null, null, null, null
            )?.use {
                it.getLongOrNull(OpenableColumns.SIZE)
            } ?: 0L
        log().d("%s: %d", uri, fileSize)
        val driveFolder = itemWithId(collection.id!!).itemWithPath(fileName)
        val inputStream = (context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not read $uri"))
        if (!maybeEncrypt && fileSize > SIMPLE_FILE_UPLOAD_SIZE_LIMIT) {
            largeFileUpload(driveFolder, inputStream, fileSize)
        } else {
            saveInputStream(driveFolder, maybeEncrypt(inputStream, maybeEncrypt))
        }
    }

    override fun deleteLockTokenFile() {
        setLockToken("")
    }

    companion object {
        const val KEY_MICROSOFT_ACCOUNT = "microsoftAccount"
        const val SIMPLE_FILE_UPLOAD_SIZE_LIMIT = 4194304 //4MB
    }

}