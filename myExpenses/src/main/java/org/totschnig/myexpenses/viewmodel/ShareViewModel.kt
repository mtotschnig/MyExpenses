package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.service.TransactionService
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import java.net.URI
import java.net.URISyntaxException

class ShareViewModel(application: Application) : AndroidViewModel(application) {
    private val _shareResult : MutableStateFlow<Result<Unit>?> = MutableStateFlow(null)
    val shareResult: StateFlow<Result<Unit>?> = _shareResult
    fun messageShown() {
        _shareResult.update {
            null
        }
    }

    fun share(ctx: Context, uriList: List<Uri>, target: String, mimeType: String) {
        _shareResult.update {
            if ("" == target) {
                handleGeneric(ctx, uriList, mimeType)
            } else {
                val uri = parseUri(target)
                if (uri == null) {
                    complain(ctx.getString(R.string.ftp_uri_malformed, target))
                } else {
                    when (val scheme = uri.scheme) {
                        "ftp" -> handleFtp(ctx, uriList, target, mimeType)
                        "mailto" -> handleMailto(ctx, uriList, mimeType, uri)
                        "http", "https" -> handleHttp(uriList, target)
                        else -> complain(
                            ctx.getString(
                                R.string.share_scheme_not_supported,
                                scheme
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleHttp(fileUris: List<Uri>, target: String): Result<Unit> = runCatching {
        TransactionService(target).sendFileListToWebService(fileUris)
    }

    private fun handleGeneric(ctx: Context, fileUris: List<Uri>, mimeType: String): Result<Unit> {
        val intent = buildIntent(ctx, fileUris, mimeType, null)
        if (Utils.isIntentAvailable(ctx, intent)) {
            // we launch the chooser in order to make action more explicit
            ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_sending)))
        } else {
            return complain("No app for sharing found")
        }
        return ResultUnit
    }

    private fun handleMailto(
        ctx: Context,
        fileUris: List<Uri>,
        mimeType: String,
        uri: URI
    ): Result<Unit> {
        val intent = buildIntent(ctx, fileUris, mimeType, uri.schemeSpecificPart)
        if (Utils.isIntentAvailable(ctx, intent)) {
            ctx.startActivity(intent)
        } else {
            return complain(ctx.getString(R.string.no_app_handling_email_available))
        }
        return ResultUnit
    }

    private fun handleFtp(
        ctx: Context,
        fileUris: List<Uri>,
        target: String,
        mimeType: String
    ): Result<Unit> {
        val intent: Intent
        if (fileUris.size > 1) {
            return complain("sending multiple file through ftp is not supported")
        } else {
            intent = Intent(Intent.ACTION_SENDTO)
            val contentUri = AppDirHelper.ensureContentUri(fileUris[0], ctx)
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            intent.setDataAndType(Uri.parse(target), mimeType)
            ctx.grantUriPermission(
                "org.totschnig.sendwithftp",
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            if (Utils.isIntentAvailable(ctx, intent)) {
                ctx.startActivity(intent)
            } else {
                return complain(ctx.getString(R.string.no_app_handling_ftp_available))
            }
        }
        return ResultUnit
    }

    private fun complain(string: String): Result<Unit> {
        return Result.failure(Throwable(string))
    }

    companion object {
        fun parseUri(target: String): URI? {
            if ("" != target) {
                try {
                    val uri = URI(target)
                    val scheme = uri.scheme
                    // strangely for mailto URIs getHost returns null,
                    // so we make sure that mailto URIs handled as valid
                    if (scheme != null && ("mailto" == scheme || uri.host != null)) {
                        return uri
                    }
                } catch (ignored: URISyntaxException) {
                }
            }
            return null
        }

        @VisibleForTesting
        fun buildIntent(
            ctx: Context,
            fileUris: List<Uri>,
            mimeType: String?,
            emailAddress: String?
        ): Intent {
            val intent: Intent
            if (fileUris.size > 1) {
                intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(fileUris.map { uri: Uri -> AppDirHelper.ensureContentUri(uri, ctx) }))
            } else {
                intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_STREAM, AppDirHelper.ensureContentUri(fileUris[0], ctx))
            }
            intent.type = mimeType
            if (emailAddress != null) {
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            }
            intent.putExtra(Intent.EXTRA_SUBJECT, R.string.export_expenses)
            return intent
        }
    }
}