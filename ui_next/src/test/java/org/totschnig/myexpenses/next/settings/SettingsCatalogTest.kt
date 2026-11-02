package org.totschnig.myexpenses.next.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Checks the catalog against the preference screens of the main module */
class SettingsCatalogTest {

    private class XmlSetting(
        val file: String,
        val key: String,
        val widget: String,
        val hasTitle: Boolean,
        val hasOptions: Boolean,
    )

    private val xmlDir = File("../myExpenses/src/main/res/xml")

    private val settings: List<XmlSetting> = xmlDir.listFiles { file -> file.name.startsWith("preference") }!!
        .flatMap { file ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val elements = document.getElementsByTagName("*")
            (0 until elements.length).map { elements.item(it) as Element }
                .filter { element ->
                    element.hasAttribute(KEY) && !element.hasAttribute(FRAGMENT) &&
                            element.widget !in STRUCTURE
                }
                .map {
                    XmlSetting(
                        file.nameWithoutExtension, it.getAttribute(KEY), it.widget,
                        hasTitle = it.hasAttribute("android:title") || it.hasAttribute("android:summary"),
                        hasOptions = it.hasAttribute("android:entries") && it.hasAttribute("android:entryValues")
                    )
                }
        }

    private val Element.widget get() = tagName.substringAfterLast('.')

    private val stringNames = R.string::class.java.fields.associate { it.getInt(null) to it.name }
    private val xmlNames = R.xml::class.java.fields.associate { it.getInt(null) to it.name }

    /** The key as written in the preference screens, e.g. `@string/pref_ui_theme_key` */
    private val PrefKey.token: String
        get() {
            val resId = PrefKey::class.java.getDeclaredField("resId").apply { isAccessible = true }.getInt(this)
            return if (resId == 0) PrefKey::class.java.getDeclaredField("_key").apply { isAccessible = true }
                .get(this) as String
            else "@string/" + stringNames.getValue(resId)
        }

    private val SettingKey.token: String?
        get() = when (this) {
            is SettingKey.Pref -> prefKey.token
            is SettingKey.Plain -> key
            is SettingKey.Next -> null
        }

    private val LegacyScreen.files: Set<String>
        get() = if (xml == 0) emptySet() else setOfNotNull(
            xmlNames.getValue(xml),
            // included by both screens
            "preferences_include_share".takeIf { this == LegacyScreen.IO || this == LegacyScreen.BACKUP_RESTORE }
        )

    @Test
    fun everySettingOfTheOldScreensIsInTheCatalogOrOmitted() {
        assertTrue("found ${settings.size} settings", settings.size > 150)
        val known = SettingsCatalog.entries.mapNotNull { it.key.token } +
                SettingsCatalog.omitted.keys.map { it.token }
        val missing = settings.map { "${it.file}: ${it.key}" }.filter { it.substringAfter(": ") !in known }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun everySettingIsInTheCatalogOnce() {
        val twice = SettingsCatalog.entries.groupBy { it.key }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet<SettingKey>(), twice)
        val omittedButListed = SettingsCatalog.entries.map { it.key }
            .filter { it is SettingKey.Pref && it.prefKey in SettingsCatalog.omitted }
        assertEquals(emptyList<SettingKey>(), omittedButListed)
    }

    @Test
    fun entriesOpenTheOldScreenThatHoldsThem() {
        val wrong = SettingsCatalog.entries.filter { it.legacy != null }.filterNot { entry ->
            val legacy = entry.legacy!!
            entry.key == legacy.header ||
                    settings.any { it.key == entry.key.token && it.file in legacy.files }
        }
        assertEquals(emptyList<SettingEntry>(), wrong)
    }

    @Test
    fun kindsMatchTheWidgetsOfTheOldScreens() {
        val widgets = settings.groupBy({ it.key }, { it.widget })
        val wrong = SettingsCatalog.entries.filter { entry ->
            val token = entry.key.token ?: return@filter false
            widgets[token]?.any { entry.kind !in (KINDS[it] ?: error("unknown widget $it")) } ?: false
        }
        assertEquals(emptyList<SettingEntry>(), wrong)
    }

