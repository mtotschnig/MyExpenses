package org.totschnig.myexpenses.fragment

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.FontSizeAdapter
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.CompactTransactionRenderer
import org.totschnig.myexpenses.compose.GroupDivider
import org.totschnig.myexpenses.compose.NewTransactionRenderer
import org.totschnig.myexpenses.compose.RenderType
import org.totschnig.myexpenses.databinding.OnboardingThemeSelectionBinding
import org.totschnig.myexpenses.databinding.OnboardingWizzardUiBinding
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.FontSizeDialogPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.asDateTimeFormatter
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.OnBoardingUiViewModel
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class OnboardingUiFragment : OnboardingFragment() {
    private var _binding: OnboardingWizzardUiBinding? = null
    private var _themeSelectionBinding: OnboardingThemeSelectionBinding? = null
    private val binding get() = _binding!!
    private val themeSelectionBinding get() = _themeSelectionBinding!!

    private val viewModel: OnBoardingUiViewModel by viewModels()

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    private var fontScale = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@OnboardingUiFragment)
            inject(viewModel)
        }
    }

    override val layoutResId = R.layout.onboarding_wizzard_ui

    override fun bindView(view: View) {
        _binding = OnboardingWizzardUiBinding.bind(view)
        _themeSelectionBinding = OnboardingThemeSelectionBinding.bind(view)
    }

    override fun configureView(savedInstanceState: Bundle?) {
        fontScale = prefHandler.getInt(PrefKey.UI_FONTSIZE, 0)
        binding.fontSize.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFontSizeDisplayName(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onFontSizeSet()
            }
        })
        binding.fontSize.progress = fontScale
        binding.fontSize.onFocusChangeListener =
            OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    onFontSizeSet()
                }
            }
        updateFontSizeDisplayName(fontScale)

        //theme
        val theme =
            prefHandler.getString(PrefKey.UI_THEME_KEY, getString(R.string.pref_ui_theme_default))

        themeSelectionBinding.themeSwitch?.let {
            val isLight = ProtectedFragmentActivity.ThemeType.light.name == theme
            it.isChecked = isLight
            setContentDescriptionToThemeSwitch(it, isLight)
            it.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                prefHandler.putString(
                    PrefKey.UI_THEME_KEY,
                    (if (isChecked) ProtectedFragmentActivity.ThemeType.light else ProtectedFragmentActivity.ThemeType.dark).name
                )
                setNightMode(prefHandler, requireContext())
            }
        }
        themeSelectionBinding.themeSpinner?.let {
            val spinnerHelper = SpinnerHelper(it)
            val themeValues = resources.getStringArray(R.array.pref_ui_theme_values)
            spinnerHelper.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    prefHandler.putString(PrefKey.UI_THEME_KEY, themeValues[position])
                    setNightMode(prefHandler, requireContext())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
            spinnerHelper.setSelection((themeValues.indexOf(theme)))
        }

        binding.designPreview.setContent {
            val demo = Transaction2(
                id = -1,
                _date = System.currentTimeMillis() / 1000,
                amount = Money(Utils.getHomeCurrency(), -7000),
                methodLabel = "CHEQUE",
                referenceNumber = "1",
                accountId = -1,
                catId = 1,
                label = stringResource(id = R.string.testData_transaction2Comment),
                comment = stringResource(id = R.string.testData_transaction4Payee),
                icon = "cart-shopping",
                year = 0,
                month = 0,
                day = 0,
                week = 0,
                tagList = listOf(stringResource(id = R.string.testData_tag_project)),
                pictureUri = Uri.EMPTY
            )
            AppTheme {
                Column {
                    val renderer = viewModel.renderer.collectAsState(initial = RenderType.New).value
                    val withCategoryIcon =
                        viewModel.withCategoryIcon.collectAsState(initial = true).value
                    RowCenter{
                        Checkbox(checked = renderer == RenderType.Legacy,
                            onCheckedChange = {
                                viewModel.setRenderer(it)
                            }
                        )
                        Text(stringResource(id = R.string.compact))
                        Checkbox(checked = withCategoryIcon,
                            onCheckedChange = {
                                viewModel.setWithCategoryIcon(it)
                            }
                        )
                        Text(stringResource(id = R.string.icons_for_categories))
                    }
                    GroupDivider()
                    (when (renderer) {
                        RenderType.Legacy -> {
                            CompactTransactionRenderer(
                                Pair(
                                    (Utils.ensureDateFormatWithShortYear(context) as SimpleDateFormat).asDateTimeFormatter,
                                    with(LocalDensity.current) {
                                        LocalTextStyle.current.fontSize.toDp()
                                    } * 4.6f
                                ),
                                withCategoryIcon
                            )
                        }
                        else -> {
                            NewTransactionRenderer(
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                                withCategoryIcon
                            )
                        }
                    }).Render(demo)
                    GroupDivider()
                }
            }
        }
        nextButton.visibility = View.VISIBLE
    }

    @Composable
    fun RowCenter(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }

    override val title: CharSequence
        get() = Utils.getTextWithAppName(context, R.string.onboarding_ui_title)

    private fun onFontSizeSet() {
        val newValue = binding.fontSize.progress
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
            binding.fontSizeDisplayName.text =
                FontSizeDialogPreference.getEntry(activity, fontScale)
            FontSizeAdapter.updateTextView(binding.fontSizeDisplayName, fontScale, activity)
        }
    }

    private fun setContentDescriptionToThemeSwitch(themeSwitch: View, isLight: Boolean) {
        themeSwitch.contentDescription = getString(
            if (isLight) R.string.pref_ui_theme_light else R.string.pref_ui_theme_dark
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _themeSelectionBinding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): OnboardingUiFragment {
            return OnboardingUiFragment()
        }
    }
}