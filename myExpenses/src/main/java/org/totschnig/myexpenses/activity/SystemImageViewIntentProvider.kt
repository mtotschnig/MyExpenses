package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.NougatFileProviderException

class SystemImageViewIntentProvider : ImageViewIntentProvider {
    override fun getViewIntent(context: Context, pictureUri: Uri): Intent {
        val uri = try {
            AppDirHelper.ensureContentUri(pictureUri, context)
        } catch (e: NougatFileProviderException) {
            return getFallbackIntent(context, pictureUri)
        }
        return instantiateIntent(uri).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            setDataAndType(uri, "image/jpeg")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun getFallbackIntent(context: Context, pictureUri: Uri) =
        Intent(Intent.ACTION_VIEW, pictureUri, context, SimpleImageActivity::class.java)

    private fun instantiateIntent(pictureUri: Uri?) = Intent(Intent.ACTION_VIEW, pictureUri)
}