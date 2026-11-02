package org.totschnig.myexpenses.next.settings

import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import androidx.core.net.toUri
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.preferences.PreferenceUiFragment.Companion.compactItemRendererTitle
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.xmlpull.v1.XmlPullParser

/**
 * A setting as its old preference screen describes it
 *
 * @param title the title, or else the summary, unless the old screen sets the title in code, see
 * [LegacySpecs.titleOverrides]
 * @param summary below the title, if there is a title
 * @param summaryOn / [summaryOff] instead of [summary] for a switch that is on or off
 * @param options value to label of a list
 * @param defaultValue as text, e.g. "true" or "80"
 * @param dependency key of the switch that turns this setting on and off
 * @param intent started when the setting is tapped
 */
data class LegacySpec(
    val title: String?,
    val summary: String?,
    val summaryOn: String? = null,
    val summaryOff: String? = null,
    val options: List<Pair<String, String>>?,
    val defaultValue: String?,
    val dependency: String?,
    val min: Int?,
    val max: Int?,
    val intent: Intent? = null,
)

/**
 * The settings as the old preference screens describe them, read from their XML
 */
class LegacySpecs(private val context: Context, private val prefHandler: PrefHandler) {

    private val fromXml: Map<String, LegacySpec> by lazy {
        // The settings for sharing are included by two screens
        (LegacyScreen.entries.map { it.xml }.filter { it != 0 } + R.xml.preferences_include_share)
            .flatMap(::readSpecs).toMap()
    }

    fun key(entry: SettingEntry): String? = when (val key = entry.key) {
        is SettingKey.Pref -> prefHandler.getKey(key.prefKey)
        is SettingKey.Plain -> key.key
        is SettingKey.Next -> null
    }

    fun spec(entry: SettingEntry): LegacySpec? = key(entry)?.let(fromXml::get)

    /** Of the setting with [key], e.g. the switch another setting depends on */
    fun defaultOf(key: String): String? = fromXml[key]?.defaultValue

    fun title(entry: SettingEntry): String = when (val key = entry.key) {
        is SettingKey.Pref -> SettingTexts.titles[key.prefKey]?.let(context::getString)
            ?: titleOverrides[key.prefKey]?.invoke(context) ?: spec(entry)?.title
        is SettingKey.Plain -> spec(entry)?.title
        is SettingKey.Next -> context.getString(key.setting.title)
    } ?: entry.key.toString()

    private fun readSpecs(xml: Int): List<Pair<String, LegacySpec>> = buildList {
        context.resources.getXml(xml).use { parser ->
            // Depth of the last setting read, whose intent follows as its child
            var settingDepth = -1
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name == "intent") {
                    if (parser.depth == settingDepth + 1) {
                        set(lastIndex, last().let { (key, spec) -> key to spec.copy(intent = parser.intent()) })
                    }
                    continue
                }
                settingDepth = -1
                val key = parser.text("key") ?: continue
                settingDepth = parser.depth
                val title = parser.text("title")
                val summary = parser.text("summary")
                val entries = parser.array("entries")
                val values = parser.array("entryValues")
                add(
                    key to LegacySpec(
                        title = title ?: summary,
                        summary = summary.takeIf { title != null },
                        summaryOn = parser.text("summaryOn"),
                        summaryOff = parser.text("summaryOff"),
                        options = if (entries != null && values != null) values.zip(entries) else null,
                        defaultValue = parser.value("defaultValue"),
                        dependency = parser.text("dependency"),
                        min = parser.getAttributeValue(APP, "min")?.toIntOrNull(),
                        max = parser.getAttributeValue(ANDROID, "max")?.toIntOrNull()
                    )
                )
            }
        }
    }

    private fun XmlResourceParser.text(attribute: String): String? {
        val resId = getAttributeResourceValue(ANDROID, attribute, 0)
        return if (resId != 0) context.getString(resId) else getAttributeValue(ANDROID, attribute)
    }

    private fun XmlResourceParser.intent() = Intent(text("action")).apply {
        text("data")?.let { data = it.toUri() }
        val targetClass = getAttributeValue(ANDROID, "targetClass")
        val targetPackage = text("targetPackage")
        if (targetClass != null && targetPackage != null) setClassName(targetPackage, targetClass)
    }

    private fun XmlResourceParser.array(attribute: String): List<String>? =
        getAttributeResourceValue(ANDROID, attribute, 0).takeIf { it != 0 }
            ?.let { id -> context.resources.getTextArray(id).map { it.toString() } }

    private fun XmlResourceParser.value(attribute: String): String? {
        val resId = getAttributeResourceValue(ANDROID, attribute, 0)
        if (resId == 0) return getAttributeValue(ANDROID, attribute)
        val resources = context.resources
        return when (resources.getResourceTypeName(resId)) {
            "bool" -> resources.getBoolean(resId).toString()
            "integer" -> resources.getInteger(resId).toString()
            "string" -> resources.getString(resId)
            else -> null
        }
    }

    companion object {
        private const val ANDROID = "http://schemas.android.com/apk/res/android"
        private const val APP = "http://schemas.android.com/apk/res-auto"

        /** Titles the old screens set in code */
        val titleOverrides: Map<PrefKey, (Context) -> String> = mapOf(
            PrefKey.NEWS to { "${it.getString(R.string.pref_news_title)} (Mastodon)" },
            // Upstream sets its title only at runtime, after reading the database
            PrefKey.ENCRYPT_DATABASE_INFO to { it.getString(R.string.database_is_encrypted) },
            PrefKey.MANAGE_CURRENCIES to { CommodityType.title(it.resources) },
            PrefKey.IMPORT_QIF to { it.getString(R.string.pref_import_title, "QIF") },
            PrefKey.IMPORT_CSV to { it.getString(R.string.pref_import_title, "CSV") },
            PrefKey.CSV_EXPORT_ORIGINAL_EQUIVALENT_AMOUNTS to {
                it.getString(R.string.menu_original_amount) + " / " + it.getString(R.string.menu_equivalent_amount)
            },
            PrefKey.SYNC_NOW_ALL to {
                it.getString(R.string.pref_sync_now_all_summary, it.getString(R.string.menu_sync_now))
            },
            PrefKey.UI_ITEM_RENDERER_LEGACY to { it.compactItemRendererTitle() },
            PrefKey.OPTIMIZE_PICTURE_MAX_SIZE to {
                it.getString(R.string.maximum_size) + " (" + it.getString(R.string.pixels) + ")"
            }
        )
    }
}
