package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.TagList

class ManageTags: ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null) {
            TagList()
        }
        setupToolbar(true)
        val action = intent.asAction
        setTitle(when(action) {
            Action.MANAGE -> R.string.tags
            Action.SELECT_FILTER -> R.string.search_tag
            else -> R.string.tags_create_or_select
        })
        setHelpVariant(when(action) {
            Action.MANAGE -> HELP_VARIANT_MANGE
            Action.SELECT_FILTER -> HELP_VARIANT_SELECT_FILTER
            else -> HELP_VARIANT_SELECT_MAPPING
        })
        if (action == Action.MANAGE) {
            floatingActionButton.visibility = View.GONE
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                doHome()
            }
        })
    }

    override val fabDescription = R.string.confirm
    override val fabIcon = R.drawable.ic_menu_done

    override val fabActionName = "TAG_CONFIRM"

    val tagList: TagList
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container) as TagList

    override fun onFabClicked() {
        super.onFabClicked()
        tagList.confirm()
    }

    override fun doHome() {
        setResult(FragmentActivity.RESULT_CANCELED, tagList.cancelIntent())
        finish()
    }
}