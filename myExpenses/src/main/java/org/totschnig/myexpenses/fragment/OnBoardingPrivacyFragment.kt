package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OnboardingWizzardPrivacyBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.distribution
import org.totschnig.myexpenses.util.tracking.Tracker
import javax.inject.Inject

class OnBoardingPrivacyFragment: OnboardingFragment() {
    private var _binding: OnboardingWizzardPrivacyBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var tracker: Tracker

    override val layoutResId = R.layout.onboarding_wizzard_privacy
    override fun bindView(view: View) {
       _binding = OnboardingWizzardPrivacyBinding.bind(view)
    }

    override val title: CharSequence
        get() = getString(R.string.onboarding_privacy_title)

    override val menuResId = R.menu.onboarding_privacy

    override fun setupMenu() {
        toolbar.menu.findItem(R.id.SqlEncrypt).isChecked = prefHandler.encryptDatabase
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.SqlEncrypt) {
                val newValue = !it.isChecked
                prefHandler.putBoolean(PrefKey.ENCRYPT_DATABASE, newValue)
                it.isChecked = newValue
                if (newValue) {
                    requireSqlCrypt()
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putString(ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.encrypt_database_info)
                            )
                        putInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE, R.id.ENCRYPT_CANCEL_COMMAND)
                        putInt(ConfirmationDialogFragment.KEY_COMMAND_NEUTRAL, R.id.ENCRYPT_LEARN_MORE_COMMAND)
                        putInt(ConfirmationDialogFragment.KEY_NEUTRAL_BUTTON_LABEL, R.string.learn_more)
                    }).show(parentFragmentManager, "ENCRYPT")
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
        if (distribution.supportsTrackingAndCrashReporting) {
            binding.crashReports.text =
                Utils.getTextWithAppName(context, R.string.crash_reports_user_info)
        } else {
            binding.crashReports.isVisible = false
            binding.crashReportsLabel.isVisible = false
            binding.tracking.isVisible = false
            binding.trackingLabel.isVisible = false
        }
        nextButton.isVisible = true
    }

    companion object {
        @JvmStatic
        fun newInstance() = OnBoardingPrivacyFragment()
    }

    override fun onNextButtonClicked() {
        val trackingEnabled = binding.tracking.isChecked
        prefHandler.putBoolean(PrefKey.TRACKING, trackingEnabled)
        tracker.setEnabled(trackingEnabled)
        val crashReportEnabled = binding.crashReports.isChecked
        prefHandler.putBoolean(PrefKey.CRASHREPORT_ENABLED, crashReportEnabled)
        crashHandler.setEnabled(crashReportEnabled)
        super.onNextButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}