package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.io.displayName

class SafSetup : ProtectedFragmentActivity() {
    private val restoreRequest =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                lifecycleScope.launch {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(
                            AccountManager.KEY_ACCOUNT_NAME,
                            BackendService.SAF.buildAccountName(
                                withContext(Dispatchers.IO) {
                                    DocumentFile.fromTreeUri(this@SafSetup, uri)!!.displayName
                                })
                        )
                        putExtra(AccountManager.KEY_USERDATA, Bundle(1).apply {
                            putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, uri.toString())
                        })
                    })
                    finish()
                }
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            restoreRequest.launch(null)
        } catch (_: ActivityNotFoundException) {
            showSnackBar(
                "Could not open directory picker. This feature may not be supported on your device.",
            )
        }
    }
}