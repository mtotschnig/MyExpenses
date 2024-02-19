package org.totschnig.onedrive.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import com.microsoft.graph.logger.DefaultLogger
import com.microsoft.graph.logger.LoggerLevel
import com.microsoft.graph.models.DriveItem
import com.microsoft.graph.models.Folder
import com.microsoft.graph.requests.GraphServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel
import org.totschnig.onedrive.BuildConfig
import org.totschnig.onedrive.getAll
import java.util.concurrent.CompletableFuture

@RequiresApi(Build.VERSION_CODES.N)
class OneDriveSetupViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    AbstractSetupViewModel(BackendService.ONEDRIVE, application, savedStateHandle) {

    private lateinit var graphClient: GraphServiceClient<Request>

    fun initWithAccessToken(accessToken: String) {
        graphClient = GraphServiceClient.builder().apply {
            if (BuildConfig.DEBUG) {
                logger(DefaultLogger().also {
                    it.loggingLevel = LoggerLevel.DEBUG
                })
            }
        }
            .authenticationProvider {
                CompletableFuture.supplyAsync { accessToken }
            }
            .buildClient()
    }

    override suspend fun getFolders() = withContext(Dispatchers.IO) {
        graphClient.drive().root().children().buildRequest().get()
            ?.getAll()
            ?.filter { it.folder != null }
            ?.map { it.id!! to it.name!! } ?: emptyList()
    }

    override suspend fun createFolderBackground(label: String) = withContext(Dispatchers.IO) {
        val result = graphClient.drive().root().children()
            .buildRequest()
            .post(DriveItem().apply {
                name = label
                folder = Folder()
            })
        result.id!! to result.name!!
    }
}