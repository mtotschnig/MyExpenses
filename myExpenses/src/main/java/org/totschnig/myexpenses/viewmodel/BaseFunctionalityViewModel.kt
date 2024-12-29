package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.source
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.getDisplayName
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.util.io.calculateSize
import org.totschnig.myexpenses.util.io.getMimeType
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class BaseFunctionalityViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    enum class Scheme { FTP, MAILTO, HTTP, HTTPS; }

    @Inject
    lateinit var okHttpBuilder: OkHttpClient.Builder

    private val _shareResult: MutableStateFlow<Result<Scheme?>?> = MutableStateFlow(null)
    val shareResult: StateFlow<Result<Scheme?>?> = _shareResult
    fun messageShown() {
        _shareResult.update {
            null
        }
    }

    fun share(ctx: Context, uriList: List<Uri>, target: String, mimeType: String) {
        viewModelScope.launch(coroutineDispatcher) {
            _shareResult.update {
                if ("" == target) {
                    handleGeneric(ctx, uriList, mimeType)
                } else {
                    val uri = parseUri(target)
                    if (uri == null) {
                        complain(ctx.getString(R.string.ftp_uri_malformed, target))
                    } else {
                        when (val scheme = enumValueOrNull<Scheme>(uri.scheme.uppercase())) {
                            Scheme.FTP -> handleFtp(ctx, uriList, target, mimeType)
                            Scheme.MAILTO -> handleMailto(ctx, uriList, mimeType, uri)
                            Scheme.HTTP, Scheme.HTTPS -> handleHttp(
                                uriList,
                                target
                            ).also { result ->
                                result.onFailure {
                                    if (BuildConfig.DEBUG || it !is IOException) {
                                        CrashHandler.report(it)
                                    }
                                }
                            }.map { scheme }
                            null -> complain(
                                ctx.getString(
                                    R.string.share_scheme_not_supported,
                                    uri.scheme
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleHttp(fileUris: List<Uri>, target: String): Result<Unit> =
        runCatching {
            for (uri in fileUris) {
                val resourceName = contentResolver.getDisplayName(uri)
                val requestBody: RequestBody = object : RequestBody() {
                    override fun contentLength(): Long = calculateSize(contentResolver, uri)

                    override fun contentType() = getMimeType(resourceName).toMediaTypeOrNull()

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        //noinspection Recycle
                        val source = (contentResolver.openInputStream(uri)
                            ?: throw IOException("Could not read $uri")).source()
                        try {
                            sink.writeAll(source)
                        } finally {
                            source.closeQuietly()
                        }
                    }
                }

                val builder: Request.Builder = Request.Builder()
                Uri.parse(target).userInfo?.let {
                    val (username, password) = it.split(":")
                    builder.header("Authorization", Credentials.basic(username, password))
                }
                builder
                    .put(requestBody)
                    .url(
                        target.toHttpUrl().newBuilder().username("").password("")
                            .addPathSegment(resourceName).build()
                    )

                val response: Response = okHttpBuilder.build().newCall(builder.build()).execute()

                if (response.isSuccessful) {
                    response.body?.let {
                        Timber.i(it.string())
                    }
                } else {
                    throw IOException("FAILURE: ${response.code}")
                }
            }
        }

    private fun handleGeneric(
        ctx: Context,
        fileUris: List<Uri>,
        mimeType: String
    ): Result<Scheme?> {
        val intent = buildIntent(ctx, fileUris, mimeType, null)
        if (Utils.isIntentAvailable(ctx, intent)) {
            // we launch the chooser in order to make action more explicit
            ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_sending)))
        } else {
            return complain("No app for sharing found")
        }
        return success(null)
    }

    private fun handleMailto(
        ctx: Context,
        fileUris: List<Uri>,
        mimeType: String,
        uri: URI
    ): Result<Scheme> {
        val intent = buildIntent(ctx, fileUris, mimeType, uri.schemeSpecificPart)
        if (Utils.isIntentAvailable(ctx, intent)) {
            ctx.startActivity(intent)
        } else {
            return complain(ctx.getString(R.string.no_app_handling_email_available))
        }
        return success(Scheme.MAILTO)
    }

    private fun handleFtp(
        ctx: Context,
        fileUris: List<Uri>,
        target: String,
        mimeType: String
    ): Result<Scheme> {
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
        return success(Scheme.FTP)
    }

    private fun complain(string: String): Result<Scheme> {
        return failure(Throwable(string))
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
        ) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(fileUris.map { uri: Uri -> AppDirHelper.ensureContentUri(uri, ctx) })
            )
            type = mimeType
            if (emailAddress != null) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            }
            putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.app_name))
        }
    }

    fun cleanupOrigFile(result: CropImage.ActivityResult) {
        if (result.originalUri.authority == AppDirHelper.getFileProviderAuthority(getApplication())) {
            viewModelScope.launch(coroutineContext()) {
                contentResolver.delete(result.originalUri, null, null)
            }
        }
    }

}