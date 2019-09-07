package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.dropbox.core.android.Auth
import org.totschnig.myexpenses.viewmodel.DropboxSetupViewModel

const val APP_KEY = "09ctg08r5gnsh5c"

class DropboxSetup: AbstractSyncBackup() {

    lateinit var viewModel: DropboxSetupViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Auth.startOAuth2Authentication(this, APP_KEY);

        viewModel = ViewModelProviders.of(this).get(DropboxSetupViewModel::class.java)
        viewModel.folderList.observe(this, Observer {
            if (it.size > 0) {
                showSelectFolderDialog(it)
            } else {
                showCreateFolderDialog()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val authToken = Auth.getOAuth2Token()
        if (authToken != null) {
            viewModel.loadDropboxRootFolders(authToken)
        }
    }
}