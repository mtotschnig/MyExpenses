package org.totschnig.myexpenses.preference

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceIOBRFragment

class BasePreferenceIOBRFragmentTest {

    @Test
    fun isHttpUrlWithoutCredentials_validHttpUrl_returnsTrue() {
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("http://example.com/upload")).isTrue()
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("https://example.com/upload")).isTrue()
    }

    @Test
    fun isHttpUrlWithoutCredentials_withUserInfo_returnsFalse() {
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("http://user:pass@example.com/upload")).isFalse()
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("https://user@example.com/upload")).isFalse()
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("http://:pass@example.com/upload")).isFalse()
    }

    @Test
    fun isHttpUrlWithoutCredentials_nonHttpScheme_returnsFalse() {
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("mailto:test@example.com")).isFalse()
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("ftp://example.com")).isFalse()
    }

    @Test
    fun isHttpUrlWithoutCredentials_emptyOrBlank_returnsFalse() {
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("")).isFalse()
        assertThat(BasePreferenceIOBRFragment.isHttpUrlWithoutCredentials("   ")).isFalse()
    }
}
