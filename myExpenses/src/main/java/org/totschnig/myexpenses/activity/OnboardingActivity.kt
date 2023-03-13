package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OnboardingBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.RestoreFromCloudDialogFragment
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment
import org.totschnig.myexpenses.fragment.OnboardingDataFragment
import org.totschnig.myexpenses.fragment.OnboardingUiFragment
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.ui.FragmentPagerAdapter
import org.totschnig.myexpenses.util.distrib.DistributionHelper.versionNumber
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.RestoreViewModel.Companion.KEY_BACKUP_FROM_SYNC
import org.totschnig.myexpenses.viewmodel.RestoreViewModel.Companion.KEY_PASSWORD
import org.totschnig.myexpenses.viewmodel.RestoreViewModel.Companion.KEY_RESTORE_PLAN_STRATEGY
import org.totschnig.myexpenses.viewmodel.SyncViewModel.SyncAccountData

class OnboardingActivity : SyncBackendSetupActivity() {
    private lateinit var binding: OnboardingBinding
    private lateinit var pagerAdapter: MyPagerAdapter

    @JvmField
    @State
    var accountName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefHandler.setDefaultValues(this)
        super.onCreate(savedInstanceState)
        binding = OnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pagerAdapter = MyPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2
    }

    override fun onCreateOptionsMenu(menu: Menu) = false //skip help

    fun navigateNext() {
        val currentItem = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentItem + 1, true)
    }

    override fun onBackPressed() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem > 0) {
            binding.viewPager.currentItem = currentItem - 1
            return
        }
        super.onBackPressed()
    }

    private val dataFragment: OnboardingDataFragment?
        get() = getFragmentAtPosition(2) as? OnboardingDataFragment

    private val privacyFragment: OnBoardingPrivacyFragment?
        get() = getFragmentAtPosition(1) as? OnBoardingPrivacyFragment

    private fun getFragmentAtPosition(pos: Int) =
        supportFragmentManager.findFragmentByTag(pagerAdapter.getFragmentName(pos))

    fun start() {
        prefHandler.putInt(PrefKey.CURRENT_VERSION, versionNumber)
        prefHandler.putInt(PrefKey.FIRST_INSTALL_VERSION, versionNumber)
        val intent = Intent(this, MyExpenses::class.java)
        startActivity(intent)
        finish()
    }

    override val createAccountTaskShouldReturnBackups = true

    override val createAccountTaskShouldQueryLocalAccounts = false

    override fun onReceiveSyncAccountData(data: SyncAccountData) {
        lifecycleScope.launchWhenResumed {
            dataFragment?.setupMenu()
            accountName = data.accountName
            if (data.backups.isNotEmpty() || data.remoteAccounts.isNotEmpty()) {
                if (checkForDuplicateUuids(data.remoteAccounts)) {
                    showSnackBar("Found accounts with duplicate uuids")
                } else {
                    RestoreFromCloudDialogFragment.newInstance(data.backups, data.remoteAccounts)
                        .show(supportFragmentManager, "RESTORE_FROM_CLOUD")
                }
            } else {
                showSnackBar("Neither backups nor sync accounts found")
            }
        }
    }

    override fun onPostRestoreTask(result: Result<Unit>) {
        super.onPostRestoreTask(result)
        result.onSuccess {
            restartAfterRestore()
        }
    }

    fun setupFromBackup(backup: String?, restorePlanStrategy: Int, password: String?) {
        doRestore(Bundle(4).apply {
            putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName)
            putString(KEY_BACKUP_FROM_SYNC, backup)
            putInt(KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategy)
            putString(KEY_PASSWORD, password)
        })
    }

    fun setupFromSyncAccounts(syncAccounts: List<AccountMetaData>) {
        doWithEncryptionCheck {
            showSnackBarIndefinite(R.string.progress_dialog_fetching_data_from_sync_backend)
            viewModel.setupFromSyncAccounts(syncAccounts.map { it.uuid() }, accountName!!)
                .observe(this) { result ->
                    dismissSnackBar()
                    result.onSuccess {
                        start()
                    }.onFailure {
                        showDismissibleSnackBar(it.safeMessage)
                    }
                }
        }
    }

    private inner class MyPagerAdapter(fm: FragmentManager?) :
        FragmentPagerAdapter(fm) {
        fun getFragmentName(currentPosition: Int): String {
            return makeFragmentName(binding.viewPager.id, getItemId(currentPosition))
        }

        override fun getItem(pos: Int) = when (pos) {
            0 -> OnboardingUiFragment.newInstance()
            1 -> OnBoardingPrivacyFragment.newInstance()
            else -> OnboardingDataFragment.newInstance()
        }

        override fun getCount() = 3
    }

    @Suppress("UNUSED_PARAMETER")
    fun editAccountColor(view: View) {
        dataFragment?.editAccountColor()
    }

    override fun onNegative(args: Bundle) {
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE) == R.id.ENCRYPT_CANCEL_COMMAND) {
            prefHandler.putBoolean(PrefKey.ENCRYPT_DATABASE, false)
            privacyFragment?.setupMenu()
        }
    }

    override fun onNeutral(args: Bundle) {
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEUTRAL) == R.id.ENCRYPT_LEARN_MORE_COMMAND) {
            startActionView("https://github.com/mtotschnig/MyExpenses/wiki/FAQ:-Data#how-does-database-encryption-work")
        }
    }

    override val snackBarContainerId: Int
        get() {
            return binding.viewPager.id
        }
}