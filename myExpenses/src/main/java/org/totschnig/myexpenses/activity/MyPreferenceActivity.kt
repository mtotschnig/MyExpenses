/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Pair
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.fragment.SettingsFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import java.io.Serializable
import java.util.*

/**
 * Present references screen defined in Layout file
 *
 * @author Michael Totschnig
 */
class MyPreferenceActivity : ProtectedFragmentActivity(), ContribIFace, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private var initialPrefToShow: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        setupToolbar(true)
        if (savedInstanceState == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, SettingsFragment(), FRAGMENT_TAG)
            ft.commit()
        }
        initialPrefToShow = if (savedInstanceState == null) intent.getStringExtra(KEY_OPEN_PREF_KEY) else null

        //when a user no longer has access to auto backup we do not want him to believe that it works
        if (!licenceHandler.hasTrialAccessTo(ContribFeature.AUTO_BACKUP)) {
            prefHandler.putBoolean(PrefKey.AUTO_BACKUP, false)
        }
    }

    private val fragment: SettingsFragment
        get() = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as SettingsFragment

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //currently no help menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            R.id.FTP_DIALOG -> return DialogUtils.sendWithFTPDialog(this)
            R.id.MORE_INFO_DIALOG -> {
                val builder = MaterialAlertDialogBuilder(this)
                val view = LayoutInflater.from(builder.context).inflate(R.layout.more_info, null)
                (view.findViewById<View>(R.id.aboutVersionCode) as TextView).text = getVersionInfo(this)
                val projectContainer = view.findViewById<TextView>(R.id.project_container)
                projectContainer.text = Utils.makeBulletList(this,
                        Utils.getProjectDependencies(this)
                                .map { project: Map<String, String> ->
                                    val name = project["name"]
                                    "${if (project.containsKey("extra_info")) "$name (${project["extra_info"]})" else name}, from ${project["url"]}, licenced under ${project["licence"]}"
                                }.toList(), R.drawable.ic_menu_forward)
                val additionalContainer = view.findViewById<TextView>(R.id.additional_container)
                val lines: List<CharSequence> = listOf(*resources.getStringArray(R.array.additional_credits),
                        "${getString(R.string.translated_by)}: ${buildTranslationCredits()}")
                additionalContainer.text = Utils.makeBulletList(this, lines, R.drawable.ic_menu_forward)
                val iconContainer = view.findViewById<LinearLayout>(R.id.additional_icons_container)
                val iconLines = listOf<CharSequence>(*resources.getStringArray(R.array.additional_icon_credits))
                val ar = resources.obtainTypedArray(R.array.additional_icon_credits_keys)
                val len = ar.length()
                val height = UiUtils.dp2Px(32f, resources)
                val drawablePadding = UiUtils.dp2Px(8f, resources)
                var i = 0
                while (i < len) {
                    val textView = TextView(this)
                    textView.gravity = Gravity.CENTER_VERTICAL
                    val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                    textView.layoutParams = layoutParams
                    textView.compoundDrawablePadding = drawablePadding
                    textView.text = iconLines[i]
                    UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(textView, ar.getResourceId(i, 0), 0, 0, 0)
                    iconContainer.addView(textView)
                    i++
                }
                ar.recycle()
                return builder.setTitle(R.string.pref_more_info_dialog_title)
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
            }
        }
        return super.onCreateDialog(id)
    }

    private fun buildTranslationCredits() = resources.getStringArray(R.array.pref_ui_language_values)
            .map { lang ->
                val parts = lang.split("-".toRegex()).toTypedArray()
                Pair.create(lang, getTranslatorsArrayResId(parts[0], if (parts.size == 2) parts[1].toLowerCase(Locale.ROOT) else null))
            }
            .filter { pair -> pair.second != 0 }
            .map { pair -> Pair.create(pair.first, resources.getStringArray(pair.second)) }
            .flatMap { pair -> pair.second.map { name -> Pair.create(name, pair.first) } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { entry -> "${entry.key} (${entry.value.joinToString(", ")})" }
            .joinToString(", ")

    fun getTranslatorsArrayResId(language: String, country: String?): Int {
        var result = 0
        val prefix = "translators_"
        if (!TextUtils.isEmpty(language)) {
            if (!TextUtils.isEmpty(country)) {
                result = resources.getIdentifier(prefix + language + "_" + country,
                        "array", packageName)
            }
            if (result == 0) {
                result = resources.getIdentifier(prefix + language,
                        "array", packageName)
            }
        }
        return result
    }

    fun validateLicence() {
        startValidationTask(TaskExecutionFragment.TASK_VALIDATE_LICENCE, R.string.progress_validating_licence)
    }

    private fun startValidationTask(taskId: Int, progressResId: Int) {
        startTaskExecution(taskId, arrayOf<String>(), null, 0)
        showSnackbar(progressResId, Snackbar.LENGTH_INDEFINITE)
    }

    override fun onFeatureAvailable(feature: Feature) {
        if (feature == Feature.WEBUI) {
            fragment.bindToWebUiService()
            activateWebUi()
        }
        if (feature == Feature.OCR) {
            fragment.configureTesseractLanguagePref()
        }
    }

    private fun activateWebUi() {
        fragment.activateWebUi()
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature === ContribFeature.CSV_IMPORT) {
            val i = Intent(this, CsvImportActivity::class.java)
            startActivity(i)
        }
        if (feature === ContribFeature.WEB_UI) {
            if (featureViewModel.isFeatureAvailable(this, Feature.WEBUI)) {
                activateWebUi()
            } else {
                featureViewModel.requestFeature(this, Feature.WEBUI)
            }
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {}
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR -> if (PermissionHelper.allGranted(grantResults)) {
                initialPrefToShow = prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID)
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (initialPrefToShow != null) {
            fragment.showPreference(initialPrefToShow)
            initialPrefToShow = null
        }
    }

    override fun onPreferenceStartScreen(preferenceFragmentCompat: PreferenceFragmentCompat,
                                         preferenceScreen: PreferenceScreen): Boolean {
        val key = preferenceScreen.key
        if (key == prefHandler.getKey(PrefKey.PERFORM_PROTECTION_SCREEN) &&
                (application as MyApplication).isProtected) {
            confirmCredentials(CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST, { startPerformProtectionScreen() }, false)
            return true
        }
        if (key == prefHandler.getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                //TODO on O we will be able to pin the shortcuts
                showSnackbar(R.string.home_screen_shortcuts_nougate_info)
                return true
            }
        }
        startPreferenceScreen(key)
        return true
    }

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        when (taskId) {
            TaskExecutionFragment.TASK_VALIDATE_LICENCE, TaskExecutionFragment.TASK_REMOVE_LICENCE -> {
                dismissSnackbar()
                if (o is Result<*>) {
                    showSnackbar(o.print(this))
                    fragment.setProtectionDependentsState()
                    fragment.configureContribPrefs()
                }
            }
            TaskExecutionFragment.TASK_RESET_EQUIVALENT_AMOUNTS -> {
                val r = o as Result<Int>
                if (r.isSuccess) {
                    showSnackbar(String.format(resources.configuration.locale, "%s (%d)", getString(R.string.reset_equivalent_amounts_success), r.extra))
                } else {
                    showSnackbar("Equivalent amount reset failed")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                startPerformProtectionScreen()
            }
        }
    }

    private fun startPerformProtectionScreen() {
        startPreferenceScreen(prefHandler.getKey(PrefKey.PERFORM_PROTECTION_SCREEN))
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        if (command == R.id.REMOVE_LICENCE_COMMAND) {
            startValidationTask(TaskExecutionFragment.TASK_REMOVE_LICENCE, R.string.progress_removing_licence)
            return true
        }
        if (command == R.id.CHANGE_COMMAND) {
            fragment.updateHomeCurrency(tag as String?)
            return true
        }
        return false
    }

    private fun startPreferenceScreen(key: String) {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key)
        fragment.arguments = args
        ft.replace(R.id.fragment_container, fragment, key)
        ft.addToBackStack(key)
        ft.commitAllowingStateLoss()
    }

    fun showUnencryptedBackupWarning() {
        if (prefHandler.getString(PrefKey.EXPORT_PASSWORD, null) == null) showMessage(unencryptedBackupWarning())
    }

    companion object {
        const val KEY_OPEN_PREF_KEY = "openPrefKey"
        const val FRAGMENT_TAG = "Settings"
    }
}