package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.databinding.RestoreFromCloudBinding
import org.totschnig.myexpenses.sync.json.AccountMetaData

class RestoreFromCloudDialogFragment : DialogViewBinding<RestoreFromCloudBinding>(),
    DialogInterface.OnClickListener,
    OnItemClickListener {
    private var backupAdapter: ArrayAdapter<String>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            RestoreFromCloudBinding.inflate(it)
        }
        binding.passwordLayout.passwordLayout.hint = getString(R.string.input_label_passphrase)
        binding.passwordLayout.passwordEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                configureSubmit()
            }
        })

        if (backups.isNotEmpty()) {
            backupAdapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_list_item_single_choice, backups
            )
            binding.backupList.adapter = backupAdapter
            binding.backupList.choiceMode = AbsListView.CHOICE_MODE_SINGLE
            binding.backupList.onItemClickListener = this
            binding.tabs.addTab(
                binding.tabs.newTab().setText(R.string.backups)
                    .setTag(
                        binding.backupListContainer
                    )
            )
        }
        if (syncAccounts.isNotEmpty()) {
            binding.syncAccountList.adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_list_item_multiple_choice, syncAccounts
            )
            binding.syncAccountList.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            binding.syncAccountList.onItemClickListener = this
            binding.tabs.addTab(
                binding.tabs.newTab()
                    .setText(R.string.onboarding_restore_from_cloud_sync_accounts).setTag(
                        binding.syncAccountListContainer
                    )
            )
        }
        binding.tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setTabVisibility(tab, View.VISIBLE)
                configureSubmit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                setTabVisibility(tab, View.GONE)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        setTabVisibility(binding.tabs.getTabAt(0)!!, View.VISIBLE)
        val dialog = builder.setTitle(R.string.onboarding_restore_from_cloud)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener(ButtonOnShowDisabler())
        return dialog
    }

    private fun setTabVisibility(tab: TabLayout.Tab, visibility: Int) {
        getContentForTab(tab).visibility = visibility
    }

    private fun getContentForTab(tab: TabLayout.Tab): LinearLayout {
        return tab.tag as LinearLayout
    }

    private fun findListView(parent: LinearLayout): ListView? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ListView) {
                return child
            }
        }
        return null
    }

    private fun configureSubmit() {
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        if (button != null) {
            button.isEnabled = isReady
        }
    }

    private val isReady: Boolean
        get() {
            val activeContent = activeContent
            val activeList = findListView(activeContent)
            if (activeContent.id == R.id.backup_list_container) {
                if (binding.passwordLayout.passwordLayout.visibility == View.VISIBLE && TextUtils.isEmpty(
                        binding.passwordLayout.passwordEdit.text.toString()
                    )
                ) {
                    return false
                }
            }
            return activeList!!.checkedItemCount > 0
        }
    private val activeContent: LinearLayout
        get() = getContentForTab(binding.tabs.getTabAt(binding.tabs.selectedTabPosition)!!)

    override fun onClick(dialog: DialogInterface, which: Int) {
        val backups = backups
        val syncAccounts = syncAccounts
        if (which == AlertDialog.BUTTON_POSITIVE) {
            val activity = requireActivity() as OnboardingActivity
            val contentForTab = activeContent
            val id = contentForTab.id
            if (id == R.id.backup_list_container) {
                val password =
                    if (binding.passwordLayout.passwordLayout.visibility == View.VISIBLE) binding.passwordLayout.passwordEdit.text.toString() else null
                activity.setupFromBackup(
                    backups[findListView(contentForTab)!!.checkedItemPosition],
                    password
                )
            } else if (id == R.id.sync_account_list_container) {
                activity.setupFromSyncAccounts(syncAccounts.filterIndexed { index, _ ->
                    findListView(
                        contentForTab
                    )!!.isItemChecked(index)
                })
            }
        }
    }

    private val syncAccounts: ArrayList<AccountMetaData>
        get() = BundleCompat.getParcelableArrayList(
            requireArguments(), KEY_SYNC_ACCOUNT_LIST,
            AccountMetaData::class.java
        )!!
    private val backups: ArrayList<String>
        get() = requireArguments().getStringArrayList(KEY_BACKUP_LIST)!!

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if ((parent.parent as LinearLayout).id == R.id.backup_list_container) {
            val item = backupAdapter!!.getItem(position)!!
            binding.passwordLayout.passwordLayout.visibility =
                if (item.endsWith("enc") || item.endsWith("bin")) View.VISIBLE else View.GONE
        }
        configureSubmit()
    }

    companion object {
        private const val KEY_BACKUP_LIST = "backupList"
        private const val KEY_SYNC_ACCOUNT_LIST = "syncAccountList"
        fun newInstance(
            backupList: List<String>,
            syncAccountList: List<AccountMetaData>,
        ): RestoreFromCloudDialogFragment {
            val arguments = Bundle(2)
            arguments.putStringArrayList(KEY_BACKUP_LIST, ArrayList(backupList))
            arguments.putParcelableArrayList(KEY_SYNC_ACCOUNT_LIST, ArrayList(syncAccountList))
            val fragment = RestoreFromCloudDialogFragment()
            fragment.arguments = arguments
            return fragment
        }
    }
}