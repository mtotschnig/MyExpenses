package at.bitfire.dav4android

import at.bitfire.dav4android.exception.HttpException
import at.bitfire.dav4android.property.GetETag
import at.bitfire.dav4android.property.ResourceType
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.totschnig.myexpenses.sync.SyncAdapter
import timber.log.Timber.Forest.tag
import java.io.IOException

class LockableDavResource(httpClient: OkHttpClient, location: HttpUrl) :
    DavResource(httpClient, location) {

    fun put(body: RequestBody, ifHeader: String?) {
        val builder = Request.Builder()
            .put(body)
            .url(location)
            .apply {
                if (ifHeader != null) header("If", ifHeader)
            }

        val response = httpClient.newCall(builder.build()).execute()
        checkStatus(response, true)
        if (response.code == 207) {
            // Apache mod_dav returns 207 if update fails due to collection being locked.
            // TODO: verify whether 207 can also indicate success in some cases.
            throw HttpException(response)
        }

        val eTag = response.header("ETag")
        if (eTag.isNullOrEmpty()) properties.remove(GetETag.NAME)
        else properties.put(GetETag.NAME, GetETag(eTag))
    }

    /**
     * Tries to establish if the Dav resource represented by this object exists on the server by sending
     * a HEAD request to it. A resource is supposed to exist unless the server explicitly returns 404
     */
    @Throws(IOException::class)
    private fun head() = httpClient.newCall(
        Request.Builder()
            .url(location)
            .head()
            .build()
    ).execute()
        .use { response -> response.code != 404 }

    /**
     * calls [.head] without throwing exception
     *
     * @return true if head request succeeds
     */
    fun exists() = try {
        head()
    } catch (_: IOException) {
        false
    }

    /**
     * Tests first if collection exists. As a workaround for
     * Webservers where testing for existence with HEAD request does not work, as a fallback we check
     * if MKCOL request returned 405 which would indicate that folder already existed
     *
     * @param ifHeader DAV compliant If header
     */
    @Throws(IOException::class)
    fun mkColWithLock(ifHeader: String?) {
        if (!exists()) {
            try {
                var response: Response? = null

                for (attempt in 0..<MAX_REDIRECTS) {
                    val builder = Request.Builder()
                        .method("MKCOL", null)
                        .url(location)
                    if (ifHeader != null) {
                        builder.header("If", ifHeader)
                    }
                    response = httpClient.newCall(builder.build()).execute()
                    if (response.isRedirect) {
                        processRedirection(response)
                    } else {
                        break
                    }
                }
                checkStatus(response, true)
            } catch (e: HttpException) {
                if (e.status != 405) {
                    throw IOException(e)
                }
            }
        }
    }

    companion object {
        fun DavResource.isCollection(): Boolean {
            val type = properties.get(ResourceType.NAME) as ResourceType?
            tag(SyncAdapter.TAG).i(
                "isCollection - properties: %s; type: %s",
                properties,
                type
            )
            return type != null && type.types.contains(ResourceType.COLLECTION)
        }

        fun DavResource.fileNameV2(): String? {
            return segments().lastOrNull()
        }

        //From io.ktor.http.Url.kt
        fun DavResource.segments(): List<String> {
            val pathSegments = this.location.pathSegments
            if (pathSegments.isEmpty()) return emptyList()
            val start = if (pathSegments.first().isEmpty() && pathSegments.size > 1) 1 else 0
            val end = if (pathSegments.last()
                    .isEmpty()
            ) pathSegments.lastIndex else pathSegments.lastIndex + 1
            return pathSegments.subList(start, end)
        }
    }
}
