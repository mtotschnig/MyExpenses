package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ConfirmBackupDialogBinding
import org.totschnig.myexpenses.preference.PrefKey

class ConfirmBackupDialogFragment :
    DialogViewBinding<ConfirmBackupDialogBinding>(),
    DialogInterface.OnClickListener {

    interface ConfirmBackupDialogListener {
        fun onConfirmBackup(syncBackend: String?)
        fun onCancelBackup()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val isProtected = args.getBoolean(KEY_IS_PROTECTED, false)
        val message = args.getCharSequence(KEY_MESSAGE) ?: ""
        val accounts = args.getStringArray(KEY_ACCOUNTS) ?: emptyArray()

        val builder = initBuilder {
            ConfirmBackupDialogBinding.inflate(it)
        }

        if (isProtected) {
            builder.setTitle(
                "${getString(R.string.menu_backup)} (${getString(R.string.encrypted)})"
            )
            builder.setIcon(R.drawable.ic_lock)
        } else {
            builder.setTitle(R.string.menu_backup)
        }

        binding.message.text = message

        when {
            accounts.isEmpty() -> {
                binding.saveToCloudCheckbox.visibility = View.GONE
                binding.backendSpinnerLayout.visibility = View.GONE
            }

            accounts.size == 1 -> {
                binding.saveToCloudCheckbox.visibility = View.VISIBLE
                binding.saveToCloudCheckbox.text =
                    getString(R.string.backup_save_to_sync_backend, accounts[0])
                binding.saveToCloudCheckbox.isChecked =
                    prefHandler.getBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, false)
                binding.backendSpinnerLayout.visibility = View.GONE
            }

            else -> {
                binding.saveToCloudCheckbox.visibility = View.VISIBLE

                val adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, accounts)
                binding.backendSpinner.setAdapter(adapter)

                val lastCloud = prefHandler.getString(PrefKey.LAST_MANUAL_BACKUP_CLOUD, null)
                val initialSelection = if (lastCloud != null && accounts.contains(lastCloud)) {
                    lastCloud
                } else {
                    accounts[0]
                }
                binding.backendSpinner.setText(initialSelection, false)

                val isInitiallyChecked =
                    prefHandler.getBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, false)
                binding.saveToCloudCheckbox.isChecked = isInitiallyChecked
                binding.backendSpinnerLayout.visibility =
                    if (isInitiallyChecked) View.VISIBLE else View.GONE

                binding.saveToCloudCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    binding.backendSpinnerLayout.visibility =
                        if (isChecked) View.VISIBLE else View.GONE
                }
            }
        }

        builder.setPositiveButton(R.string.menu_backup, this)
        builder.setNegativeButton(android.R.string.cancel, this)

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val listener = activity as? ConfirmBackupDialogListener ?: return
        if (which == AlertDialog.BUTTON_POSITIVE) {
            val accounts = requireArguments().getStringArray(KEY_ACCOUNTS) ?: emptyArray()
            val syncBackend: String? = when {
                accounts.isEmpty() -> null

                accounts.size == 1 -> {
                    val checked = binding.saveToCloudCheckbox.isChecked
                    prefHandler.putBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, checked)
                    if (checked) accounts[0] else null
                }

                else -> {
                    val checked = binding.saveToCloudCheckbox.isChecked
                    prefHandler.putBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, checked)
                    if (checked) {
                        val chosen = binding.backendSpinner.text.toString()
                        if (chosen.isNotEmpty() && accounts.contains(chosen)) {
                            prefHandler.putString(PrefKey.LAST_MANUAL_BACKUP_CLOUD, chosen)
                            chosen
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            listener.onConfirmBackup(syncBackend)
        } else {
            listener.onCancelBackup()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        (activity as? ConfirmBackupDialogListener)?.onCancelBackup()
    }

    companion object {
        private const val KEY_MESSAGE = "message"
        private const val KEY_IS_PROTECTED = "isProtected"
        private const val KEY_ACCOUNTS = "accounts"

        fun newInstance(
            message: CharSequence,
            isProtected: Boolean,
            accounts: Array<String>
        ): ConfirmBackupDialogFragment = ConfirmBackupDialogFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(KEY_MESSAGE, message)
                putBoolean(KEY_IS_PROTECTED, isProtected)
                putStringArray(KEY_ACCOUNTS, accounts)
            }
        }
    }
}
