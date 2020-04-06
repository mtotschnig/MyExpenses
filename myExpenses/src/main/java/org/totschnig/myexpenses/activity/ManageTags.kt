package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.View
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.ACTION_MANAGE
import org.totschnig.myexpenses.fragment.TagList

class ManageTags: ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeIdEditDialog)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        setupToolbar(true)
        configureFloatingActionButton(R.string.content_description_tags_confirm)
        val shouldManage = intent?.action?.equals(ACTION_MANAGE) ?: false
        if (shouldManage) {
            floatingActionButton.visibility = View.GONE
            setTitle(R.string.tags)
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_menu_done)
            setTitle(R.string.tags_create_or_select)
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.CREATE_COMMAND) {
            setResult(RESULT_OK, (supportFragmentManager.findFragmentById(R.id.tag_list) as TagList).resultIntent())
            finish()
        }
        return super.dispatchCommand(command, tag)
    }
}