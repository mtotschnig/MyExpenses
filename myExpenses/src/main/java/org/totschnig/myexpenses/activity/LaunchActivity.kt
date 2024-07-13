package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import androidx.lifecycle.lifecycleScope
import com.vmadalin.easypermissions.EasyPermissions.somePermissionPermanentlyDenied
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.licence.LicenceHandler.Companion.log
import org.totschnig.myexpenses.util.licence.LicenceStatus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
abstract class LaunchActivity : IapActivity() {

    @Inject
    lateinit var plannerUtils: PlannerUtils

    override val shouldQueryIap: Boolean
        get() = true

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (isGithub) {
                if (licenceHandler.licenceStatus != null) {
                    val now = System.currentTimeMillis()
                    if (licenceHandler.licenceStatus === LicenceStatus.PROFESSIONAL) {
                        val licenceValidity = licenceHandler.validUntilMillis
                        if (licenceValidity != 0L) {
                            val daysToGo = TimeUnit.MILLISECONDS.toDays(licenceValidity - now)
                            if (daysToGo <= 7 && (now -
                                        prefHandler.getLong(
                                            PrefKey.PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN,
                                            0
                                        )
                                        > DateUtils.DAY_IN_MILLIS)
                            ) {
                                val message: String = if (daysToGo > 1) {
                                    getString(R.string.licence_expires_n_days, daysToGo)
                                } else if (daysToGo == 1L) {
                                    getString(R.string.licence_expires_tomorrow)
                                } else if (daysToGo == 0L) {
                                    getString(R.string.licence_expires_today)
                                } else if (daysToGo == -1L) {
                                    getString(R.string.licence_expired_yesterday)
                                } else {
                                    if (daysToGo < -7) { //grace period is over,
                                        lifecycleScope.launch {
                                            licenceHandler.handleExpiration()
                                        }
                                    }
                                    getString(R.string.licence_has_expired_n_days, -daysToGo)
                                }
                                prefHandler.putLong(
                                    PrefKey.PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN,
                                    now
                                )
                                ExtendProLicenceDialogFragment.newInstance(message)
                                    .show(
                                    supportFragmentManager, "UP_SELL"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        super.onPermissionsDenied(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR &&
            (PermissionHelper.PermissionGroup.CALENDAR.androidPermissions.any { perms.contains(it) }) &&
            somePermissionPermanentlyDenied(
                this,
                PermissionHelper.PermissionGroup.CALENDAR.androidPermissions
            )
        ) {
            plannerUtils.removePlanner(prefHandler)
        }
    }

    override fun onBillingSetupFinished() {
        if (!licenceHandler.hasAccessTo(ContribFeature.AD_FREE)) {
            checkGdprConsent(false)
        }
    }

    override fun onBillingSetupFailed(reason: String) {
        log().w("Billing setup failed (%s)", reason)
        checkGdprConsent(false)
    }

    override fun onLicenceStatusSet(success: Boolean, newStatus: String?) {
        if (success) {
            showSnackBar(getString(R.string.licence_validation_premium) + " (" + newStatus + ")")
        } else if (newStatus != null) {
          showSnackBar(newStatus)
        } else {
            showSnackBar(R.string.licence_validation_failure)
        }
    }
}