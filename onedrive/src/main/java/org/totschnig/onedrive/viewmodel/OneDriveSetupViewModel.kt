package org.totschnig.onedrive.viewmodel

import android.app.Application
import com.microsoft.graph.requests.GraphServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel
import java.util.concurrent.CompletableFuture

class OneDriveSetupViewModel(application: Application) :
    AbstractSetupViewModel(BackendService.ONEDRIVE, application) {

    private lateinit var graphClient: GraphServiceClient<Request>

    fun initWithAccessToken(accessToken: String) {
        graphClient = GraphServiceClient.builder()
            .authenticationProvider {
                CompletableFuture.supplyAsync { accessToken }
            }
            .buildClient()
    }

    override suspend fun getFolders() = withContext(Dispatchers.IO) {
        val displayName = graphClient.me().buildRequest().get()?.displayName ?: "?"
        listOf("me" to displayName)
    }

    override suspend fun createFolderBackground(label: String): Pair<String, String> {
        TODO("Not yet implemented")
    }
}