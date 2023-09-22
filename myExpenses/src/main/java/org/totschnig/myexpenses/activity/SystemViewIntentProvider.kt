package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri

class SystemViewIntentProvider : ViewIntentProvider {
    override fun getViewIntent(uri: Uri) =
        Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
}