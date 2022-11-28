package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentResultListener
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.fragment.ConfirmTagDialogFragment
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import timber.log.Timber

class TagHandler(val activity: BaseMyExpenses): FragmentResultListener {

    init {
        activity.supportFragmentManager.setFragmentResultListener(CONFIRM_MAP_TAG_REQUEST, activity, this)
    }

    private val getTags =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                ConfirmTagDialogFragment().also {
                    it.arguments = result.data!!.extras
                }.show(activity.supportFragmentManager, "CONFIRM")
            }
        }

    fun tag() {
        with(activity) {
            checkSealed(selectionState.map { it.id }) {
                getTags.launch(Intent(this, ManageTags::class.java).apply {
                    action = ACTION_SELECT_MAPPING
                })
            }
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        with(activity) {
            viewModel.tag(
                selectionState.map { it.id },
                result.getParcelableArrayList(KEY_TAG_LIST)!!,
                result.getBoolean(KEY_REPLACE, false)
            )
            finishActionMode()
        }
    }

    companion object {
        const val KEY_REPLACE = "replace"
        const val CONFIRM_MAP_TAG_REQUEST = "confirmMapTag"
    }
}
