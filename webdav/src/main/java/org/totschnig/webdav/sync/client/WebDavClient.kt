/*
 * Copyright 2016 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.webdav.sync.client

import at.bitfire.dav4android.BasicDigestAuthHandler
import at.bitfire.dav4android.DavResource
import at.bitfire.dav4android.LockableDavResource
import at.bitfire.dav4android.UrlUtils
import at.bitfire.dav4android.XmlUtils
import at.bitfire.dav4android.exception.DavException
import at.bitfire.dav4android.exception.HttpException
import at.bitfire.dav4android.property.DisplayName
import at.bitfire.dav4android.property.ResourceType
import dagger.internal.Preconditions
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.webdav.sync.DaggerWebDavComponent
import org.totschnig.webdav.sync.client.CertificateHelper.createSocketFactory
import org.totschnig.webdav.sync.client.CertificateHelper.createTrustManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSession

class WebDavClient(
    appComponent: AppComponent?,
    baseUrl: String,
    userName: String?,
    password: String?,
    trustedCertificate: X509Certificate?,
    allowUnverified: Boolean
) {
    private val MIME_XML: MediaType = "application/xml; charset=utf-8".toMediaTypeOrNull()!!
    private val httpClient: OkHttpClient
    private val mBaseUri: HttpUrl
    private var currentLockToken: String? = null

    @Inject
    lateinit var builder: OkHttpClient.Builder

    @Inject
    lateinit var prefHandler: PrefHandler

    init {
        DaggerWebDavComponent.builder().appComponent(appComponent).build().inject(this)

        mBaseUri = baseUrl.let {
            if (!it.endsWith("/")) "$it/" else it
        }.toHttpUrl()
        val timeout = prefHandler.getInt(PrefKey.WEBDAV_TIMEOUT, 10)
        builder.connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
        if (userName != null && password != null) {
            val authHandler = BasicDigestAuthHandler(
                UrlUtils.hostToDomain(mBaseUri.host), userName, password
            )
            builder.authenticator(authHandler).addNetworkInterceptor(authHandler)
        }
        if (trustedCertificate != null) {
            val trustManager = createTrustManager(trustedCertificate)
            val sf = createSocketFactory(trustManager)
            builder.sslSocketFactory(sf, trustManager)
            builder.hostnameVerifier { _: String?, session: SSLSession ->
                try {
                    val certificate = session.peerCertificates[0] as X509Certificate
                    return@hostnameVerifier certificate == trustedCertificate
                } catch (e: SSLException) {
                    return@hostnameVerifier false
                }
            }
        } else if (allowUnverified) {
            builder.hostnameVerifier { hostname: String, _: SSLSession? -> mBaseUri.host == hostname }
        }
        builder.followRedirects(false)
        httpClient = builder.build()
    }

    @Throws(IOException::class, HttpException::class)
    fun upload(
        fileName: String,
        requestBody: RequestBody?,
        parent: DavResource,
        withLock: Boolean = true
    ): LockableDavResource {
        val resource = LockableDavResource(httpClient, buildResourceUri(fileName, parent.location))
        resource.put(requestBody!!, if (withLock) buildIfHeader(parent.location) else null)
        return resource
    }

    private fun buildIfHeader(folderPath: HttpUrl): String? {
        return if (currentLockToken == null) {
            null
        } else webdavCodedUrl(folderPath.toString()) + " " +
                webDavIfHeaderConditionList(webdavCodedUrl(currentLockToken))
    }

    @Throws(IOException::class)
    fun mkCol(folderName: String) {
        val folder = LockableDavResource(httpClient, buildCollectionUri(folderName))
        folder.mkColWithLock(null)
    }

    @Throws(IOException::class)
    fun mkCol(folderName: String, parent: DavResource) =
        LockableDavResource(httpClient, buildCollectionUri(folderName, parent.location)).also {
            it.mkColWithLock(buildIfHeader(parent.location))
        }

    /**
     * @param folderPath if null, members of base uri are returned
     */
    @Throws(IOException::class)
    fun getFolderMembers(vararg folderPath: String): Set<DavResource> {
        return getFolderMembers(DavResource(httpClient, buildCollectionUri(*folderPath)))
    }

    @Throws(IOException::class)
    fun getFolderMembers(folder: DavResource): Set<DavResource> {
        try {
            //TODO GetContentLength.NAME
            folder.propfind(1, DisplayName.NAME, ResourceType.NAME)
        } catch (e: DavException) {
            throw IOException(e)
        } catch (e: HttpException) {
            throw IOException(e)
        }
        return folder.members
    }

    val base: LockableDavResource
        get() = LockableDavResource(httpClient, mBaseUri)

    fun getCollection(collectionName: String, vararg parentPath: String): LockableDavResource {
        return LockableDavResource(
            httpClient,
            buildCollectionUri(collectionName, buildCollectionUri(*parentPath))
        )
    }

    fun getResource(resourceName: String, vararg folderPath: String): LockableDavResource {
        return LockableDavResource(httpClient, buildResourceUri(resourceName, *folderPath))
    }

    fun getResource(folderUri: HttpUrl, resourceName: String): LockableDavResource {
        return LockableDavResource(httpClient, buildResourceUri(resourceName, folderUri))
    }

    fun lock(folderName: String): Boolean {
        currentLockToken = null
        val lockXml: RequestBody = """<d:lockinfo xmlns:d="DAV:">
<d:lockscope><d:exclusive/></d:lockscope>
<d:locktype><d:write/></d:locktype>
<d:owner>
<d:href>http://www.myexpenses.mobi</d:href>
</d:owner>
</d:lockinfo>"""
            .toRequestBody(MIME_XML)
        val request: Request = Request.Builder()
            .url(buildCollectionUri(folderName))
            .header("Timeout", LOCK_TIMEOUT)
            .method("LOCK", lockXml)
            .build()
        var response: Response? = null
        try {
            response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                var foundTokenNode = false
                val xpp = XmlUtils.newPullParser()
                xpp.setInput(response.body!!.charStream())
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    if (xpp.eventType == XmlPullParser.START_TAG) {
                        if (xpp.namespace == NS_WEBDAV && xpp.name == "locktoken") {
                            foundTokenNode = true
                        } else if (foundTokenNode && xpp.namespace == NS_WEBDAV && xpp.name == "href") {
                            //TODO persist lock token
                            currentLockToken = xpp.nextText()
                            return true
                        }
                    }
                    xpp.next()
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        } catch (e: XmlPullParserException) {
            report(e)
        } finally {
            cleanUp(response)
        }
        return false
    }

    fun unlock(folderName: String): Boolean {
        Preconditions.checkNotNull(currentLockToken)
        val request: Request = Request.Builder()
            .url(buildCollectionUri(folderName))
            .header("Lock-Token", webdavCodedUrl(currentLockToken))
            .method("UNLOCK", null)
            .build()
        var response: Response? = null
        return try {
            response = httpClient.newCall(request).execute()
            currentLockToken = null
            response.isSuccessful
        } catch (e: IOException) {
            false
        } finally {
            cleanUp(response)
        }
    }

    private fun cleanUp(response: Response?) {
        response?.close()
    }

    private fun webdavCodedUrl(url: String?): String {
        return "<$url>"
    }

    private fun webDavIfHeaderConditionList(condition: String): String {
        return "($condition)"
    }

    private fun buildCollectionUri(vararg folderPath: String): HttpUrl {
        return if (folderPath.isEmpty()) {
            mBaseUri
        } else {
            val builder = mBaseUri.newBuilder()
            for (segment in folderPath) {
                builder.addPathSegment(segment)
            }
            builder.addPathSegment("").build()
        }
    }

    private fun buildCollectionUri(collectionName: String, parent: HttpUrl): HttpUrl {
        return parent.newBuilder().addPathSegment(collectionName).addPathSegment("").build()
    }

    private fun buildResourceUri(resourceName: String, vararg folderPath: String): HttpUrl {
        val builder = mBaseUri.newBuilder()
        if (folderPath.isNotEmpty()) {
            for (segment in folderPath) {
                builder.addPathSegment(segment)
            }
        }
        return builder.addPathSegment(resourceName).build()
    }

    private fun buildResourceUri(resourceName: String, parent: HttpUrl): HttpUrl {
        return parent.newBuilder().addPathSegment(resourceName).build()
    }

    @Throws(IOException::class, HttpException::class, DavException::class)
    fun testLogin() {
        try {
            val baseResource = LockableDavResource(httpClient, mBaseUri)
            baseResource.options()
            baseResource.propfind(1, ResourceType.NAME)
            if (!baseResource.isCollection) {
                throw IOException("Not a folder")
            }
            if (!baseResource.capabilities.contains("2")) {
                throw NotCompliantWebDavException(baseResource.capabilities.contains("1"))
            }
        } catch (e: SSLHandshakeException) {
            var innerEx: Throwable? = e
            while (innerEx != null && innerEx !is CertPathValidatorException) {
                innerEx = innerEx.cause
            }
            if (innerEx != null) {
                var cert: X509Certificate? = null
                try {
                    cert = (innerEx as CertPathValidatorException)
                        .certPath
                        .certificates[0] as X509Certificate
                } catch (e2: Exception) {
                    report(e2)
                }
                if (cert != null) {
                    throw UntrustedCertificateException(cert)
                }
            }
        }
    }

    @Throws(Exception::class)
    fun testClass2Locking() {
        val folderName = "testClass2Locking"
        mkCol(folderName)
        val folder = LockableDavResource(httpClient, buildCollectionUri(folderName))
        try {
            if (lock(folderName)) {
                if (unlock(folderName)) {
                    if (lock(folderName)) {
                        if (unlock(folderName)) {
                            return
                        }
                    }
                }
            }
            throw NotCompliantWebDavException(true)
        } finally {
            try {
                folder.delete(null)
            } catch (ignore: Exception) {
                //this can fail, if the unlocking mechanism does not work. We live with not being able to delete the test folder
            }
        }
    }

    companion object {
        private val LOCK_TIMEOUT = String.format(Locale.ROOT, "Second-%d", 30 * 60)
        private const val NS_WEBDAV = "DAV:"
    }
}
