package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OnboardingWizzardPrivacyBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_TAG_POSITIVE_BUNDLE
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.distribution
import org.totschnig.myexpenses.util.tracking.Tracker
import java.time.LocalTime
import javax.inject.Inject

class OnBoardingPrivacyFragment : OnboardingFragment() {
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
                    hostActivity.requireSqlCrypt()
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putString(
                            ConfirmationDialogFragment.KEY_MESSAGE,
                            getString(R.string.encrypt_database_info)
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                            R.id.ENCRYPT_CANCEL_COMMAND
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                            R.id.FAQ_COMMAND
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                            R.string.learn_more
                        )
                        putBundle(KEY_TAG_POSITIVE_BUNDLE, Bundle(1).apply {
                            putString(KEY_PATH, "data-encryption")
                        })

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

    @SuppressLint("SetTextI18n")
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
        val defaultAutoBackupTime =
            LocalTime.of(TimePreference.DEFAULT_VALUE / 100, TimePreference.DEFAULT_VALUE % 100)
        binding.autoBackup.text =
            getString(R.string.pref_auto_backup_summary) + " ($defaultAutoBackupTime)"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.autoBackup.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    hostActivity.requestNotificationPermission(PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_AUTO_BACKUP)
                }
            }
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
        prefHandler.putBoolean(PrefKey.AUTO_BACKUP, binding.autoBackup.isChecked)
        super.onNextButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}