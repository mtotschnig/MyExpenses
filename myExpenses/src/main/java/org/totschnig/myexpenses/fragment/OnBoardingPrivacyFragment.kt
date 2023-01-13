package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OnboardingWizzardPrivacyBinding
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import javax.inject.Inject

class OnBoardingPrivacyFragment: OnboardingFragment(), CompoundButton.OnCheckedChangeListener {
    private var _binding: OnboardingWizzardPrivacyBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var featureManager: FeatureManager

    override fun getLayoutResId() = R.layout.onboarding_wizzard_privacy
    override fun bindView(view: View) {
       _binding = OnboardingWizzardPrivacyBinding.bind(view)
    }

    override fun getTitle() = getString(R.string.onboarding_privacy_title)

    override fun getMenuResId() = R.menu.onboarding_privacy

    override fun setupMenu() {
        toolbar.menu.findItem(R.id.SqlEncrypt).isChecked = prefHandler.getBoolean(PrefKey.ENCRYPT_DATABASE, false)
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.SqlEncrypt) {
                val newValue = !it.isChecked
                prefHandler.putBoolean(PrefKey.ENCRYPT_DATABASE, newValue)
                it.isChecked = newValue
                if (featureManager.isFeatureInstalled(Feature.SQLCRYPT, requireContext())) {
                    featureManager.requestFeature(Feature.SQLCRYPT, requireContext())
                }
                true
            } else false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun configureView(savedInstanceState: Bundle?) {
        binding.crashReports.text = Utils.getTextWithAppName(context, R.string.crash_reports_user_info)
        binding.crashReports.setOnCheckedChangeListener(this)
        binding.tracking.setOnCheckedChangeListener(this)
        nextButton.visibility = View.VISIBLE
    }

    companion object {
        @JvmStatic
        fun newInstance() = OnBoardingPrivacyFragment()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when(buttonView.id) {
            R.id.crash_reports -> PrefKey.CRASHREPORT_ENABLED
            R.id.tracking -> PrefKey.TRACKING
            else -> null
        }?.let {
            prefHandler.putBoolean(it, isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}