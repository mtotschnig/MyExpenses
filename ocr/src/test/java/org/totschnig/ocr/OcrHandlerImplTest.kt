package org.totschnig.ocr

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.totschnig.myexpenses.feature.getLocaleForUserCountry
import java.util.*

@RunWith(Parameterized::class)
class OcrHandlerImplTest(private val country: String?, private val expectedLanguage: String) {
    @Test
    fun localeForUserCountry() {
        val countryHasSingleLocale = Locale.getAvailableLocales().count { it.country == country } == 1
        val value = if (countryHasSingleLocale) getLocaleForUserCountry(country) else
            getLocaleForUserCountry(country, Locale(expectedLanguage))
        Assertions.assertThat(value.language).isEqualTo(expectedLanguage)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
                arrayOf(null, Locale.getDefault().language),
                arrayOf("AE", "ar"), arrayOf("JO", "ar"), arrayOf("SY", "ar"), arrayOf("HR", "hr"), arrayOf("BE", "fr"), arrayOf("PA", "es"), arrayOf("MT", "mt"), arrayOf("VE", "es"), arrayOf("TW", "zh"), arrayOf("DK", "da"), arrayOf("PR", "es"), arrayOf("VN", "vi"), arrayOf("US", "en"), arrayOf("ME", "sr"), arrayOf("SE", "sv"), arrayOf("BO", "es"), arrayOf("SG", "en"), arrayOf("BH", "ar"), arrayOf("SA", "ar"), arrayOf("YE", "ar"), arrayOf("IN", "hi"), arrayOf("MT", "en"), arrayOf("FI", "fi"), arrayOf("BA", "sr"), arrayOf("UA", "uk"), arrayOf("CH", "fr"), arrayOf("AR", "es"), arrayOf("EG", "ar"), arrayOf("JP", "ja"), arrayOf("SV", "es"), arrayOf("BR", "pt"), arrayOf("IS", "is"), arrayOf("CZ", "cs"), arrayOf("PL", "pl"), arrayOf("ES", "ca"), arrayOf("CS", "sr"), arrayOf("MY", "ms"), arrayOf("ES", "es"), arrayOf("CO", "es"), arrayOf("BG", "bg"), arrayOf("BA", "sr"), arrayOf("PY", "es"), arrayOf("EC", "es"), arrayOf("US", "es"), arrayOf("SD", "ar"), arrayOf("RO", "ro"), arrayOf("PH", "en"), arrayOf("TN", "ar"), arrayOf("ME", "sr"), arrayOf("GT", "es"), arrayOf("KR", "ko"), arrayOf("CY", "el"), arrayOf("MX", "es"), arrayOf("RU", "ru"), arrayOf("HN", "es"), arrayOf("HK", "zh"), arrayOf("NO", "no"), arrayOf("HU", "hu"), arrayOf("TH", "th"), arrayOf("IQ", "ar"), arrayOf("CL", "es"), arrayOf("MA", "ar"), arrayOf("IE", "ga"), arrayOf("TR", "tr"), arrayOf("EE", "et"), arrayOf("QA", "ar"), arrayOf("PT", "pt"), arrayOf("LU", "fr"), arrayOf("OM", "ar"), arrayOf("AL", "sq"), arrayOf("DO", "es"), arrayOf("CU", "es"), arrayOf("NZ", "en"), arrayOf("RS", "sr"), arrayOf("CH", "de"), arrayOf("UY", "es"), arrayOf("GR", "el"), arrayOf("IL", "iw"), arrayOf("ZA", "en"), arrayOf("TH", "th"), arrayOf("FR", "fr"), arrayOf("AT", "de"), arrayOf("NO", "no"), arrayOf("AU", "en"), arrayOf("NL", "nl"), arrayOf("CA", "fr"), arrayOf("LV", "lv"), arrayOf("LU", "de"), arrayOf("CR", "es"), arrayOf("KW", "ar"), arrayOf("LY", "ar"), arrayOf("CH", "it"), arrayOf("DE", "de"), arrayOf("DZ", "ar"), arrayOf("SK", "sk"), arrayOf("LT", "lt"), arrayOf("IT", "it"), arrayOf("IE", "en"), arrayOf("SG", "zh"), arrayOf("CA", "en"), arrayOf("BE", "nl"), arrayOf("CN", "zh"), arrayOf("JP", "ja"), arrayOf("GR", "de"), arrayOf("RS", "sr"), arrayOf("IN", "en"), arrayOf("LB", "ar"), arrayOf("NI", "es"), arrayOf("MK", "mk"), arrayOf("BY", "be"), arrayOf("SI", "sl"), arrayOf("PE", "es"), arrayOf("ID", "in"), arrayOf("GB", "en")
        )
    }
}