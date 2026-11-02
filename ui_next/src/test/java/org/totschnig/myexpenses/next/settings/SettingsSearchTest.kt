package org.totschnig.myexpenses.next.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.preference.PrefKey

class SettingsSearchTest {

    private fun entry(prefKey: PrefKey, group: SettingsGroup, section: SettingsSection? = null) =
        SettingEntry(SettingKey.Pref(prefKey), group, SettingKind.SWITCH, LegacyScreen.UI, section)

    private val theme = entry(PrefKey.UI_THEME, SettingsGroup.APPEARANCE)
    private val language = entry(PrefKey.UI_LANGUAGE, SettingsGroup.APPEARANCE)
    private val about = entry(PrefKey.MORE_INFO_DIALOG, SettingsGroup.ABOUT)
    private val format = entry(PrefKey.OPTIMIZE_PICTURE_FORMAT, SettingsGroup.ENTRY, SettingsSection.ENTRY_ATTACHMENTS)
    private val pngSize = entry(PrefKey.OPTIMIZE_PICTURE_MAX_SIZE, SettingsGroup.ENTRY, SettingsSection.ENTRY_ATTACHMENTS)

    private val titles = mapOf(
        theme to "Theme", language to "Sprache", about to "Weitere Informationen",
        format to "Format", pngSize to "Größe von PNG"
    )
    private val places = mapOf(
        theme to "Darstellung", language to "Darstellung", about to "Über",
        format to "Erfassen › Anhänge & Fotos", pngSize to "Erfassen › Anhänge & Fotos"
    )
    private val details = mapOf(
        theme to "Hell Dunkel", language to "Deutsch English", about to "", format to "JPEG PNG WEBP", pngSize to ""
    )

    private fun search(query: String) =
        searchSettings(query, listOf(theme, language, about, format, pngSize), titles::getValue, places::getValue, details::getValue)

    @Test
    fun findsByTitleIgnoringCase() {
        assertEquals(listOf(language), search("SPRA"))
    }

    @Test
    fun findsByGroupTitleInTheOrderOfTheCatalog() {
        assertEquals(listOf(theme, language), search("darstellung"))
    }

    @Test
    fun ignoresAccents() {
        assertEquals(listOf(about), search("uber"))
    }

    @Test
    fun needsEveryWord() {
        assertEquals(listOf(theme), search("theme darst"))
        assertEquals(emptyList<SettingEntry>(), search("theme info"))
    }

    @Test
    fun findsBySection() {
        assertEquals(listOf(format, pngSize), search("anhange"))
    }

    @Test
    fun findsByDetailsAfterThoseFoundByName() {
        assertEquals(listOf(language), search("english"))
        assertEquals(listOf(pngSize, format), search("png"))
    }

    @Test
    fun findsNothingForABlankQuery() {
        assertEquals(emptyList<SettingEntry>(), search("  "))
    }
}
