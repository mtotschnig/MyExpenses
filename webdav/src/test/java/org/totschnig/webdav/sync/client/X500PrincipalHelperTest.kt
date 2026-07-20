package org.totschnig.webdav.sync.client

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.security.auth.x500.X500Principal

class X500PrincipalHelperTest {

    @Test
    fun testParseCN() {
        val principal = X500Principal("CN=example.com,O=MyOrg,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.cn).isEqualTo("example.com")
    }

    @Test
    fun testParseO() {
        val principal = X500Principal("CN=example.com,O=MyOrg,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.o).isEqualTo("MyOrg")
    }

    @Test
    fun testParseC() {
        val principal = X500Principal("CN=example.com,O=MyOrg,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.c).isEqualTo("US")
    }

    @Test
    fun testParseMultipleCNReturnsFirst() {
        val principal = X500Principal("CN=first.com,CN=second.com,O=MyOrg")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.cn).isEqualTo("first.com")
    }

    @Test
    fun testParseOU() {
        val principal = X500Principal("CN=example.com,OU=Engineering,O=MyOrg,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.ou).isEqualTo("Engineering")
    }

    @Test
    fun testParseL() {
        val principal = X500Principal("CN=example.com,L=San Francisco,ST=CA,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.l).isEqualTo("San Francisco")
    }

    @Test
    fun testParseST() {
        val principal = X500Principal("CN=example.com,L=San Francisco,ST=California,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.st).isEqualTo("California")
    }

    @Test
    fun testParseUID() {
        val principal = X500Principal("UID=user123,CN=example.com,O=MyOrg")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.uid).isEqualTo("user123")
    }

    @Test
    fun testMissingAttributeReturnsNull() {
        val principal = X500Principal("CN=example.com,O=MyOrg")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.l).isNull()
        assertThat(helper.st).isNull()
    }

    @Test
    fun testParseRFC2253Format() {
        val principal = X500Principal("CN=John Doe,OU=Engineering,O=Acme Corp,L=Seattle,ST=Washington,C=US")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.cn).isEqualTo("John Doe")
        assertThat(helper.ou).isEqualTo("Engineering")
        assertThat(helper.o).isEqualTo("Acme Corp")
        assertThat(helper.l).isEqualTo("Seattle")
        assertThat(helper.st).isEqualTo("Washington")
        assertThat(helper.c).isEqualTo("US")
    }

    @Test
    fun testParseWithSpecialCharsInCN() {
        val principal = X500Principal("CN=Test User (Admin),O=MyOrg")
        val helper = X500PrincipalHelper(principal)
        assertThat(helper.cn).isEqualTo("Test User (Admin)")
    }
}
