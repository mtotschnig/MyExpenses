package org.totschnig.myexpenses.fragment

import android.content.Context
import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment: PreferenceFragmentCompat(), LocalizedFormatEditTextPreference.OnValidationErrorListener {
    @Inject
    lateinit var featureManager: FeatureManager
    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
    }

    override fun onValidationError(messageResId: Int) {
        activity().showSnackbar(messageResId)
    }

    fun activity() = activity as MyPreferenceActivity

    fun configureUninstallPrefs() {
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures()) { featureManager.uninstallFeatures(it) }
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_LANGUAGES, featureManager.installedLanguages()) { featureManager.uninstallLanguages(it) }
    }

    private fun configureMultiSelectListPref(prefKey: PrefKey, entries: Set<String>, action: (Set<String>) -> Unit ) {
        (requirePreference(prefKey) as? MultiSelectListPreference)?.apply {
            if (entries.isEmpty()) {
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { _, newValue ->
                    @Suppress("UNCHECKED_CAST")
                    (newValue as? Set<String>)?.let { action(it) }
                    false
                }
                entries.toTypedArray<CharSequence>().let {
                    setEntries(it)
                    entryValues = it
                }
            }
        }
    }

    fun <T: Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefHandler.getKey(prefKey)) ?: throw IllegalStateException("Preference not found")
    }

    fun getLocaleArray(context: Context) =
            context.resources.getStringArray(R.array.pref_ui_language_values)
                    .map { localeString -> getLocaleDisplayName(context, localeString) }
                    .toTypedArray()

    fun getTesseractLanguageArray(context: Context) =
            context.resources.getStringArray(R.array.pref_tesseract_language_values)
                    .map { localeString ->
                        val localeParts = localeString.split("_")
                        val lang = when(localeParts[0]) {
                            "kmr" -> "kur"
                            else -> localeParts[0]
                        }
                        if (localeParts.size == 2) {
                            val script = when(localeParts[1]) {
                                "sim" -> "Hans"
                                "tra" -> "Hant"
                                else -> localeParts[1]
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                Locale.Builder().setLanguage(lang).setScript(script).build().getDisplayName(Utils.localeFromContext(context))
                            } else {
                                "%s (%s)".format(Locale(lang).getDisplayName(Utils.localeFromContext(context)), script)
                            }
                        } else
                            Locale(lang).getDisplayName(Utils.localeFromContext(context))
                    }
                    .toTypedArray()

    private fun getLocaleDisplayName(context: Context, localeString: CharSequence) =
            if (localeString == "default") {
                context.getString(R.string.pref_ui_language_default)
            } else {
                val localeParts = localeString.split("-")
                val locale = if (localeParts.size == 2)
                    Locale(localeParts[0], localeParts[1])
                else
                    Locale(localeParts[0])
                locale.getDisplayName(locale)
            }
}