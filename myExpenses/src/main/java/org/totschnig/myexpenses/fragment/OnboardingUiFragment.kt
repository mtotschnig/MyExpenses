package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import butterknife.BindView
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.FontSizeAdapter
import org.totschnig.myexpenses.preference.FontSizeDialogPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.setNightMode
import javax.inject.Inject

class OnboardingUiFragment : OnboardingFragment() {
    @BindView(R.id.font_size_display_name)
    lateinit var fontSizeDisplayNameTextView: TextView

    @BindView(R.id.font_size)
    lateinit var fontSizeSeekBar: SeekBar

    @BindView(R.id.theme)
    lateinit var themeSwitch: View

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var prefHandler: PrefHandler

    private var fontScale = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApplication.getInstance().appComponent.inject(this)
    }

    override fun getLayoutResId(): Int {
        return R.layout.onboarding_wizzard_ui
    }

    override fun configureView(savedInstanceState: Bundle?) {
        //fontsize
        fontScale = prefHandler.getInt(PrefKey.UI_FONTSIZE, 0)
        fontSizeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFontSizeDisplayName(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onFontSizeSet()
            }
        })
        fontSizeSeekBar.progress = fontScale
        fontSizeSeekBar.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                onFontSizeSet()
            }
        }
        updateFontSizeDisplayName(fontScale)

        //theme
        val theme = prefHandler.getString(PrefKey.UI_THEME_KEY, getString(R.string.pref_ui_theme_default))
        (themeSwitch as? SwitchCompat)?.let {
            val isLight = ProtectedFragmentActivity.ThemeType.light.name == theme
            it.isChecked = isLight
            setContentDescriptionToThemeSwitch(it, isLight)
            it.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                prefHandler.putString(PrefKey.UI_THEME_KEY,
                        (if (isChecked) ProtectedFragmentActivity.ThemeType.light else ProtectedFragmentActivity.ThemeType.dark).name)
                setNightMode(prefHandler, requireContext())
            }
        }
        (themeSwitch as? Spinner)?.let {
            val spinnerHelper = SpinnerHelper(it)
            val themeValues = resources.getStringArray(R.array.pref_ui_theme_values)
            spinnerHelper.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                    prefHandler.putString(PrefKey.UI_THEME_KEY, themeValues[position])
                    setNightMode(prefHandler, requireContext())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
            spinnerHelper.setSelection((themeValues.indexOf(theme)))
        }
        nextButton.visibility = View.VISIBLE
    }

    override fun getTitle(): CharSequence {
        return Utils.getTextWithAppName(context, R.string.onboarding_ui_title)
    }

    private fun onFontSizeSet() {
        val newValue = fontSizeSeekBar.progress
        if (fontScale != newValue) {
            prefHandler.putInt(PrefKey.UI_FONTSIZE, newValue)
            recreate()
        }
    }

    private fun recreate() {
        val activity = activity
        activity?.recreate()
    }

    private fun updateFontSizeDisplayName(fontScale: Int) {
        val activity = activity
        if (activity != null) {
            fontSizeDisplayNameTextView.text = FontSizeDialogPreference.getEntry(activity, fontScale)
            FontSizeAdapter.updateTextView(fontSizeDisplayNameTextView, fontScale, activity)
        }
    }

    private fun setContentDescriptionToThemeSwitch(themeSwitch: View, isLight: Boolean) {
        themeSwitch.contentDescription = getString(
                if (isLight) R.string.pref_ui_theme_light else R.string.pref_ui_theme_dark)
    }

    companion object {
        @JvmStatic
        fun newInstance(): OnboardingUiFragment {
            return OnboardingUiFragment()
        }
    }
}