package org.totschnig.webdav.sync.client

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import javax.security.auth.x500.X500Principal

class CertificateHelperTest {

    @Test
    fun testClientCertMissingExceptionMessage() {
        val exception = ClientCertMissingException()
        assertThat(exception.message).contains("client certificate")
    }

    @Test
    fun testFromStringWithInvalidInput() {
        try {
            CertificateHelper.fromString("not a certificate")
            fail("Expected CertificateException")
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(java.security.cert.CertificateException::class.java)
        }
    }

    @Test
    fun testCreateTrustManager() {
        val testCert = generateTestCertificate()
        val trustManager = CertificateHelper.createTrustManager(testCert)
        assertThat(trustManager).isNotNull()
    }

    @Test
    fun testCreateSocketFactory() {
        val testCert = generateTestCertificate()
        val trustManager = CertificateHelper.createTrustManager(testCert)
        val socketFactory = CertificateHelper.createSocketFactory(trustManager)
        assertThat(socketFactory).isNotNull()
    }

    private fun generateTestCertificate(): java.security.cert.X509Certificate {
        val certFactory = CertificateFactory.getInstance("X.509")
        val certInput = ByteArrayInputStream(VALID_TEST_CERT.toByteArray())
        return certFactory.generateCertificate(certInput) as java.security.cert.X509Certificate
    }

    companion object {
        private const val VALID_TEST_CERT = """
-----BEGIN CERTIFICATE-----
MIIDOzCCAiOgAwIBAgIUAp7AmVX4BL/HO1vfPLK4f2cPoFIwDQYJKoZIhvcNAQEL
BQAwLTEZMBcGA1UEAwwQdGVzdC5leGFtcGxlLmNvbTEQMA4GA1UECgwHVGVzdE9y
ZzAeFw0yNjA0MDYxMjU1NTNaFw0yNzA0MDYxMjU1NTNaMC0xGTAXBgNVBAMMEHRl
c3QuZXhhbXBsZS5jb20xEDAOBgNVBAoMB1Rlc3RPcmcwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQCacO8tOWhYdc2WTei5bA4xl49tukdukR5YgJhtpuy7
8IJ4LUQ5z4o2Jxy2fE2JK9xEWQMUxNnAUKCKFQoNKKGiwAo5Lo3KEaSfg4ppWuFt
usP8AQcKw2ZpuxRdXnHMi68KGJa7Gwa9kLbvCaquBBHfcccLkh/vM+cPwchnk7II
UubV+m3zyL77C3n1IExxbRpCejEuG2jNLiT4HfaNIy+pH+Rk/e6o6X/F1cUQjHlV
NS+SSJ9mg8rxof7Nr7zoS3XMuO0eaf9oo/Q3DAPTZGpEXiiAzyQveWaxONkiBuc/
A6GodfuUZRQQt4otReknklEPH9jAkrY29Ml8TL6klexTAgMBAAGjUzBRMB0GA1Ud
DgQWBBRV6dPTCkGberucqNpv+l5RnofzYTAfBgNVHSMEGDAWgBRV6dPTCkGberuc
qNpv+l5RnofzYTAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAF
GEx1JnguzZXo8K58t6WjUMJthA/oQS6VPuH54sjitWPO57u312frxOJnpjWVYoZz
fIEYLOchm9fKuzfctF1k9kwvyLJx45nrHFIqLsbA/EYoM2Y/eLfefOmiR7vJph9M
vfandh0o+0FyqBb4OnX1a035Mt+Lk4oaGN6ticVRSVexPG0Tf+Da49ho5EzdiCde
b8G8GtdemMWQ9UPC+C6Et5KKAolyxTBda6aArGePmpIwcJEwK6OeVNahbUruU/di
FGectPCNSKkWsgbd7NMm3EB2ZFSDXMWpzrrOeirtMRDp1vgv6MQFZKixLq6Ll9PL
H5f9DH60ZiLLrVEQWFvc
-----END CERTIFICATE-----
"""
    }
}
