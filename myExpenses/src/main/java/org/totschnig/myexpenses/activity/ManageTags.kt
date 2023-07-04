package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.TagList

class ManageTags: ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        setupToolbar(true)
        val action = intent.asAction
        setTitle(when(action) {
            Action.MANAGE -> R.string.tags
            Action.SELECT_FILTER -> R.string.search_tag
            else -> R.string.tags_create_or_select
        })
        setHelpVariant(when(action) {
            Action.MANAGE -> HelpVariant.manage
            Action.SELECT_FILTER -> HelpVariant.select_filter
            else -> HelpVariant.select_mapping
        })
        if (action == Action.MANAGE) {
            floatingActionButton.visibility = View.GONE
        }
    }

    override val fabDescription = R.string.confirm
    override val fabIcon = R.drawable.ic_menu_done

    override val fabActionName = "TAG_CONFIRM"

    override fun onFabClicked() {
        super.onFabClicked()
        (supportFragmentManager.findFragmentById(R.id.tag_list) as TagList).confirm()
    }

    override fun doHome() {
        setResult(FragmentActivity.RESULT_CANCELED, (supportFragmentManager.findFragmentById(R.id.tag_list) as TagList).cancelIntent())
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        doHome()
    }
}