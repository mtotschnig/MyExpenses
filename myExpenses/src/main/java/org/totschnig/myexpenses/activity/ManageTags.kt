package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ActivityTagsBinding
import org.totschnig.myexpenses.fragment.TagList

class ManageTags: ProtectedFragmentActivity() {
    private lateinit var binding: ActivityTagsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagsBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

    override val _floatingActionButton: FloatingActionButton
        get() = binding.fab.CREATECOMMAND

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
}