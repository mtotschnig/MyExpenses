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

import android.content.Context
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Base64
import okhttp3.internal.tls.OkHostnameVerifier.allSubjectAltNames
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CertificateHelper {
    @JvmStatic
    fun getShortDescription(certificate: X509Certificate, context: Context?): String {
        val dateFormat = DateFormat.getMediumDateFormat(context)
        val subjectHelper = X500PrincipalHelper(certificate.subjectX500Principal)
        val subject = subjectHelper.cn
        val subjectAltNames: SortedSet<String?> = TreeSet(allSubjectAltNames(certificate))
        val issuerHelper = X500PrincipalHelper(certificate.issuerX500Principal)
        val issuer = issuerHelper.cn
        val serialNumber = certificate.serialNumber.toString(16).uppercase(Locale.ROOT)
            .replace("(?<=..)(..)".toRegex(), ":$1")
        val validFrom = dateFormat.format(certificate.notBefore)
        val validUntil = dateFormat.format(certificate.notAfter)
        return String.format(
            """
    Subject: %s
    Alt. names: %s
    Serialnumber: %s
    Issuer: %s
    Valid: %s - %s
    
    """.trimIndent(),
            subject,
            TextUtils.join(", ", subjectAltNames),
            serialNumber,
            issuer,
            validFrom,
            validUntil
        )
    }

    @Throws(CertificateEncodingException::class)
    fun X509Certificate.encode(): String {
        val header = "-----BEGIN CERTIFICATE-----\n"
        val cert = Base64.encodeToString(encoded, Base64.DEFAULT)
        val footer = "-----END CERTIFICATE-----"
        return header + cert + footer
    }

    @JvmStatic
    @Throws(CertificateException::class)
    fun fromString(certificate: String): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(ByteArrayInputStream(certificate.toByteArray())) as X509Certificate
    }

    @JvmStatic
    @Throws(InvalidCertificateException::class)
    fun createTrustManager(certificate: X509Certificate) = try {
        with(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())) {
            init(KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("ca", certificate)
            })
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                ("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers))
            }
            trustManagers[0] as X509TrustManager
        }
    } catch (e: Exception) {
        throw InvalidCertificateException(certificate, e)
    }

    @JvmStatic
    @Throws(InvalidCertificateException::class)
    fun createSocketFactory(trustManager: X509TrustManager): SSLSocketFactory =
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }.socketFactory
}