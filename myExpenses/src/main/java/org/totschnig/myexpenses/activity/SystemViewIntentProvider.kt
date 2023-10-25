package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.NougatFileProviderException

class SystemViewIntentProvider : ViewIntentProvider {
    override fun getViewIntent(context: Context, uri: Uri) = try {
        Intent(Intent.ACTION_VIEW, AppDirHelper.ensureContentUri(uri, context)).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    } catch (e: NougatFileProviderException) {
        getFallbackIntent(context, uri)
    }

    private fun getFallbackIntent(context: Context, pictureUri: Uri) =
        Intent(Intent.ACTION_VIEW, pictureUri, context, SimpleImageActivity::class.java)
}