    @Test
    fun headersOfTheOldScreensExist() {
        val headers = Regex("""android:key="([^"]+)"""")
            .findAll(xmlDir.resolve("preference_headers.xml").readText()).map { it.groupValues[1] }.toSet()
        LegacyScreen.entries.forEach { screen ->
            screen.header?.token?.let { assertTrue("${screen.name}: $it", it in headers) }
        }
    }

    @Test
    fun everyEntryHasATitle() {
        val untitled = SettingsCatalog.entries.filter { entry ->
            val token = entry.key.token ?: return@filter false
            (entry.key as? SettingKey.Pref)?.prefKey !in LegacySpecs.titleOverrides &&
                    settings.none { it.key == token && it.hasTitle } &&
                    // a header of the list of all screens, e.g. premium
                    entry.key != entry.legacy?.header
        }
        assertEquals(emptyList<SettingEntry>(), untitled)
    }

    @Test
    fun nativeSettingsCanBeShownNatively() {
        val byKey = SettingsCatalog.entries.associateBy { (it.key as? SettingKey.Pref)?.prefKey }
        val wrong = NativeSettings.all.filter { (prefKey, native) ->
            val entry = byKey[prefKey] ?: return@filter true
            when (entry.kind) {
                SettingKind.CHOICE, SettingKind.MULTI_CHOICE -> native.options == null &&
                        settings.none { it.key == prefKey.token && it.hasOptions }
                SettingKind.SWITCH, SettingKind.TEXT, SettingKind.NUMBER, SettingKind.SLIDER,
                SettingKind.TIME, SettingKind.INFO, SettingKind.PASSWORD -> false
                else -> true
            } || native.storage == Storage.DATA_STORE && entry.kind !in setOf(SettingKind.SWITCH, SettingKind.CHOICE)
        }.keys
        assertEquals(emptySet<PrefKey>(), wrong)
    }

    @Test
    fun everyGroupHasEntries() {
        SettingsGroup.entries.forEach { assertTrue(it.name, SettingsCatalog.of(it).isNotEmpty()) }
    }

    @Test
    fun textsInEverydayWordsAreForEntriesOfTheCatalog() {
        val keys = SettingsCatalog.entries.mapNotNull { (it.key as? SettingKey.Pref)?.prefKey }.toSet()
        val explained = SettingTexts.titles.keys + SettingTexts.explanations.keys
        assertEquals(emptySet<PrefKey>(), explained - keys)
    }

    @Test
    fun groupsWithSectionsHaveOneForEveryEntry() {
        val withSections = SettingsSection.entries.map { it.group }.toSet()
        val wrong = SettingsCatalog.entries.filter { entry ->
            if (entry.group in withSections) entry.section?.group != entry.group else entry.section != null
        }.map { it.key }
        assertEquals(emptyList<SettingKey>(), wrong)
    }

    @Test
    fun everySectionHasEntriesInOneBlock() {
        SettingsSection.entries.forEach { assertTrue(it.name, SettingsCatalog.of(it).isNotEmpty()) }
        SettingsGroup.entries.forEach { group ->
            // in the order of the enum and each in one block, as the page shows them
            val blocks = SettingsCatalog.of(group).mapNotNull { it.section }
                .fold(emptyList<SettingsSection>()) { blocks, section -> if (blocks.lastOrNull() == section) blocks else blocks + section }
            assertEquals(group.name, blocks.sorted(), blocks)
            assertEquals(group.name, blocks.distinct(), blocks)
        }
    }

    companion object {
        private const val KEY = "android:key"
        private const val FRAGMENT = "android:fragment"

        /** Widgets that group or describe settings instead of holding one */
        private val STRUCTURE = setOf("PreferenceCategory", "PreferenceScreen", "HeaderPreference")

        private val KINDS: Map<String, Set<SettingKind>> = mapOf(
            "SwitchPreferenceCompat" to setOf(SettingKind.SWITCH),
            "ListPreference" to setOf(SettingKind.CHOICE),
            "MultiSelectListPreference" to setOf(SettingKind.MULTI_CHOICE),
            // Some are picked from options instead, e.g. kinds of files instead of MIME types
            "EditTextPreference" to setOf(SettingKind.TEXT, SettingKind.CHOICE, SettingKind.MULTI_CHOICE),
            "LongSummaryEditTextPreference" to setOf(SettingKind.TEXT),
            "EditNumberPreference" to setOf(SettingKind.NUMBER, SettingKind.CHOICE),
            "SeekBarPreference" to setOf(SettingKind.SLIDER, SettingKind.CHOICE),
            "FloatSeekBarPreference" to setOf(SettingKind.SLIDER),
            "TimePreference" to setOf(SettingKind.TIME),
            "SimplePasswordPreference" to setOf(SettingKind.PASSWORD),
            "LegacyPasswordPreference" to setOf(SettingKind.PASSWORD),
            "Preference" to setOf(SettingKind.ACTION, SettingKind.INFO),
            "SafePreference" to setOf(SettingKind.ACTION),
            "PopupMenuPreference" to setOf(SettingKind.ACTION),
            "AccountPreference" to setOf(SettingKind.ACTION),
            "FontSizeDialogPreference" to setOf(SettingKind.SLIDER),
            "NumberFormatPreference" to setOf(SettingKind.CHOICE),
            "DateFormatPreference" to setOf(SettingKind.CHOICE)
        )
    }
}
