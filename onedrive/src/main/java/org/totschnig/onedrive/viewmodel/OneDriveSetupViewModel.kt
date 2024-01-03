package org.totschnig.onedrive.viewmodel

import android.app.Application
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel

class OneDriveSetupViewModel(application: Application) :
    AbstractSetupViewModel(BackendService.ONEDRIVE, application) {
    override suspend fun getFolders(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun createFolderBackground(label: String): Pair<String, String> {
        TODO("Not yet implemented")
    }
}