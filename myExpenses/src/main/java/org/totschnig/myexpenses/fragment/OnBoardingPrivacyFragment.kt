package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import butterknife.BindView
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import javax.inject.Inject

class OnBoardingPrivacyFragment: OnboardingFragment(), CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.crash_reports)
    lateinit var crashReports: SwitchCompat
    @BindView(R.id.tracking)
    lateinit var tracking: SwitchCompat

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun getLayoutResId() = R.layout.onboarding_wizzard_privacy;
    override fun getTitle() = getString(R.string.onboarding_privacy_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApplication.getInstance().appComponent.inject(this)
    }

    override fun configureView(view: View, savedInstanceState: Bundle?) {
        crashReports.setText(Utils.getTextWithAppName(context, R.string.crash_reports_user_info))
        crashReports.setOnCheckedChangeListener(this)
        tracking.setOnCheckedChangeListener(this)
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
}